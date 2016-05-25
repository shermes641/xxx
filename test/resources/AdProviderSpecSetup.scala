package resources

import models._
import play.api.test.Helpers._
import play.api.test._

trait AdProviderSpecSetup extends SpecificationWithFixtures {
  /**
    * Helper function to build JSON configuration for AdProviders.
    *
    * @param requiredParams  An Array of tuples used to generate the JSON elements for the requiredParams array.
    * @param reportingParams An Array of tuples used to generate the JSON elements for the reportingParams array.
    * @param callbackParams  An Array of tuples used to generate the JSON elements for the callbackParams array.
    * @return A JSON string to be saved in the configuration field of the ad_providers table.
    */
  def buildAdProviderConfig(requiredParams: Array[(String, Option[String], Option[String], Option[String])],
                            reportingParams: Array[(String, Option[String], Option[String], Option[String])],
                            callbackParams: Array[(String, Option[String], Option[String], Option[String])]) = {
    def buildJson(paramInfo: Array[(String, Option[String], Option[String], Option[String])]) = {
      paramInfo.map { params =>
        "{\"description\": \"Some Description\", \"key\": \"" + params._1 + "\", \"value\":\"" + params._2.getOrElse("") + "\", " +
          "\"dataType\": \"" + params._3.getOrElse("String") + "\", \"refreshOnAppRestart\": \"" + params._4.getOrElse("false") + "\"}"
      }.mkString(", ")
    }
    "{" +
      "\"requiredParams\":[" + buildJson(requiredParams) + "], " +
      "\"reportingParams\": [" + buildJson(reportingParams) + "], " +
      "\"callbackParams\": [" + buildJson(callbackParams) + "]" +
      "}"
  }

  val adColonyID = running(FakeApplication(additionalConfiguration = testDB)) {
    val name = "AdColony"
    val adColonyConfig = buildAdProviderConfig(Array(("appID", None, None, Some("true")), ("zoneIds", None, None, Some("true"))), Array(("APIKey", None, None, None)), Array(("APIKey", None, None, None)))
    val adColonyCallbackUrl = Some("/v1/reward_callbacks/%s/ad_colony?id=[ID]&uid=[USER_ID]&amount=[AMOUNT]&currency=[CURRENCY]&open_udid=[OpenUDID]&udid=[UDID]&odin1=[ODIN1]&mac_sha1=[MAC_SHA1]&verifier=[VERIFIER]&custom_id=[CUSTOM_ID]")
    AdProvider.create(
      name = name,
      displayName = name,
      configurationData = adColonyConfig,
      platformID = Platform.Ios.PlatformID,
      callbackUrlFormat = adColonyCallbackUrl,
      callbackUrlDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(name)
    ).get
  }

  val appLovinID = running(FakeApplication(additionalConfiguration = testDB)) {
    val name = "AppLovin"
    val appLovinConfig = buildAdProviderConfig(Array(("appID", None, None, Some("true"))), Array(("APIKey", None, None, None)), Array(("APIKey", None, None, None)))
    AdProvider.create(
      name = name,
      displayName = name,
      configurationData = appLovinConfig,
      platformID = Platform.Ios.PlatformID,
      callbackUrlFormat = None,
      callbackUrlDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(name)
    ).get
  }

  val vungleID = running(FakeApplication(additionalConfiguration = testDB)) {
    val name = "Vungle"
    val vungleConfig = buildAdProviderConfig(Array(("appID", None, None, Some("true"))), Array(("APIKey", None, None, None)), Array(("APIKey", None, None, None)))
    val vungleCallbackUrl = Some("/v1/waterfall/%s/vungle_completion?uid=%%user%%&openudid=%%udid%%&mac=%%mac%%&ifa=%%ifa%%&transaction_id=%%txid%%&digest=%%digest%%")
    AdProvider.create(
      name = name,
      displayName = name,
      configurationData = vungleConfig,
      platformID = Platform.Ios.PlatformID,
      callbackUrlFormat = vungleCallbackUrl,
      callbackUrlDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(name)
    ).get
  }

  val unityAdsID = running(FakeApplication(additionalConfiguration = testDB)) {
    val unityAdsConfig = buildAdProviderConfig(
      Array((Constants.UnityAds.GameID, None, None, Some("true"))),
      Array((Constants.AdProviderConfig.APIKey, None, None, None)),
      Array((Constants.AdProviderConfig.APIKey, None, None, None)))
    AdProvider.create(
      name = Constants.UnityAds.Name,
      displayName = Constants.UnityAds.DisplayName,
      configurationData = unityAdsConfig,
      platformID = Platform.Ios.PlatformID,
      callbackUrlFormat = Some(Constants.UnityAds.CallbackUrlSpec),
      callbackUrlDescription = Platform.Ios.UnityAds.callbackURLDescription.format(Constants.UnityAds.DisplayName)
    ).get
  }

  val hyprMarketplaceID = running(FakeApplication(additionalConfiguration = testDB)) {
    val name = "HyprMarketplace"
    val hyprMarketplaceConfig = buildAdProviderConfig(Array(("distributorID", None, None, None), ("appID", None, None, None)),
      Array(("APIKey", None, None, None), ("placementID", None, None, None), ("appID", None, None, None)), Array(("APIKey", None, None, None)))
    AdProvider.create(
      name = name,
      displayName = name,
      configurationData = hyprMarketplaceConfig,
      platformID = Platform.Ios.PlatformID,
      callbackUrlFormat = None,
      callbackUrlDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(name)
    ).get
  }
}

import play.api.libs.json._

//@formatter:off
trait AdProviderRequests {
  val appLovinRequest: JsValue = Json.obj(
    "method"  -> "GET",
    "path"    -> "/v1/reward_callbacks/2216d743-0f1c-4682-9895-2ccc1f4bb10e/app_lovin",
    "query"   -> Json.obj(
      "user_id"   -> "jcaintic@jungroup.com",
      "amount"    -> "1.000000",
      "currency"  -> "Points",
      "idfa"      -> "61F324DD-ABC7-438A-99BB-9F9DFFD47542",
      "hadid"     -> "",
      "event_id"  -> "fe07bfca25f8b26daa66d6e2d91e3bd10c1c264e"))

  val adColonyRequest: JsValue = Json.obj(
    "method"  -> "GET",
    "path"    -> "/v1/reward_callbacks/bb5f9b08-745f-43f9-afab-03e9bc6e05c1/ad_colony",
    "query"   -> Json.obj(
      "verifier"  -> "71846a125aeb001245f53431df3b98ed",
      "odin1"     -> "",
      "udid"      -> "",
      "mac_sha1"  -> "",
      "amount"    -> "1",
      "id"        -> "1416866680120",
      "open_udid" -> "",
      "currency"  -> "Credits",
      "uid"       -> "61f324dd-abc7-438a-99bb-9f9dffd47542",
      "custom_id" -> "bakhti.ios.testing100",
      "zone"      -> "vz592745972223473d9c"))

  val hyprRequest: JsValue = Json.obj(
    "method"  -> "GET",
    "path"    -> "/v1/reward_callbacks/1e15566a-4859-4c89-9f1d-7a9576a2e3d3/hyprmarketplace",
    "query"   -> Json.obj(
      "quantity"      -> "1",
      "offer_profit"  -> "0.01",
      "sig"           -> "0b89f364ee22f8cee55e5fdc4b9951397fbc1571b20c8606d6fe6f58415f670d",
      "reward_id"     -> "0",
      "sub_id"        -> "",
      "uid"           -> "TestUser",
      "time"          -> "1427143627"))

  val vungleRequest: JsValue = Json.obj(
    "method"  -> "GET",
    "path"    -> "/v1/reward_callbacks/bb9423fb-c5b0-489c-b5b5-112a49c9e611/vungle",
    "query"   -> Json.obj(
      "quantity"      -> "1",
      "offer_profit"  -> "0.01",
      "sig"           -> "0b89f364ee22f8cee55e5fdc4b9951397fbc1571b20c8606d6fe6f58415f670d",
      "reward_id"     -> "0",
      "sub_id"        -> "",
      "uid"           -> "TestUser",
      "time"          -> "1427143627"))

  val unityAdsRequest: JsValue = Json.obj(
    "method"  -> "GET",
    "path"    -> "/v1/reward_callbacks/d0574148-3d03-4525-89ac-bc8861951682/unity_ads",
    "query"   -> Json.obj(
      "sid"     -> "8a875d96ac9e948e6bab1fbff4a40e19",
      "oid"     -> "646480045",
      "hmac"    -> "a304f0025a1c531e90bc82363f27dc3c"))
}
//@formatter:on

