package models

import play.api.db.Database
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import scala.language.postfixOps

/**
 * Encapsulates interactions with Vungle's reporting API
 * @param wapID                      The ID of the WaterfallAdProvider to be updated
 * @param configurationData          The WaterfallAdProvider's configuration data containing required params for calling the reporting API
 * @param database                   A shared database
 * @param waterfallAdProviderService A shared instance of the WaterfallAdProviderService class
 * @param configVars                 Shared ENV configuration variables
 * @param ws                         A shared web service client
 */
case class VungleReportingAPI(wapID: Long,
                              configurationData: JsValue,
                              database: Database,
                              waterfallAdProviderService: WaterfallAdProviderService,
                              configVars: ConfigVars,
                              ws: WSClient) extends ReportingAPI {
  override val db = database
  override val wsClient = ws
  override val wapService = waterfallAdProviderService
  val reportingParams = configurationData \ "reportingParams"
  val apiID = (reportingParams \ "APIID").as[String] // This value identifies the specific app for which to get eCPM data
  override val BaseURL = configVars.ConfigVarsReporting.vungleUrl + "/" + apiID
  override val waterfallAdProviderID = wapID

  override val queryString: List[(String, String)] = {
    val apiKey = (reportingParams \ "APIKey").as[String] // This value identifies the publisher for Vungle's reporting API
    val date = currentTime.toString(dateFormat)
    List("key" -> apiKey, "date" -> date)
  }

  /**
   * Parses response from reporting API and calls updateEcpm if necessary.
   * @param waterfallAdProviderID The ID of the WaterfallAdProvider to be updated.
   * @param response The response from the reporting API.
   * @return If the update is successful, returns the new generation number; otherwise, None.
   */
  override def parseResponse(waterfallAdProviderID: Long, response: WSResponse) = {
    response.status match {
      case 200 | 304 => {
        val results = Json.parse(response.body)
        if(results.as[JsArray].as[List[JsValue]].nonEmpty) {
          (results(0) \ "eCPM").toOption match {
            case None =>
              logResponseError("eCPM key was not present", waterfallAdProviderID, response)
            case Some(eCPM: JsNumber) =>
              updateEcpm(waterfallAdProviderID, eCPM.as[Double])
            case _ =>
              logResponseError("eCPM was not updated", waterfallAdProviderID, response)
          }
        } else {
          logResponseDebug("There were no events returned", waterfallAdProviderID, response)
        }
      }
      case _ =>
        logResponseError("Received an unsuccessful reporting API response", waterfallAdProviderID, response)
    }
  }
}
