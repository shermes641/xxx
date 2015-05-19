package models

import java.security.MessageDigest

/**
 * Encapsulates the logic for verifying server to server requests from Vungle.
 * @param appToken The token for the App to which the completion will belong.
 * @param transactionID A unique ID that verifies the completion.
 * @param digest A hashed value to authenticate the origin of the request.
 * @param amount The amount of virtual currency to be rewarded based on the callback URL. We do not use this value.
 */
class VungleCallback(appToken: String, transactionID: String, digest: String, amount: Int) extends CallbackVerificationHelper {
  override val adProviderName = "Vungle"
  override val token = appToken
  override val receivedVerification = digest
  override val verificationInfo = new CallbackVerificationInfo(isValid, adProviderName, transactionID, appToken, payout, currencyAmount, adProviderRewardInfo)

  /**
   * Generates a security digest using the steps provided in Vungle's documentation.
   * @return A hashed String
   */
  override def generatedVerification: String = {
    val md = MessageDigest.getInstance("SHA-256")
    val transactionString = secretKey("APIKey") + ":" + transactionID
    val firstDigestBytes = md.digest(transactionString.getBytes)
    md.digest(firstDigestBytes).map("%02x".format(_)).mkString
  }
}
