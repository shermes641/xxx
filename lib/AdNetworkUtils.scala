/**
 * When an Ad Network creation fails in Player, this script will attempt to recreate the Ad Network.
 */

import anorm._
import java.sql.Connection
import models._
import play.api.db.DB
import play.api.Logger
import play.api.Play.current
import scala.language.postfixOps
import scala.util.{Failure, Try, Success}
// $COVERAGE-OFF$
new play.core.StaticApplication(new java.io.File("."))

object AdNetworkUtils extends JsonConversion with UpdateHyprMarketplace {
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
        case List(waterfallAdProvider) =>
          updateHyprMarketplaceDistributorID(waterfallAdProvider)
        case _ =>
          Logger.error("HyprMarketplace WaterfallAdProvider could not be found!")
      }
    }
  }

  /**
   * Updates a HyprMarketplace WaterfallAdProvider with the correct configuration JSON when
   * the Player API call fails with an ad network name duplication error.
   * @param apiToken The API Token for the app that must be updated (This is sent in the ad network creation failure email).
   * @param adNetworkID The ID of the ad_networks record existing in Player. This value must be looked up in the Player database or in Player's admin UI.
   */
  def reconfigureHyprMarketplace(apiToken: String, adNetworkID: Long) = {
    case class RecordNotFoundException(recordType: String) extends Exception

    def rollback(errorMessage: String)(implicit connection: Connection) = {
      Logger.error("Transaction rolling back: " + errorMessage)
      connection.rollback()
    }

    val currentApp = {
      DB.withConnection { implicit connection =>
        SQL(
          """
              SELECT * FROM apps WHERE apps.token = {token};
          """
        ).on("token" -> apiToken).as(App.AppParser*) match {
          case List(app) =>
            app
          case _ =>
            throw new RecordNotFoundException(recordType = "App")
        }
      }
    }

    DB.withTransaction { implicit connection =>
      val hyprWaterfallAdProvider = {
        SQL(
          """
              SELECT waterfall_ad_providers.*
              FROM waterfall_ad_providers
              JOIN waterfalls ON waterfalls.id = waterfall_ad_providers.waterfall_id
              JOIN apps ON apps.id = waterfalls.app_id
              WHERE apps.id = {app_id} AND ad_provider_id = {hypr_ad_provider_id}
              FOR UPDATE;
          """
        ).on(
            "app_id"              -> currentApp.id,
            "hypr_ad_provider_id" -> Platform.find(currentApp.platformID).hyprMarketplaceID
          ).as(WaterfallAdProvider.waterfallAdProviderParser*) match {
          case List(waterfallAdProvider) =>
            waterfallAdProvider
          case _ =>
            throw new RecordNotFoundException(recordType = "HyprMarketplace WaterfallAdProvider")
        }
      }

      Try(
        WaterfallAdProvider.updateHyprMarketplaceConfig(
          hyprWaterfallAdProvider,
          adNetworkID,
          apiToken,
          currentApp.name
        )
      ) match {
        case Success(rowsUpdated: Int) =>
          if(rowsUpdated == 1) {
            AppConfig.createWithWaterfallIDInTransaction(
              hyprWaterfallAdProvider.waterfallID,
              currentGenerationNumber = None
            ) match {
              case Some(generationNumber) =>
                Logger.debug("The HyprMarketplace WaterfallAdProvider was updated successfully!")
              case _ =>
                rollback("The App Config was not generated correctly.")
            }
          } else {
            rollback("The HyprMarketplace WaterfallAdProvider was not updated properly.")
          }
        case Failure(errorReceived) =>
          errorReceived match {
            case error: RecordNotFoundException =>
              rollback(error.recordType + " could not be found. Make sure you have the right API Token.")
            case error: org.postgresql.util.PSQLException =>
              rollback("Received the following Postgres exception while updating: " + error.getServerErrorMessage)
            case error: IllegalArgumentException =>
              rollback("There was a problem with the current generation number. Please retry the script.")
          }
      }
    }
  }
}
// $COVERAGE-ON$