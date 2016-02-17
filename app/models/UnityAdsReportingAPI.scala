package models

import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.{Logger, Play}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

/**
  * Encapsulates interactions with Vungle's reporting API.
  *
  * @param wapID             The ID of the WaterfallAdProvider to be updated.
  * @param configurationData The WaterfallAdProvider's configuration data containing required params for calling the reporting API.
  */
case class UnityAdsReportingAPI(wapID: Long, configurationData: JsValue) extends ReportingAPI {
  val BaseURL = Play.current.configuration.getString("unityads.reporting_url").getOrElse("should never happen")
  val waterfallAdProviderID = wapID

  override val dateFormat = DateTimeFormat.forPattern("YYYY-MM-d")
  val startDt = new DateTime(DateTimeZone.UTC)
  val endDate = startDt.plusDays(1).toString(dateFormat)
  val startDate = startDt.toString(dateFormat)

  override val queryString: List[(String, String)] = List(
    "apikey" -> (configurationData \ "reportingParams" \ "APIKey").as[String],
    "splitBy" -> "none",
    "start" -> startDate,
    "end" -> endDate,
    "scale" -> "day",
    "sourceIds" -> (configurationData \ "requiredParams" \ "appID").as[String]) //"1038305")

  /**
    *
    *
    * @param url Reporting url, allows tests to specify non default value
    * @param qs  Query string, allows tests to specify non default value
    * @return Future(true) on success
    */
  def unityUpdateRevenueData(url: String = BaseURL,
                             qs: List[(String, String)] = queryString): Future[Boolean] = {
    WS.url(url).withRequestTimeout(10000).withQueryString(qs: _*).get.map { response =>
      response.status match {
        case 200 | 304 =>
          val bodyArray = response.body.replaceAll("\"", "").split("\n").map(_.trim)
          // Should have at least 1 day of data
          bodyArray.length match {
            case len if len > 1 =>
              // Data is in csv form, first index is the header, then each index is more recent data
              // Always use the most recent day of data
              val reportingData = (bodyArray(0).split(',') zip bodyArray(len - 1).split(",")).toMap
              reportingData.contains("views") && reportingData.contains("revenue") match {
                case true =>
                  val eCPM = calculateEcpm(reportingData.getOrElse("revenue", "0.0").toDouble, reportingData.getOrElse("views", "1.0").toDouble)
                  updateEcpm(waterfallAdProviderID, eCPM) match {
                    case Some(generationNumber) =>
                      Logger.debug(s"Unity Ads Server to server callback to Distributor's servers updated eCPM: $eCPM , generation number: $generationNumber")
                      true

                    case _ =>
                      Logger.error(s"Unity Ads Server to server callback unable to save eCPM: $eCPM \n Waterfall ID: $waterfallAdProviderID")
                      false
                  }

                case _ =>
                  Logger.error(s"Unity Ads Server to server callback does not contain views and revenue: \n Waterfall ID: $waterfallAdProviderID \n$reportingData")
                  false
              }

            case _ =>
              logResponseError("Unity Ads Encountered a parsing error", waterfallAdProviderID, response)
              false
          }

        case status =>
          Logger.error(s"Unity Ads Server to server callback to Distributor's servers returned a status code of $status for URL: $url")
          false
      }
    } recoverWith {
      case exception =>
        Logger.error(s"Unity Ads Server to server callback to Distributor's servers generated exception:\nURL: $url \n Waterfall ID: $waterfallAdProviderID \nException: $exception")
        Future(false)
    }
  }
}
