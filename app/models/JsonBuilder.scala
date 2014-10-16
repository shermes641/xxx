package models

import play.api._
import play.api.mvc._
import play.api.Play.current
import models.Waterfall.AdProviderInfo
import play.api.libs.json._
import scala.language.implicitConversions

object JsonBuilder {
  /**
   * Converts a list of AdProviderInfo instances into a JSON response which is returned by the APIController.
   * @param adProviders List of AdProviderInfo instances containing ad provider names and configuration info.
   * @return JSON object with an ordered array of ad providers and their respective configuration info.
   */
  def waterfallResponse(adProviders: List[AdProviderInfo]): JsValue = {
    val configuration = analyticsConfiguration.deepMerge(
      JsObject(
        Seq(
          "adProviderConfigurations" -> adProviders.foldLeft(JsArray())((array, el) =>
            array ++
              JsArray(
                JsObject(
                  Seq(
                    "providerName" -> JsString(el.providerName.get),
                    "eCPM" -> (el.cpm match {
                      case Some(eCPM) => JsNumber(eCPM)
                      case None => JsNull
                    })
                  )
                ).deepMerge((el.configurationData.get \ "requiredParams").as[JsObject]) :: Nil
              )
          )
        )
      )
    )
    configuration.deepMerge(virtualCurrencyConfiguration(adProviders(0)))
  }

  /**
   * Converts an optional Long value to a JsValue in virtualCurrencyConfiguration.
   * @param param The original optional Long value found in the adProviderInfo instance.
   * @return A JsNumber if a Long value is found; otherwise, returns JsNull.
   */
  implicit def optionalLongToJsValue(param: Option[Long]): JsValue = {
    param match {
      case Some(paramValue) => JsNumber(paramValue)
      case None => JsNull
    }
  }

  /**
   * Creates a JSON object for virtual currency information
   * @param adProviderInfo An instance of the AdProviderInfo class containing virtual currency information.
   * @return A JsObject with virtual currency information.
   */
  def virtualCurrencyConfiguration(adProviderInfo: AdProviderInfo): JsObject = {
    JsObject(
      Seq(
        "virtualCurrency" -> JsObject(
          Seq(
            "exchangeRate" -> adProviderInfo.exchangeRate,
            "roundUp" -> JsBoolean(adProviderInfo.roundUp.get),
            "rewardMin" -> adProviderInfo.rewardMin,
            "rewardMax" -> adProviderInfo.rewardMax
          )
        )
      )
    )
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
