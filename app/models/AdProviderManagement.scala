package models

import anorm._
import play.api.db.DB
import play.api.Logger
import play.api.Play.current

/**
 * Encapsulates AdProvider functions that are used outside of application code.
 */
trait AdProviderManagement {
  /**
   * Updates a single ad provider.
   * @param adProvider A class encapsulating all of the updatable information for an ad provider.
   * @return The number of rows updated if the ad provider is updated successfully; otherwise, 0.
   */
  def update(adProvider: UpdatableAdProvider): Long = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          UPDATE ad_providers
          SET configuration_data=CAST({configuration_data} AS json), callback_url_format={callback_url_format},
          configurable={configurable}, default_ecpm={default_ecpm}
          WHERE name={name} AND platform_id={platform_id};
        """
      ).on(
          "name" -> adProvider.name,
          "configuration_data" -> adProvider.configurationData,
          "callback_url_format" -> adProvider.callbackURLFormat,
          "configurable" -> adProvider.configurable,
          "default_ecpm" -> adProvider.defaultEcpm,
          "platform_id" -> adProvider.platformID
        ).executeUpdate()
    }
  }

  /**
   * Updates all AdProviders using the constantized version of each AdProvider found in the allProviders list.
   * This is called during the AppConfig regeneration process.
   * @return The number of successfully updated AdProviders
   */
  def updateAll(): Int = {
    var successfullyUpdatedProvidersCount: Int = 0
    (Platform.Ios.allAdProviders ++ Platform.Android.allAdProviders).foreach { adProvider =>
      update(adProvider) match {
        case 1 => {
          successfullyUpdatedProvidersCount += 1
          Logger.debug(adProvider.name + " was updated successfully!")
        }
        case _ => Logger.error(adProvider.name + " was not updated successfully.")
      }
    }
    successfullyUpdatedProvidersCount
  }

  /**
   * Inserts new records into the ad_providers table if they don't already exist.
   * This is used solely for environment setup and testing.
   */
  def loadAll(): Unit = {
    if(AdProvider.findAllByPlatform(Platform.Ios.PlatformID).size == 0) {
      val adColonyResult = AdProvider.create(Platform.Ios.AdColony.name, Platform.Ios.AdColony.configurationData, Platform.Ios.PlatformID, Platform.Ios.AdColony.callbackURLFormat, Platform.Ios.AdColony.configurable, Platform.Ios.AdColony.defaultEcpm)
      val hyprResult = AdProvider.create(Platform.Ios.HyprMarketplace.name, Platform.Ios.HyprMarketplace.configurationData, Platform.Ios.PlatformID, Platform.Ios.HyprMarketplace.callbackURLFormat, Platform.Ios.HyprMarketplace.configurable, Platform.Ios.HyprMarketplace.defaultEcpm)
      val vungleResult = AdProvider.create(Platform.Ios.Vungle.name, Platform.Ios.Vungle.configurationData, Platform.Ios.PlatformID, Platform.Ios.Vungle.callbackURLFormat, Platform.Ios.Vungle.configurable, Platform.Ios.Vungle.defaultEcpm)
      val appLovinResult = AdProvider.create(Platform.Ios.AppLovin.name, Platform.Ios.AppLovin.configurationData, Platform.Ios.PlatformID, Platform.Ios.AppLovin.callbackURLFormat, Platform.Ios.AppLovin.configurable, Platform.Ios.AppLovin.defaultEcpm)
      (adColonyResult, hyprResult, vungleResult, appLovinResult) match {
        case (Some(adColonyID), Some(hyprID), Some(vungleID), Some(appLovinID)) => Logger.debug("All iOS ad providers created successfully!")
        case (_, _, _, _) => Logger.error("Something went wrong and one or more of the iOS ad providers were not created properly.")
      }
    } else {
      Logger.warn("Running this script without an empty ad_providers table may result in duplicate ad providers.  If you need to create one or more of these ad providers, you should do it manually.")
    }

    if(AdProvider.findAllByPlatform(Platform.Android.PlatformID).size == 0) {
      val adColonyResult = AdProvider.create(Platform.Android.AdColony.name, Platform.Android.AdColony.configurationData, Platform.Android.PlatformID, Platform.Android.AdColony.callbackURLFormat, Platform.Android.AdColony.configurable, Platform.Android.AdColony.defaultEcpm)
      val hyprResult = AdProvider.create(Platform.Android.HyprMarketplace.name, Platform.Android.HyprMarketplace.configurationData, Platform.Android.PlatformID, Platform.Android.HyprMarketplace.callbackURLFormat, Platform.Android.HyprMarketplace.configurable, Platform.Android.HyprMarketplace.defaultEcpm)
      val appLovinResult = AdProvider.create(Platform.Android.AppLovin.name, Platform.Android.AppLovin.configurationData, Platform.Android.PlatformID, Platform.Android.AppLovin.callbackURLFormat, Platform.Android.AppLovin.configurable, Platform.Android.AppLovin.defaultEcpm)
      (adColonyResult, hyprResult, appLovinResult) match {
        case (Some(adColonyID), Some(hyprID), Some(appLovinID)) => Logger.debug("All Android ad providers created successfully!")
        case (_, _, _) => Logger.error("Something went wrong and one or more of the Android ad providers were not created properly.")
      }
    } else {
      Logger.warn("Running this script without an empty ad_providers table may result in duplicate ad providers.  If you need to create one or more of these ad providers, you should do it manually.")
    }
  }
}
