package models

import java.security.MessageDigest
import play.api.Play

/**
 * Encapsulates callback information for HyprMarketplace.
 * @param appToken The token for the App to which the completion will belong.
 * @param userID A unique ID for a user in HyprMarketplace's network.
 * @param signature A hashed value to authenticate the origin of the request.
 * @param time The timestamp for the callback.
 * @param transactionID A unique ID to verify the completion.
 * @param offerProfit The amount earned from the completion.
 * @param quantity The amount of virtual currency earned.
 */
class HyprMarketplaceCallback(appToken: String, userID: String, signature: String, time: String, transactionID: String, offerProfit: Option[Double], quantity: Int) extends CallbackVerificationHelper {
  override val adProviderName = "HyprMarketplace"
  override val token = appToken
  override val receivedVerification = signature
  override val currencyAmount = quantity
  override def payout = offerProfit
  override val verificationInfo = new CallbackVerificationInfo(isValid, adProviderName, transactionID, appToken, payout, currencyAmount)

  /**
   * Generates a security digest using the steps provided in HyprMarketplace's documentation.
   * @return A hashed String
   */
  override def generatedVerification: String = {
    val md = MessageDigest.getInstance("SHA-256")
    val transactionString = List(userID, transactionID, time, Play.current.configuration.getString("jungroup.token").get).mkString("")
    md.digest(transactionString.getBytes).map("%02x".format(_)).mkString
  }
}
