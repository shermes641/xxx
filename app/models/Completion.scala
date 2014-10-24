package models

import anorm._
import models.Waterfall.WaterfallCallbackInfo
import play.api.db.DB
import play.api.Play.current
import play.api.libs.json.{JsNumber, Json}
import play.api.libs.ws.WS
import scala.concurrent.Future
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

object Completion extends Completion

class Completion {
  /**
   * Creates a new record in the completions table.
   * @param waterfallToken The token for the waterfall to which the completion belongs.
   * @param adProviderName The name of the ad provider to which the completion belongs.
   * @param transactionID A unique ID that verifies a completion.
   * @param offerProfit The estimated revenue earned by a Distributor for a Completion.
   * @return The ID of the new completion record if the insertion succeeds; otherwise, returns None.
   */
  def create(waterfallToken: String, adProviderName: String, transactionID: String, offerProfit: Option[Double]): Option[Long] = {
    DB.withConnection { implicit connection =>
      try{
        SQL(
          """
          INSERT INTO completions (waterfall_token, ad_provider_name, transaction_id, offer_profit)
          VALUES ({waterfall_token}, {ad_provider_name}, {transaction_id}, {offer_profit});
          """
        ).on("waterfall_token" -> waterfallToken, "ad_provider_name" -> adProviderName, "transaction_id" -> transactionID, "offer_profit" -> offerProfit).executeInsert()
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
   * @param requestBody The original postback from the ad provider
   * @return A boolean future indicating the success of the call to the App's reward callback.
   */
  def createWithNotification(verificationInfo: CallbackVerificationInfo, requestBody: String = ""): Future[Boolean] = {
    (create(verificationInfo.waterfallToken, verificationInfo.adProviderName, verificationInfo.transactionID, verificationInfo.offerProfit), Waterfall.findCallbackInfo(verificationInfo.waterfallToken)) match {
      case (Some(id: Long), Some(callbackInfo: WaterfallCallbackInfo)) if(callbackInfo.serverToServerEnabled) => {
        postCallback(callbackInfo.callbackURL, "Completion successful.", requestBody, verificationInfo)
      }
      case (Some(id: Long), _) => {
        Future { true }
      }
      case (None, Some(callbackInfo: WaterfallCallbackInfo)) if(callbackInfo.serverToServerEnabled) => {
        postCallback(callbackInfo.callbackURL, "Completion was not successful.", requestBody, verificationInfo)
      }
      case (_, _) => {
        Future { false }
      }
    }
  }

  /**
   * Sends POST request to callback URL if one exists.
   * @param callbackURL The target URL for the POST request.
   * @param message A message indicating the success or failure of the Completion.
   * @param body The original postback from the ad provider.
   * @param verificationInfo Class containing information to verify the postback and create a new Completion.
   * @return A boolean future indicating the success of the call to the App's reward callback.
   */
  def postCallback(callbackURL: Option[String], message: String, body: String, verificationInfo: CallbackVerificationInfo): Future[Boolean] = {
    callbackURL match {
      case Some(url: String) => {
        val data = Map(
          "status" -> Seq(message),
          "original_postback" -> Seq(body),
          "ad_provider" -> Seq(verificationInfo.adProviderName),
          "reward_quantity" -> Seq(verificationInfo.rewardQuantity.toString),
          "calculated_offer_profit" -> Seq(verificationInfo.offerProfit match {
            case Some(profit: Double) => profit.toString
            case None => ""
          })
        )
        WS.url(url).post(data).map(response =>
          response.status match {
            case status: Int if(status == 200) => true
            case _ => true
          }
        )
      }
      case _ => Future { false }
    }
  }
}
