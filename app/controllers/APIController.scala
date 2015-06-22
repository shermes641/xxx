package controllers

import java.util.concurrent.TimeoutException
import models._
import play.api.mvc._
import play.api.libs.json._
import play.api.Logger
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.language.implicitConversions
import scala.language.postfixOps

object APIController extends Controller {
  val DefaultTimeout = 5000 // The default timeout for all server to server calls (in milliseconds).

  /**
   * Responds with configuration JSON from the app_configs table.
   * @param appToken Random string which both authenticates the request and identifies the app.
   * @return If an AppConfig is found, return the configuration field.  Otherwise, return a JSON error message.
   */
  def appConfigV1(appToken: String) = Action { implicit request =>
    AppConfig.findLatest(appToken) match {
      case Some(response) if((response.configuration \ "status").isInstanceOf[JsUndefined]) => Ok(response.configuration)
      case Some(response) => BadRequest(response.configuration)
      case None => NotFound(Json.obj("status" -> "error", "message" -> "App Configuration not found."))
    }
  }

  /**
   * Accepts server to server callback info from Vungle, then starts the reward completion process.
   * @param appToken The token for the App to which the completion will belong.
   * @param amount The amount of virtual currency to be rewarded.
   * @param uid The ID of the device on Vungle's network.
   * @param transactionID A unique ID that verifies the completion.
   * @param digest A hashed value to authenticate the origin of the request.
   * @return If the incoming request is valid, returns a 200; otherwise, returns 400.
   */
  def vungleCompletionV1(appToken: String, transactionID: Option[String], digest: Option[String], amount: Option[Int], uid: Option[String]) = Action { implicit request =>
    (transactionID, digest, amount) match {
      case (Some(transactionIDValue: String), Some(digestValue: String), Some(amountValue: Int)) => {
        val callback = new VungleCallback(appToken, transactionIDValue, digestValue, amountValue)
        callbackResponse(callback, request)
      }
      case (_, _, _) => BadRequest
    }
  }

  /**
   * Accepts server to server callback info from Ad Colony, then starts the reward completion process.
   * @param appToken The token for the App to which the completion will belong.
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
  def adColonyCompletionV1(appToken: String, transactionID: Option[String], uid: Option[String], amount: Option[Int], currency: Option[String], openUDID: Option[String], udid: Option[String], odin1: Option[String], macSha1: Option[String], verifier: Option[String], customID: Option[String]) = Action { implicit request =>
    (transactionID, uid, amount, currency, openUDID, udid, odin1, macSha1, verifier, customID) match {
      case (Some(transactionIDValue: String), Some(uidValue: String), Some(amountValue: Int), Some(currencyValue: String), Some(openUDIDValue: String), Some(udidValue: String), Some(odin1Value: String), Some(macSha1Value: String), Some(verifierValue: String), Some(customIDValue: String)) => {
        val callback = new AdColonyCallback(appToken, transactionIDValue, uidValue, amountValue, currencyValue, openUDIDValue, udidValue, odin1Value, macSha1Value, verifierValue, customIDValue)
        callbackResponse(callback, request)
      }
      case (_, _, _, _, _, _, _, _, _, _) => AdColonyCallback.DefaultFailure
    }
  }

  /**
   * Accepts server to server callback info from AppLovin, then starts the reward completion process.
   * @param appToken The token for the App to which the completion will belong.
   * @param eventID A unique ID that verifies the completion.
   * @param amount The amount of virtual currency to be rewarded
   * @param idfa Advertising ID
   * @param hadid SHA1 hash of lowercase IDFA
   * @param currency The type of virtual currency to be rewarded
   * @param userID A unique ID set for each user.
   * @return If the incoming request is valid, returns a 200; otherwise, returns 400.
   */
  def appLovinCompletionV1(appToken: String, eventID: Option[String], amount: Option[Double], idfa: Option[String], hadid: Option[String], currency: Option[String], userID: Option[String]) = Action { implicit request =>
    (eventID, amount) match {
      case (Some(eventIDValue: String), Some(amountValue: Double)) => {
        val callback = new AppLovinCallback(eventIDValue, appToken, amountValue)
        callbackResponse(callback, request)
      }
      case (_, _) => BadRequest
    }
  }

  /**
   * Accepts server to server callback info from Flurry, then starts the reward completion process.
   * @param appToken The token for the App to which the completion will belong.
   * @param idfa Advertising ID.
   * @param sha1Mac SHA1 hash of MAC address.
   * @param fguid A unique ID that verifies the completion.
   * @param rewardQuantity The amount of virtual currency to be rewarded.
   * @param fhash A hashed value to authenticate the origin of the request.
   * @param udid A unique device ID.
   * @return If the incoming request is valid, returns a 200; otherwise, returns 400.
   */
  def flurryCompletionV1(appToken: String, idfa: Option[String], sha1Mac: Option[String], fguid: Option[String], rewardQuantity: Option[Int], fhash: Option[String], udid: Option[String]) = Action { implicit request =>
    (fguid, rewardQuantity, fhash) match {
      case (Some(fguidValue: String), Some(rewardQuantityValue: Int), Some(fhashValue: String)) => {
        val callback = new FlurryCallback(appToken, fguidValue, rewardQuantityValue, fhashValue)
        callbackResponse(callback, request)
      }
      case (_, _, _) => BadRequest
    }
  }

  /**
   * Accepts server to server callback info from HyprMarketplace, then starts the reward completion process.
   * @param appToken The token for the App to which the completion will belong.
   * @param time The timestamp for the callback.
   * @param sig A hashed value to authenticate the origin of the request.
   * @param quantity The amount of virtual currency to be rewarded.
   * @param offerProfit Ad spend generated.
   * @param rewardID A unique ID to identify a virtual currency.
   * @param uid A unique ID to identify a user.
   * @param subID A unique ID to verify the transaction.
   * @return If the incoming request is valid, returns a 200; otherwise, returns 400.
   */
  def hyprMarketplaceCompletionV1(appToken: String, time: Option[String], sig: Option[String], quantity: Option[Int], offerProfit: Option[Double], rewardID: Option[String], uid: Option[String], subID: Option[String]) = Action { implicit request =>
    (uid, sig, time, subID, quantity) match {
      case (Some(userIDValue: String), Some(signatureValue: String), Some(timeValue: String), Some(subIDValue: String), Some(quantityValue: Int)) => {
        val callback = new HyprMarketplaceCallback(appToken, userIDValue, signatureValue, timeValue, subIDValue, offerProfit, quantityValue)
        callbackResponse(callback, request)
      }
      case (_, _, _, _, _) => BadRequest
    }
  }

  /**
   * Converts Ad Provider request into standardized JSON format to be stored in the database.
   * @param request The original postback from the Ad Provider.
   * @return JSON object containing the HTTP method, path and query string from the original postback.
   */
  implicit def requestToJsonBuilder(request: Request[Any]): JsValue = {
    val query = request.queryString.foldLeft(JsObject(Seq()))((json, el) => json.deepMerge(JsObject(Seq(el._1 -> JsString(el._2.mkString(""))))))
    JsObject(Seq("method" -> JsString(request.method), "path" -> JsString(request.path))).deepMerge(JsObject(Seq("query" -> query)))
  }

  /**
   * Creates a Completion if the reward callback is valid and notifies the Distributor if server to server callbacks are enabled.
   * @param callback Any class instance which extends CallbackVerificationHelper.  This encapsulates callback information to determine the validity of the incoming request.
   * @param adProviderRequest The original postback from the ad provider.
   * @param completion A new instance of the Completion class.
   * @return Creates a Completion if the callback if valid and notifies the Distributor if server to server callbacks are enabled; otherwise, returns None.
   */
  def callbackResponse(callback: CallbackVerificationHelper, adProviderRequest: JsValue, completion: Completion = new Completion) = {
    callback.verificationInfo.isValid match {
      case true => {
        try {
          Await.result(completion.createWithNotification(callback.verificationInfo, adProviderRequest), Duration(DefaultTimeout, "millis")) match {
            case true => callback.returnSuccess
            case false => {
              Logger.error("Server to server callback to Distributor's servers was unsuccessful for Ad Provider: " +
                callback.adProviderName + "API Token: " + callback.token + " Callback URL: " + callback.verificationInfo.callbackURL)
              callback.returnFailure
            }
          }
        } catch {
          case _: TimeoutException => {
            Logger.error("Server to server callback to Distributor's servers timed out for Ad Provider: " +
              callback.adProviderName + "API Token: " + callback.token + " Callback URL: " + callback.verificationInfo.callbackURL)
            callback.returnFailure
          }
        }
      }
      case false => {
        Logger.error("Invalid server to server callback verification for Ad Provider: " + callback.adProviderName + " API Token: " + callback.token)
        callback.returnFailure
      }
    }
  }
}
