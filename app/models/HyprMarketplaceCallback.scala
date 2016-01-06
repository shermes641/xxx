package models

import java.security.MessageDigest
import play.api.Play

/**
 * Encapsulates callback information for HyprMarketplace.
 * @param appToken The token for the App to which the completion will belong.
 * @param userID A unique ID for a user in HyprMarketplace's network.
 * @param signature A hashed value to authenticate the origin of the request.
 * @param time The timestamp for the callback.
 * @param subID A unique ID to verify the completion (corresponds to the sub_id in Player).
 * @param offerProfit The amount earned from the completion.
 * @param quantity The amount of virtual currency earned.
 * @param transactionID A unique ID to verify the completion (corresponds to the partner_code in Player).
 */
class HyprMarketplaceCallback(appToken: String, userID: String, signature: String, time: String, subID: String, offerProfit: Option[Double], quantity: Int, transactionID: Option[String]) extends CallbackVerificationHelper {
  val defaultTransactionID = ""
  override val adProviderName = "HyprMarketplace"
  override val token = appToken
  override val receivedVerification = signature
  override def payout = offerProfit
  override val verificationInfo = new CallbackVerificationInfo(isValid, adProviderName, transactionID.getOrElse(defaultTransactionID), appToken, payout, currencyAmount, adProviderRewardInfo)

  /**
   * Generates a security digest using the steps provided in HyprMarketplace's documentation.
   * @return A hashed String
   */
  override def generatedVerification: String = {
    val md = MessageDigest.getInstance("SHA-256")
    val transactionString = List(userID, subID, transactionID.getOrElse(defaultTransactionID), time, Play.current.configuration.getString("jungroup.token").get).mkString("")
    md.digest(transactionString.getBytes).map("%02x".format(_)).mkString
  }
}
