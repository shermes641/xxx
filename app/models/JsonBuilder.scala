package models

import play.api._
import play.api.mvc._
import play.api.Play.current
import models.Waterfall.AdProviderInfo
import play.api.libs.functional.syntax._
import play.api.libs.json._
import scala.language.implicitConversions

object JsonBuilder extends ValueToJsonHelper with RequiredParamJsReader {
  val LogFullConfig = true
  val DefaultCanShowAdTimeout = 10 // This value represents seconds.
  val DefaultRewardTimeout = 10 // This value represents seconds.

  /**
   * Converts a list of AdProviderInfo instances into a JSON response which is returned by the APIController.
   * @param adProviderList List of AdProviderInfo instances containing ad provider names and configuration info.
   * @param configInfo All other configuration info not related to specific AdProviders.
   * @return JSON object with an ordered array of ad providers and their respective configuration info.
   */
  def appConfigResponseV1(adProviderList: List[AdProviderInfo], configInfo: AdProviderInfo): JsValue = {
    val adProviderConfigurations = {
      JsObject(
        Seq(
          "adProviderConfigurations" -> adProviderList.foldLeft(JsArray())((array, el) =>
            array ++
              JsArray(
                JsObject(
                  Seq(
                    "providerName" -> el.providerName,
                    "providerID" -> el.providerID,
                    "eCPM" -> (el.cpm match {
                      case Some(eCPM) => JsNumber(eCPM)
                      case None => JsNull
                    }),
                    "sdkBlacklistRegex" -> el.sdkBlacklistRegex
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
        )
      )
    }
    val configurationsList = List(analyticsConfiguration, errorReportingConfiguration, virtualCurrencyConfiguration(configInfo), appNameConfiguration(configInfo),
      distributorConfiguration(configInfo), sdkConfiguration(configInfo.appConfigRefreshInterval), testModeConfiguration, pausedConfiguration(configInfo), timeoutConfigurations)
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
        "logFullConfig" -> JsBoolean(LogFullConfig)
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
   * Creates JSON object containing configuration data for our error report to our analytics service (keen.io)
   * @return JSON object to be merged into JSON API response.
   */
  def errorReportingConfiguration: JsObject = {
    JsObject(
      Seq(
        "errorReportingConfiguration" -> JsObject(
          Seq(
            "errorReportingPostUrl" -> JsString("https://api.keen.io/3.0/projects/" + Play.current.configuration.getString("keen.errorReportingProject").get),
            "errorReportingWriteKey" -> JsString(Play.current.configuration.getString("keen.errorReportingWriteKey").get)
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

  /**
   * Creates a JSON object for paused information.
   * @param adProviderInfo An instance of the AdProviderInfo class containing app information.
   * @return A JsObject containing paused status.
   */
  def pausedConfiguration(adProviderInfo: AdProviderInfo): JsObject = {
    JsObject(
      Seq(
        "paused" -> JsBoolean(adProviderInfo.paused)
      )
    )
  }

  /**
   * Creates JSON object containing the default time to wait for a "can show ad" response from each ad provider in the SDK.
   * @return JSON object to be merged in to the JSON API response.
   */
  def timeoutConfigurations: JsObject = {
    JsObject(
      Seq(
        "canShowAdTimeout" -> JsNumber(DefaultCanShowAdTimeout),
        "rewardTimeout" -> JsNumber(DefaultRewardTimeout)
      )
    )
  }

  /**
   * The top level keys for WaterfallAdProvider configurationData JSON.
   */
  val waterfallAdProviderParamList = List("requiredParams", "callbackParams", "reportingParams")

  /**
   * Constructs WaterfallAdProvider JSON data to either be stored in the database or displayed in the UI.
   * @param jsonAssemblyFunction A function that determines how the JSON will be structured (for database or UI).
   * @param config The WaterfallAdProvider configurationData either being passed in from UI or pulled from the database.
   * @return A JSON object to be displayed in the UI or saved to the configuration_data column of the waterfall_ad_providers table.
   */
  def buildWAPParams(jsonAssemblyFunction: List[RequiredParam] => JsValue, config: Any): JsObject = {
    waterfallAdProviderParamList.foldLeft(JsObject(Seq()))((outputJson, params) => {
      outputJson.deepMerge(
        JsObject(
          Seq(
            params -> jsonAssemblyFunction(
              config match {
                case configJson: JsValue => (configJson \ params).as[List[RequiredParam]]
                case configData: WaterfallAdProviderConfig => configData.mappedFields(params)
                case _ => List()
              }
            )
          )
        )
      )
    })
  }

  /**
   * Assembles WaterfallAdProvider params in a standard JSON format to be displayed in the UI.
   * @param list The list of RequiredParams to be stored in JSON.
   * @return A JsArray containing RequiredParam JSON objects.
   */
  def buildWAPParamsForUI(list: List[RequiredParam]): JsArray = {
    list.foldLeft(JsArray(Seq()))((array, param) => array ++ JsArray(Seq(param)))
  }

  /**
   * Assembles WaterfallAdProvider params in a standard JSON format to be stored in the configuration_data field of the waterfall_ad_providers table.
   * @param list The list of RequiredParams to be stored in JSON.
   * @return A JsObject containing WaterfallAdProvider params.
   */
  def buildWAPParamsForDB(list: List[RequiredParam]): JsObject = {
    list.foldLeft(JsObject(Seq()))((jsonObject, param) => {
      jsonObject.deepMerge(
        JsObject(
          Seq(
            param.key.get -> {
              param.dataType match {
                case Some(dataTypeVal) if(dataTypeVal == "Array") => {
                  param.value match {
                    case Some(arrayElements) => {
                      arrayElements.split(",").foldLeft(JsArray(Seq()))((array, element) => array :+ JsString(element.trim))
                    }
                    case _ => JsArray(Seq())
                  }
                }
                case _ => {
                  JsString(param.value.getOrElse(""))
                }
              }
            }
          )
        )
      )
    })
  }

  /**
   * Converts RequiredParam class to JSON object.
   * @param param The an instance of the RequiredParam class to be converted.
   * @return JSON object to be used in the edit action.
   */
  implicit def requiredParamWrites(param: RequiredParam): JsObject = {
    JsObject(
      Seq(
        "displayKey" -> JsString(param.displayKey.getOrElse("")),
        "key" -> JsString(param.key.getOrElse("")),
        "dataType" -> JsString(param.dataType.getOrElse("")),
        "description" -> JsString(param.description.getOrElse("")),
        "value" -> JsString(param.value.getOrElse("")),
        "refreshOnAppRestart" -> JsBoolean(param.refreshOnAppRestart),
        "minLength" -> JsNumber(param.minLength)
      )
    )
  }
}

/**
 * Implicit value to convert JSON to an instance of RequiredParam.
 */
trait RequiredParamJsReader {
  /**
   * Converts RequiredParam class instance to JSON
   */
  implicit val requiredParamReads: Reads[RequiredParam] = (
    (JsPath \ "displayKey").readNullable[String] and
      (JsPath \ "key").readNullable[String] and
      (JsPath \ "dataType").readNullable[String] and
      (JsPath \ "description").readNullable[String] and
      (JsPath \ "value").readNullable[String] and
      (JsPath \ "refreshOnAppRestart").read[Boolean] and
      (JsPath \ "minLength").read[Long].orElse(Reads.pure(0))
    )(RequiredParam.apply _)
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
