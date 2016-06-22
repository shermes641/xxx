package models

import akka.actor.{Actor, Props}
import play.api.Logger
import play.api.Play.current
import play.api.db.DB
import play.api.libs.concurrent.Akka
import play.api.libs.json._
import play.api.libs.ws._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import akka.actor.{Actor, ActorSystem, Props}
import javax.inject._
import play.api.db.Database
import play.api.libs.json._
import play.api.libs.ws._
import play.api.{Configuration, Logger, Play}
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, TimeoutException}
import scala.language.postfixOps

/**
 * Encapsulates DistributorUser information for creating a new AdNetwork in Player
 * @param distributorUser         The DistributorUser who owns the new AdNetwork
 * @param waterfallID             The ID of the newly created waterfall
 * @param hyprWaterfallAdProvider The newly created WaterfallAdProvider for HyprMarketplace
 * @param app                     The newly created app
 * @param api                     An instance of the JunGroupAPI class
 */
case class CreateAdNetwork(distributorUser: DistributorUser,
                           waterfallID: Long,
                           hyprWaterfallAdProvider: WaterfallAdProvider,
                           app: App,
                           api: JunGroupAPI)

/**
 * Encapsulates interactions with Player.
 */
/**
  * Encapsulates interactions with Player.
  * @param models            Helper class to encapsulate model service classes
  * @param db                A shared database
  * @param wsClient          A shared web service client
  * @param actorSystem       A shared Akka actor system
  * @param configVars        Shared ENV config variables
  * @param appEnvironment    Environment in which the app is running
  */
@Singleton
class JunGroupAPI(models: ModelService,
                  db: Database,
                  wsClient: WSClient,
                  actorSystem: ActorSystem,
                  configVars: ConfigVars,
                  appEnvironment: Environment) {
  val distributorService = models.distributorService
  val platform = models.platform

  val PlayerStagingURL: String = "staging.hyprmx.com"
  val PlayerProdURL: String = "live.hyprmx.com"
  val PlayerSignature = {
    val params = List("UID", "PARTNER_CODE", "EPOCH_SECONDS", configVars.ConfigVarsJunGroup.token)
    JsArray(Seq(params.map(param => JsString(param)): _*))
  }

  /**
    * Creates the request using the config passed.
    *
    * @param adNetwork JSON containing the appropriate information to create a new AdNetwork in Player.
    * @return Future[WSResponse]
    */
  def createRequest(adNetwork: JsObject): Future[WSResponse] = {
    wsClient.url("http://" + configVars.ConfigVarsJunGroup.url + "/admin/ad_networks")
      .withAuth(configVars.ConfigVarsJunGroup.user, configVars.ConfigVarsJunGroup.pw, WSAuthScheme.DIGEST)
      .post(adNetwork)
  }

  /**
   * Sends failure email to account specified in application config.
   * @param distributorUser The DistributorUser who created the App/Waterfall.
   * @param waterfallAdProviderID The ID of the HyprMarketplace WaterfallAdProvider instance.
   * @param appToken The unique identifier for the App to which the WaterfallAdProvider belongs.
   * @param failureReason The error from Player.
   */
  def sendFailureEmail(distributorUser: DistributorUser, waterfallAdProviderID: Long, appToken: String, failureReason: String, mailer: Mailer) {
    val subject = "Player Ad Network Creation Failure"
    val body = "Jun Group ad network account was not created successfully. <br> <b>Email:</b> " + distributorUser.email +
      " <br> <b>WaterfallAdProviderID:</b> " + waterfallAdProviderID +
      " <br> <b>AppToken:</b> " + appToken +
      " <br> <b>Environment:</b> " + appEnvironment.mode +
      " <br> <b>Domain:</b> " + configVars.ConfigVarsApp.domain +
      " <br><br> <b>Error:</b> " + failureReason +
      " <br><br> For More information visit: <a href='https://wiki.jungroup.com/display/MED/Create+Ad+Network+for+HyprMarketplace+on+Player+API+Failure'>Ad Network Documentation</a>"
    val emailActor = actorSystem.actorOf(Props(new JunGroupEmailActor(configVars.ConfigVarsJunGroup.email, subject, body, mailer, configVars)))
    emailActor ! "email"
  }

  /**
    * Creates a JSON object of the adNetwork configuration
    *
    * @param companyName The name of the Distributor to which the App belongs.
    * @param app         The newly created App.
    * @return A JsObject with ad network information.
    */
  def adNetworkConfiguration(companyName: String, app: App): JsObject = {
    val hyprMarketplacePayoutUrl = configVars.ConfigVarsCallbackUrls.player.format(app.token)
    val platformName = platform.find(app.platformID).PlatformName
    val adNetworkName = companyName + " - " + app.name + " - " + platformName
    val createdInContext = configVars.ConfigVarsApp.domain + " - " + appEnvironment.mode
    val payoutURLEnvironment: String = {
      val playerURL: String = configVars.ConfigVarsJunGroup.url
      if(playerURL == PlayerProdURL) {
        "production"
      } else if (playerURL == PlayerStagingURL) {
        "staging"
      } else {
        appEnvironment.mode
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
            "is_test" -> JsBoolean(!appEnvironment.isProd),
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
 * @param models A helper class to encapsulate model service classes
 * @param db     A shared database
 * @param mailer A shared instance of the Mailer class
 */
class JunGroupAPIActor(models: ModelService, db: Database, mailer: Mailer) extends Actor {
  val distributorService = models.distributorService
  val waterfallAdProviderService = models.waterfallAdProviderService
  val appConfigService = models.appConfigServiceService

  private var counter = 0
  private val RETRY_COUNT = 3
  private val RETRY_FREQUENCY = 60.seconds
  var lastFailure = ""

  /**
   * Retries the API call to Player.
   * @param createAdNetwork A class encapsulating all necessary information for creating an ad network
   * @return                A scheduled retry of the JunGroupAPIActor.
   */
  def retry(createAdNetwork: CreateAdNetwork) = {
    context.system.scheduler.scheduleOnce(RETRY_FREQUENCY, self, createAdNetwork)
  }

  def receive = {
    case createAdNetwork: CreateAdNetwork => {
      if(counter > RETRY_COUNT) {
        val emailError = lastFailure
        createAdNetwork.api.sendFailureEmail(createAdNetwork.distributorUser, createAdNetwork.hyprWaterfallAdProvider.id, createAdNetwork.app.token, emailError, mailer)
        context.stop(self)
      } else {
        val companyName = distributorService.find(createAdNetwork.distributorUser.distributorID.get).get.name
        val adNetwork = createAdNetwork.api.adNetworkConfiguration(companyName, createAdNetwork.app)
        createAdNetwork.api.createRequest(adNetwork) map {
          response => {
            if (response.status != 500) {
              try {
                Json.parse(response.body) match {
                  case results if response.status == 200 || response.status == 304 => {
                    (results \ "success").toOption match {
                      case Some(success: JsBoolean) if success.as[Boolean] =>
                        (results \ "ad_network" \ "id").toOption match {
                          case Some(adNetworkID: JsNumber) =>
                            db.withTransaction { implicit connection =>
                              try {
                                waterfallAdProviderService.updateHyprMarketplaceConfig(createAdNetwork.hyprWaterfallAdProvider, adNetworkID.as[Long], createAdNetwork.app.token, createAdNetwork.app.name)
                                appConfigService.createWithWaterfallIDInTransaction(createAdNetwork.waterfallID, None)
                              } catch {
                                case error: org.postgresql.util.PSQLException => {
                                  val message = "Encountered a Postgres Exception while updating the HyprMarketplace WaterfallAdProvider"
                                  assembleAndLogError(message, createAdNetwork.app, createAdNetwork.hyprWaterfallAdProvider, Some(response.body))
                                  connection.rollback()
                                }
                              }
                            }
                          case _ =>
                            lastFailure = {
                              val message = "Could net get ad network ID from Player's response"
                              assembleAndLogError(message, createAdNetwork.app, createAdNetwork.hyprWaterfallAdProvider, Some(response.body))
                            }
                        }
                      case failure =>
                        val error = (results \ "error").as[JsString].as[String]
                        lastFailure = {
                          val message = "There was an error while creating the ad network in Player: " + error
                          assembleAndLogError(message, createAdNetwork.app, createAdNetwork.hyprWaterfallAdProvider, Some(response.body))
                        }
                        retry(createAdNetwork)
                    }
                  }
                  case error => {
                    lastFailure = {
                      val message = "Received a " + response.status + " status code from Player"
                      assembleAndLogError(message, createAdNetwork.app, createAdNetwork.hyprWaterfallAdProvider, Some(response.body))
                    }
                    retry(createAdNetwork)
                  }
                }
              } catch {
                case _: com.fasterxml.jackson.core.JsonParseException | _: play.api.libs.json.JsResultException => {
                  lastFailure = {
                    val message = "Received a JSON parsing error"
                    assembleAndLogError(message, createAdNetwork.app, createAdNetwork.hyprWaterfallAdProvider, Some(response.body))
                  }
                  retry(createAdNetwork)
                }
              }
            } else {
              lastFailure = {
                val message = "Received a 500 response from Player"
                assembleAndLogError(message, createAdNetwork.app, createAdNetwork.hyprWaterfallAdProvider, Some(response.body))
              }
              retry(createAdNetwork)
            }
          }
        } recover {
          case _: TimeoutException => {
            lastFailure = {
              val message = "Request to Player timed out"
              assembleAndLogError(message, createAdNetwork.app, createAdNetwork.hyprWaterfallAdProvider)
            }
            retry(createAdNetwork)
          }
          case error => {
            lastFailure = {
              val message = "Recovered from Player response error: " + error.getMessage
              assembleAndLogError(message, createAdNetwork.app, createAdNetwork.hyprWaterfallAdProvider)
            }
            retry(createAdNetwork)
          }
        }
      }
      counter += 1
    }
  }

  /**
   * Appends identifiable app information to an error message and logs it to the console
   * @param errorMessage            The message to be logged
   * @param app                     The newly created app
   * @param hyprWaterfallAdProvider The newly created WaterfallAdProvider instance for HyprMarketplace
   * @param responseBody            The body of Player's response
   */
  def assembleAndLogError(errorMessage: String, app: App, hyprWaterfallAdProvider: WaterfallAdProvider, responseBody: Option[String] = None): String = {
    val error =  errorMessage + " Response Body: " + responseBody.getOrElse("None")
    Logger.error("JunGroupAPI Error for API Token: " + app.token + "\nWaterfallAdProvider ID: " + hyprWaterfallAdProvider.id + "\n" + error)
    error
  }
}

/**
 * Sends email on failure.  Called by sendFailureEmail
 * @param toAddress  The address where the email will be sent
 * @param subject    The subject of the email
 * @param body       The body of the email
 * @param mailer     A shared instance of the Mailer class
 * @param configVars Shared ENV configuration variables
 */
class JunGroupEmailActor (toAddress: String, subject: String, body: String, mailer: Mailer, configVars: ConfigVars) extends Actor {
  def receive = {
    case email: String => {
      sendJunGroupEmail(email)
    }
  }

  /**
    * Sends email to new DistributorUser.  This is called on a successful sign up.
    *
    * @param description Description of failure
    */
  def sendJunGroupEmail(description: String): Unit = {
    mailer.sendEmail(host = configVars.ConfigVarsApp.domain, recipient = toAddress, sender = mailer.PublishingEmail, subject = subject, body = body)
  }
}
