package models

import java.security.MessageDigest

/**
 * Encapsulates callback information for Flurry.
 * @param appToken The token of the App to which this completion will belong.
 * @param transactionID A unique ID to verify the completion.
 * @param rewardQuantity The amount of virtual currency to be rewarded.
 * @param fhash A hashed value to authenticate the origin of the request.
 */
class FlurryCallback(appToken: String, transactionID: String, rewardQuantity: Int, fhash: String) extends CallbackVerificationHelper {
  override val adProviderName = "Flurry"
  override val token = appToken
  override val receivedVerification = fhash
  override val currencyAmount = rewardQuantity
  override val verificationInfo = new CallbackVerificationInfo(isValid, adProviderName, transactionID, appToken, payout, currencyAmount)

  /**
   * Generates a security digest using the steps provided in Flurry's documentation.
   * @return A hashed String
   */
  override def generatedVerification: String = {
    val digest = MessageDigest.getInstance("MD5")
    val hashableString = transactionID + ":" + rewardQuantity + ":" + secretKey("APIKey")
    digest.digest(hashableString.getBytes).map("%02x".format(_)).mkString
  }
}
