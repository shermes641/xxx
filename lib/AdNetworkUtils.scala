/**
 * When an Ad Network creation fails in Player, this script will attempt to recreate the Ad Network.
 */

// $COVERAGE-OFF$
import anorm._
import java.sql.Connection
import play.api.db.DB
import play.api.Logger
import play.api.Play.current
import scala.language.postfixOps
import scala.util.{Failure, Try, Success}

import models._
import play.api.Logger
import play.api._
import play.api.ApplicationLoader.Context
import play.api.db.evolutions.Evolutions
import play.api.db.{DBComponents, Database, HikariCPComponents}
import play.api.libs.mailer._
import play.api.libs.ws.ning.NingWSClient
import play.api.mvc._
import play.api.mvc.Results._
import router.Routes
import scala.concurrent.Future
import Play.current

// The "correct" way to start the app
val env = Environment(new java.io.File("."), this.getClass.getClassLoader, Mode.Prod)
val context = ApplicationLoader.createContext(env)
val loader = ApplicationLoader(context)

val components = new MainComponents(context)
val adProviderService = components.adProviderService
val db = components.database
val waterfallAdProviderService = components.waterfallAdProviderService
val appConfigService = components.appConfigService
val platform = components.platform
val appService = components.appService

object AdNetworkUtils extends JsonConversion with UpdateHyprMarketplace {
  val akkaActorSystem = components.actorSystem
  val ws = components.wsClient
  val modelService = components.modelService
  val database = components.database
  val appService = components.appService
  val waterfallAdProviderService = components.waterfallAdProviderService
  val appConfigService = components.appConfigService
  val config = components.configVars
  val appEnv = components.appEnvironment

  /**
   * Recreates the ad network for a single WaterfallAdProvider.  This can be used to properly configure the HyprMarketplace
   * WaterfallAdProvider instance when the original call to Player fails.
   * @param waterfallAdProviderID The ID of the WaterfallAdProvider to be updated.
   */
  def recreateAdNetwork(waterfallAdProviderID: Long) = {
    db.withConnection { implicit connection =>
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
      db.withConnection { implicit connection =>
        SQL(
          """
              SELECT * FROM apps WHERE apps.token = {token};
          """
        ).on("token" -> apiToken).as(appService.AppParser*) match {
          case List(app) =>
            app
          case _ =>
            throw new RecordNotFoundException(recordType = "App")
        }
      }
    }

    db.withTransaction { implicit connection =>
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
            "hypr_ad_provider_id" -> platform.find(currentApp.platformID).hyprMarketplaceID
          ).as(waterfallAdProviderService.waterfallAdProviderParser*) match {
          case List(waterfallAdProvider) =>
            waterfallAdProvider
          case _ =>
            throw new RecordNotFoundException(recordType = "HyprMarketplace WaterfallAdProvider")
        }
      }

      Try(
        waterfallAdProviderService.updateHyprMarketplaceConfig(
          hyprWaterfallAdProvider,
          adNetworkID,
          apiToken,
          currentApp.name
        )
      ) match {
        case Success(rowsUpdated: Int) =>
          if(rowsUpdated == 1) {
            appConfigService.createWithWaterfallIDInTransaction(
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