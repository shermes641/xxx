package models

import anorm._
import play.api.db.DB
import play.api.Logger
import play.api.Play.current

/**
  * Encapsulates AdProvider functions that are used outside of application code.
  */
trait AdProviderManagement {

  object AdProviderResult extends Enumeration {
    type AdProviderResult = Value
    val CREATED, UPDATED, EXISTS, INVALID_PLATFORM_ID, FAILED = Value
  }

  /**
    * Updates a single ad provider.
    *
    * @param adProvider A class encapsulating all of the updatable information for an ad provider.
    * @return UPDATED or FAILED
    */
  def updateSingleAdProvider(adProvider: UpdatableAdProvider): AdProviderResult.AdProviderResult = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          UPDATE ad_providers
          SET configuration_data=CAST({configuration_data} AS json), callback_url_format={callback_url_format},
          configurable={configurable}, default_ecpm={default_ecpm}, display_name={display_name}
          WHERE name={name} and platform_id={platform_id};
        """
      ).on(
        "name" -> adProvider.name,
        "display_name" -> adProvider.displayName,
        "platform_id" -> adProvider.platformID,
        "configuration_data" -> adProvider.configurationData,
        "callback_url_format" -> adProvider.callbackURLFormat,
        "configurable" -> adProvider.configurable,
        "default_ecpm" -> adProvider.defaultEcpm
      ).executeUpdate() match {
        case 1 => AdProviderResult.UPDATED
        case cnt =>
          Logger.error(s"Update failed number of records updated: $cnt")
          AdProviderResult.FAILED
      }

    }
  }

  /**
    * Updates all AdProviders using the constantized version of each AdProvider found in the allProviders list.
    * This is called during the AppConfig regeneration process.
    *
    * @return The number of successfully updated AdProviders
    */
  def updateAll(): Int = {
    var successfullyUpdatedProvidersCount: Int = 0
    (Platform.Ios.allAdProviders ++ Platform.Android.allAdProviders).foreach { adProvider =>
      updateSingleAdProvider(adProvider) match {
        case AdProviderResult.UPDATED =>
          successfullyUpdatedProvidersCount += 1
          Logger.debug(adProvider.name + " was updated successfully!")

        case result =>
          Logger.error(s"${adProvider.name} platform ID: ${adProvider.platformID} was not updated successfully. result: $result")
      }
    }
    successfullyUpdatedProvidersCount
  }

  /**
    * Inserts new records into the ad_providers table if they don't already exist.
    * This is used solely for environment setup and testing.
    * Note: The order is important because there are Environment vars referring to specific IDs
    *
    * @return list of AdProviderResult enums
    */
  def loadAll(): List[AdProviderResult.AdProviderResult] = {
    List(createAdProvider(Platform.IosPlatformID, Platform.Ios.AdColony),
    createAdProvider(Platform.IosPlatformID, Platform.Ios.HyprMarketplace),
    createAdProvider(Platform.IosPlatformID, Platform.Ios.Vungle),
    createAdProvider(Platform.IosPlatformID, Platform.Ios.AppLovin),

    createAdProvider(Platform.AndroidPlatformID, Platform.Android.AdColony),
    createAdProvider(Platform.AndroidPlatformID, Platform.Android.HyprMarketplace),
    createAdProvider(Platform.AndroidPlatformID, Platform.Android.AppLovin),
    createAdProvider(Platform.AndroidPlatformID, Platform.Android.Vungle),

    createAdProvider(Platform.IosPlatformID, Platform.Ios.UnityAds),
    createAdProvider(Platform.AndroidPlatformID, Platform.Android.UnityAds))
  }

  /**
    * Creates an adprovider for the given platform.
    * If the adprovider for the platform exists it does nothing.
    *
    * @param platformID ID for the platform (currently only 2 platforms supported)
    * @param provider   Ad provider
    * @return 1 of 4 AdProviderResult enums
    */
  protected def createAdProvider(platformID: Long, provider: UpdatableAdProvider): AdProviderResult.AdProviderResult = {
    platformID match {
      case Platform.AndroidPlatformID => Platform.AndroidPlatformName
      case Platform.IosPlatformID => Platform.IosPlatformName
      case _ => ""
    }
  } match {
    case "" =>
      Logger.warn(s"Invalid Platform ID: $platformID")
      AdProviderResult.INVALID_PLATFORM_ID

    case platformName =>
      AdProvider.findByPlatformAndName(platformID, provider.name).isEmpty match {
        case false =>
          Logger.warn(s"${provider.name} for $platformName platform already exists, the configuration was not changed")
          AdProviderResult.EXISTS

        case _ =>
          AdProvider.create(provider.name,
            provider.displayName,
            provider.configurationData,
            platformID,
            provider.callbackURLFormat,
            provider.configurable,
            provider.defaultEcpm) match {
              case Some(adsID) =>
                Logger.debug(s"The ${provider.name} provider for the $platformName platform created successfully!")
                AdProviderResult.CREATED

              case _ =>
                Logger.error(s"Something went wrong adding the ${provider.name} provider for the $platformName platform.")
                AdProviderResult.FAILED
          }
      }
  }
}
