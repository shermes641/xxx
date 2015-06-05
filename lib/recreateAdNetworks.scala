/**
 * When the Player staging database is dropped, this script will recreate the ad networks on the Player side
 * for each app in the HyprMediate database.
 */

import anorm._
import models._
import play.api.db.DB
import play.api.Logger
import play.api.Play
import play.api.Play.current
import scala.language.postfixOps

new play.core.StaticApplication(new java.io.File("."))

object Script extends JsonConversion with UpdateHyprMarketplace {
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
        case List(waterfallAdProvider) => updateHyprMarketplaceDistributorID(waterfallAdProvider)
        case _ => Logger.error("WaterfallAdProvider ID could not be found!")
      }
    }
    unsuccessfulWaterfallAdProviderIDs = List()
    successfulWaterfallAdProviderIDs = List()
  }

  /**
   * Sends a create ad network request to Player for each HyprMarketplace WaterfallAdProvider instance found in the database.
   */
  def recreateAllAdNetworks() = {
    if(!Environment.isProd) {
      val adProviders = getHyprMarketplaceWaterfallAdProviders
      adProviders.foreach { wap => updateHyprMarketplaceDistributorID(wap) }

      if(unsuccessfulWaterfallAdProviderIDs.size > 0) {
        Logger.error("Ad networks were not created for the following WaterfallAdProvider IDs: [ " + unsuccessfulWaterfallAdProviderIDs.mkString(", ") + " ]\n" +
        "This may be due to the fact that Ad networks already exist for these WaterfallAdProviders.  Check the output above for the reason why each WaterfallAdProvider was not updated.")
      } else if(successfulWaterfallAdProviderIDs.size != adProviders.size) {
        Logger.debug("Not all WaterfallAdProviders were successfully updated yet.  Player is taking a while to respond to requests but we will log any errors as we receive them.")
      } else {
        Logger.debug("All ad networks were created successfully")
      }
    } else {
      Logger.warn("YOU ARE CURRENTLY IN A PRODUCTION ENVIRONMENT - DO NOT RUN THIS SCRIPT")
    }
    unsuccessfulWaterfallAdProviderIDs = List()
    successfulWaterfallAdProviderIDs = List()
  }
}
