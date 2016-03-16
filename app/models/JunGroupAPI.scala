package models

import akka.actor.{Props, Actor}
import play.api.db.DB
import play.api.libs.concurrent.Akka
import play.api.libs.json._
import play.api.libs.ws._
import play.api.{Logger, Play}
import play.api.Play.current
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{TimeoutException, Future}
import scala.language.postfixOps

/**
 * Encapsulates DistributorUser information for creating a new AdNetwork in Player.
 * @param distributorUser The DistributorUser who owns the new AdNetwork.
 */
case class CreateAdNetwork(distributorUser: DistributorUser)

/**
 * Encapsulates interactions with Player.
 */
class JunGroupAPI {
  val PlayerStagingURL: String = "staging.hyprmx.com"
  val PlayerProdURL: String = "live.hyprmx.com"
  val PlayerSignature = {
    val params = List("UID", "PARTNER_CODE", "EPOCH_SECONDS", Play.current.configuration.getString("jungroup.token").get)
    JsArray(Seq(params.map(param => JsString(param)): _*))
  }

  /**
   * Creates the request using the config passed.
   * @param adNetwork JSON containing the appropriate information to create a new AdNetwork in Player.
   * @return Future[WSResponse]
   */
  def createRequest(adNetwork: JsObject): Future[WSResponse] = {
    val config = Play.current.configuration
    WS.url("http://" + config.getString("jungroup.url").get + "/admin/ad_networks").withAuth(config.getString("jungroup.user").get, config.getString("jungroup.password").get, WSAuthScheme.DIGEST).post(adNetwork)
  }

  /**
   * Sends failure email to account specified in application config.
   * @param distributorUser The DistributorUser who created the App/Waterfall.
   * @param waterfallAdProviderID The ID of the HyprMarketplace WaterfallAdProvider instance.
   * @param appToken The unique identifier for the App to which the WaterfallAdProvider belongs.
   * @param failureReason The error from Player.
   */
  def sendFailureEmail(distributorUser: DistributorUser, waterfallAdProviderID: Long, appToken: String, failureReason: String) {
    val subject = "Player Ad Network Creation Failure"
    val body = "Jun Group ad network account was not created successfully. <br> <b>Email:</b> " + distributorUser.email +
      " <br> <b>WaterfallAdProviderID:</b> " + waterfallAdProviderID +
      " <br> <b>AppToken:</b> " + appToken +
      " <br> <b>Environment:</b> " + Environment.mode +
      " <br> <b>Domain:</b> " + Play.current.configuration.getString("app_domain").getOrElse("") +
      " <br><br> <b>Error:</b> " + failureReason +
      " <br><br> For More information visit: <a href='https://wiki.jungroup.com/display/MED/Create+Ad+Network+for+HyprMarketplace+on+Player+API+Failure'>Ad Network Documentation</a>"
    val emailActor = Akka.system.actorOf(Props(new JunGroupEmailActor(Play.current.configuration.getString("jungroup.email").get, subject, body)))
    emailActor ! "email"
  }

  /**
   * Creates a JSON object of the adNetwork configuration
   * @param companyName The name of the Distributor to which the App belongs.
   * @param app The newly created App.
   * @return A JsObject with ad network information.
   */
  def adNetworkConfiguration(companyName: String, app: App): JsObject = {
    val hyprMarketplacePayoutUrl = Play.current.configuration.getString("jungroup.callbackurl").get.format(app.token)
    val platformName = Platform.find(app.platformID).PlatformName
    val adNetworkName = companyName + " - " + app.name + " - " + platformName
    val createdInContext = Play.current.configuration.getString("app_domain").getOrElse("") + " - " + Environment.mode
    val payoutURLEnvironment: String = {
      val playerURL: String = Play.current.configuration.getString("jungroup.url").getOrElse("")
      if(playerURL == PlayerProdURL) {
        "production"
      } else if(playerURL == PlayerStagingURL) {
        "staging"
      } else {
        Environment.mode
      }
    }
    JsObject(
      Seq(
        "ad_network" -> JsObject(
          Seq(
            "name" -> JsString(adNetworkName),
            // Always set to true due to mediation being SDK only
            "mobile" -> JsBoolean(true),
            "mediation_reporting_api_key" -> JsString(app.token),
            "mediation_reporting_placement_id" -> JsString(app.token),
            "created_in_context" -> JsString(createdInContext),
            "is_test" -> JsBoolean(!Environment.isProd),
            "demographic_targeting_enabled" -> JsBoolean(true)
          )
        ),
        "payout_url" -> JsObject(
          Seq(
            "url" -> JsString(hyprMarketplacePayoutUrl),
            "method" -> JsString("get"),
            "environment" -> JsString(payoutURLEnvironment),
            "signature" -> PlayerSignature
          )
        )
      )
    )
  }
}

/**
 * Actor that creates requests and retries on failure up to RETRY_COUNT times every RETRY_FREQUENCY
 * @param waterfallID The ID of the Waterfall to which the WaterfallAdProvider belongs.
 * @param hyprWaterfallAdProvider The HyprMarketplace WaterfallAdProvider instance that was just created.
 * @param app The newly created App.
 * @param distributorID The ID of the Distributor to which the App belongs.
 * @param api Instance of the JunGroupAPI class.
 */
class JunGroupAPIActor(waterfallID: Long, hyprWaterfallAdProvider: WaterfallAdProvider, app: App, distributorID: Long, api: JunGroupAPI) extends Actor {
  private var counter = 0
  private val RETRY_COUNT = 3
  private val RETRY_FREQUENCY = 60.seconds
  var lastFailure = ""
  val companyName = Distributor.find(distributorID).get.name

  /**
   * Retries the API call to Player.
   * @param distributorUser The DistributorUser who owns the new WaterfallAdProvider.
   * @return A scheduled retry of the JunGroupAPIActor.
   */
  def retry(distributorUser: DistributorUser) = {
    context.system.scheduler.scheduleOnce(RETRY_FREQUENCY, self, CreateAdNetwork(distributorUser))
  }

  def receive = {
    case CreateAdNetwork(distributorUser: DistributorUser) => {
      if(counter > RETRY_COUNT) {
        val emailError = lastFailure
        api.sendFailureEmail(distributorUser, hyprWaterfallAdProvider.id, app.token, emailError)
        context.stop(self)
      } else {
        val adNetwork = api.adNetworkConfiguration(companyName, app)
        api.createRequest(adNetwork) map {
          response => {
            if(response.status != 500) {
              try {
                Json.parse(response.body) match {
                  case error: JsUndefined => {
                    lastFailure = assembleAndLogError("Received a JsUndefined error while parsing Player's response", Some(response.body))
                    retry(distributorUser)
                  }
                  case results if(response.status == 200 || response.status == 304) => {
                    val success: JsValue = results \ "success"
                    val adNetworkID: Long = (results \ "ad_network" \ "ad_network" \ "id").as[Long]
                    if(success.as[JsBoolean] != JsBoolean(false)) {
                      DB.withTransaction { implicit connection =>
                        try {
                          WaterfallAdProvider.updateHyprMarketplaceConfig(hyprWaterfallAdProvider, adNetworkID, app.token, app.name)
                          AppConfig.createWithWaterfallIDInTransaction(waterfallID, None)
                        } catch {
                          case error: org.postgresql.util.PSQLException => {
                            assembleAndLogError("Encountered a Postgres Exception while updating the HyprMarketplace WaterfallAdProvider", Some(response.body))
                            connection.rollback()
                          }
                        }
                      }
                    } else {
                      val error: JsValue = results \ "error"
                      lastFailure = assembleAndLogError("There was an error while creating the ad network in Player: " + error.as[String], Some(response.body))
                      retry(distributorUser)
                    }
                  }
                  case error => {
                    lastFailure = assembleAndLogError("Received a " + response.status + " status code from Player", Some(response.body))
                    retry(distributorUser)
                  }
                }
              } catch {
                case _: com.fasterxml.jackson.core.JsonParseException | _: play.api.libs.json.JsResultException => {
                  lastFailure = assembleAndLogError("Received a JSON parsing error", Some(response.body))
                  retry(distributorUser)
                }
              }
            } else {
              lastFailure = assembleAndLogError("Received a 500 response from Player", Some(response.body))
              retry(distributorUser)
            }
          }
        } recover {
          case _: TimeoutException => {
            lastFailure = assembleAndLogError("Request to Player timed out")
            retry(distributorUser)
          }
          case error => {
            lastFailure = assembleAndLogError("Recovered from Player response error", Some(error.getMessage))
            retry(distributorUser)
          }
        }
      }
      counter += 1
    }
  }

  /**
   * Appends identifiable app information to an error message and logs it to the console
   * @param errorMessage The message to be logged
   * @param responseBody The body of Player's response
   */
  def assembleAndLogError(errorMessage: String, responseBody: Option[String] = None): String = {
    val error =  errorMessage + " Response Body: " + responseBody.getOrElse("None")
    Logger.error("JunGroupAPI Error for API Token: " + app.token + "\nWaterfallAdProvider ID: " + hyprWaterfallAdProvider.id + "\n" + error)
    error
  }
}

/**
 * Sends email on failure.  Called by sendFailureEmail
 * @param toAddress The address where the email will be sent.
 * @param subject The subject of the email.
 * @param body The body of the email.
 */
class JunGroupEmailActor(toAddress: String, subject: String, body: String) extends Actor with Mailer {
  def receive = {
    case email: String => {
      sendJunGroupEmail(email)
    }
  }

  /**
   * Sends email to new DistributorUser.  This is called on a successful sign up.
   * @param description Description of failure
   */
  def sendJunGroupEmail(description: String): Unit = {
    sendEmail(recipient = toAddress, sender = PublishingEmail, subject = subject, body = body)
  }
}
