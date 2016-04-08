package models

import org.joda.time.format.DateTimeFormat
import play.api.db.Database
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.libs.ws.WSClient
import scala.language.postfixOps

/**
 * Encapsulates interactions with AdColony's reporting API
 * @param wapID                      The ID of the WaterfallAdProvider to be updated
 * @param configurationData          The WaterfallAdProvider's configuration data containing required params for calling the reporting API
 * @param database                   A shared database
 * @param waterfallAdProviderService A shared instance of the WaterfallAdProviderService class
 * @param configVars                 Shared ENV configuration variables
 * @param ws                         A shared web service client
 */
case class AdColonyReportingAPI(wapID: Long,
                                configurationData: JsValue,
                                database: Database,
                                waterfallAdProviderService: WaterfallAdProviderService,
                                configVars: ConfigVars,
                                ws: WSClient) extends ReportingAPI {
  override val db = database
  override val wsClient = ws
  override val wapService = waterfallAdProviderService
  override val BaseURL = configVars.ConfigVarsReporting.adcolonyUrl
  override val dateFormat = DateTimeFormat.forPattern("MMddyyyy")
  override val waterfallAdProviderID = wapID

  final val RequiredParamsKey = "requiredParams"
  final val ImpressionsKey = "impressions"

  override val queryString: List[(String, String)] = {
    val appID = (configurationData \ RequiredParamsKey \ "appID").as[String]
    val apiKey = (configurationData \ "reportingParams" \ "APIKey").as[String]
    val date = currentTime.toString(dateFormat)
    List(
      "user_credentials" -> apiKey,
      "date"             -> date,
      "app_id"           -> appID,
      "group_by"         -> "zone"
    )
  }

  // The list of zone IDs the user has entered into the HyprMediate dashboard.
  val zoneIDs: List[String] = {
    (configurationData \ RequiredParamsKey \ "zoneIds").as[List[String]]
  }

  /**
   * Finds one specific zone ID in a list of AdColony zones.
   * @param zoneID     A zone ID that the user has configured for use with HyprMediate.
   * @param apiResults The full list of zones from AdColony's API.
   * @return           List containing JsValues with zone data if the zone ID is found; otherwise, an empty list.
   */
  def getZoneData(zoneID: String, apiResults: List[JsValue]): List[JsValue] = {
    apiResults.filter(result =>
      (result \ "zone_id").toOption match {
        case Some(id: JsString) => id.as[String].trim.equalsIgnoreCase(zoneID.trim)
        case _ => false
      }
    )
  }

  /**
   * Adds up the total impressions for all relevant zones.
   * @param zones List of all zones that correspond to the zone IDs from the HyprMediate dashboard
   * @return      The total number of impressions from AdColony's reporting API
   */
  def calculateTotalImpressions(zones: List[JsValue]): Double = {
    zones.foldLeft(0L) { (total, zoneData) =>
      val zoneImpressions: Long = (zoneData \ ImpressionsKey).toOption match {
        case Some(count: JsNumber) => count.as[Long]
        case _ => 0
      }
      total + zoneImpressions
    }
  }

  /**
   * Calculates the appropriate eCPM for one or more zones.
   * @param zones            List of all zones that correspond to the zone IDs from the HyprMediate dashboard
   * @param totalImpressions The total number of impressions from AdColony's reporting API
   * @param response         The reporting API response from AdColony
   * @return                 The ecpm field when only one zone is present, and a weighted eCPM for multiple zones
   */
  def calculateNewEcpm(zones: List[JsValue],
                       totalImpressions: Double,
                       response: WSResponse): Double = {
    zones.foldLeft(0.0) { (totalWeightedEcpm, zoneData) =>
      val zoneEcpm = ((zoneData \ "ecpm").toOption, (zoneData \ ImpressionsKey).toOption) match {
        case (Some(eCPM: JsNumber), Some(impressions: JsNumber)) =>
          eCPM.as[Double] * (impressions.as[Double] / totalImpressions)
        case (_, _) =>
          logResponseDebug("Could not find eCPM or impression data", waterfallAdProviderID, response)
          0.0
      }
      totalWeightedEcpm + zoneEcpm
    }
  }

  /**
   * Parses response from reporting API and calls updateEcpm if necessary.
   * @param waterfallAdProviderID The ID of the WaterfallAdProvider to be updated.
   * @param response              The response from the reporting API.
   * @return                      If the update is successful, returns the new generation number; otherwise, None.
   */
  override def parseResponse(waterfallAdProviderID: Long, response: WSResponse): Option[Long] = {
    response.status match {
      case 200 | 304 =>
        (Json.parse(response.body) \ "results").toOption match {
          case Some(results: JsArray) if results.as[List[JsValue]].nonEmpty =>
            val resultZones = results.as[JsArray].as[List[JsValue]]
            val relevantZones: List[JsValue] = zoneIDs.flatMap(getZoneData(_, resultZones))
            val totalImpressions: Double = calculateTotalImpressions(relevantZones)

            if(totalImpressions > 0) {
              val newEcpm: Double = calculateNewEcpm(relevantZones, totalImpressions, response)
              updateEcpm(waterfallAdProviderID, newEcpm)
            } else {
              val debugMessage = "eCPM was not updated because we couldn't find enough impressions for each zone(s)" +
              " Number of Zones: " + relevantZones.length + " Number of total impressions: " + totalImpressions
              logResponseDebug(debugMessage, waterfallAdProviderID, response)
            }
          case _ =>
            logResponseDebug("eCPM was not updated", waterfallAdProviderID, response)
        }
      case _ =>
        logResponseError("Received an unsuccessful reporting API response", waterfallAdProviderID, response)
    }
  }
}
