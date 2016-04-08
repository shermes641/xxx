package models

import play.api.db.Database
import play.api.libs.json._
import play.api.libs.ws.WSClient
import scala.language.postfixOps

/**
 * Encapsulates interactions with AppLovin's reporting API.
 * @param wapID                      The ID of the WaterfallAdProvider to be updated.
 * @param configurationData          The WaterfallAdProvider's configuration data containing required params for calling the reporting API.
 * @param database                   A shared database
 * @param waterfallAdProviderService A shared instance of the WaterfallAdProviderService class
 * @param configVars                 Shared ENV configuration variables
 * @param ws                         A shared web service client
 */
case class AppLovinReportingAPI(wapID: Long,
                                configurationData: JsValue,
                                database: Database,
                                waterfallAdProviderService: WaterfallAdProviderService,
                                configVars: ConfigVars,
                                ws: WSClient) extends ReportingAPI {
  override val db = database
  override val wsClient = ws
  override val wapService = waterfallAdProviderService
  override val BaseURL = configVars.ConfigVarsReporting.applovinUrl
  override val waterfallAdProviderID = wapID

  override val queryString: List[(String, String)] = {
    val reportingParams = configurationData \ "reportingParams"
    val appName = (reportingParams \ "appName").as[String]
    val apiKey = (reportingParams \ "APIKey").as[String]
    val date = currentTime.toString(dateFormat)
    List("api_key" -> apiKey, "start" -> date, "end" -> date, "format" -> "json", "columns" -> "application,impressions,clicks,ctr,revenue,ecpm", "filter_application" -> appName)
  }
}
