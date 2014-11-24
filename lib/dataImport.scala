/**
 * Creates seed data to bootstrap the database.  users and appNames array determine the number of users/apps created.
 */

import models.DistributorUser
import models.App
import models.AdProvider

// The "correct" way to start the app
new play.core.StaticApplication(new java.io.File("."))

// Create ad providers
val adColonyCallbackUrl = Some("/v1/waterfall/%s/ad_colony_completion?id=[ID]&uid=[USER_ID]&amount=[AMOUNT]&currency=[CURRENCY]&open_udid=[OpenUDID]&udid=[UDID]&odin1=[ODIN1]&mac_sha1=[MAC_SHA1]&verifier=[VERIFIER]")
AdProvider.create("AdColony", "{\"requiredParams\":[{\"description\": \"Your AdColony App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}, {\"description\": \"Your AdColony Zones\", \"key\": \"zoneIds\", \"value\":\"\", \"dataType\": \"Array\"}], \"reportingParams\": [{\"description\": \"Your API Key for Fyber\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}], \"callbackParams\": [{\"description\": \"Your Event API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}]}", adColonyCallbackUrl, true, None)
AdProvider.create("HyprMarketplace", "{\"requiredParams\":[{\"description\": \"Your HyprMX Distributor ID\", \"key\": \"distributorID\", \"value\":\"\", \"dataType\": \"String\"}, {\"description\": \"Your HyprMX App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}], \"reportingParams\": [{\"description\": \"Your API Key for HyprMX\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}, {\"description\": \"Your Placement ID\", \"key\": \"placementID\", \"value\":\"\", \"dataType\": \"String\"}, {\"description\": \"Your App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}], \"callbackParams\": [{\"description\": \"Your Event API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}]}", None, false, Some(20))
val vungleCallbackUrl = Some("/v1/waterfall/%s/vungle_completion?amount=1&uid=%%user%%&openudid=%%udid%%&mac=%%mac%%&ifa=%%ifa%%&transaction_id=%%txid%%&digest=%%digest%%")
AdProvider.create("Vungle", "{\"requiredParams\":[{\"description\": \"Your Vungle App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}], \"reportingParams\": [{\"description\": \"Your API Key for Fyber\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}], \"callbackParams\": [{\"description\": \"Your Event API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}]}", vungleCallbackUrl, true, None)
val appLovinCallbackUrl = Some("/v1/waterfall/%s/app_lovin_completion?idfa={IDFA}&hadid={HADID}&amount={AMOUNT}&currency={CURRENCY}&event_id={EVENT_ID}&user_id={USER_ID}")
AdProvider.create("AppLovin", "{\"requiredParams\":[{\"description\": \"Your AppLovin App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}], \"reportingParams\": [{\"description\": \"Your API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}], \"callbackParams\": [{\"description\": \"Your Event API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}]}", appLovinCallbackUrl, true, None)
val flurryCalbackUrl = Some("/v1/waterfall/%s/flurry_completion?idfa=%%{idfa}&sha1Mac=%%{sha1Mac}&appPrice=%%{appPrice}&fguid=%%{fguid}&rewardquantity=%%{rewardquantity}&fhash=%%{fhash}")
AdProvider.create("Flurry", "{\"requiredParams\":[{\"description\": \"Your Flurry App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}, {\"description\": \"Your Flurry Space Name\", \"key\": \"spaceName\", \"value\":\"\", \"dataType\": \"String\"}], \"reportingParams\": [{\"description\": \"Your API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}], \"callbackParams\": [{\"description\": \"Your Event API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}]}", flurryCalbackUrl, true, None)

