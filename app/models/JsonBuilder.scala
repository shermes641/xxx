package models

import play.api._
import play.api.mvc._
import play.api.Play.current
import models.Waterfall.AdProviderInfo
import play.api.libs.json.{JsArray, JsString, JsObject, JsValue}

object JsonBuilder {
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
            "analyticsPostUrl" -> JsString("https://api.keen.io/3.0/projects/" + Play.current.configuration.getString("keen.project").get),
            "analyticsWriteKey" -> JsString(Play.current.configuration.getString("keen.writeKey").get)
          )
        )
      )
    )
  }

}
