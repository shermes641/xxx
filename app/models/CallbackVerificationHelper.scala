package models

import play.api.libs.json.{JsUndefined, JsValue}

// Helper functions included in Callback models.
trait CallbackVerificationHelper {
  val currencyAmount: Int
  val receivedVerification: String
  val adProviderName: String
  val token: String
  lazy val waterfallAdProviderInfo = WaterfallAdProvider.findByAdProvider(token, adProviderName)
  def generatedVerification: String

  /**
   * Pulls value from waterfall ad provider configuration data.
   * @param jsonKey The key used to retrieve a value from the JSON configuration data.
   * @return The value stored in the configuration data if one exists; otherwise, returns an empty string.
   */
  def secretKey(jsonKey: String): String = {
    waterfallAdProviderInfo match {
      case Some(wap) => {
        wap.configurationData match {
          case _:JsUndefined => ""
          case configData: JsValue => (configData \ "callbackParams" \ jsonKey).as[String]
          case _ => ""
        }
      }
      case None => ""
    }
  }

  /**
   * Calculates the payout information for a completion
   * @return A Double value correlating to a dollar amount.
   */
  def payout: Option[Double] = {
    waterfallAdProviderInfo match {
      case Some(wap) => {
        wap.cpm match {
          case Some(cpm: Double) => {
            Some(BigDecimal(cpm / 1000).setScale(2, BigDecimal.RoundingMode.FLOOR).toDouble)
          }
          case _ => None
        }
      }
      case _ => None
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
