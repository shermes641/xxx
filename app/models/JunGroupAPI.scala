package models

import models.{Mailer}
import akka.actor.{Props, Actor}
import play.api.libs.json._
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.ws._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import play.api.libs.json.{JsArray, JsValue}
import play.api.{Play, Application}
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
  def sendFailureEmail(distributorUser: DistributorUser) {
    val email = Play.current.configuration.getString("jungroup.email").get
    val subject = "Distribution Sign Up Failure"
    val body = distributorUser.email + " did not have an account created successfully on JunGroup Ad Server."
    println(body)
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
                JsString("TOKENPLACEHOLDER")
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

  def retry(distributorUser: DistributorUser) = {
    context.system.scheduler.scheduleOnce(RETRY_FREQUENCY, self, CreateAdNetwork(distributorUser))
  }

  def receive = {
    case CreateAdNetwork(distributorUser: DistributorUser) => {
      counter += 1
      if(counter > RETRY_COUNT){
        JunGroupAPI().sendFailureEmail(distributorUser)
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
                  } else {
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