package models

import java.net.URL

import anorm._
import play.api.Logger
import play.api.Play.current
import play.api.db.DB
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{TimeoutException, Future}
import scala.language.{implicitConversions, postfixOps}
import scala.util.{Failure, Success, Try}

/**
  * Encapsulates information pertaining to a successful server to server callback.
  */
class Completion extends JsonConversion {
  /**
    * Creates a new record in the completions table.
    *
    * @param appToken          The token for the App to which the completion belongs.
    * @param adProviderName    The name of the ad provider to which the completion belongs.
    * @param transactionID     A unique ID that verifies a completion.
    * @param offerProfit       The estimated revenue earned by a Distributor for a Completion. In the case of HyprMarketplace, this value is passed to us in the server to server callback.
    * @param rewardQuantity    The amount to reward the user.  This is calculated based on cpm of the WaterfallAdProvider and the VirtualCurrency information on the server at the time the callback is received.
    *                          This can differ from the reward calculated by the SDK and the reward quantity passed to us from the ad provider in the server to server callback.
    * @param generationNumber  The generationNumber from the latest AppConfig at the time the server to server callback is received.
    * @param adProviderRequest The original server to server request from the Ad Provider.
    * @return The ID of the new completion record if the insertion succeeds; otherwise, returns None.
    */
  private def create(appToken: String, adProviderName: String, transactionID: String, offerProfit: Option[Double], rewardQuantity: Long, generationNumber: Option[Long], adProviderRequest: JsValue): Option[Long] = {
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
        case exception: org.postgresql.util.PSQLException => None
      }
    }
  }

  /**
    * Creates a new Completion record and notifies the distributor if server to server callbacks are enabled.
    *
    * @param verificationInfo  Class containing information to verify the postback and create a new Completion.
    * @param adProviderRequest The original postback from the ad provider
    * @return A boolean future indicating the success of the call to the App's reward callback.
    */
  def createWithNotification(verificationInfo: CallbackVerificationInfo,
                             adProviderRequest: JsValue,
                             hmacQueryParams: Option[Seq[(String, String)]] = None): Future[Boolean] = {
    create(
      verificationInfo.appToken, verificationInfo.adProviderName, verificationInfo.transactionID,
      verificationInfo.offerProfit, verificationInfo.rewardQuantity, verificationInfo.generationNumber, adProviderRequest
    ) match {
      case Some(id: Long) =>
        // validate uri and do not perform POST if it's bad
        Try(new URL(verificationInfo.callbackURL.getOrElse(""))) match {
          case Success(uri) if verificationInfo.serverToServerEnabled =>
              postCallback(verificationInfo.callbackURL, adProviderRequest, verificationInfo, hmacQueryParams)

          case _ =>
            if (!verificationInfo.serverToServerEnabled) {
              Logger.debug(s"""Server to Server callbacks not enabled url: '${verificationInfo.callbackURL.getOrElse("")}'""")
              Future(true)
            } else {
              Logger.error(s"""Unable to validate callback URL, POST not executed url: '${verificationInfo.callbackURL.getOrElse("")}'""")
              Future(false)
            }
        }

      case None =>
        // the create call logs an error if it fails
        Future(false)
    }
  }

  /**
    * Assembles data for JSON body and sends POST request to callback URL if one exists.
    *
    * @param callbackURL        The target URL for the POST request.
    * @param adProviderRequest  The original postback from the ad provider.
    * @param verificationInfo   Class containing information to verify the postback and create a new Completion.
    * @param hmacQueryParams    Optional sequence of query parameters if using hmac signing
    * @return A boolean future indicating the success of the call to the App's reward callback.
    */
  def postCallback(callbackURL: Option[String],
                   adProviderRequest: JsValue,
                   verificationInfo: CallbackVerificationInfo,
                   hmacQueryParams: Option[Seq[(String, String)]] = None): Future[Boolean] = {

    callbackURL match {
      case Some(url: String) =>
        Try(new URL(url)) match {
          case Success(uri) =>
            val data = postbackData(adProviderRequest, verificationInfo)
            sendPost(url, data, hmacQueryParams).map { response =>
              response.status match {
                case status: Int if status == 200 => true

                case status =>
                  Logger.error("Server to server callback to Distributor's servers returned a status code of " + status + " for URL: " +
                    url + " API Token: " + verificationInfo.appToken + " Ad Provider: " + verificationInfo.adProviderName)
                  false

              }
            } recover {
              case e: TimeoutException => false
              case exception: Throwable =>
                Logger.error("Server to server callback to Distributor's servers generated exception:\nURL: " + url +
                  "\nAPI Token: " + verificationInfo.appToken + "\nException: " + exception)
                false

              case _ => false
            }

          case Failure(ex) =>
            Logger.error(s"Invalid URL exception: $ex")
            Future(false)
        }

      case _ =>
        Logger.error("postCallback called without a URL")
        Future(false)
    }
  }

  /**
   * Builds the JSON that we POST to the distributor's servers on a successful sever to server callback
   *
   * @param adProviderRequest The original postback from the ad provider.
   * @param verificationInfo  Class containing information to verify the postback and create a new Completion.
   * @return                  JSON containing all necessary postback params from our documentation
   */
  def postbackData(adProviderRequest: JsValue, verificationInfo: CallbackVerificationInfo): JsObject = {
    Json.obj(
      "original_postback"      -> adProviderRequest,
      "ad_provider"            -> verificationInfo.adProviderName,
      "reward_quantity"        -> verificationInfo.rewardQuantity,
      "estimated_offer_profit" -> verificationInfo.offerProfit,
      "transaction_id"         -> verificationInfo.transactionID
    )
  }

  /**
    * Sends POST request to callback URL
    *
    * @param url              The callback URL specified in the app
    * @param data             The JSON to be POST'ed to the callback URL
    * @param hmacQueryParams  Optional sequence of query parameters if using hmac signing
    * @return A successful or unsuccessful WSResponse
    */
  def sendPost(url: String, data: JsValue, hmacQueryParams: Option[Seq[(String, String)]] = None): Future[WSResponse] = {
    hmacQueryParams match {
      case Some(hmacQp: Seq[(String, String)]) =>
        WS.url(url).withQueryString(hmacQp: _*).post(data)

      case _ =>
        WS.url(url).post(data)
    }
  }
}

