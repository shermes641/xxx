package models

import java.security.MessageDigest
import play.api.mvc.Controller

/**
 * Companion object to define DefaultFailure used in the APIController.
 */
object AdColonyCallback extends Controller {
  val DefaultFailure = Ok("vc_decline")
}

/**
 * Encapsulates the logic for verifying server to server requests from Ad Colony.
 * @param appToken The token for the App to which the completion will belong.
 * @param transactionID A unique ID that verifies the completion.
 * @param uid The ID of the device on Ad Colony's network.
 * @param amount The amount of virtual currency to reward based on AdColony's dashboard. We do not use this amount.
 * @param currency The type of virtual currency to reward based on AdColony's dashboard. We do not use this currency name.
 * @param openUDID A unique device identifier (not Apple).
 * @param udid Apple device identifier.
 * @param odin1 Open device identification number.
 * @param macSha1 Ad Colony device ID
 * @param verifier A hashed value to authenticate the origin of the request.
 * @param customID A custom param used to pass user ID.
 */
class AdColonyCallback(appToken: String, transactionID: String, uid: String, amount: Int, currency: String, openUDID: String, udid: String, odin1: String, macSha1: String, verifier: String, customID: String) extends CallbackVerificationHelper with Controller {
  override val adProviderName = "AdColony"
  override val token = appToken
  override val receivedVerification = verifier
  override val verificationInfo = new CallbackVerificationInfo(isValid, adProviderName, transactionID, appToken, payout, currencyAmount, adProviderRewardInfo)

  /**
   * Per AdColony's documentation, we return 'vc_success' to acknowledge that the reward process was successful.
   * @return A 200 response containing 'vc_success'
   */
  override def returnSuccess = Ok("vc_success")

  /**
   * Per AdColony's documentation, we return 'vc_decline' to acknowledge that the reward process was unsuccessful.
   * @return A 400 response containing 'vc_decline'
   */
  override def returnFailure = AdColonyCallback.DefaultFailure

  /**
   * Generates a security digest using the steps provided in Ad Colony's documentation.
   * @return If a transaction ID is present, return a hashed String; otherwise, returns None.
   */
  override def generatedVerification: String = {
    val verifierString = List(transactionID, uid, amount, currency, secretKey("APIKey"), openUDID, udid, odin1, macSha1, customID).mkString("")
    MessageDigest.getInstance("MD5").digest(verifierString.getBytes).map("%02x".format(_)).mkString
  }
}
