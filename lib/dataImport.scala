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
      "{\"description\": \"Your AdColony App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"true\"}, " +
      "{\"description\": \"Your AdColony Zones\", \"key\": \"zoneIds\", \"value\":\"\", \"dataType\": \"Array\", \"refreshOnAppRestart\": \"true\"}" +
    "], " +
    "\"reportingParams\": [" +
      "{\"description\": \"Your API Key for Fyber\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}" +
    "], " +
    "\"callbackParams\": [" +
      "{\"description\": \"Your Event API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}" +
    "]" +
  "}"
}

AdProvider.create("AdColony", adColonyConfiguration, adColonyCallbackUrl, true, None)

// Create HyprMarketplace AdProvider
val hyprMarketplaceConfiguration = {
  "{" +
    "\"requiredParams\":[" +
      "{\"description\": \"Your HyprMX Distributor ID\", \"key\": \"distributorID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}, " +
      "{\"description\": \"Your HyprMX App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}" +
    "], " +
    "\"reportingParams\": [" +
      "{\"description\": \"Your API Key for HyprMX\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}, " +
      "{\"description\": \"Your Placement ID\", \"key\": \"placementID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}, " +
      "{\"description\": \"Your App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}" +
    "], " +
    "\"callbackParams\": [" +
      "{\"description\": \"Your Event API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}" +
    "]" +
  "}"
}

AdProvider.create("HyprMarketplace", hyprMarketplaceConfiguration, None, false, Some(20))

// Create Vungle AdProvider
val vungleCallbackUrl = Some("/v1/reward_callbacks/%s/vungle?amount=1&uid=%%user%%&openudid=%%udid%%&mac=%%mac%%&ifa=%%ifa%%&transaction_id=%%txid%%&digest=%%digest%%")

val vungleConfiguration = {
  "{" +
    "\"requiredParams\":[" +
      "{\"description\": \"Your Vungle App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"true\"}" +
    "], " +
    "\"reportingParams\": [" +
      "{\"description\": \"Your API Key for Fyber\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}" +
    "], " +
    "\"callbackParams\": [" +
      "{\"description\": \"Your Event API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}" +
    "]" +
  "}"
}

AdProvider.create("Vungle", vungleConfiguration, vungleCallbackUrl, true, None)

// Create AppLovin AdProvider
val appLovinCallbackUrl = Some("/v1/reward_callbacks/%s/app_lovin?idfa={IDFA}&hadid={HADID}&amount={AMOUNT}&currency={CURRENCY}&event_id={EVENT_ID}&user_id={USER_ID}")

val appLovinConfiguration = {
  "{" +
    "\"requiredParams\":[" +
      "{\"description\": \"Your AppLovin App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"true\"}" +
    "], " +
    "\"reportingParams\": [" +
      "{\"description\": \"Your API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}" +
    "], " +
    "\"callbackParams\": [" +
      "{\"description\": \"Your Event API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}" +
    "]" +
  "}"
}

AdProvider.create("AppLovin", appLovinConfiguration, appLovinCallbackUrl, true, None)

// Create Flurry AdProvider
val flurryCalbackUrl = Some("/v1/reward_callbacks/%s/flurry?idfa=%%{idfa}&sha1Mac=%%{sha1Mac}&appPrice=%%{appPrice}&fguid=%%{fguid}&rewardquantity=%%{rewardquantity}&fhash=%%{fhash}")

val flurryConfiguration = {
  "{" +
    "\"requiredParams\":[" +
      "{\"description\": \"Your Flurry App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"true\"}, " +
      "{\"description\": \"Your Flurry Space Name\", \"key\": \"spaceName\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"true\"}" +
    "], " +
    "\"reportingParams\": [" +
      "{\"description\": \"Your API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}" +
    "], " +
    "\"callbackParams\": [" +
      "{\"description\": \"Your Event API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}" +
    "]" +
  "}"
}

AdProvider.create("Flurry", flurryConfiguration, flurryCalbackUrl, true, None)
