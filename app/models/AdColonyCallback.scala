package models

import java.security.MessageDigest

/**
 * Encapsulates the logic for verifying server to server requests from Ad Colony.
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
 */
class AdColonyCallback(appToken: String, transactionID: String, uid: String, amount: Int, currency: String, openUDID: String, udid: String, odin1: String, macSha1: String, verifier: String) extends CallbackVerificationHelper {
  override val adProviderName = "AdColony"
  override val token = appToken
  override val receivedVerification = verifier
  override val currencyAmount = amount
  val verificationInfo = new CallbackVerificationInfo(isValid, adProviderName, transactionID, appToken, payout, currencyAmount)

  /**
   * Generates a security digest using the steps provided in Ad Colony's documentation.
   * @return If a transaction ID is present, return a hashed String; otherwise, returns None.
   */
  override def generatedVerification: String = {
    val verifierString = List(transactionID, uid, amount, currency, secretKey("APIKey"), openUDID, udid, odin1, macSha1).mkString("")
    MessageDigest.getInstance("MD5").digest(verifierString.getBytes).map("%02x".format(_)).mkString
  }
}
