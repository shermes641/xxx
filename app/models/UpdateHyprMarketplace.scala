package models

import anorm._
import anorm.SqlParser._
import java.util.concurrent.TimeoutException
import play.api.Play
import play.api.db.DB
import play.api.Play.current
import play.api.libs.json._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

/**
 * Encapsulates behavior used in the keenImport.scala and recreateAdNetwork.scala scripts.
 */
trait UpdateHyprMarketplace extends JsonConversion {
  var unsuccessfulWaterfallAdProviderIDs: List[Long] = List()
  var successfulWaterfallAdProviderIDs: List[Long] = List()

  /**
   * Encapsulates all WaterfallAdProvider information required for running this script.
   * @param id The ID of the WaterfallAdProvider
   * @param waterfallID The ID of the Waterfall to which the WaterfallAdProvider belongs.
   * @param adProviderID The ID of the AdProvider to which the WaterfallAdProvider belongs.
   * @param waterfallOrder The position of the WaterfallAdProvider in the Waterfall.
   * @param cpm cost per thousand impressions
   * @param active determines if the waterfall_ad_provider should be included in a waterfall
   * @param fillRate the ratio of ads shown to inventory checks
   * @param pending A boolean that determines if the waterfall_ad_provider is able to be activated.  This is used only in the case of HyprMarketplace while we wait for a Distributor ID.
   * @param appToken The token value of the App to which the WaterfallAdProvider belongs.
   * @param appName The name of the App to which the WaterfallAdProvider belongs.
   * @param companyName The name of the Distributor who owns the WaterfallAdProvider.
   */
  case class WaterfallAdProviderWithAppData (id:Long, waterfallID:Long, adProviderID:Long, waterfallOrder: Option[Long], cpm: Option[Double], active: Option[Boolean],
                                             fillRate: Option[Float], configurationData: JsValue, reportingActive: Boolean, pending: Boolean, appToken: String, appName: String, companyName: String)

  val parser: RowParser[WaterfallAdProviderWithAppData] = {
    get[Long]("waterfall_ad_providers.id") ~
    get[Long]("waterfall_id") ~
    get[Long]("ad_provider_id") ~
    get[Option[Long]]("waterfall_order") ~
    get[Option[Double]]("cpm") ~
    get[Option[Boolean]]("active") ~
    get[Option[Float]]("fill_rate") ~
    get[JsValue]("configuration_data") ~
    get[Boolean]("reporting_active") ~
    get[Boolean]("pending") ~
    get[String]("token") ~
    get[String]("app_name") ~
    get[String]("company_name") map {
      case id ~ waterfall_id ~ ad_provider_id ~ waterfall_order ~ cpm ~ active ~
        fill_rate ~ configuration_data ~ reporting_active ~ pending ~ token ~ app_name ~ company_name =>
        WaterfallAdProviderWithAppData(id, waterfall_id, ad_provider_id, waterfall_order, cpm, active, fill_rate, configuration_data, reporting_active, pending, token, app_name, company_name)
    }
  }

  /**
   * Prints error message in console and adds the WaterfallAdProvider ID to the list of unsuccessfully updated IDs.
   * @param waterfallAdProviderID The ID of the WaterfallAdProvider for which the request failed.
   * @param errorMessage The error message to be logged to the console.
   */
  def displayError(waterfallAdProviderID: Long, errorMessage: String) = {
    unsuccessfulWaterfallAdProviderIDs = unsuccessfulWaterfallAdProviderIDs :+ waterfallAdProviderID
    println(errorMessage + "\nWaterfallAdProvider ID: " + waterfallAdProviderID + "\n")
  }

  /**
   * Sends the create ad network request to Player and updates the HyprMarketplace
   * WaterfallAdProvider instance with the new distributor ID
   * @param wap The WaterfallAdProvider instance to be updated
   */
  def updateHyprMarketplaceDistributorID(wap: WaterfallAdProviderWithAppData) = {
    val api = new JunGroupAPI
    val adNetwork = api.adNetworkConfiguration(wap.companyName, wap.appName, wap.appToken)
    Await.result(
      api.createRequest(adNetwork) map {
        response => {
          if(response.status != 500) {
            try {
              Json.parse(response.body) match {
                case _:JsUndefined => {
                  displayError(wap.id, "Received a JsUndefined error from Player")
                }
                case results if(response.status == 200 || response.status == 304) => {
                  val success: JsValue = results \ "success"
                  if(success.as[JsBoolean] != JsBoolean(false)) {
                    DB.withTransaction { implicit connection =>
                      try {
                        val adNetworkID: Long = (results \ "ad_network" \ "ad_network" \ "id").as[Long]
                        val hyprWaterfallAdProvider = new WaterfallAdProvider(wap.id, wap.waterfallID, wap.adProviderID, wap.waterfallOrder, wap.cpm, wap.active, wap.fillRate, wap.configurationData, wap.reportingActive, wap.pending)
                        WaterfallAdProvider.updateHyprMarketplaceConfig(hyprWaterfallAdProvider, adNetworkID, wap.appToken, wap.appName)
                        AppConfig.createWithWaterfallIDInTransaction(wap.waterfallID, None)
                        successfulWaterfallAdProviderIDs = successfulWaterfallAdProviderIDs :+ wap.id
                        println("WaterfallAdProvider successfully updated with new distributor ID: " + adNetworkID + "\nWaterfallAdProvider ID: " + wap.id)
                      } catch {
                        case error: org.postgresql.util.PSQLException => {
                          connection.rollback()
                        }
                      }
                    }
                  } else {
                    displayError(wap.id, "Ad network already exists in Player")
                  }
                }
                case _ => {
                  displayError(wap.id, "Received status code " + response.status + " from Player")
                }
              }
            } catch {
              case parsingError: com.fasterxml.jackson.core.JsonParseException => {
                displayError(wap.id, "There was a JSON parsing error")
              }
            }
          } else {
            displayError(wap.id, "Received a status code of 500 from Player")
          }
        }
      } recover {
        case _: TimeoutException => {
          displayError(wap.id, "Request to Player timed out")
        }
        case error => {
          displayError(wap.id, "Request to Player yielded the following error: " + error.getMessage)
        }
      },
      Duration(60000, "millis")
    )
  }
}