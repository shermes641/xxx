package models

/**
 * Contains logic for determining whether an AdProvider should be included in the Waterfall or not.
 */
trait RewardThreshold {
  val roundUpVal: Option[Boolean]
  val cpmVal: Option[Double]
  val exchangeRateVal: Option[Long]
  val rewardMinVal: Long

  /**
   * Determines whether ad provider meets the reward threshold determined by the app configuration.
   * @return Boolean value whether ad provider meets reward threshold
   */
  def meetsRewardThreshold: Boolean = {
    (roundUpVal, cpmVal) match {
      case (Some(roundUpValue: Boolean), _) if(roundUpValue) => true
      case (Some(roundUpValue: Boolean), Some(cpmVal: Double)) if(!roundUpValue) => {
        cpmVal * exchangeRateVal.get >= rewardMinVal * 1000.0

      }
      // If there is no cpm value for an ad provider and the virtual currency does not roundUp, this will ensure it is excluded from the waterfall.
      case (Some(roundUpValue: Boolean), None) if(!roundUpValue) => false
      case (_, _) => true
    }
  }
}
