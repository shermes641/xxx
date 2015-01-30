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
  "{" +
    "\"requiredParams\":[" +
      "{\"description\": \"Your AdColony App ID\", \"displayKey\": \"AdColony App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"true\"}, " +
      "{\"description\": \"Your AdColony Zones\", \"displayKey\": \"Zone IDs\", \"key\": \"zoneIds\", \"value\":\"\", \"dataType\": \"Array\", \"refreshOnAppRestart\": \"true\"}" +
    "], " +
    "\"reportingParams\": [" +
      "{\"description\": \"Your Reporting API Key\", \"displayKey\": \"Reporting API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}" +
    "], " +
    "\"callbackParams\": [" +
      "{\"description\": \"Your Event API Key\", \"displayKey\": \"V4VC Secret Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}" +
    "]" +
  "}"
}

AdProvider.create("AdColony", adColonyConfiguration, adColonyCallbackUrl, true, None)

// Create HyprMarketplace AdProvider
val hyprMarketplaceConfiguration = {
  "{" +
    "\"requiredParams\":[" +
      "{\"description\": \"Your HyprMX Distributor ID\", \"displayKey\": \"Distributor ID\", \"key\": \"distributorID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}, " +
      "{\"description\": \"Your HyprMX Property ID\", \"displayKey\": \"\", \"key\": \"propertyID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}" +
    "], " +
    "\"reportingParams\": [" +
      "{\"description\": \"Your API Key for HyprMX\", \"displayKey\": \"API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}, " +
      "{\"description\": \"Your Placement ID\", \"displayKey\": \"Placement ID\", \"key\": \"placementID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}, " +
      "{\"description\": \"Your App ID\", \"displayKey\": \"App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}" +
    "], " +
    "\"callbackParams\": [" +
      "{\"description\": \"Your Event API Key\", \"displayKey\": \"Callback Secret\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}" +
    "]" +
  "}"
}

AdProvider.create("HyprMarketplace", hyprMarketplaceConfiguration, None, false, Some(20))

// Create Vungle AdProvider
val vungleCallbackUrl = Some("/v1/reward_callbacks/%s/vungle?amount=1&uid=%%user%%&openudid=%%udid%%&mac=%%mac%%&ifa=%%ifa%%&transaction_id=%%txid%%&digest=%%digest%%")

val vungleConfiguration = {
  "{" +
    "\"requiredParams\":[" +
      "{\"description\": \"Your Vungle App ID\", \"displayKey\": \"App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"true\"}" +
    "], " +
    "\"reportingParams\": [" +
      "{\"description\": \"Your Reporting API ID\", \"displayKey\": \"Reporting API ID\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}" +
    "], " +
    "\"callbackParams\": [" +
      "{\"description\": \"Secret Key for Secure Callback\", \"displayKey\": \"Secret Key for Secure Callback\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}" +
    "]" +
  "}"
}

AdProvider.create("Vungle", vungleConfiguration, vungleCallbackUrl, true, None)

// Create AppLovin AdProvider
val appLovinCallbackUrl = Some("/v1/reward_callbacks/%s/app_lovin?idfa={IDFA}&hadid={HADID}&amount={AMOUNT}&currency={CURRENCY}&event_id={EVENT_ID}&user_id={USER_ID}")

val appLovinConfiguration = {
  "{" +
    "\"requiredParams\":[" +
      "{\"description\": \"Your AppLovin SDK Key\", \"displayKey\": \"SDK Key\", \"key\": \"sdkKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"true\"}" +
    "], " +
    "\"reportingParams\": [" +
      "{\"description\": \"Your AppLovin Report Key\", \"displayKey\": \"Report Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}" +
    "], " +
    "\"callbackParams\": [" +
    "]" +
  "}"
}

AdProvider.create("AppLovin", appLovinConfiguration, appLovinCallbackUrl, true, None)
