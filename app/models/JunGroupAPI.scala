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

  /**
   * Creates the request using the config passed.
   * @param adNetwork JSON containing the appropriate information to create a new AdNetwork in Player.
   * @return Future[WSResponse]
   */
  def createRequest(adNetwork: JsObject): Future[WSResponse] = {
    val config = Play.current.configuration
    WS.url("http://" + config.getString("jungroup.url").get + "/admin/ad_network").withAuth(config.getString("jungroup.user").get, config.getString("jungroup.password").get, WSAuthScheme.DIGEST).post(adNetwork)
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
    val body = "Jun Group ad network account was not created successfully. <br> Email: " + distributorUser.email +
      " <br> WaterfallAdProviderID: " + waterfallAdProviderID + " <br> AppToken: " + appToken + " <br> Error: " + failureReason +
      " <br> For More information visit: https://wiki.jungroup.com/display/MED/Create+Ad+Network+for+HyprMarketplace+on+Player+API+Failure"
    val emailActor = Akka.system.actorOf(Props(new JunGroupEmailActor(Play.current.configuration.getString("jungroup.email").get, subject, body)))
    emailActor ! "email"
  }

  /**
   * Creates a JSON object of the adNetwork configuration
   * @param companyName The name of the Distributor to which the App belongs.
   * @param appName The name of the App.
   * @param appToken The unique identifier for the App to which the WaterfallAdProvider belongs.
   * @return A JsObject with ad network information.
   */
  def adNetworkConfiguration(companyName: String, appName: String, appToken: String): JsObject = {
    val hyprMarketplacePayoutUrl = Play.current.configuration.getString("jungroup.callbackurl").get.format(appToken)
    val adNetworkName = companyName + " - " + appName
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
            "mediation_reporting_api_key" -> JsString(appToken),
            "mediation_reporting_placement_id" -> JsString(appToken),
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
            "signature" -> JsArray(
              Seq(
                JsString("UID"),
                JsString("SID"),
                JsString("EPOCH_SECONDS"),
                JsString(Play.current.configuration.getString("jungroup.token").get)
              )
            )
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
 * @param appToken The unique identifier for the App to which the WaterfallAdProvider belongs.
 * @param appName The name of the newly created App.
 * @param distributorID The ID of the Distributor to which the App belongs.
 * @param api Instance of the JunGroupAPI class.
 */
class JunGroupAPIActor(waterfallID: Long, hyprWaterfallAdProvider: WaterfallAdProvider, appToken: String, appName: String, distributorID: Long, api: JunGroupAPI) extends Actor {
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
        api.sendFailureEmail(distributorUser, hyprWaterfallAdProvider.id, appToken, emailError)
        context.stop(self)
      } else {
        val adNetwork = api.adNetworkConfiguration(companyName, appName, appToken)
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
                          WaterfallAdProvider.updateHyprMarketplaceConfig(hyprWaterfallAdProvider, adNetworkID, appToken, appName)
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
                case parsingError: com.fasterxml.jackson.core.JsonParseException => {
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
    Logger.error("JunGroupAPI Error for API Token: " + appToken + "\nWaterfallAdProvider ID: " + hyprWaterfallAdProvider.id + "\n" + error)
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
    sendEmail(toAddress, subject, body)
  }
}
