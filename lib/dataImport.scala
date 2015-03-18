/**
 * Creates seed data to bootstrap the database.  users and appNames array determine the number of users/apps created.
 */

import models.AdProvider

// The "correct" way to start the app
new play.core.StaticApplication(new java.io.File("."))

// Create AdProviders

// Create AdColony AdProvider
val adColonyCallbackUrl = Some("/v1/reward_callbacks/%s/ad_colony?id=[ID]&uid=[USER_ID]&amount=[AMOUNT]&" +
  "currency=[CURRENCY]&open_udid=[OpenUDID]&udid=[UDID]&odin1=[ODIN1]&mac_sha1=[MAC_SHA1]&verifier=[VERIFIER]&custom_id=[CUSTOM_ID]")

val adColonyConfiguration = {
  val adColonyZoneIDDetails = {
    "Add a single zone or multiple zones separated by commas. Please note, we currently only support Value Exchange Zones. " +
      "For more information on configuration, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/Configuring+AdColony' target='_blank'>documentation</a>"
  }

  val adColonyAppIDDescription = {
    "Your AdColony App ID can be found on the AdColony dashboard.  For more information on configuring AdColony, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/Configuring+AdColony' target='_blank'>documentation</a>."
  }

  val adColonyReportingDescription = {
    "Your Read-Only API Key can be found on the AdColony dashboard.  For more information on configuring reporting for AdColony, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/Configuring+AdColony' target='_blank'>documentation</a>."
  }

  val adColonyCallbackDescription = {
    "Your V4VC Secret Key can be found on the AdColony dashboard.  For more information on configuring server to server callbacks for AdColony, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/AdColony+Server+to+Server+Callbacks+Setup' target='_blank'>documentation</a>."
  }

  "{" +
    "\"requiredParams\":[" +
      "{\"description\": \"" + adColonyAppIDDescription + "\", \"displayKey\": \"AdColony App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": true, \"minLength\": 1}, " +
      "{\"description\": \"" + adColonyZoneIDDetails + "\", \"displayKey\": \"Zone IDs\", \"key\": \"zoneIds\", \"value\":\"\", \"dataType\": \"Array\", \"refreshOnAppRestart\": true, \"minLength\": 1}" +
    "], " +
    "\"reportingParams\": [" +
      "{\"description\": \"" + adColonyReportingDescription + "\", \"displayKey\": \"Reporting API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
    "], " +
    "\"callbackParams\": [" +
      "{\"description\": \"" + adColonyCallbackDescription + "\", \"displayKey\": \"V4VC Secret Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
    "]" +
  "}"
}

AdProvider.create("AdColony", adColonyConfiguration, adColonyCallbackUrl, true, None)

// Create HyprMarketplace AdProvider
val hyprMarketplaceConfiguration = {
  "{" +
    "\"requiredParams\":[" +
      "{\"description\": \"Your HyprMX Distributor ID\", \"displayKey\": \"Distributor ID\", \"key\": \"distributorID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false, \"minLength\": 1}, " +
      "{\"description\": \"Your HyprMX Property ID\", \"displayKey\": \"\", \"key\": \"propertyID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false, \"minLength\": 1}" +
    "], " +
    "\"reportingParams\": [" +
      "{\"description\": \"Your API Key for HyprMX\", \"displayKey\": \"API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}, " +
      "{\"description\": \"Your Placement ID\", \"displayKey\": \"Placement ID\", \"key\": \"placementID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}, " +
      "{\"description\": \"Your App ID\", \"displayKey\": \"App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
    "], " +
    "\"callbackParams\": [" +
    "]" +
  "}"
}

AdProvider.create("HyprMarketplace", hyprMarketplaceConfiguration, None, false, Some(20))

// Create Vungle AdProvider
val vungleCallbackUrl = Some("/v1/reward_callbacks/%s/vungle?amount=%s&uid=%%user%%&openudid=%%udid%%&mac=%%mac%%&ifa=%%ifa%%&transaction_id=%%txid%%&digest=%%digest%%")

val vungleConfiguration = {
  val vungleAppIDDescription = {
    "Your App ID can be found on the Vungle dashboard.  For more information on configuring Vungle, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/Configuring+Vungle' target='_blank'>documentation</a>."
  }

  val vungleReportingDescription = {
    "Your Reporting API Key can be found on the Vungle dashboard.  For more information on configuring reporting for Vungle, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/Configuring+Vungle' target='_blank'>documentation</a>."
  }

  val vungleCallbackDescription = {
    "Your Secret Key for Secure Callback can be found on the Vungle dashboard.  For more information on configuring server to server callbacks for Vungle, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/Vungle+Server+to+Server+Callbacks+Setup' target='_blank'>documentation</a>."
  }

  "{" +
    "\"requiredParams\":[" +
      "{\"description\": \"" + vungleAppIDDescription + "\", \"displayKey\": \"App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": true, \"minLength\": 1}" +
    "], " +
    "\"reportingParams\": [" +
      "{\"description\": \"" + vungleReportingDescription + "\", \"displayKey\": \"Reporting API ID\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
    "], " +
    "\"callbackParams\": [" +
      "{\"description\": \"" + vungleCallbackDescription + "\", \"displayKey\": \"Secret Key for Secure Callback\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
    "]" +
  "}"
}

AdProvider.create("Vungle", vungleConfiguration, vungleCallbackUrl, true, None)

// Create AppLovin AdProvider
val appLovinCallbackUrl = Some("/v1/reward_callbacks/%s/app_lovin?idfa={IDFA}&hadid={HADID}&amount={AMOUNT}&currency={CURRENCY}&event_id={EVENT_ID}&user_id={USER_ID}")

val appLovinConfiguration = {
  val appLovinSDKKeyDescription = {
    "Your SDK Key can be found on the AppLovin dashboard.  For more information on configuring AppLovin, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/Configuring+AppLovin' target='_blank'>documentation</a>."
  }

  val appLovinReportingDescription = {
    "Your Report Key can be found on the AppLovin dashboard.  For more information on configuring reporting for AppLovin, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/Configuring+AppLovin' target='_blank'>documentation</a>."
  }

  "{" +
    "\"requiredParams\":[" +
      "{\"description\": \"" + appLovinSDKKeyDescription + "\", \"displayKey\": \"SDK Key\", \"key\": \"sdkKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": true, \"minLength\": 4}" +
    "], " +
    "\"reportingParams\": [" +
      "{\"description\": \"" + appLovinReportingDescription + "\", \"displayKey\": \"Report Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
    "], " +
    "\"callbackParams\": [" +
    "]" +
  "}"
}

AdProvider.create("AppLovin", appLovinConfiguration, appLovinCallbackUrl, true, None)
