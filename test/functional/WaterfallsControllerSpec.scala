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
          "optimizedOrder" -> JsString("false"),
          "testMode" -> JsString("false")
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

      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, waterfallID).url)
      browser.executeScript("$('ul').prepend($('li').last());")
      browser.executeScript("postUpdate();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").areDisplayed()
      Waterfall.order(currentWaterfall.token)(0).providerName must not equalTo(firstProvider)
    }

    "remove an ad provider from the order when a user clicks the Deactivate button" in new WithFakeBrowser with WaterfallEditSetup {
      val listSize = Waterfall.order(currentWaterfall.token).size

      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, waterfallID).url)
      browser.$("button[name=status]").first().click()
      browser.executeScript("postUpdate();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").areDisplayed()
      Waterfall.order(currentWaterfall.token).filter(adProvider => adProvider.active.get).size must equalTo(listSize - 1)
    }

    "return waterfall edit page with one waterfall" in new WithFakeBrowser with WaterfallEditSetup with JsonTesting {
      logInUser()

      val appID = App.create(distributorID, "App List").get
      Waterfall.create(appID, "New Waterfall").get

      browser.goTo(controllers.routes.WaterfallsController.list(distributorID, appID).url)
      browser.pageSource must contain("Edit Waterfall")
    }

    "return to app list is multiple waterfalls are found" in new WithFakeBrowser with WaterfallEditSetup with JsonTesting {
      logInUser()

      val appID = App.create(distributorID, "App List").get
      Waterfall.create(appID, "New Waterfall").get
      Waterfall.create(appID, "Second Waterfall").get

      browser.goTo(controllers.routes.WaterfallsController.list(distributorID, appID).url)
      browser.pageSource must contain("Waterfall could not be found.")
    }

    "configure an ad provider from the waterfall edit page" in new WithFakeBrowser with WaterfallEditSetup with JsonTesting {
      DB.withConnection { implicit connection =>
        SQL("update ad_providers set configuration_data = CAST({configuration_data} AS json) where id = {wap2_id};").on("configuration_data" -> configurationData, "wap2_id" -> wap2.id).executeInsert()
      }
      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, waterfallID).url)
      browser.$("button[name=configure-wap]").first().click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      val configKey = "some key"
      browser.fill("input").`with`(configKey)
      browser.click("button[name=update-ad-provider]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").isPresent()
      val waterfallAdProviderParams = WaterfallAdProvider.find(wap2.id).get.configurationData \ "requiredParams"
      (waterfallAdProviderParams \ configurationParams(0)).as[String] must beEqualTo(configKey)
    }

    "toggle waterfall optimization on and off" in new WithFakeBrowser with WaterfallEditSetup {
      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, waterfallID).url)
      Waterfall.find(currentWaterfall.id).get.optimizedOrder must beEqualTo(false)
      browser.executeScript("var button = $(':checkbox[id=optimized-mode-switch]'); button.prop('checked', true); postUpdate();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").areDisplayed()
      Waterfall.find(currentWaterfall.id).get.optimizedOrder must beEqualTo(true)
    }

    "toggle waterfall test mode on and off" in new WithFakeBrowser with WaterfallEditSetup {
      Waterfall.update(currentWaterfall.id, false, false)
      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, waterfallID).url)
      Waterfall.find(currentWaterfall.id).get.testMode must beEqualTo(false)
      browser.executeScript("var button = $(':checkbox[id=test-mode-switch]'); button.prop('checked', true); postUpdate();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").areDisplayed()
      Waterfall.find(currentWaterfall.id).get.testMode must beEqualTo(true)
    }

    "not set waterfall to live mode when no ad providers are configured" in new WithFakeBrowser {
      val newAppID = App.create(distributorID, "New App").get
      val newWaterfallID = Waterfall.create(newAppID, "New App").get

      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, newWaterfallID).url)
      Waterfall.find(newWaterfallID).get.testMode must beEqualTo(true)
      browser.executeScript("var button = $(':checkbox[id=test-mode-switch]'); button.prop('checked', true); button.click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-error").areDisplayed()
      Waterfall.find(newWaterfallID).get.testMode must beEqualTo(true)
    }

    "update the status of reporting for a waterfall ad provider" in new WithFakeBrowser {
      val newAppID = App.create(distributorID, "New App").get
      val newWaterfallID = Waterfall.create(newAppID, "New App").get

      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, newWaterfallID).url)
      Waterfall.find(newWaterfallID).get.testMode must beEqualTo(true)
      browser.$("button[name=status]").first().click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").areDisplayed()
      val newWap = WaterfallAdProvider.findAllByWaterfallID(newWaterfallID)(0)
      newWap.reportingActive must beEqualTo(false)
      browser.$("button[name=configure-wap]").first().click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      browser.executeScript("var button = $(':checkbox[id=reporting-active-switch]'); button.click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#wap-edit-success").areDisplayed()
      WaterfallAdProvider.find(newWap.id).get.reportingActive must beEqualTo(true)
    }
  }

  trait WaterfallEditSetup {
    val currentWaterfall = running(FakeApplication(additionalConfiguration = testDB)) {
      Waterfall.find(waterfallID).get
    }
    running(FakeApplication(additionalConfiguration = testDB)) {
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1.id, wap1.waterfallID, wap1.adProviderID, Some(1), wap1.cpm, Some(true), wap1.fillRate, wap1.configurationData, wap1.reportingActive))
      WaterfallAdProvider.update(new WaterfallAdProvider(wap2.id, wap2.waterfallID, wap2.adProviderID, Some(0), wap2.cpm, Some(true), wap2.fillRate, wap2.configurationData, wap1.reportingActive))
      Waterfall.update(waterfallID, false, false)
    }
  }
  step(clean)
}

