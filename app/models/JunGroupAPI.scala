package models

import akka.actor.{Props, Actor}
import play.api.libs.json._
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.ws._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import play.api.libs.json.{JsArray, JsValue}
import play.api.{Play}
import play.api.libs.concurrent.Akka
import scala.concurrent.duration.DurationInt

case class CreateAdNetwork(distributorUser: DistributorUser)

case class JunGroupAPI() {
  /**
   * Sends request to Jungroup API using settings in the config
   */
  def createJunGroupAdNetwork(distributorUser: DistributorUser) = {
    val actor = Akka.system(current).actorOf(Props(new JunGroupAPIActor()))
    actor ! CreateAdNetwork(distributorUser)
  }


  /**
   * Creates the request using the config passed.
   * @return Future[WSResponse]
   */
  def createRequest(adNetwork: JsObject): Future[WSResponse] = {
    val config = Play.current.configuration
    WS.url("http://" + config.getString("jungroup.url").get + "/admin/ad_network/create").withAuth(config.getString("jungroup.user").get, config.getString("jungroup.password").get, WSAuthScheme.DIGEST).post(adNetwork)
  }

  /**
   * Sends failure email to account specified in application config
   */
  def sendFailureEmail(distributorUser: DistributorUser, failureReason: String) {
    val emailActor = Akka.system.actorOf(Props(new JunGroupEmailActor))
    emailActor ! distributorUser.email + ". Error: " + failureReason
  }

  /**
   * Sends success email on ad network creation
   */
  def sendSuccessEmail(distributorUser: DistributorUser) {
    val emailActor = Akka.system.actorOf(Props(new JunGroupSuccessEmailActor))
    emailActor ! distributorUser.email
  }

  /**
   * Creates a JSON object of the adnetwork configuration
   * @param distributorUser An instance of the distributorUser with the information need to create an ad network account
   * @return A JsObject with ad network information.
   */
  def adNetworkConfiguration(distributorUser: DistributorUser): JsObject = {
    JsObject(
      Seq(
        "ad_network" -> JsObject(
          Seq(
            "name" -> JsString(distributorUser.email),
            // Always set to true due to mediation being SDK only
            "mobile" -> JsBoolean(true)
          )
        ),
        "payout_url" -> JsObject(
          Seq(
            "url" -> JsString(Play.current.configuration.getString("jungroup.callbackurl").get),
            "environment" -> JsString("production"),
            "signature" -> JsArray(
              Seq(
                JsString("UID"),
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
 */
class JunGroupAPIActor() extends Actor {
  private var counter = 0
  private val RETRY_COUNT = 3
  private val RETRY_FREQUENCY = 3 seconds
  private var lastFailure = ""

  def retry(distributorUser: DistributorUser) = {
    context.system.scheduler.scheduleOnce(RETRY_FREQUENCY, self, CreateAdNetwork(distributorUser))
  }

  def receive = {
    case CreateAdNetwork(distributorUser: DistributorUser) => {
      counter += 1
      if(counter > RETRY_COUNT){
        JunGroupAPI().sendFailureEmail(distributorUser, lastFailure)
        context.stop(self)
      } else {
        val adNetwork = JunGroupAPI().adNetworkConfiguration(distributorUser)
        JunGroupAPI().createRequest(adNetwork) map {
          case response => {
            if(response.status != 500) {
              Json.parse(response.body) match {
                case _:JsUndefined => {
                  retry(distributorUser)
                }
                case results if(response.status == 200 || response.status == 304) => {
                  val success: JsValue = results \ "success"
                  if(success.as[JsBoolean] != JsBoolean(false)) {
                    DistributorUser.setActive(distributorUser)
                    JunGroupAPI().sendSuccessEmail(distributorUser)
                  } else {
                    val error: JsValue = results \ "error"
                    lastFailure = error.as[String]
                    retry(distributorUser)
                  }
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
 */
class JunGroupEmailActor extends Actor with Mailer {
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
    val subject = "Distribution Sign Up Failure"
    val body = "Jun group ad network account was not created successfully for " + description
    sendEmail(Play.current.configuration.getString("jungroup.email").get, subject, body)
  }
}


/**
 * Sends email on success.  Called by sendSuccessEmail
 */
class JunGroupSuccessEmailActor extends Actor with Mailer {
  def receive = {
    case email: String => {
      sendJunGroupEmail(email)
    }
  }

  /**
   * Sends email to new DistributorUser.  This is called on a successful sign up.
   * @param description Description of failure
   */
  def sendJunGroupEmail(email: String): Unit = {
    val subject = "Account has been activated"
    val body = "Your account has been activated you can now begin creating apps."
    sendEmail(email, subject, body)
  }
}

