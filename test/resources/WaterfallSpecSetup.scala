package resources

import models._
import play.api.test.Helpers._
import play.api.test._

trait WaterfallSpecSetup extends SpecificationWithFixtures with DistributorUserSetup with AppSpecSetup {
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
}
