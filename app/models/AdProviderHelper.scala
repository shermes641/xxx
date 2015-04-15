package models

import anorm._
import play.api.db.DB
import play.api.Play.current

trait AdProviderHelper {
  val AdColony = {
    val name = "AdColony"

    val callbackURLFormat = Some("/v1/reward_callbacks/%s/ad_colony?id=[ID]&uid=[USER_ID]&amount=[AMOUNT]&" +
      "currency=[CURRENCY]&open_udid=[OpenUDID]&udid=[UDID]&odin1=[ODIN1]&mac_sha1=[MAC_SHA1]&verifier=[VERIFIER]&custom_id=[CUSTOM_ID]")

    val configurable = true

    val defaultEcpm: Option[Double] = Some(10)

    val configuration = {
      val zoneIDDetails = {
        "Add a single zone or multiple zones separated by commas. Please note, we currently only support Value Exchange Zones. " +
          "For more information on configuration, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/AdColony' target='_blank'>documentation</a>"
      }

      val appIDDescription = {
        "Your AdColony App ID can be found on the AdColony dashboard.  For more information on configuring AdColony, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/AdColony' target='_blank'>documentation</a>."
      }

      val reportingDescription = {
        "Your Read-Only API Key can be found on the AdColony dashboard.  For more information on configuring reporting for AdColony, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/AdColony' target='_blank'>documentation</a>."
      }

      val callbackDescription = {
        "Your V4VC Secret Key can be found on the AdColony dashboard.  For more information on configuring server to server callbacks for AdColony, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/AdColony+Server+to+Server+Callbacks+Setup' target='_blank'>documentation</a>."
      }

      "{" +
        "\"requiredParams\":[" +
        "{\"description\": \"" + appIDDescription + "\", \"displayKey\": \"AdColony App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": true, \"minLength\": 1}, " +
        "{\"description\": \"" + zoneIDDetails + "\", \"displayKey\": \"Zone IDs\", \"key\": \"zoneIds\", \"value\":\"\", \"dataType\": \"Array\", \"refreshOnAppRestart\": true, \"minLength\": 1}" +
        "], " +
        "\"reportingParams\": [" +
        "{\"description\": \"" + reportingDescription + "\", \"displayKey\": \"Reporting API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
        "], " +
        "\"callbackParams\": [" +
        "{\"description\": \"" + callbackDescription + "\", \"displayKey\": \"V4VC Secret Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
        "]" +
        "}"
    }

    val sdkBlacklistRegex = ".^"

    new UpdatableAdProvider(name, configuration, callbackURLFormat, configurable, defaultEcpm, sdkBlacklistRegex)
  }

  val HyprMarketplace = {
    val name = "HyprMarketplace"

    val callbackURLFormat = None

    val configurable = false

    val defaultEcpm: Option[Double] = Some(20)

    val configuration = {
      "{" +
        "\"requiredParams\":[" +
        "{\"description\": \"Your HyprMarketplace Distributor ID\", \"displayKey\": \"Distributor ID\", \"key\": \"distributorID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false, \"minLength\": 1}, " +
        "{\"description\": \"Your HyprMarketplace Property ID\", \"displayKey\": \"\", \"key\": \"propertyID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false, \"minLength\": 1}" +
        "], " +
        "\"reportingParams\": [" +
        "{\"description\": \"Your API Key for HyprMarketplace\", \"displayKey\": \"API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}, " +
        "{\"description\": \"Your Placement ID\", \"displayKey\": \"Placement ID\", \"key\": \"placementID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}, " +
        "{\"description\": \"Your App ID\", \"displayKey\": \"App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
        "], " +
        "\"callbackParams\": [" +
        "]" +
        "}"
    }

    val sdkBlacklistRegex = ".^"

    new UpdatableAdProvider(name, configuration, callbackURLFormat, configurable, defaultEcpm, sdkBlacklistRegex)
  }

  val Vungle = {
    val name = "Vungle"

    val callbackURLFormat = Some("/v1/reward_callbacks/%s/vungle?amount=%s&uid=%%user%%&openudid=%%udid%%&mac=%%mac%%&ifa=%%ifa%%&transaction_id=%%txid%%&digest=%%digest%%")

    val configurable = true

    val defaultEcpm: Option[Double] = Some(10)

    val configuration = {
      val appIDDescription = {
        "Your App ID can be found on the Vungle dashboard.  For more information on configuring Vungle, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/Vungle' target='_blank'>documentation</a>."
      }

      val reportingDescription = {
        "Your Reporting API Key can be found on the Vungle dashboard.  For more information on configuring reporting for Vungle, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/Vungle' target='_blank'>documentation</a>."
      }

      val callbackDescription = {
        "Your Secret Key for Secure Callback can be found on the Vungle dashboard.  For more information on configuring server to server callbacks for Vungle, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/Vungle+Server+to+Server+Callbacks+Setup' target='_blank'>documentation</a>."
      }

      "{" +
        "\"requiredParams\":[" +
        "{\"description\": \"" + appIDDescription + "\", \"displayKey\": \"App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": true, \"minLength\": 1}" +
        "], " +
        "\"reportingParams\": [" +
        "{\"description\": \"" + reportingDescription + "\", \"displayKey\": \"Reporting API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
        "], " +
        "\"callbackParams\": [" +
        "{\"description\": \"" + callbackDescription + "\", \"displayKey\": \"Secret Key for Secure Callback\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
        "]" +
        "}"
    }

    val sdkBlacklistRegex = ".^"

    new UpdatableAdProvider(name, configuration, callbackURLFormat, configurable, defaultEcpm, sdkBlacklistRegex)
  }

  val AppLovin = {
    val name = "AppLovin"

    val callbackURLFormat = Some("/v1/reward_callbacks/%s/app_lovin?idfa={IDFA}&hadid={HADID}&amount={AMOUNT}&currency={CURRENCY}&event_id={EVENT_ID}&user_id={USER_ID}")

    val configurable = true

    val defaultEcpm: Option[Double] = Some(10)

    val configuration = {
      val sdkKeyDescription = {
        "Your SDK Key can be found on the AppLovin dashboard.  For more information on configuring AppLovin, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/AppLovin' target='_blank'>documentation</a>."
      }

      val reportingDescription = {
        "Your Report Key can be found on the AppLovin dashboard.  For more information on configuring reporting for AppLovin, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/AppLovin' target='_blank'>documentation</a>."
      }

      val appLovinAppNameDescription = {
        "Your application name can be found in AppLovin's dashboard.  This is the same as the Bundle identifier in your iOS target properties.  For more information on configuring reporting for AppLovin, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/AppLovin' target='_blank'>documentation</a>."
      }

      "{" +
        "\"requiredParams\":[" +
        "{\"description\": \"" + sdkKeyDescription + "\", \"displayKey\": \"SDK Key\", \"key\": \"sdkKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": true, \"minLength\": 4}" +
        "], " +
        "\"reportingParams\": [" +
        "{\"description\": \"" + reportingDescription + "\", \"displayKey\": \"Report Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}," +
        "{\"description\": \"" + appLovinAppNameDescription + "\", \"displayKey\": \"Application Name\", \"key\": \"appName\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
        "], " +
        "\"callbackParams\": [" +
        "]" +
        "}"
    }

    val sdkBlacklistRegex = ".^"

    new UpdatableAdProvider(name, configuration, callbackURLFormat, configurable, defaultEcpm, sdkBlacklistRegex)
  }

  val allProviders = List(AdColony, HyprMarketplace, Vungle, AppLovin)

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
        configurable={configurable}, default_ecpm={default_ecpm}, sdk_blacklist_regex={sdk_blacklist_regex}
        WHERE name={name};
        """
      ).on("name" -> adProvider.name, "configuration_data" -> adProvider.configurationData, "callback_url_format" -> adProvider.callbackURLFormat,
          "configurable" -> adProvider.configurable, "default_ecpm" -> adProvider.defaultEcpm, "sdk_blacklist_regex" -> adProvider.sdkBlacklistRegex).executeUpdate()
    }
  }

  /**
   * Updates all AdProviders using the constantized version of each AdProvider found in the allProviders list.
   * @return The number of successfully updated AdProviders
   */
  def updateAll: Int = {
    var successfullyUpdatedProvidersCount: Int = 0
    allProviders.foreach { adProvider =>
      update(adProvider) match {
        case 1 => {
          successfullyUpdatedProvidersCount += 1
          println(adProvider.name + " was updated successfully!")
        }
        case _ => println(adProvider.name + " was not updated successfully.")
      }
    }
    successfullyUpdatedProvidersCount
  }

}
