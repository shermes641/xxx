package models

import models.Waterfall.AdProviderInfo
import play.api.libs.json.{JsArray, JsString, JsObject, JsValue}

object JsonBuilder {
  val ANALYTICS_POST_URL = "http://api.keen.io/3.0/projects/mediation"
  val ANALYTICS_WRITE_KEY = "writeKey"

  /**
   * Converts a list of AdProviderInfo instances into a JSON response which is returned by the APIController.
   * @param adProviders List of AdProviderInfo instances containing ad provider names and configuration info.
   * @return JSON object with an ordered array of ad providers and their respective configuration info.
   */
  def waterfallResponse(adProviders: List[AdProviderInfo]): JsValue = {
    val configuration = JsObject(
      Seq(
        "adProviderConfigurations" -> adProviders.foldLeft(JsArray())((array, el) =>
          array ++
            JsArray(
              JsObject(
                Seq(
                  "providerName" -> JsString(el.providerName.get)
                )
              ).deepMerge((el.configurationData.get \ "requiredParams").as[JsObject]) :: Nil
            )
        )
      )
    )
    analyticsConfiguration.deepMerge(configuration)
  }

  /**
   * Creates JSON object containing configuration data for our analytics service (keen.io)
   * @return JSON object to be merged into JSON API response.
   */
  def analyticsConfiguration: JsObject = {
    JsObject(
      Seq(
        "analyticsConfiguration" -> JsObject(
          Seq(
            "analyticsPostUrl" -> JsString(ANALYTICS_POST_URL),
            "analyticsWriteKey" -> JsString(ANALYTICS_WRITE_KEY)
          )
        )
      )
    )
  }

}
