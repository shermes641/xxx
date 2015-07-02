/**
 * When an Ad Network creation fails in Player, this script will attempt to recreate the Ad Network.
 */

import anorm._
import models._
import play.api.db.DB
import play.api.{Play, Logger}
import play.api.Play.current
import scala.language.postfixOps

new play.core.StaticApplication(new java.io.File("."))

object Script extends JsonConversion with UpdateHyprMarketplace {
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
          WHERE wap.id = {wap_id} AND wap.ad_provider_id = {hypr_ad_provider_id};
        """
      ).on("wap_id" -> waterfallAdProviderID, "hypr_ad_provider_id" -> Play.current.configuration.getLong("hyprmarketplace.ad_provider_id").get)

      query.as(parser*) match {
        case List(waterfallAdProvider) => updateHyprMarketplaceDistributorID(waterfallAdProvider)
        case _ => Logger.error("HyprMarketplace WaterfallAdProvider could not be found!")
      }
    }
    unsuccessfulWaterfallAdProviderIDs = List()
    successfulWaterfallAdProviderIDs = List()
  }
}
