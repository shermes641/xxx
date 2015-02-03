package resources

import play.api.libs.json.{JsString, JsObject}

/**
 * Configuration data for setting up WaterfallAdProviders
 */
trait JsonTesting {
  val configurationParams = List("key1", "key2")
  val configurationValues = List("value1", "value2")
  def paramJson(paramKey: Int) = "{\"key\":\"" + configurationParams(paramKey) + "\", \"displayKey\": \"" + configurationParams(paramKey) + "\", \"value\":\"\", \"dataType\": \"String\", \"description\": \"some description\", \"refreshOnAppRestart\": \"true\"}"
  val configurationData = "{\"requiredParams\": [" + paramJson(0) + ", " + paramJson(1) + "], \"reportingParams\": [], \"callbackParams\": []}"
  val configurationJson = JsObject(Seq("requiredParams" -> JsObject(Seq(configurationParams(0) -> JsString(configurationValues(0)), configurationParams(1) -> JsString(configurationValues(1))))))
}
