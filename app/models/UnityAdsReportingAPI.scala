package models

import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.Logger
import play.api.db.Database
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

/**
  * Encapsulates interactions with Unity Ad's reporting API.
  *
  * @param wapID                      The ID of the WaterfallAdProvider to be updated
  * @param configurationData          The WaterfallAdProvider's configuration data containing required params for calling the reporting API
  * @param database                   The default database
  * @param waterfallAdProviderService Encapsulates all WaterfallAdProvider functions
  * @param configVars                 Shared ENV configuration variables
  * @param ws                         A shared web service client
  */
case class UnityAdsReportingAPI(wapID: Long,
                                configurationData: JsValue,
                                database: Database,
                                waterfallAdProviderService: WaterfallAdProviderService,
                                configVars: ConfigVars,
                                ws: WSClient) extends ReportingAPI {
  override val db = database
  override val wsClient = ws
  override val wapService = waterfallAdProviderService
  val BaseURL = configVars.ConfigVarsReporting.unityadsUrl
  val waterfallAdProviderID = wapID

  override val dateFormat = DateTimeFormat.forPattern("YYYY-MM-d")
  val startDt = new DateTime(DateTimeZone.UTC)
  val endDate = startDt.plusDays(1).toString(dateFormat)
  val startDate = startDt.toString(dateFormat)

  override val queryString: List[(String, String)] = List(
    "apikey" -> (configurationData \ Constants.AdProviderConfig.ReportingParams \ Constants.AdProviderConfig.APIKey).as[String],
    "splitBy" -> "none",
    "start" -> startDate,
    "end" -> endDate,
    "scale" -> "day",
    "sourceIds" -> (configurationData \ Constants.AdProviderConfig.RequiredParams \ Constants.UnityAds.GameID).as[String])

  /**
    * Update eCPM and generation number based on reporting data
    *
    * @param url     Reporting url, allows tests to specify non default value
    * @param qs      Query string, allows tests to specify non default value
    * @param timeOut Set request time out, used in testing
    * @return        Future(true) on success
    */
  def unityUpdateRevenueData(url: String = BaseURL,
                             qs: List[(String, String)] = queryString,
                             timeOut: Int = Constants.DefaultReportingTimeoutMs): Future[Boolean] = {
    wsClient.url(url).withRequestTimeout(timeOut).withQueryString(qs: _*).get.map { response =>
      response.status match {
        case 200 | 304 =>
          val bodyArray = response.body.replaceAll("\"", "").split("\n").map(_.trim)
          // Should have at least 1 day of data
          bodyArray.length match {
            case len if len > 1 =>
              // Data is in csv form, first index is the header, then each index is more recent data
              // Always use the most recent day of data
              val reportingData = (bodyArray(0).split(',') zip bodyArray(len - 1).split(",")).toMap
              reportingData.contains(Constants.UnityAds.ReportingRevenue) && reportingData.contains(Constants.UnityAds.ReportingStarted) match {
                case true =>
                  val eCPM = calculateEcpm(reportingData.getOrElse(Constants.UnityAds.ReportingRevenue, "0.0").toDouble,
                    reportingData.getOrElse(Constants.UnityAds.ReportingStarted, "1.0").toDouble)
                  updateEcpm(waterfallAdProviderID, eCPM) match {
                    case Some(generationNumber) =>
                      Logger.debug(s"Unity Ads Server to server callback to Distributor's servers updated eCPM: $eCPM , generation number: $generationNumber")
                      true

                    case _ =>
                      Logger.error(s"Unity Ads Server to server callback unable to save eCPM: $eCPM \n Waterfall ID: $waterfallAdProviderID")
                      false
                  }

                case _ =>
                  Logger.error(s"Unity Ads Server to server callback does not contain started and revenue: \n Waterfall ID: $waterfallAdProviderID \n$reportingData")
                  false
              }

            case len if len == 1 =>
              logResponseDebug("Not updating eCPM because no events were present for Unity Ads", wapID, response)
              false

            case _ =>
              response.body.contains("error") match {
                case true =>
                  logResponseError (s"Unity Ads responded with an error: ${response.body}", waterfallAdProviderID, response)
                  false

                case _ =>
                  logResponseError ("Unity Ads Encountered a parsing error", waterfallAdProviderID, response)
                false
              }
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
