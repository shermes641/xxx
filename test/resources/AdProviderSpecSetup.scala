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
    val adColonyConfig = buildAdProviderConfig(Array(("appID", None, None, Some("true")), ("zoneIds", None, None, Some("true"))), Array(("APIKey", None, None, None)), Array(("APIKey", None, None, None)))
    AdProvider.create("AdColony", adColonyConfig, None).get
  }

  val appLovinID = running(FakeApplication(additionalConfiguration = testDB)) {
    val appLovinConfig = buildAdProviderConfig(Array(("appID", None, None, Some("true"))), Array(("APIKey", None, None, None)), Array(("APIKey", None, None, None)))
    AdProvider.create("AppLovin", appLovinConfig, None).get
  }

  val flurryID = running(FakeApplication(additionalConfiguration = testDB)) {
    val flurryConfig = buildAdProviderConfig(Array(("appID", None, None, Some("true")), ("spaceName", None, None, Some("true"))), Array(("APIKey", None, None, None)), Array(("APIKey", None, None, None)))
    AdProvider.create("Flurry", flurryConfig, None).get
  }

  val vungleID = running(FakeApplication(additionalConfiguration = testDB)) {
    val vungleConfig = buildAdProviderConfig(Array(("appID", None, None, Some("true"))), Array(("APIKey", None, None, None)), Array(("APIKey", None, None, None)))
    AdProvider.create("Vungle", vungleConfig, None).get
  }

  val hyprMarketplaceID = running(FakeApplication(additionalConfiguration = testDB)) {
    val hyprMarketplaceConfig = buildAdProviderConfig(Array(("distributorID", None, None, None), ("appID", None, None, None)),
      Array(("APIKey", None, None, None), ("placementID", None, None, None), ("appID", None, None, None)), Array(("APIKey", None, None, None)))
    AdProvider.create("HyprMarketplace", hyprMarketplaceConfig, None).get
  }
}
