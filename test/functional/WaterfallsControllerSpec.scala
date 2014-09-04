package functional

import models._
import org.specs2.runner._
import org.junit.runner._
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test._
import anorm.SQL
import play.api.db.DB

@RunWith(classOf[JUnitRunner])
class WaterfallsControllerSpec extends SpecificationWithFixtures {
  val distributorUser = running(FakeApplication(additionalConfiguration = testDB)) {
    DistributorUser.create(email, password, "Company Name")
  }

  val distributorID = running(FakeApplication(additionalConfiguration = testDB)) {
    Distributor.create("Company Name").get
  }

  val waterfallID = running(FakeApplication(additionalConfiguration = testDB)) {
    val appID = App.create(distributorID, "App 1").get
    Waterfall.create(appID, "New Waterfall").get
  }

  "Waterfall.update" should {
    "respond with a 200 with update is successful" in new WithFakeBrowser {
      val adProviderID1 = {
        DB.withConnection { implicit connection =>
          SQL("insert into ad_providers (name) values ('test ad provider 1')").executeInsert()
        }
      }
      val adProviderID2 = {
        DB.withConnection { implicit connection =>
          SQL("insert into ad_providers (name) values ('test ad provider 2')").executeInsert()
        }
      }
      val waterfallAdProviderID1 = WaterfallAdProvider.create(waterfallID, adProviderID1.get).get
      val waterfallAdProviderID2 = WaterfallAdProvider.create(waterfallID, adProviderID2.get).get
      val configInfo1 = JsObject(
        Seq(
          "id" -> JsString(waterfallAdProviderID1.toString),
          "newRecord" -> JsString("false"),
          "active" -> JsString("true"),
          "waterfallOrder" -> JsString("0")
        )
      )
      val configInfo2 = JsObject(
        Seq(
          "id" -> JsString(waterfallAdProviderID2.toString),
          "newRecord" -> JsString("false"),
          "active" -> JsString("true"),
          "waterfallOrder" -> JsString("1")
        )
      )
      val body = JsObject(
        Seq(
          "adProviderOrder" ->
            JsArray(Seq(
              configInfo1,
              configInfo2
            )),
          "waterfallName" -> JsString("New Waterfall")
        )
      )
      val request = FakeRequest(
        POST,
        controllers.routes.WaterfallsController.update(distributorID, waterfallID).url,
        FakeHeaders(Seq("Content-type" -> Seq("application/json"))),
        body
      )
      val Some(result) = route(request.withSession("distributorID" -> distributorID.toString(), "username" -> email))
      status(result) must equalTo(200)
    }

    "respond with 400 if JSON is not formed properly" in new WithFakeBrowser {
      val request = FakeRequest(
        POST,
        controllers.routes.WaterfallsController.update(distributorID, waterfallID).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request.withSession("distributorID" -> distributorID.toString(), "username" -> email))
      status(result) must equalTo(400)
    }
  }
  step(clean)
}
