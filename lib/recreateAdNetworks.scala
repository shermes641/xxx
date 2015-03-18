/**
 * When the Player staging database is dropped, this script will recreate the ad networks on the Player side
 * for each app in the HyprMediate database.
 */

import anorm._
import anorm.SqlParser._
import java.util.concurrent.TimeoutException
import models._
import play.api.Play
import play.api.db.DB
import play.api.Play.current
import play.api.libs.json._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

new play.core.StaticApplication(new java.io.File("."))

object Script extends JsonConversion {
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
   * Retrieves a list of HyprMarketplace WaterfallAdProvider instances.
   * @return A list of WaterfallAdProvider instances.
   */
  def getHyprMarketplaceWaterfallAdProviders = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT wap.*, apps.token, apps.name as app_name, d.name as company_name
          FROM waterfall_ad_providers wap
          JOIN waterfalls w ON w.id = wap.waterfall_id
          JOIN apps ON apps.id = w.app_id
          JOIN distributors d ON d.id = apps.distributor_id
          WHERE wap.ad_provider_id = {ad_provider_id}
          ORDER BY wap.created_at ASC;
        """
      ).on("ad_provider_id" -> Play.current.configuration.getString("hyprmarketplace.ad_provider_id").get.toLong)

      query.as(parser*).toList
    }
  }

  /**
   * Prints error message in console and adds the WaterfallAdProvider ID to the list of unsuccessfully updated IDs.
   * @param waterfallAdProviderID The ID of the WaterfallAdProvider for which the request failed.
   * @param errorMessage The error message to be logged to the console.
   */
  def displayError(waterfallAdProviderID: Long, errorMessage: String) = {
    unsuccessfulWaterfallAdProviderIDs = unsuccessfulWaterfallAdProviderIDs :+ waterfallAdProviderID
    println(errorMessage + "\nWaterfallAdProvider ID: " + waterfallAdProviderID)
  }

  /**
   * Sends the create ad network request to Player and updates the HyprMarketplace
   * WaterfallAdProvider instance with the new distributor ID
   * @param wap The WaterfallAdProvider instance to be updated
   */
  def updateDistributorID(wap: WaterfallAdProviderWithAppData) = {
    wap.configurationData \ "requiredParams" \ "distributorID" match {
      case _: JsUndefined => {
        displayError(wap.id, "No required Params found")
      }
      case currentDistributorID: JsValue => {
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
                        displayError(wap.id, "Received an unsuccessful 200 response from Player")
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
  }

  /**
   * Recreates the ad network for a single WaterfallAdProvider.  This can be used to properly configure the HyprMarketplace
   * WaterfallAdProvider instance when the original call to Player fails.
   * @param waterfallAdProviderID The ID of the WaterfallAdProvider to be updated.
   */
  def recreateAdNetwork(waterfallAdProviderID: Long) = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT wap.*, apps.token, apps.name as app_name, d.name as company_name
          FROM waterfall_ad_providers wap
          JOIN waterfalls w ON w.id = wap.waterfall_id
          JOIN apps ON apps.id = w.app_id
          JOIN distributors d ON d.id = apps.distributor_id
          WHERE wap.id = {wap_id};
        """
      ).on("wap_id" -> waterfallAdProviderID)

      query.as(parser*) match {
        case List(waterfallAdProvider) => updateDistributorID(waterfallAdProvider)
        case _ => println("WaterfallAdProvider ID could not be found!")
      }
    }
    unsuccessfulWaterfallAdProviderIDs = List()
    successfulWaterfallAdProviderIDs = List()
  }

  /**
   * Sends a create ad network request to Player for each HyprMarketplace WaterfallAdProvider instance found in the database.
   */
  def recreateAllAdNetworks = {
    if(!Environment.isProd) {
      val adProviders = getHyprMarketplaceWaterfallAdProviders
      adProviders.foreach { wap => updateDistributorID(wap) }

      if(unsuccessfulWaterfallAdProviderIDs.size > 0) {
        println("Ad networks were not created for the following WaterfallAdProvider IDs: [ " + unsuccessfulWaterfallAdProviderIDs.mkString(", ") + " ]")
      } else if(successfulWaterfallAdProviderIDs.size != adProviders.size) {
        println("Not all WaterfallAdProviders were successfully updated yet.  Player is taking a while to respond to requests but we will log any errors as we receive them.")
      } else {
        println("All ad networks were created successfully")
      }
    } else {
      println("YOU ARE CURRENTLY IN A PRODUCTION ENVIRONMENT - DO NOT RUN THIS SCRIPT")
    }
    unsuccessfulWaterfallAdProviderIDs = List()
    successfulWaterfallAdProviderIDs = List()
  }
}
