package models

import play.api.libs.Codecs
import play.api.libs.json._
import play.api.{Logger, Play}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._
import scala.concurrent.Await

/**
 * Encapsulates interactions with HyprMarketplace's reporting API.
 * @param wapID The ID of the WaterfallAdProvider to be updated.
 * @param configurationData The WaterfallAdProvider's configuration data containing required params for calling the reporting API.
 */
case class HyprMarketplaceReportingAPI(wapID: Long, configurationData: JsValue) extends ReportingAPI {
  override val BaseURL = Play.current.configuration.getString("hyprmarketplace.reporting_url").get
  override val waterfallAdProviderID = wapID

  override val queryString: List[(String, String)] = {
    val reportingParams = configurationData \ "reportingParams"
    val appID = (reportingParams \ "appID").as[String]
    val placementID = (reportingParams \ "placementID").as[String]
    val apiKey = (reportingParams \ "APIKey").as[String]
    val date = currentTime.toString(dateFormat)
    // Per Fyber's documentation (and our own implementation in Player), these key names must maintain alphabetical order before being encrypted
    val queryParams = Map("app_id" -> appID, "end_date" -> date, "placement_id" -> placementID, "start_date" -> date)
    val queryString = queryParams.flatMap((k) => List(k._1 + "=" +  k._2)).mkString("&") + "&" + apiKey
    val hashValue = Codecs.sha1(queryString)
    List("app_id" -> appID, "placement_id" -> placementID, "start_date" -> date, "end_date" -> date, "hash_value" -> hashValue)
  }

  lazy val app: Option[App] = WaterfallAdProvider.find(wapID) match {
    case Some(wap) => App.findByWaterfallID(wap.waterfallID)
    case None => None
  }

  /**
   * Calculates the new eCPM for a WatefallAdProvider.
   * @param revenue The revenue value retrieved from the reporting API.
   * @param impressions The impression count retrieved from the reporting API.
   * @return If the impression count is greater than 0, return the new eCPM value; otherwise, return 0.00.
   */
  def calculateEcpm(revenue: Double, impressions: Double): Double = {
    if(impressions > 0) { (revenue/impressions) * 1000 } else { 0.00 }
  }

  /**
   * Hack to fix Player reporting. From DPS, ecpm are inflated due to non reporting of errors, and
   * disqualification.
   */
  def getImpressions(): Option[String] = {
    app match {
      case Some(app) => {
        val adDisplayed = new KeenRequest().function("count")
          .select("ad_displayed")
          .filterWith("app_id", "eq", app.id.toString)
          .thisDays(1)

        val impressionData = for {
          displayed <- adDisplayed.collect()
        } yield (Json.parse(displayed.body) \ ("result"))

        Try(Await.result(impressionData, 50 seconds)) match {
          case Success((dis: JsNumber)) => Some(dis.as[Long].toString)
          case Success(_) => Logger.error("Failed to parse response from keen"); None
          case Failure(exception) => Logger.error("Failed to read impressions from keen"); throw exception
        }
      }
      case None => None
    }
  }

  /**
   * Receives data from the HyprMarketplace reporting API and updates the eCPM of the WaterfallAdProvider.
   * @return Future which updates the cpm field of WaterfallAdProvider.
   */
  override def updateRevenueData = {
    retrieveAPIData(queryString) map {
      case response => {
        response.status match {
          case 200 | 304 => {
            Json.parse(response.body) \ "results" match {
              case _: JsUndefined => logResponseError("Encountered a parsing error", waterfallAdProviderID, response.body)
              case results: JsArray if results.value.nonEmpty => {
                val result = results.value.last
                (result \ "global_stats" \ "revenue", getImpressions()) match {
                  case (revenue: JsValue, Some(impressions)) => {
                    updateEcpm(waterfallAdProviderID, calculateEcpm(revenue.as[String].toDouble, impressions.toDouble))
                  }
                  case (_, _) => logResponseError("stats keys were not present in JSON response", waterfallAdProviderID, response.body)
                }
              }
              case _ => logResponseError("eCPM was not updated", waterfallAdProviderID, response.body)
            }
          }
          case _ => logResponseError("Received an unsuccessful reporting API response", waterfallAdProviderID, response.body)
        }
      }
    }
  }
}
