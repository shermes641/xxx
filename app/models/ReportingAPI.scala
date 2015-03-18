package models

import java.text.SimpleDateFormat
import java.util.Calendar
import play.api.db.DB
import play.api.libs.json._
import play.api.libs.ws._
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

/**
 * Encapsulates the common functionality for calling various reporting APIs.
 * All reporting API classes extend this class.
 */
abstract class ReportingAPI {
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
  val calendar = Calendar.getInstance
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
          case _: JsUndefined => None
          case results: JsValue if(results.as[JsArray].as[List[JsValue]].size > 0) => {
            val result = results.as[JsArray].as[List[JsValue]].last
            result \ "ecpm" match {
              case eCPM: JsString => {
                updateEcpm(waterfallAdProviderID, eCPM.as[String].toDouble)
              }
              case eCPM: JsNumber => {
                updateEcpm(waterfallAdProviderID, eCPM.as[Double])
              }
              case _ => None
            }
          }
          case _ => None
        }
      }
      case _ => None
    }
  }
}