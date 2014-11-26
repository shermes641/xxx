package models

import anorm._
import play.api.Play.current
import play.api.db.DB
import play.api.libs.json.{JsObject, JsString}
import play.api.test.Helpers._
import play.api.test._

trait WaterfallSpecSetup extends SpecificationWithFixtures {
  val user = running(FakeApplication(additionalConfiguration = testDB)) {
    DistributorUser.create(email, password, "Company Name")
    DistributorUser.findByEmail(email).get
  }

  val distributor = running(FakeApplication(additionalConfiguration = testDB)) {
    Distributor.find(user.distributorID.get).get
  }

  val app1 = running(FakeApplication(additionalConfiguration = testDB)) {
    val appID = App.create(distributor.id.get, "App 1").get
    App.find(appID).get
  }

  val virtualCurrency1 = running(FakeApplication(additionalConfiguration = testDB)) {
    val vcID = VirtualCurrency.create(app1.id, "Gold", 10, None, None, Some(true)).get
    VirtualCurrency.find(vcID).get
  }

  val waterfall = running(FakeApplication(additionalConfiguration = testDB)) {
    val waterfallID = DB.withTransaction { implicit connection =>
      val id = Waterfall.create(app1.id, app1.name)
      AppConfig.create(app1.id, app1.token, generationNumber(app1.id))
      id.get
    }
    Waterfall.find(waterfallID)
  }

  val adProviders = List("test ad provider 1", "test ad provider 2")

  val adProviderID1 = running(FakeApplication(additionalConfiguration = testDB)) {
    AdProvider.create(adProviders(0), "{\"requiredParams\":[{\"description\": \"Your HyprMX Distributor ID\", \"key\": \"distributorID\", \"value\":\"\", \"dataType\": \"String\"}, {\"description\": \"Your HyprMX App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}], \"reportingParams\": [{\"description\": \"Your API Key for Fyber\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}, {\"description\": \"Your Placement ID\", \"key\": \"placementID\", \"value\":\"\", \"dataType\": \"String\"}, {\"description\": \"Your App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}], \"callbackParams\": [{\"description\": \"Your Event API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}]}", None)
  }

  val adProviderID2 = running(FakeApplication(additionalConfiguration = testDB)) {
    AdProvider.create(adProviders(1), "{\"requiredParams\":[{\"description\": \"Your HyprMX Distributor ID\", \"key\": \"distributorID\", \"value\":\"\", \"dataType\": \"String\"}, {\"description\": \"Your HyprMX App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}], \"reportingParams\": [{\"description\": \"Your API Key for Fyber\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}, {\"description\": \"Your Placement ID\", \"key\": \"placementID\", \"value\":\"\", \"dataType\": \"String\"}, {\"description\": \"Your App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}], \"callbackParams\": [{\"description\": \"Your Event API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}]}", None)
  }
}
