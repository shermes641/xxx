package models

import play.api._
import play.api.mvc._
import play.api.Play.current
import models.Waterfall.AdProviderInfo
import play.api.libs.json._
import scala.language.implicitConversions

object JsonBuilder extends ValueToJsonHelper {
  /**
   * Converts a list of AdProviderInfo instances into a JSON response which is returned by the APIController.
   * @param adProviders List of AdProviderInfo instances containing ad provider names and configuration info.
   * @return JSON object with an ordered array of ad providers and their respective configuration info.
   */
  def waterfallResponse(adProviders: List[AdProviderInfo]): JsValue = {
    val adProviderConfigurations = {
      JsObject(
        Seq(
          "adProviderConfigurations" -> adProviders.foldLeft(JsArray())((array, el) =>
            array ++
              JsArray(
                JsObject(
                  Seq(
                    "providerName" -> el.providerName,
                    "providerID" -> el.providerID,
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
    }
    val configurationsList = List(analyticsConfiguration, virtualCurrencyConfiguration(adProviders(0)), appNameConfiguration(adProviders(0)), distributorConfiguration(adProviders(0)))
    configurationsList.foldLeft(adProviderConfigurations)((jsObject, el) =>
      jsObject.deepMerge(el)
    )
  }

  /**
   * Creates a JSON object for distributor information.
   * @param adProviderInfo An instance of the AdProviderInfo class containing distributor information.
   * @return A JsObject containing distributorID and distributorName.
   */
  def distributorConfiguration(adProviderInfo: AdProviderInfo): JsObject = {
    JsObject(
      Seq(
        "distributorName" -> adProviderInfo.distributorName,
        "distributorID" -> adProviderInfo.distributorID
      )
    )
  }

  /**
   * Creates a JSON object for app information.
   * @param adProviderInfo An instance of the AdProviderInfo class containing app information.
   * @return A JsObject containing app name and ID.
   */
  def appNameConfiguration(adProviderInfo: AdProviderInfo): JsObject = {
    JsObject(
      Seq(
        "appName" -> adProviderInfo.appName,
        "appID" -> adProviderInfo.appID
      )
    )
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
            "name" -> adProviderInfo.virtualCurrencyName,
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

/**
 * Implicit functions to convert Scala values to JSON values.
 */
trait ValueToJsonHelper {
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
   * Converts an optional String value to a JsValue.
   * @param param The original optional String value found in the adProviderInfo instance.
   * @return A JsString if a String value is found; otherwise, returns JsNull.
   */
  implicit def optionalStringToJsValue(param: Option[String]): JsValue = {
    param match {
      case Some(paramValue) => JsString(paramValue)
      case None => JsNull
    }
  }
}

/**
 * Implicit functions to convert JSON values to Scala values.
 */
trait JsonToValueHelper {
  /**
   * Converts a JsValue to an Optional Double.
   * @param param The value to be converted.
   * @return A Double if a value exists; otherwise, None.
   */
  implicit def jsValueToOptionalDouble(param: JsValue): Option[Double] = {
    param match {
      case value: JsUndefined => None
      case value: JsValue if(value.as[String] == "") => None
      case value: JsValue => Some(value.as[String].toDouble)
      case _ => None
    }
  }

  /**
   * Converts a JsValue to an Optional Long.
   * @param param The value to be converted.
   * @return A long if a value exists; otherwise, None.
   */
  implicit def jsValueToOptionalLong(param: JsValue): Option[Long] = {
    param match {
      case value: JsUndefined => None
      case value: JsValue if(value.as[String] == "") => None
      case value: JsValue => Some(value.as[String].toLong)
      case _ => None
    }
  }
}
