package models

import java.text.SimpleDateFormat
import java.util.Calendar
import play.api.db.DB
import play.api.libs.Codecs
import play.api.libs.json._
import play.api.libs.ws._
import play.api.Play
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

case class HyprMarketplaceAPI(waterfallAdProviderID: Long, configurationData: JsValue) {
  val HYPR_MARKETPLACE_BASE_URL = Play.current.configuration.getString("hyprmarketplace.reporting_url").get
  val currentTimeFormat = new SimpleDateFormat("yyyy-MM-dd")
  val calendar = Calendar.getInstance

  /**
   * Receives data from HyprMarketplace API and updates WaterfallAdProvider
   * @return Future which updates the cpm field of WaterfallAdProvider
   */
  def updateRevenueData = {
    retrieveHyprMarketplaceData(configurationData) map {
      case response => {
        Json.parse(response.body) \ "results" match {
          case _:JsUndefined => None
          case results if(response.status == 200 || response.status == 304) => {
            val result = results.as[JsArray].as[List[JsValue]].last
            result \ "global_stats" match {
              case _:JsUndefined => None
              case stats => {
                val revenueData = new RevenueData(stats)
                DB.withTransaction { implicit connection =>
                  try {
                    WaterfallAdProvider.updateEcpm(waterfallAdProviderID, revenueData.eCPM)
                  } catch {
                    case error: org.postgresql.util.PSQLException => {
                      connection.rollback()
                      Some(false)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * Calls HyprMarketplace reporting API to get today's most recent revenue and impression stats.
   * @param configurationData JSON field in waterfall_ad_providers table containing the required HyprMarketplace API keys.
   * @return A future object containing the HyprMarketplace API response.
   */
  def retrieveHyprMarketplaceData(configurationData: JsValue): Future[WSResponse] = {
    val reportingParams = configurationData \ "reportingParams"
    val appID = (reportingParams \ "appID").as[String]
    val placementID = (reportingParams \ "placementID").as[String]
    val apiKey = (reportingParams \ "APIKey").as[String]
    val endDate = currentTimeFormat.format(calendar.getTime)
    calendar.add(Calendar.DAY_OF_YEAR, -1)
    val startDate = currentTimeFormat.format(calendar.getTime)
    // Per Fyber's documentation, these key names must maintain alphabetical order before being encrypted
    val queryParams = Map("app_id" -> appID, "end_date" -> endDate, "placement_id" -> placementID, "start_date" -> startDate)
    val queryString = queryParams.flatMap((k) => List(k._1 + "=" +  k._2)).mkString("&") + "&" + apiKey
    val hashValue = Codecs.sha1(queryString)
    WS.url(HYPR_MARKETPLACE_BASE_URL).withQueryString("app_id" -> appID, "placement_id" -> placementID, "start_date" -> startDate, "end_date" -> endDate, "hash_value" -> hashValue).get
  }
}

/**
 * Encapsulates revenue data from HyprMarketplace's reporting API to be used in a future calculation.
 * @param stats JSON containing revenue and impression count from HyprMarketplace's API.
 */
case class RevenueData(stats: JsValue) {
  val revenue: Double = (stats \ "revenue").as[String].toDouble
  val impressionCount: Long = (stats \ "impressions").as[String].toLong
  lazy val eCPM = {
    BigDecimal((revenue/impressionCount) * 1000).setScale(2, BigDecimal.RoundingMode.FLOOR).toDouble
  }
}
