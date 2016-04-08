package models

/**
 * Encapsulates callback information for AppLovin.
 * @param transactionID A unique ID to verify the completion.
 * @param appToken      The token for the App to which the completion will belong.
 * @param amount        The amount of virtual currency to reward based on AppLovin's dashboard. We do not use this amount.
 * @param userID        The user ID to be rewarded.
 * @param wapService    A shared instance of the WaterfallAdProviderService class
 */
class AppLovinCallback(transactionID: String,
                       appToken: String,
                       amount: Double,
                       userID: Option[String],
                       wapService: WaterfallAdProviderService) extends CallbackVerificationHelper {
  val adProviderName = "AppLovin"
  override val waterfallAdProviderService = wapService
  override val token = appToken
  override val receivedVerification = ""
  override def generatedVerification: String = ""
  override val adProviderUserID = userID.getOrElse(Constants.NoValue)
  override val verificationInfo = new CallbackVerificationInfo(true, adProviderName, transactionID, appToken, payout, currencyAmount, adProviderRewardInfo)
}
