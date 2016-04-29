package models

/**
 * Base class for Android and Ios AdProvider classes.
  *
  * @param id Maps to the id column in the platforms table.
 * @param name Maps to the name column in the platforms table.
 */
abstract class Platform(id: Long, name: String) {
  val PlatformID: Long = id
  val PlatformName: String = name

   // The ID of the platform-specific HyprMarketplace record in the ad_providers table.
  val hyprMarketplaceID: Long

  // The platform-specific server to server callback domain shown in the configuration modal for each WaterfallAdProvider.
  val serverToServerDomain: String

  val AdColony = {
    val name = "AdColony"

    val displayName = name

    val callbackURLFormat = Some("/v1/reward_callbacks/%s/ad_colony?id=[ID]&uid=[USER_ID]&amount=[AMOUNT]&" +
      "currency=[CURRENCY]&open_udid=[OpenUDID]&udid=[UDID]&odin1=[ODIN1]&mac_sha1=[MAC_SHA1]&verifier=[VERIFIER]&custom_id=[CUSTOM_ID]")

    val configurable = true

    val defaultEcpm: Option[Double] = Some(10)

    val configuration = {
      val zoneIDDetails = {
        "Add a single zone or multiple zones separated by commas. Please note, we currently only support Value Exchange Zones. " +
          "For more information on configuration, please see our <a href='https://documentation.hyprmx.com/display/ADMIN/AdColony' target='_blank'>documentation</a>"
      }

      val appIDDescription = {
        "Your AdColony App ID can be found on the AdColony dashboard.  For more information on configuring AdColony, please see our <a href='https://documentation.hyprmx.com/display/ADMIN/AdColony' target='_blank'>documentation</a>."
      }

      val reportingDescription = {
        "Your Read-Only API Key can be found on the AdColony dashboard.  For more information on configuring reporting for AdColony, please see our <a href='https://documentation.hyprmx.com/display/ADMIN/AdColony' target='_blank'>documentation</a>."
      }

      val callbackDescription = {
        "Your V4VC Secret Key can be found on the AdColony dashboard.  For more information on configuring server to server callbacks for AdColony, please see our <a href='https://documentation.hyprmx.com/display/ADMIN/AdColony+Server+to+Server+Callbacks+Setup' target='_blank'>documentation</a>."
      }

      "{" +
        "\"requiredParams\":[" +
        "{\"description\": \"" + appIDDescription + "\", \"displayKey\": \"AdColony App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": true, \"minLength\": 1}, " +
        "{\"description\": \"" + zoneIDDetails + "\", \"displayKey\": \"Zone IDs\", \"key\": \"zoneIds\", \"value\":\"\", \"dataType\": \"Array\", \"refreshOnAppRestart\": true, \"minLength\": 1}" +
        "], " +
        "\"reportingParams\": [" +
        "{\"description\": \"" + reportingDescription + "\", \"displayKey\": \"Read-Only API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
        "], " +
        "\"callbackParams\": [" +
        "{\"description\": \"" + callbackDescription + "\", \"displayKey\": \"V4VC Secret Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
        "]" +
        "}"
    }

    new UpdatableAdProvider(name, displayName, configuration, PlatformID, callbackURLFormat, configurable, defaultEcpm)
  }

  val HyprMarketplace = {
    val name = "HyprMarketplace"

    val displayName = name

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

    new UpdatableAdProvider(name, displayName, configuration, PlatformID, callbackURLFormat, configurable, defaultEcpm)
  }

  val Vungle = {
    val name = "Vungle"

    val displayName = name

    val callbackURLFormat = Some("/v1/reward_callbacks/%s/vungle?amount=%s&uid=%%user%%&openudid=%%udid%%&mac=%%mac%%&ifa=%%ifa%%&transaction_id=%%txid%%&digest=%%digest%%")

    val configurable = true

    val defaultEcpm: Option[Double] = Some(10)

    val configuration = {
      val appIDDescription = {
        "Your App ID can be found on the Vungle dashboard.  For more information on configuring Vungle, please see our <a href='https://documentation.hyprmx.com/display/ADMIN/Vungle' target='_blank'>documentation</a>."
      }

      val reportingAPIKeyDescription = {
        "Your Reporting API Key can be found on the Vungle dashboard.  For more information on configuring reporting for Vungle, please see our <a href='https://documentation.hyprmx.com/display/ADMIN/Vungle' target='_blank'>documentation</a>."
      }

      val reportingAPIIDDescription = {
        "Your Reporting API ID can be found on the Vungle dashboard.  For more information on configuring reporting for Vungle, please see our <a href='https://documentation.hyprmx.com/display/ADMIN/Vungle' target='_blank'>documentation</a>."
      }

      val callbackDescription = {
        "Your Secret Key for Secure Callback can be found on the Vungle dashboard.  For more information on configuring server to server callbacks for Vungle, please see our <a href='https://documentation.hyprmx.com/display/ADMIN/Vungle+Server+to+Server+Callbacks+Setup' target='_blank'>documentation</a>."
      }

      "{" +
        "\"requiredParams\":[" +
        "{\"description\": \"" + appIDDescription + "\", \"displayKey\": \"App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": true, \"minLength\": 1}" +
        "], " +
        "\"reportingParams\": [" +
        "{\"description\": \"" + reportingAPIKeyDescription + "\", \"displayKey\": \"Reporting API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}," +
        "{\"description\": \"" + reportingAPIIDDescription + "\", \"displayKey\": \"Reporting API ID\", \"key\": \"APIID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
        "], " +
        "\"callbackParams\": [" +
        "{\"description\": \"" + callbackDescription + "\", \"displayKey\": \"Secret Key for Secure Callback\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
        "]" +
        "}"
    }
    new UpdatableAdProvider(name, displayName, configuration, PlatformID, callbackURLFormat, configurable, defaultEcpm)
  }

  val AppLovin = {
    val name = "AppLovin"

    val displayName = name

    val callbackURLFormat = Some("/v1/reward_callbacks/%s/app_lovin?idfa={IDFA}&ip={IP}&amount={AMOUNT}&currency={CURRENCY}&event_id={EVENT_ID}&user_id={USER_ID}")

    val configurable = true

    val defaultEcpm: Option[Double] = Some(10)

    val configuration = {
      val sdkKeyDescription = {
        "Your SDK Key can be found on the AppLovin dashboard.  For more information on configuring AppLovin, please see our <a href='https://documentation.hyprmx.com/display/ADMIN/AppLovin' target='_blank'>documentation</a>."
      }

      val reportingDescription = {
        "Your Report Key can be found on the AppLovin dashboard.  For more information on configuring reporting for AppLovin, please see our <a href='https://documentation.hyprmx.com/display/ADMIN/AppLovin' target='_blank'>documentation</a>."
      }

      val appLovinAppNameDescription = {
        "Your application name can be found in AppLovin's dashboard. For more information on configuring reporting for AppLovin, please see our <a href='https://documentation.hyprmx.com/display/ADMIN/AppLovin' target='_blank'>documentation</a>."
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

    new UpdatableAdProvider(name, displayName, configuration, PlatformID, callbackURLFormat, configurable, defaultEcpm)
  }

  val UnityAds = {
    val name = Constants.UnityAds.Name

    val displayName = Constants.UnityAds.DisplayName

    val callbackURLFormat = Some(Constants.UnityAds.CallbackUrl)

    val configurable = true

    val defaultEcpm: Option[Double] = Some(10)

    val configurationDocumentationLink = "https://documentation.hyprmx.com/display/ADMIN/Unity+Ads"

    val s2sCallbackDocumentationLink = "https://documentation.hyprmx.com/display/ADMIN/Unity+Ads+Server+to+Server+Callbacks+Setup"

    val configuration = {
      val appIDDescription = {
        s"Your GAME ID can be found on the $displayName dashboard. For more information on configuring $displayName, please see our <a href='$configurationDocumentationLink' target='_blank'>documentation</a>."
      }

      val reportingDescription = {
        s"Your API Key can be found on the $displayName dashboard. For more information on configuring reporting for $displayName, please see our <a href='$configurationDocumentationLink' target='_blank'>documentation</a>."
      }

      val callbackDescription = {
        s"Your Shared Hash Key for Secure Callbacks must be obtained from $displayName support via email. For more information on configuring server to server callbacks for $displayName, please see our <a href='$s2sCallbackDocumentationLink' target='_blank'>documentation</a>."
      }
      s"""{ "requiredParams":[{"description": "$appIDDescription",
         |  "displayKey": "GAME ID",
         |  "key": "${Constants.UnityAds.GameID}",
         |  "value": "",
         |  "dataType": "String",
         |  "refreshOnAppRestart": true,
         |  "minLength": 1
         |  }],
         |  "reportingParams": [{"description": "$reportingDescription",
         |  "displayKey": "API Key",
         |  "key": "APIKey",
         |  "value": "",
         |  "dataType": "String",
         |  "refreshOnAppRestart": false
         |  }],
         |  "callbackParams": [{"description": "$callbackDescription",
         |  "displayKey": "Shared Hash Key",
         |  "key": "APIKey",
         |  "value": "",
         |  "dataType": "String",
         |  "refreshOnAppRestart": false
         |  }]}""".stripMargin.replaceAll("[\r]","").replaceAll("[\n]","")

    }
    new UpdatableAdProvider(name, displayName, configuration, PlatformID, callbackURLFormat, configurable, defaultEcpm)
  }
  val allAdProviders = List(AdColony, HyprMarketplace, Vungle, AppLovin, UnityAds)
}

object Platform extends ConfigVars {
  val IosPlatformID: Long = 1
  val IosPlatformName: String = "iOS"
  object Ios extends Platform(IosPlatformID, IosPlatformName) {
    val hyprMarketplaceID = ConfigVarsAdProviders.iosID
    val serverToServerDomain = ConfigVarsCallbackUrls.ios
  }

  val AndroidPlatformID: Long = 2
  val AndroidPlatformName: String = "Android"
  object Android extends Platform(AndroidPlatformID, AndroidPlatformName) {
    val hyprMarketplaceID = ConfigVarsAdProviders.androidID
    val serverToServerDomain = ConfigVarsCallbackUrls.android
  }

  /**
   * Finds the platform based on the ID
    *
    * @param platformID The ID of the Platform to which the app belongs (e.g. Android or iOS)
   * @return The Android or iOS object depending on the platform ID
   */
  def find(platformID: Long): Platform = {
    if(platformID == Platform.Android.PlatformID) Platform.Android else Platform.Ios
  }
}
