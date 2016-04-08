package models

import play.api.libs.json.JsString
import play.api.mvc.Controller

// Helper functions included in Callback models.
trait CallbackVerificationHelper extends Controller {
  val waterfallAdProviderService: WaterfallAdProviderService
  val receivedVerification: String
  val adProviderName: String
  val token: String
  val adProviderUserID: String
  val verificationInfo: CallbackVerificationInfo

  // Retrieve ad provider's eCPM and configuration data, virtual currency settings, and callback URL info.
  lazy val adProviderRewardInfo = waterfallAdProviderService.findRewardInfo(token, adProviderName)

  // Calculates the profit from a single completion without rounding down to a dollar amount.
  lazy val rawPayoutAmount: Option[Double] = {
    adProviderRewardInfo match {
      case Some(rewardInfo) => {
        rewardInfo.cpm match {
          case Some(cpm: Double) => {
            Some(cpm / 1000.0)
          }
          case _ => None
        }
      }
      case _ => None
    }
  }

  /**
   * The verification string we generate on our servers and compare with the one sent in the callback from the ad provider.
   * @return A hash of several params from the incoming postback.
   */
  def generatedVerification: String

  /**
   * Default success response for rewarded postbacks.
   * @return A 200 response.
   */
  def returnSuccess = Ok("")

  /**
   * Default failure response for rewarded postbacks.
   * @return A 400 response.
   */
  def returnFailure = BadRequest("")

  /**
   * Pulls value from waterfall ad provider configuration data.
   * @param jsonKey The key used to retrieve a value from the JSON configuration data.
   * @return The value stored in the configuration data if one exists; otherwise, returns an empty string.
   */
  def secretKey(jsonKey: String): String = {
    adProviderRewardInfo match {
      case Some(rewardInfo) => {
        (rewardInfo.configurationData \ "callbackParams" \ jsonKey).toOption match {
          case Some(key: JsString) => key.as[String]
          case _ => ""
        }
      }
      case None => ""
    }
  }

  /**
   * Calculates the payout information for a completion based on the current cpm of the WaterfallAdProvider.
   * @return A Double value correlating to a dollar amount.
   */
  def payout: Option[Double] = {
    rawPayoutAmount match {
      case Some(amount) => {
        Some(BigDecimal(amount).setScale(2, BigDecimal.RoundingMode.FLOOR).toDouble)
      }
      case None => None
    }
  }

  /**
   * Calculates the amount of virtual currency to award.
   * This is calculated based on cpm of the WaterfallAdProvider and the VirtualCurrency
   * information on the server at the time the callback is received.
   * This can differ from the reward calculated by the SDK and the reward quantity
   * passed to us from the ad provider in the server to server callback.
   * @return The amount of virtual currency floored to the nearest integer.
   */
  def currencyAmount: Long = {
    adProviderRewardInfo match {
      case Some(rewardInfo) => {
        rawPayoutAmount match {
          case Some(profitPerCompletion: Double) => {
            val rewardAmount = BigDecimal(rewardInfo.exchangeRate * profitPerCompletion).setScale(0, BigDecimal.RoundingMode.FLOOR).toInt
            rewardInfo.rewardMax match {
              case Some(max) if rewardAmount > max => {
                max
              }
              case _ if rewardAmount < rewardInfo.rewardMin => {
                if (rewardInfo.roundUp) rewardInfo.rewardMin else 0
              }
              case _ => rewardAmount
            }
          }
          case None => 0
        }
      }
      case None => 0
    }
  }

  /**
   * Checks if the generated security digest is equal to the one provided in the request.
   * @return true if the two digests are equal; otherwise, returns false.
   */
  def isValid: Boolean = {
    receivedVerification == generatedVerification
  }
}
