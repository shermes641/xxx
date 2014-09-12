package functional

import models._
import org.specs2.runner._
import org.junit.runner._
import play.api.test.Helpers._
import play.api.test._
import anorm.SQL
import play.api.db.DB
import play.api.Play.current

@RunWith(classOf[JUnitRunner])
class APIControllerSpec extends SpecificationWithFixtures {
  "APIController.waterfall" should {
    val distributor = running(FakeApplication(additionalConfiguration = testDB)) {
      val distributorID = Distributor.create("Company Name").get
      Distributor.find(distributorID).get
    }

    val app1 = running(FakeApplication(additionalConfiguration = testDB)) {
      val appID = App.create(distributor.id.get, "App 1").get
      App.find(appID).get
    }

    val waterfall = running(FakeApplication(additionalConfiguration = testDB)) {
      val waterfallID = Waterfall.create(app1.id, app1.name).get
      Waterfall.find(waterfallID).get
    }

    val adProviderID1 = running(FakeApplication(additionalConfiguration = testDB)) {
      DB.withConnection { implicit connection =>
        SQL("insert into ad_providers (name) values ('test ad provider 1')").executeInsert()
      }
    }

    "respond with 400 if token is not valid or no ad providers are configured" in new WithFakeBrowser {
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.waterfallV1("some-fake-token").url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(400)
      contentAsString(result) must contain("No ad providers are active.")
    }

    "respond with waterfall configuration if token is valid" in new WithFakeBrowser {
      WaterfallAdProvider.create(waterfall.id, adProviderID1.get, Some(0))
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.waterfallV1(waterfall.token).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
    }
  }
}
