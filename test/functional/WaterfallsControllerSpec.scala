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
class WaterfallsControllerSpec extends SpecificationWithFixtures with WaterfallSpecSetup {
  val currentEmail = "someuser@jungroup.com"
  val currentPassword = "password"
  val distributorUser = running(FakeApplication(additionalConfiguration = testDB)) {
    DistributorUser.create(email, password, "Company Name")
    DistributorUser.findByEmail(email).get
  }

  val distributorID = distributorUser.distributorID.get

  val waterfallID = running(FakeApplication(additionalConfiguration = testDB)) {
    val appID = App.create(distributorID, "App 1").get
    Waterfall.create(appID, "New Waterfall").get
  }

  val waterfallAdProviderID1 = running(FakeApplication(additionalConfiguration = testDB)) {
    WaterfallAdProvider.create(waterfallID, adProviderID1.get).get
  }

  val wap1 = running(FakeApplication(additionalConfiguration = testDB)) {
    WaterfallAdProvider.find(waterfallAdProviderID1).get
  }

  val waterfallAdProviderID2 = running(FakeApplication(additionalConfiguration = testDB)) {
    WaterfallAdProvider.create(waterfallID, adProviderID2.get).get
  }

  val wap2 = running(FakeApplication(additionalConfiguration = testDB)) {
    WaterfallAdProvider.find(waterfallAdProviderID2).get
  }

  "Waterfall.update" should {
    "respond with a 200 with update is successful" in new WithFakeBrowser {
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
          "optimizedOrder" -> JsString("false")
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

    "reorder the waterfall in the same configuration as the drag and drop list" in new WithFakeBrowser with WaterfallEditSetup {
      val firstProvider = Waterfall.order(currentWaterfall.token)(0).providerName

      browser.goTo("http://localhost:" + port + "/login")
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.click("button")

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, waterfallID).url)
      browser.executeScript("$('ul').prepend($('li').last());")
      browser.executeScript("postUpdate();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#success-message").areDisplayed()
      Waterfall.order(currentWaterfall.token)(0).providerName must not equalTo(firstProvider)
    }

    "remove an ad provider from the order when a user clicks the Deactivate button" in new WithFakeBrowser with WaterfallEditSetup {
      val listSize = Waterfall.order(currentWaterfall.token).size

      browser.goTo("http://localhost:" + port + "/login")
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.click("button")

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, waterfallID).url)
      browser.$("button[name=status]").first().click()
      browser.executeScript("postUpdate();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#success-message").areDisplayed()
      Waterfall.order(currentWaterfall.token).size must equalTo(listSize - 1)
    }

    "configure an ad provider from the waterfall edit page" in new WithFakeBrowser with WaterfallEditSetup with JsonTesting {
      DB.withConnection { implicit connection =>
        SQL("update ad_providers set configuration_data = CAST({configuration_data} AS json) where id = {wap2_id};").on("configuration_data" -> configurationData, "wap2_id" -> wap2.id).executeInsert()
      }
      browser.goTo("http://localhost:" + port + "/login")
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.click("button")

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, waterfallID).url)
      browser.$("button[name=configure-wap]").first().click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      val configKey = "some key"
      browser.fill("input").`with`(configKey)
      browser.click("button[name=update-ad-provider]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#success-message").isPresent()
      (WaterfallAdProvider.find(wap2.id).get.configurationData \ configurationParams(0)).as[String] must beEqualTo(configKey)
    }

    "toggle waterfall optimization on and off" in new WithFakeBrowser with WaterfallEditSetup {
      browser.goTo("http://localhost:" + port + "/login")
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.click("button")

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, waterfallID).url)
      browser.$("button[name=optimized-order]").first().getAttribute("data-optimized-order") must beEqualTo("false")
      browser.$("button[name=optimized-order]").first().click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#success-message").areDisplayed()
      browser.$("button[name=optimized-order]").first().getAttribute("data-optimized-order") must beEqualTo("true")
      Waterfall.find(currentWaterfall.id).get.optimizedOrder must beEqualTo(true)
    }
  }

  trait WaterfallEditSetup {
    val currentWaterfall = running(FakeApplication(additionalConfiguration = testDB)) {
      Waterfall.find(waterfallID).get
    }
    running(FakeApplication(additionalConfiguration = testDB)) {
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1.id, wap1.waterfallID, wap1.adProviderID, Some(1), wap1.cpm, Some(true), wap1.fillRate, wap1.configurationData))
      WaterfallAdProvider.update(new WaterfallAdProvider(wap2.id, wap2.waterfallID, wap2.adProviderID, Some(0), wap2.cpm, Some(true), wap2.fillRate, wap2.configurationData))
      Waterfall.update(waterfallID, false)
    }
  }
  step(clean)
}

