package models

/**
 * Encapsulates callback information for AppLovin.
 * @param transactionID A unique ID to verify the completion.
 * @param appToken The token for the App to which the completion will belong.
 * @param amount The amount of virtual currency to be rewarded.
 */
class AppLovinCallback(transactionID: String, appToken: String, amount: Double) extends CallbackVerificationHelper {
  val adProviderName = "AppLovin"
  val currencyAmount = amount.toInt
  override val token = appToken
  override val receivedVerification = ""
  override def generatedVerification: String = ""

  val verificationInfo = new CallbackVerificationInfo(true, adProviderName, transactionID, appToken, payout, currencyAmount)
}
