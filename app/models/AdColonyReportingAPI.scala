package models

import org.joda.time.format.DateTimeFormat
import play.api.libs.json._
import play.api.Play
import scala.language.postfixOps

/**
 * Encapsulates interactions with AdColony's reporting API.
 * @param wapID The ID of the WaterfallAdProvider to be updated.
 * @param configurationData The WaterfallAdProvider's configuration data containing required params for calling the reporting API.
 */
case class AdColonyReportingAPI(wapID: Long, configurationData: JsValue) extends ReportingAPI {
  override val BaseURL = Play.current.configuration.getString("adcolony.reporting_url").get
  override val dateFormat = DateTimeFormat.forPattern("MMddyyyy")
  override val waterfallAdProviderID = wapID

  override val queryString: List[(String, String)] = {
    val appID = (configurationData \ "requiredParams" \ "appID").as[String]
    val apiKey = (configurationData \ "reportingParams" \ "APIKey").as[String]
    val date = currentTime.toString(dateFormat)
    List("user_credentials" -> apiKey, "date" -> date, "app_id" -> appID)
  }
}
