package models

import play.api.libs.json.JsValue

/**
 * Encapsulates information to create a completion if a callback is valid.
 * @param isValid True if the callback is valid; otherwise, false.
 * @param adProviderName The name of the ad provider to which the callback belongs.
 * @param transactionID A unique ID to verify the completion.
 * @param waterfallToken The token for the waterfall to which the completion will belong.
 * @param offerProfit The payout amount for a Completion.
 * @param rewardQuantity The amount of virtual currency earned.
 */
case class CallbackVerificationInfo(isValid: Boolean, adProviderName: String, transactionID: String, waterfallToken: String, offerProfit: Option[Double], rewardQuantity: Int)
