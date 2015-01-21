package models

import akka.actor.{Props, Actor}
import play.api.db.DB
import play.api.libs.concurrent.Akka
import play.api.libs.json._
import play.api.libs.ws._
import play.api.Play
import play.api.Play.current
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
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
    val subject = "Distribution Sign Up Failure"
    val body = "Jun group ad network account was not created successfully for Email: " + distributorUser.email +
      ", WaterfallAdProviderID: " + waterfallAdProviderID + ", AppToken: " + appToken + ". Error: " + failureReason
    val emailActor = Akka.system.actorOf(Props(new JunGroupEmailActor(Play.current.configuration.getString("jungroup.email").get, subject, body)))
    emailActor ! "email"
  }

  /**
   * Sends success email on ad network creation
   * @param distributorUser The DistributorUser who will receive a success email when a new AdNetwork is created in Player.
   * @param appName The name of the newly created App.
   */
  def sendSuccessEmail(distributorUser: DistributorUser, appName: String) {
    val subject = "HyprMarketplace is ready for use in " + appName + "!"
    val body = "HyprMarketplace is now ready to use in the waterfall for " + appName + "."
    val emailActor = Akka.system.actorOf(Props(new JunGroupEmailActor(distributorUser.email, subject, body)))
    emailActor ! "email"
  }

  /**
   * Creates a JSON object of the adNetwork configuration
   * @param distributorUser An instance of the distributorUser with the information need to create an ad network account
   * @param appToken The unique identifier for an app.
   * @return A JsObject with ad network information.
   */
  def adNetworkConfiguration(distributorUser: DistributorUser, appToken: String): JsObject = {
    val hyprMarketplacePayoutUrl = Play.current.configuration.getString("jungroup.callbackurl").get.format(appToken)
    val adNetworkName = appToken + "." + distributorUser.email
    JsObject(
      Seq(
        "ad_network" -> JsObject(
          Seq(
            "name" -> JsString(adNetworkName),
            // Always set to true due to mediation being SDK only
            "mobile" -> JsBoolean(true)
          )
        ),
        "payout_url" -> JsObject(
          Seq(
            "url" -> JsString(hyprMarketplacePayoutUrl),
            "method" -> JsString("get"),
            "environment" -> JsString("production"),
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
 * @param appToken The unique identifier of the App to which the Waterfall and WaterfallAdProvider belong.
 * @param api Instance of the JunGroupAPI class.
 * @param appName The name of the newly created App.
 */
class JunGroupAPIActor(waterfallID: Long, hyprWaterfallAdProvider: WaterfallAdProvider, appToken: String, appName: String, api: JunGroupAPI) extends Actor {
  private var counter = 0
  private val RETRY_COUNT = 3
  private val RETRY_FREQUENCY = 3.seconds
  var lastFailure = ""

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
      counter += 1
      if(counter > RETRY_COUNT){
        api.sendFailureEmail(distributorUser, hyprWaterfallAdProvider.id, appToken, lastFailure)
        context.stop(self)
      } else {
        val adNetwork = api.adNetworkConfiguration(distributorUser, appToken)
        api.createRequest(adNetwork) map {
          response => {
            if(response.status != 500) {
              try {
                Json.parse(response.body) match {
                  case _:JsUndefined => {
                    retry(distributorUser)
                  }
                  case results if(response.status == 200 || response.status == 304) => {
                    val success: JsValue = results \ "success"
                    val adNetworkID: Long = (results \ "ad_network" \ "ad_network" \ "id").as[Long]
                    if(success.as[JsBoolean] != JsBoolean(false)) {
                      DB.withTransaction { implicit connection =>
                        try {
                          val updateResult = WaterfallAdProvider.updateHyprMarketplaceConfig(hyprWaterfallAdProvider, adNetworkID)
                          AppConfig.createWithWaterfallIDInTransaction(waterfallID, None)
                          updateResult match {
                            case 1 => {
                              api.sendSuccessEmail(distributorUser, appName)
                            }
                            case _ => None
                          }
                        } catch {
                          case error: org.postgresql.util.PSQLException => {
                            connection.rollback()
                          }
                        }
                      }
                    } else {
                      val error: JsValue = results \ "error"
                      lastFailure = error.as[String]
                      retry(distributorUser)
                    }
                  }
                  case _ => retry(distributorUser)
                }
              } catch {
                case parsingError: com.fasterxml.jackson.core.JsonParseException => {
                  lastFailure = response.body
                  retry(distributorUser)
                }
              }
            } else {
              retry(distributorUser)
            }
          }
        }
      }
    }
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
