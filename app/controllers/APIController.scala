package controllers

import models._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.language.postfixOps

object APIController extends Controller {
  /**
   * Responds with waterfall order in JSON form.
   * @param token Random string which both authenticates the request and identifies the waterfall.
   * @return If a waterfall is found and ad providers are configured, return a JSON with the ordered ad providers and configuration info.  Otherwise, return a JSON with an error message.
   */
  def waterfallV1(token: String) = Action { implicit request =>
    Waterfall.responseV1(token) match {
      case jsonResponse: JsValue if((jsonResponse \ "status").isInstanceOf[JsUndefined]) => Ok(jsonResponse)
      case jsonResponse: JsValue => BadRequest(jsonResponse)
    }
  }

  /**
   * Accepts server to server callback info from Vungle, then starts the reward completion process.
   * @param waterfallToken The token for the waterfall to which the completion will belong.
   * @param amount The amount of virtual currency to be rewarded.
   * @param uid The ID of the device on Vungle's network.
   * @param transactionID A unique ID that verifies the completion.
   * @param digest A hashed value to authenticate the origin of the request.
   * @return If the incoming request is valid, returns a 200; otherwise, returns 400.
   */
  def vungleCompletionV1(waterfallToken: String, transactionID: Option[String], digest: Option[String], amount: Option[Int], uid: Option[String]) = Action { implicit request =>
    (transactionID, digest, amount) match {
      case (Some(transactionIDValue: String), Some(digestValue: String), Some(amountValue: Int)) => {
        val callback = new VungleCallback(waterfallToken, transactionIDValue, digestValue, amountValue)
        callbackResponse(callback.verificationInfo, request.toString)
      }
      case (_, _, _) => BadRequest
    }
  }

  /**
   * Accepts server to server callback info from Ad Colony, then starts the reward completion process.
   * @param waterfallToken The token for the waterfall to which the completion will belong.
   * @param transactionID A unique ID that verifies the completion.
   * @param uid The ID of the device on Ad Colony's network.
   * @param amount The amount of virtual currency to be rewarded.
   * @param currency The type of virtual currency to be rewarded.
   * @param openUDID A unique device identifier (not Apple).
   * @param udid Apple device identifier.
   * @param odin1 Open device identification number.
   * @param macSha1 Ad Colony device ID
   * @param verifier A hashed value to authenticate the origin of the request.
   * @return If the incoming request is valid, returns a 200; otherwise, returns 400.
   */
  def adColonyCompletionV1(waterfallToken: String, transactionID: Option[String], uid: Option[String], amount: Option[Int], currency: Option[String], openUDID: Option[String], udid: Option[String], odin1: Option[String], macSha1: Option[String], verifier: Option[String]) = Action { implicit request =>
    (transactionID, uid, amount, currency, openUDID, udid, odin1, macSha1, verifier) match {
      case (Some(transactionIDValue: String), Some(uidValue: String), Some(amountValue: Int), Some(currencyValue: String), Some(openUDIDValue: String), Some(udidValue: String), Some(odin1Value: String), Some(macSha1Value: String), Some(verifierValue: String)) => {
        val callback = new AdColonyCallback(waterfallToken, transactionIDValue, uidValue, amountValue, currencyValue, openUDIDValue, udidValue, odin1Value, macSha1Value, verifierValue)
        callbackResponse(callback.verificationInfo, request.toString)
      }
      case (_, _, _, _, _, _, _, _, _) => BadRequest
    }
  }

  /**
   * Accepts server to server callback info from AppLovin, then starts the reward completion process.
   * @param waterfallToken The token for the waterfall to which the completion will belong.
   * @param eventID A unique ID that verifies the completion.
   * @param amount The amount of virtual currency to be rewarded
   * @param idfa Advertising ID
   * @param hadid SHA1 hash of lowercase IDFA
   * @param currency The type of virtual currency to be rewarded
   * @param userID A unique ID set for each user.
   * @return If the incoming request is valid, returns a 200; otherwise, returns 400.
   */
  def appLovinCompletionV1(waterfallToken: String, eventID: Option[String], amount: Option[Double], idfa: Option[String], hadid: Option[String], currency: Option[String], userID: Option[String]) = Action { implicit request =>
    (eventID, amount) match {
      case (Some(eventIDValue: String), Some(amountValue: Double)) => {
        val callback = new AppLovinCallback(eventIDValue, waterfallToken, amountValue)
        callbackResponse(callback.verificationInfo, request.toString)
      }
      case (_, _) => BadRequest
    }
  }

  /**
   * Accepts server to server callback info from Flurry, then starts the reward completion process.
   * @param waterfallToken The token for the waterfall to which the completion will belong.
   * @param idfa Advertising ID.
   * @param sha1Mac SHA1 hash of MAC address.
   * @param fguid A unique ID that verifies the completion.
   * @param rewardQuantity The amount of virtual currency to be rewarded.
   * @param fhash A hashed value to authenticate the origin of the request.
   * @param udid A unique device ID.
   * @return If the incoming request is valid, returns a 200; otherwise, returns 400.
   */
  def flurryCompletionV1(waterfallToken: String, idfa: Option[String], sha1Mac: Option[String], fguid: Option[String], rewardQuantity: Option[Int], fhash: Option[String], udid: Option[String]) = Action { implicit request =>
    (fguid, rewardQuantity, fhash) match {
      case (Some(fguidValue: String), Some(rewardQuantityValue: Int), Some(fhashValue: String)) => {
        val callback = new FlurryCallback(waterfallToken, fguidValue, rewardQuantityValue, fhashValue)
        callbackResponse(callback.verificationInfo, request.toString)
      }
      case (_, _, _) => BadRequest
    }
  }

  /**
   * Accepts server to server callback info from HyprMX, then starts the reward completion process.
   * @param waterfallToken The token for the waterfall to which the completion will belong.
   * @param time The timestamp for the callback.
   * @param sig A hashed value to authenticate the origin of the request.
   * @param quantity The amount of virtual currency to be rewarded.
   * @param offerProfit Ad spend generated.
   * @param rewardID A unique ID to identify a virtual currency.
   * @param uid A unique ID to identify a user.
   * @param subID A unique ID to verify the transaction.
   * @return If the incoming request is valid, returns a 200; otherwise, returns 400.
   */
  def hyprMXCompletionV1(waterfallToken: String, time: Option[String], sig: Option[String], quantity: Option[Int], offerProfit: Option[Double], rewardID: Option[String], uid: Option[String], subID: Option[String]) = Action { implicit request =>
    (uid, sig, time, subID, quantity) match {
      case (Some(userIDValue: String), Some(signatureValue: String), Some(timeValue: String), Some(subIDValue: String), Some(quantityValue: Int)) => {
        val callback = new HyprMXCallback(waterfallToken, userIDValue, signatureValue, timeValue, subIDValue, offerProfit, quantityValue)
        callbackResponse(callback.verificationInfo, request.toString)
      }
      case (_, _, _, _, _) => BadRequest
    }
  }

  /**
   * Creates a Completion if the reward callback is valid and notifies the Distributor if server to server callbacks are enabled.
   * @param verificationInfo Encapsulates callback information to determine the validity of the incoming request.
   * @param body The original postback from the ad provider.
   * @return Creates a Completion if the callback if valid and notifies the Distributor if server to server callbacks are enabled; otherwise, returns None.
   */
  def callbackResponse(verificationInfo: CallbackVerificationInfo, body: String) = {
    verificationInfo.isValid match {
      case true => {
        Await.result(Completion.createWithNotification(verificationInfo, body), Duration(5000, "millis")) match {
          case true => Ok
          case false => BadRequest
        }
      }
      case false => BadRequest
    }
  }
}
