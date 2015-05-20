package models

import play.api.libs.json._
import play.api.Play
import play.api.libs.ws.WSResponse
import scala.language.postfixOps

/**
 * Encapsulates interactions with Vungle's reporting API.
 * @param wapID The ID of the WaterfallAdProvider to be updated.
 * @param configurationData The WaterfallAdProvider's configuration data containing required params for calling the reporting API.
 */
case class VungleReportingAPI(wapID: Long, configurationData: JsValue) extends ReportingAPI {
  val reportingParams = configurationData \ "reportingParams"
  val apiID = (reportingParams \ "APIID").as[String] // This value identifies the specific app for which to get eCPM data
  override val BaseURL = Play.current.configuration.getString("vungle.reporting_url").get + "/" + apiID
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
        Json.parse(response.body) \ "eCPM" match {
          case _: JsUndefined => None
          case eCPM: JsNumber => {
            updateEcpm(waterfallAdProviderID, eCPM.as[Double])
          }
          case _ => None
        }
      }
      case _ => None
    }
  }
}
