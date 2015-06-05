package models

import anorm._
import play.api.db.DB
import play.api.libs.json._
import play.api.libs.ws.{WSResponse, WS}
import play.api.Logger
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions
import scala.language.postfixOps

/**
 * Encapsulates information pertaining to a successful server to server callback.
 */
class Completion extends JsonConversion {
  /**
   * Creates a new record in the completions table.
   * @param appToken The token for the App to which the completion belongs.
   * @param adProviderName The name of the ad provider to which the completion belongs.
   * @param transactionID A unique ID that verifies a completion.
   * @param offerProfit The estimated revenue earned by a Distributor for a Completion.
   * @param rewardQuantity The amount to reward the user.  This is calculated based on cpm of the WaterfallAdProvider and the VirtualCurrency information on the server at the time the callback is received.
   *                       This can differ from the reward calculated by the SDK and the reward quantity passed to us from the ad provider in the server to server callback.
   * @param generationNumber The generationNumber from the latest AppConfig at the time the server to server callback is received.
   * @param adProviderRequest The original server to server request from the Ad Provider.
   * @return The ID of the new completion record if the insertion succeeds; otherwise, returns None.
   */
  def create(appToken: String, adProviderName: String, transactionID: String, offerProfit: Option[Double], rewardQuantity: Long, generationNumber: Option[Long], adProviderRequest: JsValue): Option[Long] = {
    DB.withConnection { implicit connection =>
      try {
        SQL(
          """
            INSERT INTO completions (app_token, ad_provider_name, transaction_id, offer_profit, reward_quantity, generation_number, ad_provider_request)
            VALUES ({app_token}, {ad_provider_name}, {transaction_id}, {offer_profit}, {reward_quantity}, {generation_number}, CAST({ad_provider_request} AS json));
          """
        ).on("app_token" -> appToken, "ad_provider_name" -> adProviderName, "transaction_id" -> transactionID, "offer_profit" -> offerProfit,
             "reward_quantity" -> rewardQuantity, "generation_number" -> generationNumber, "ad_provider_request" -> Json.stringify(adProviderRequest)).executeInsert()
      } catch {
        case exception: org.postgresql.util.PSQLException => {
          None
        }
      }
    }
  }

  /**
   * Creates a new Completion record and notifies the distributor if server to server callbacks are enabled.
   * @param verificationInfo Class containing information to verify the postback and create a new Completion.
   * @param adProviderRequest The original postback from the ad provider
   * @return A boolean future indicating the success of the call to the App's reward callback.
   */
  def createWithNotification(verificationInfo: CallbackVerificationInfo, adProviderRequest: JsValue): Future[Boolean] = {
    create(
      verificationInfo.appToken, verificationInfo.adProviderName, verificationInfo.transactionID,
      verificationInfo.offerProfit, verificationInfo.rewardQuantity, verificationInfo.generationNumber, adProviderRequest
    ) match {
      case Some(id: Long) => {
        if(verificationInfo.serverToServerEnabled) {
          postCallback(verificationInfo.callbackURL, adProviderRequest, verificationInfo)
        } else {
          Future { true }
        }
      }
      case None => Future { false }
    }
  }

  /**
   * Assembles data for JSON body and sends POST request to callback URL if one exists.
   * @param callbackURL The target URL for the POST request.
   * @param adProviderRequest The original postback from the ad provider.
   * @param verificationInfo Class containing information to verify the postback and create a new Completion.
   * @return A boolean future indicating the success of the call to the App's reward callback.
   */
  def postCallback(callbackURL: Option[String], adProviderRequest: JsValue, verificationInfo: CallbackVerificationInfo): Future[Boolean] = {
    callbackURL match {
      case Some(url: String) => {
        val data: JsValue = Json.obj(
          "original_postback" -> adProviderRequest,
          "ad_provider" -> verificationInfo.adProviderName,
          "reward_quantity" -> verificationInfo.rewardQuantity,
          "estimated_offer_profit" -> verificationInfo.offerProfit
        )
        sendPost(url, data).map(response =>
          response.status match {
            case status: Int if(status == 200) => true
            case status => {
              Logger.error("Server to server callback to Distributor's servers returned a status code of " + status + " for URL: " +
                url + " API Token: " + verificationInfo.appToken + " Ad Provider: " + verificationInfo.adProviderName)
              false
            }
          }
        )
      }
      case _ => Future { false }
    }
  }

  /**
   * Sends POST request to callback URL
   * @param url The callback URL specified in the app
   * @param data The JSON to be POST'ed to the callback URL
   * @return A successful or unsuccessful WSResponse
   */
  def sendPost(url: String, data: JsValue): Future[WSResponse] = WS.url(url).post(data)
}
