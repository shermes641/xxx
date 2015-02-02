package models

import play.api._
import play.api.mvc._
import play.api.Play.current
import models.Waterfall.AdProviderInfo
import play.api.libs.json._
import scala.language.implicitConversions

object JsonBuilder extends ValueToJsonHelper {
  val LOG_FULL_CONFIG = true

  /**
   * Converts a list of AdProviderInfo instances into a JSON response which is returned by the APIController.
   * @param adProviderList List of AdProviderInfo instances containing ad provider names and configuration info.
   * @param configInfo All other configuration info not related to specific AdProviders.
   * @return JSON object with an ordered array of ad providers and their respective configuration info.
   */
  def appConfigResponseV1(adProviderList: List[AdProviderInfo], configInfo: AdProviderInfo): JsValue = {
    def buildAdProviderConfigurations: JsValue = {
      if(adProviderList.size == 0) {
        JsArray(Seq())
      } else {
        adProviderList.foldLeft(JsArray())((array, el) =>
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
              ).deepMerge(
                  el.configurationData match {
                    case Some(data) => {
                      (data \ "requiredParams") match {
                        case _: JsUndefined => JsObject(Seq())
                        case json: JsValue => json.as[JsObject]
                      }
                    }
                    case None => JsObject(Seq())
                  }
                ) :: Nil
            )
        )
      }
    }
    val adProviderConfigurations = {
      JsObject(
        Seq(
          "adProviderConfigurations" -> buildAdProviderConfigurations
        )
      )
    }
    val configurationsList = List(analyticsConfiguration, virtualCurrencyConfiguration(configInfo), appNameConfiguration(configInfo),
      distributorConfiguration(configInfo), sdkConfiguration(configInfo.appConfigRefreshInterval), testModeConfiguration)
    configurationsList.foldLeft(adProviderConfigurations)((jsObject, el) =>
      jsObject.deepMerge(el)
    )
  }

  /**
   * Creates a JSON object for SDK configuration info.
   * @param appConfigRefreshInterval Determines the TTL for AppConfigs used by the SDK.
   * @return A JsObject containing SDK configuration info.
   */
  def sdkConfiguration(appConfigRefreshInterval: Long): JsObject = {
    JsObject(
      Seq(
        "appConfigRefreshInterval" -> JsNumber(appConfigRefreshInterval),
        "logFullConfig" -> JsBoolean(LOG_FULL_CONFIG)
      )
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
            "rewardMin" -> JsNumber(adProviderInfo.rewardMin),
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

  /**
   * Creates JSON object containing indication whether the app is in test mode or not.
   * @return JSON object to be merged into JSON API response.
   */
  def testModeConfiguration: JsObject = {
    JsObject(
      Seq(
        "testMode" -> JsBoolean(false)
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
