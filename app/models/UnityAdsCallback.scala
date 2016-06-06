package models

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import org.apache.commons.codec.binary.Hex
import play.api.Logger
import play.api.mvc.Controller

import scala.util.{Failure, Success, Try}

/**
  * Encapsulates the logic for verifying server to server requests from Unity Ads.
  *
  * @param appToken    The application for this callback
  * @param queryString The entire query string sent from Unity Ads
  */
case class UnityAdsCallback(appToken: String,
                            queryString: Map[String, Seq[String]]) extends CallbackVerificationHelper with Controller {

  final val algo = "HmacMD5"
  final val BadSigningResult = ""
  final val HmacKey = "hmac"
  final val TransactionKey = "oid"

  val transactionID = queryString.getOrElse(TransactionKey, Seq(Constants.NoValue)).head

  override val adProviderName = Constants.UnityAds.Name
  override val token = appToken
  override val receivedVerification = queryString.getOrElse(HmacKey, Seq(Constants.NoValue)).head
  override val verificationInfo = new CallbackVerificationInfo(
    isValid,
    adProviderName,
    transactionID,
    token,
    payout,
    currencyAmount,
    adProviderRewardInfo)

  /**
    * Per Unity Ad's documentation, we return 1 to acknowledge that the reward process was successful.
    *
    * @return A 200 response containing 1
    */
  override def returnSuccess = Ok(Constants.UnityAds.Success)

  /**
    * Per Unity Ad's documentation, we return 400 response to acknowledge that the reward process was unsuccessful.
    *
    * @return A 400 response containing an error message
    */
  override def returnFailure = {
    Logger.error(s"""Unity Ads S2S callback failed for API Token: $appToken\n Query String: $queryString""")
    BadRequest(Constants.UnityAds.VerifyFailure)
  }

  /**
    * Per Unity Ad's documentation, we only hash the oid and sid parameters
    *
    * @return A hash of several params from the incoming postback.
    */
  override def generatedVerification: String = {
    val sharedSecret = secretKey("APIKey")

    val verifierString = {
      val queryParams = queryString - HmacKey // remove the actual hmac param before generating the hmac signature
      queryParams.toSeq
        .sortWith(_._1 < _._1) // params must be alphabetized
        .map(param => s"${param._1}=${param._2.head}") // format based on Unity Ads' documentation
        .toList
        .mkString(",") // format based on Unity Ads' documentation
    }

    Try {
      val keySpec = new SecretKeySpec(sharedSecret.getBytes("UTF-8"), algo)
      val mac = Mac.getInstance(algo)
      mac.init(keySpec)
      Hex.encodeHexString(mac.doFinal(verifierString.getBytes("UTF-8")))
    } match {
      case Success(encodedHexString) =>
        encodedHexString

      case Failure(e) =>
        Logger.error(s"Exception calculating Unity Ads HMAC exception: $e")
        BadSigningResult
    }
  }
}
