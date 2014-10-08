package models

import java.security.MessageDigest
import play.api.libs.json.JsNumber

/**
 * Encapsulates callback information for HyprMX.
 * @param waterfallToken The token for the waterfall to which the completion will belong.
 * @param userID A unique ID for a user in HyprMX's network.
 * @param signature A hashed value to authenticate the origin of the request.
 * @param time The timestamp for the callback.
 * @param transactionID A unique ID to verify the completion.
 * @param offerProfit The amount earned from the completion.
 */
class HyprMXCallback(waterfallToken: String, userID: String, signature: String, time: String, transactionID: String, offerProfit: Option[Double]) extends CallbackVerificationHelper {
  override val adProviderName = "HyprMX"
  override val token = waterfallToken
  override val receivedVerification = signature
  override val currencyAmount = 1
  override def payout = offerProfit
  val verificationInfo = new CallbackVerificationInfo(isValid, adProviderName, transactionID, waterfallToken, payout)

  /**
   * Generates a security digest using the steps provided in HyprMX's documentation.
   * @return A hashed String
   */
  override def generatedVerification: String = {
    val md = MessageDigest.getInstance("SHA-256")
    val transactionString = List(userID, transactionID, time).mkString("") + secretKey("APIKey")
    md.digest(transactionString.getBytes).map("%02x".format(_)).mkString
  }
}
