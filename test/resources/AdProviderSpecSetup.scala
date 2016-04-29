package resources

import models._
import play.api.test.Helpers._
import play.api.test._

trait AdProviderSpecSetup extends SpecificationWithFixtures {
  /**
   * Helper function to build JSON configuration for AdProviders.
   * @param requiredParams An Array of tuples used to generate the JSON elements for the requiredParams array.
   * @param reportingParams An Array of tuples used to generate the JSON elements for the reportingParams array.
   * @param callbackParams An Array of tuples used to generate the JSON elements for the callbackParams array.
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
    AdProvider.create(name = name, displayName = name, configurationData = adColonyConfig, platformID = Platform.Ios.PlatformID, callbackUrlFormat = adColonyCallbackUrl).get
  }

  val appLovinID = running(FakeApplication(additionalConfiguration = testDB)) {
    val name = "AppLovin"
    val appLovinConfig = buildAdProviderConfig(Array(("appID", None, None, Some("true"))), Array(("APIKey", None, None, None)), Array(("APIKey", None, None, None)))
    AdProvider.create(name = name, displayName = name, configurationData = appLovinConfig, platformID = Platform.Ios.PlatformID, callbackUrlFormat = None).get
  }

  val vungleID = running(FakeApplication(additionalConfiguration = testDB)) {
    val name = "Vungle"
    val vungleConfig = buildAdProviderConfig(Array(("appID", None, None, Some("true"))), Array(("APIKey", None, None, None)), Array(("APIKey", None, None, None)))
    val vungleCallbackUrl = Some("/v1/waterfall/%s/vungle_completion?uid=%%user%%&openudid=%%udid%%&mac=%%mac%%&ifa=%%ifa%%&transaction_id=%%txid%%&digest=%%digest%%")
    AdProvider.create(name = name, displayName = name, configurationData = vungleConfig, platformID = Platform.Ios.PlatformID, callbackUrlFormat = vungleCallbackUrl).get
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
      callbackUrlFormat = Some(Constants.UnityAds.CallbackUrlSpec)
    ).get
  }

  val hyprMarketplaceID = running(FakeApplication(additionalConfiguration = testDB)) {
    val name = "HyprMarketplace"
    val hyprMarketplaceConfig = buildAdProviderConfig(Array(("distributorID", None, None, None), ("appID", None, None, None)),
      Array(("APIKey", None, None, None), ("placementID", None, None, None), ("appID", None, None, None)), Array(("APIKey", None, None, None)))
    AdProvider.create(name = name, displayName = name, configurationData = hyprMarketplaceConfig, platformID = Platform.Ios.PlatformID, callbackUrlFormat = None).get
  }
}
