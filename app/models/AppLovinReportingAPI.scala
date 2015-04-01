package models

import play.api.libs.json._
import play.api.Play
import scala.language.postfixOps

/**
 * Encapsulates interactions with AppLovin's reporting API.
 * @param wapID The ID of the WaterfallAdProvider to be updated.
 * @param configurationData The WaterfallAdProvider's configuration data containing required params for calling the reporting API.
 */
case class AppLovinReportingAPI(wapID: Long, configurationData: JsValue) extends ReportingAPI {
  override val BaseURL = Play.current.configuration.getString("applovin.reporting_url").get
  override val waterfallAdProviderID = wapID

  override val queryString: List[(String, String)] = {
    val reportingParams = configurationData \ "reportingParams"
    val appName = (reportingParams \ "appName").as[String]
    val apiKey = (reportingParams \ "APIKey").as[String]
    val date = currentTime.toString(dateFormat)
    List("api_key" -> apiKey, "start" -> date, "end" -> date, "format" -> "json", "columns" -> "application,impressions,clicks,ctr,revenue,ecpm", "filter_application" -> appName)
  }
}
