package resources

import models._
import play.api.libs.json.JsObject
import play.api.test.Helpers._
import play.api.test._

trait WaterfallSpecSetup extends SpecificationWithFixtures with DistributorUserSetup with AppCreationHelper {
  val (user, distributor) = running(FakeApplication(additionalConfiguration = testDB)) {
    newDistributorUser()
  }

  val (app1, waterfall, virtualCurrency1, _) = running(FakeApplication(additionalConfiguration = testDB)) {
    setUpApp(distributor.id.get)
  }

  val adProviderConfigData = {
    "{" +
      "\"requiredParams\":[" +
      "{\"description\": \"Your Distributor ID\", \"key\": \"distributorID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"true\"}, " +
      "{\"description\": \"Your App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"true\"}" +
      "], " +
      "\"reportingParams\": [" +
      "{\"description\": \"Your Reporting API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}, " +
      "{\"description\": \"Your Placement ID\", \"key\": \"placementID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}, " +
      "{\"description\": \"Your App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}" +
      "], " +
      "\"callbackParams\": [" +
      "{\"description\": \"Your Event API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}" +
      "]" +
      "}"
  }

  val adProviders = List("test ad provider 1", "test ad provider 2")

  val adProviderID1 = running(FakeApplication(additionalConfiguration = testDB)) {
    AdProvider.create(adProviders(0), adProviderConfigData, None)
  }

  val adProviderID2 = running(FakeApplication(additionalConfiguration = testDB)) {
    AdProvider.create(adProviders(1), adProviderConfigData, None)
  }

  /**
   * Helper function to create WaterfallAdProvider with configuration JSON.
   * @param waterfallID ID of the Waterfall to which the new WaterfallAdProvider belongs
   * @param adProviderID ID of the Ad Provider to which the new WaterfallAdProvider belongs
   * @param cpm The estimated cost per thousand completions for an AdProvider.
   * @param configurable Determines if the cpm value can be edited for the WaterfallAdProvider.
   * @param active Boolean value determining if the WaterfallAdProvider can be included in the AppConfig list of AdProviders.
   * @param configuration The JSON configuration.
   * @return An instance of the WaterfallAdProvider class.
   */
  def createWaterfallAdProvider(waterfallID: Long, adProviderID: Long, waterfallOrder: Option[Long] = None, cpm: Option[Double] = None, configurable: Boolean = true, active: Boolean = true, configuration: JsObject = JsObject(Seq("requiredParams" -> JsObject(Seq())))): WaterfallAdProvider = {
    val id = WaterfallAdProvider.create(waterfallID, adProviderID, waterfallOrder, cpm, configurable, active).get
    val wap = WaterfallAdProvider.find(id).get
    WaterfallAdProvider.update(new WaterfallAdProvider(id, wap.waterfallID, wap.adProviderID, None, None, Some(true), None, configuration, false))
    WaterfallAdProvider.find(id).get
  }
}
