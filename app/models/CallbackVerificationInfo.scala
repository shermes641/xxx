package models

import models.WaterfallAdProvider.AdProviderRewardInfo

/**
 * Encapsulates information to create a completion if a callback is valid.
 * @param isValid True if the callback is valid; otherwise, false.
 * @param adProviderName The name of the ad provider to which the callback belongs.
 * @param transactionID A unique ID to verify the completion.
 * @param appToken The token for the App to which the completion will belong.
 * @param offerProfit The payout amount for a Completion.
 * @param rewardQuantity The amount of virtual currency earned.
 */
case class CallbackVerificationInfo(isValid: Boolean, adProviderName: String, transactionID: String, appToken: String, offerProfit: Option[Double], rewardQuantity: Long, adProviderRewardInfo: Option[AdProviderRewardInfo]) {
  val generationNumber: Option[Long] = {
    adProviderRewardInfo match {
      case Some(rewardInfo) => Some(rewardInfo.generationNumber)
      case None => None
    }
  }

  val serverToServerEnabled: Boolean = {
    adProviderRewardInfo match {
      case Some(rewardInfo) => rewardInfo.serverToServerEnabled
      case None => false
    }
  }

  val callbackURL: Option[String] = {
    adProviderRewardInfo match {
      case Some(rewardInfo) => rewardInfo.callbackURL
      case None => None
    }
  }
}
