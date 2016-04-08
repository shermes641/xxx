package models

import anorm._
import hmac.{HmacConstants, HmacHashData, Signer}
import play.api.Logger
import java.net.URL
import play.api.db.Database
import anorm.SqlParser._
import com.github.nscala_time.time.Imports._
import play.api.Play.current
import play.api.db.DB
import java.net.URL
import play.api.db.Database
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, TimeoutException}
import scala.language.{implicitConversions, postfixOps}
import scala.util.{Failure, Success, Try}

/**
 * Encapsulates information pertaining to a successful server to server callback.
 * @param db       A shared database
 * @param wsClient The client used for all web service calls
 */
class Completion(db: Database, wsClient: WSClient, appService: AppService, signer: Signer) extends JsonConversion {
  /**
   * Creates a new record in the completions table.
   * @param appToken The token for the App to which the completion belongs.
   * @param adProviderName The name of the ad provider to which the completion belongs.
   * @param transactionID A unique ID that verifies a completion.
   * @param offerProfit The estimated revenue earned by a Distributor for a Completion. In the case of HyprMarketplace, this value is passed to us in the server to server callback.
   * @param rewardQuantity The amount to reward the user.  This is calculated based on cpm of the WaterfallAdProvider and the VirtualCurrency information on the server at the time the callback is received.
   *                       This can differ from the reward calculated by the SDK and the reward quantity passed to us from the ad provider in the server to server callback.
   * @param generationNumber The generationNumber from the latest AppConfig at the time the server to server callback is received.
   * @param adProviderRequest The original server to server request from the Ad Provider.
   * @return The ID of the new completion record if the insertion succeeds; otherwise, returns None.
   */
  private def create(appToken: String, adProviderName: String, transactionID: String, offerProfit: Option[Double], rewardQuantity: Long, generationNumber: Option[Long], adProviderRequest: JsValue): Option[Long] = {
    db.withConnection { implicit connection =>
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
    * @param verificationInfo   Class containing information to verify the postback and create a new Completion.
    * @param adProviderRequest  The original postback from the ad provider
    * @param adProviderUserID   ad provider supplied user id
    * @return A boolean future indicating the success of the call to the App's reward callback.
    */
  def createWithNotification(verificationInfo: CallbackVerificationInfo,
                             adProviderRequest: JsValue,
                             adProviderUserID: String): Future[Boolean] = {
    create(
      verificationInfo.appToken, verificationInfo.adProviderName, verificationInfo.transactionID,
      verificationInfo.offerProfit, verificationInfo.rewardQuantity, verificationInfo.generationNumber, adProviderRequest
    ) match {
      case Some(id: Long) =>
        verificationInfo.serverToServerEnabled match {
          case true =>
            postCallback(verificationInfo.callbackURL, adProviderRequest, verificationInfo, adProviderUserID, appService.findHmacSecretByToken(verificationInfo.appToken).getOrElse(Constants.NoValue))

          case _ =>
            Logger.debug(s"""Server to Server callbacks not enabled url: '${verificationInfo.callbackURL.getOrElse(Constants.NoValue)}'""")
            Future(true)
        }

      case None =>
        // the create call logs an error if it fails
        Future(false)
    }
  }

  /**
    * Assembles data for JSON body and sends POST request to callback URL if one exists.
    *
    * @param callbackURL       The target URL for the POST request.
    * @param adProviderRequest The original postback from the ad provider.
    * @param verificationInfo  Class containing information to verify the postback and create a new Completion.
    * @param adProviderUserID  User ID from ad provider
    * @param sharedSecretKey   Used to HMAC sign the request
    * @return A boolean future indicating the success of the call to the App's reward callback.
    */
  def postCallback(callbackURL: Option[String],
                   adProviderRequest: JsValue,
                   verificationInfo: CallbackVerificationInfo,
                   adProviderUserID: String,
                   sharedSecretKey: String): Future[Boolean] = {

    callbackURL match {
      case Some(url: String) =>
        Try(new URL(url)) match {
          case Success(uri) =>
            val hmacData = HmacHashData(adProviderRequest, verificationInfo, adProviderUserID, signer)

            Logger.info(s"hmacData:\n'${hmacData.postBackData.toString}'")

            val signature = hmacData.toHash(sharedSecretKey)
            Logger.debug(s"signature: '$signature' secret: '$sharedSecretKey'")
            sendPost(url, hmacData.postBackData, signature).map { response =>
              response.status match {
                case status: Int if status == 200 =>
                  true

                case status =>
                  Logger.error("Server to server callback to Distributor's servers returned a status code of " + status + " for URL: " +
                    url + " API Token: " + verificationInfo.appToken + " Ad Provider: " + verificationInfo.adProviderName)
                  false
              }
            } recover {
              case e: TimeoutException =>
                false
              case exception: Throwable =>
                Logger.error("Server to server callback to Distributor's servers generated exception:\nURL: " + url +
                  "\nAPI Token: " + verificationInfo.appToken + "\nException: " + exception)
                false

              case _ =>
                false
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
    * Sends POST request to callback URL
    *
    * @param url       The callback URL specified in the app
    * @param data      The JSON to be POST'ed to the callback URL
    * @param signature Optional sequence of query parameters if using hmac signing
    * @return A successful or unsuccessful WSResponse
    */
  def sendPost(url: String, data: JsValue, signature: Option[String]): Future[WSResponse] = {
    signature match {
      case Some(sig) =>
        wsClient.url(url).withQueryString(Seq((HmacConstants.QsHmac, sig),(HmacConstants.QsVersionKey, HmacConstants.QsVersionValue1_0)): _*).post(data)
      case _ =>
        wsClient.url(url).post(data)
    }
  }
}
