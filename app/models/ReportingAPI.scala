package models

import com.github.nscala_time.time.Imports._
import play.api.db.DB
import play.api.libs.json._
import play.api.libs.ws._
import play.api.Logger
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

/**
 * Encapsulates the common functionality for calling various reporting APIs.
 * All reporting API classes extend this class.
 */
abstract class ReportingAPI {
  val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
  val currentTime = new DateTime(DateTimeZone.UTC)
  val BaseURL: String
  val waterfallAdProviderID: Long
  val queryString: List[(String, String)]

  /**
   * Make GET request to the reporting API of the Ad Provider.
   * @param queryString Params to be included in the call to the reporting API.
   * @return Future HTTP response containing the results of the API call.
   */
  def retrieveAPIData(queryString: List[(String, String)]): Future[WSResponse] = {
    WS.url(BaseURL).withQueryString(queryString: _*).get()
  }

  /**
   * Receives data from the reporting API and updates the eCPM of the WaterfallAdProvider.
   * @return Future which updates the cpm field of WaterfallAdProvider.
   */
  def updateRevenueData = {
    retrieveAPIData(queryString) map {
      case response => parseResponse(waterfallAdProviderID, response)
    }
  }

  /**
   * Updates the eCPM for a WaterfallAdProvider.
   * @param waterfallAdProviderID The ID of the WaterfallAdProvider to be updated.
   * @param eCPM The new eCPM value.
   * @return If the update is successful, returns the new generation number; otherwise, None.
   */
  def updateEcpm(waterfallAdProviderID: Long, eCPM: Double) = {
    DB.withTransaction { implicit connection =>
      try {
        WaterfallAdProvider.updateEcpm(waterfallAdProviderID, eCPM)
      } catch {
        case error: org.postgresql.util.PSQLException => {
          connection.rollback()
          None
        }
      }
    }
  }

  /**
   * Parses response from reporting API and calls updateEcpm if necessary.
   * @param waterfallAdProviderID The ID of the WaterfallAdProvider to be updated.
   * @param response The response from the reporting API.
   * @return If the update is successful, returns the new generation number; otherwise, None.
   */
  def parseResponse(waterfallAdProviderID: Long, response: WSResponse) = {
    response.status match {
      case 200 | 304 => {
        Json.parse(response.body) \ "results" match {
          case _: JsUndefined => logResponseError("Encountered a parsing error", waterfallAdProviderID, response.body)
          case results: JsValue if(results.as[JsArray].as[List[JsValue]].size > 0) => {
            val result = results.as[JsArray].as[List[JsValue]].last
            result \ "ecpm" match {
              case eCPM: JsString => {
                updateEcpm(waterfallAdProviderID, eCPM.as[String].toDouble)
              }
              case eCPM: JsNumber => {
                updateEcpm(waterfallAdProviderID, eCPM.as[Double])
              }
              case _ => logResponseError("ecpm key was not present in JSON response", waterfallAdProviderID, response.body)
            }
          }
          case _ => logResponseError("eCPM was not updated", waterfallAdProviderID, response.body)
        }
      }
      case _ => logResponseError("Received an unsuccessful reporting API response", waterfallAdProviderID, response.body)
    }
  }

  /**
   * Logs reporting errors in Papertrail
   * @param errorMessage The message to be logged
   * @param waterfallAdProviderID The ID of the WaterfallAdProvider that was supposed to be updated
   * @param responseBody The JSON response from the reporting API
   * @return None
   */
  def logResponseError(errorMessage: String, waterfallAdProviderID: Long, responseBody: String) = {
    Logger.error(errorMessage + " URL: " + BaseURL + " WaterfallAdProvider ID: " + waterfallAdProviderID + " Response Body: " + responseBody)
    None
  }
}