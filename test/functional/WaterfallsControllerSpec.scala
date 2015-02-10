package functional

import anorm.SQL
import models._
import org.specs2.runner._
import org.junit.runner._
import play.api.db.DB
import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers._
import resources._

@RunWith(classOf[JUnitRunner])
class WaterfallsControllerSpec extends SpecificationWithFixtures with WaterfallSpecSetup {
  val wap1 = running(FakeApplication(additionalConfiguration = testDB)) {
    createWaterfallAdProvider(waterfall.id, adProviderID1.get, None, None, true, true)
  }

  val wap2 = running(FakeApplication(additionalConfiguration = testDB)) {
    createWaterfallAdProvider(waterfall.id, adProviderID2.get, None, None, true, true)
  }

  "WaterfallsController.list" should {
    "return waterfall edit page with one waterfall" in new WithAppBrowser(distributor.id.get) {
      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(distributor.id.get, currentApp.id, None).url)
      browser.pageSource must contain("Edit Waterfall")
      browser.pageSource must contain(currentWaterfall.name)
    }

    "return to app list if multiple waterfalls are found" in new WithAppBrowser(distributor.id.get) {
      logInUser()

      DB.withTransaction { implicit connection => createWaterfallWithConfig(currentApp.id, "New Waterfall") }
      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(distributor.id.get, currentApp.id, None).url)
      browser.pageSource must contain("Waterfall could not be found.")
    }
  }

  "WaterfallsController.update" should {
    "respond with a 200 and an updated generation number if update is successful" in new WithFakeBrowser {
      val originalGeneration = generationNumber(app1.id)
      val configInfo1 = JsObject(
        Seq(
          "waterfallAdProviderID" -> JsString(wap1.id.toString),
          "newRecord" -> JsBoolean(false),
          "active" -> JsBoolean(true),
          "waterfallOrder" -> JsNumber(0),
          "cpm" -> JsNumber(5.0),
          "configurable" -> JsBoolean(true),
          "pending" -> JsBoolean(true)
        )
      )
      val configInfo2 = JsObject(
        Seq(
          "waterfallAdProviderID" -> JsString(wap1.id.toString),
          "newRecord" -> JsBoolean(false),
          "active" -> JsBoolean(true),
          "waterfallOrder" -> JsNumber(1),
          "cpm" -> JsNumber(5.0),
          "configurable" -> JsBoolean(true),
          "pending" -> JsBoolean(true)
        )
      )
      val body = JsObject(
        Seq(
          "adProviderOrder" ->
            JsArray(Seq(
              configInfo1,
              configInfo2
            )),
          "optimizedOrder" -> JsBoolean(false),
          "testMode" -> JsBoolean(false),
          "appToken" -> JsString(app1.token),
          "waterfallID" -> JsString(waterfall.id.toString),
          "generationNumber" -> JsString((generationNumber(app1.id)).toString)
        )
      )
      val request = FakeRequest(
        POST,
        controllers.routes.WaterfallsController.update(distributor.id.get, waterfall.id).url,
        FakeHeaders(Seq("Content-type" -> Seq("application/json"))),
        body
      )
      val Some(result) = route(request.withSession("distributorID" -> distributor.id.get.toString, "username" -> email))
      status(result) must equalTo(200)
      val generationNumberResponse = (Json.parse(contentAsString(result)) \ "newGenerationNumber").as[String].toLong
      generationNumberResponse must beEqualTo(originalGeneration + 1)
      generationNumber(app1.id) must beEqualTo(originalGeneration + 1)
    }

    "respond with 400 if JSON is not formed properly" in new WithFakeBrowser {
      val originalGeneration = generationNumber(app1.id)
      val request = FakeRequest(
        POST,
        controllers.routes.WaterfallsController.update(distributor.id.get, waterfall.id).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request.withSession("distributorID" -> distributor.id.get.toString, "username" -> email))
      status(result) must equalTo(400)
      generationNumber(app1.id) must beEqualTo(originalGeneration)
    }

    "reorder the waterfall in the same configuration as the drag and drop list" in new WithFakeBrowser {
      clearGeneration(app1.id)
      val originalGeneration = generationNumber(app1.id)
      val waterfallOrder = DB.withTransaction { implicit connection => Waterfall.order(app1.token) }
      val firstProvider = waterfallOrder(0).providerName

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, waterfall.id).url)
      browser.executeScript("var providers = angular.element($('#waterfall-controller')).scope().waterfallData.waterfallAdProviderList; angular.element($('#waterfall-controller')).scope().waterfallData.waterfallAdProviderList = [providers.pop()].concat(providers); angular.element($('#waterfall-controller')).scope().sortableOptions.stop();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").areDisplayed()
      val newOrder = DB.withTransaction { implicit connection => Waterfall.order(app1.token) }
      newOrder(0).providerName must not equalTo(firstProvider)
      generationNumber(app1.id) must beEqualTo(originalGeneration + 1)
    }

    "remove an ad provider from the order when a user clicks the Deactivate button" in new WithFakeBrowser {
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalOrder = DB.withTransaction { implicit connection => Waterfall.order(app1.token) }

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, waterfall.id).url)
      browser.$("button[name=status]").first().click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").areDisplayed()
      val newOrder = DB.withTransaction { implicit connection => Waterfall.order(app1.token) }
      newOrder.filter(adProvider => adProvider.active.get).size must equalTo(originalOrder.size - 1)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration + 1)
    }

    "configure an ad provider from the waterfall edit page" in new WithFakeBrowser with JsonTesting {
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1.id, wap1.waterfallID, wap1.adProviderID, Some(1), wap1.cpm, Some(true), wap1.fillRate, wap1.configurationData, wap1.reportingActive))
      WaterfallAdProvider.update(new WaterfallAdProvider(wap2.id, wap2.waterfallID, wap2.adProviderID, Some(0), wap2.cpm, Some(true), wap2.fillRate, wap2.configurationData, wap1.reportingActive))
      Waterfall.update(waterfall.id, false, false)
      clearGeneration(app1.id)
      val originalGeneration = generationNumber(app1.id)
      DB.withConnection { implicit connection =>
        SQL("update ad_providers set configuration_data = CAST({configuration_data} AS json) where id = {wap2_id};").on("configuration_data" -> configurationData, "wap2_id" -> wap2.id).executeInsert()
      }

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, waterfall.id).url)
      browser.$("button[name=configure-wap]").first().click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      val configKey = "some key"
      browser.fill("input").`with`("5.0", configKey)
      browser.click("button[name=update-ad-provider]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").isPresent()
      val waterfallAdProviderParams = WaterfallAdProvider.find(wap2.id).get.configurationData \ "requiredParams"
      (waterfallAdProviderParams \ configurationParams(0)).as[String] must beEqualTo(configKey)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration + 1)
    }

    "toggle waterfall optimization on and off" in new WithFakeBrowser {
      clearGeneration(app1.id)
      val originalGeneration = generationNumber(app1.id)

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, waterfall.id).url)
      Waterfall.find(waterfall.id, distributor.id.get).get.optimizedOrder must beEqualTo(false)
      browser.executeScript("$('#optimized-mode-switch').click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").areDisplayed()
      Waterfall.find(waterfall.id, distributor.id.get).get.optimizedOrder must beEqualTo(true)
      generationNumber(app1.id) must beEqualTo(originalGeneration + 1)
    }

    "toggle waterfall test mode on and off" in new WithFakeBrowser {
      Waterfall.update(waterfall.id, false, false)
      DB.withTransaction { implicit connection => AppConfig.createWithWaterfallIDInTransaction(waterfall.id, None) }
      val originalGeneration = generationNumber(waterfall.app_id)
      AppConfig.findLatest(app1.token).get.configuration \ "testMode" must beEqualTo(JsBoolean(false))

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, waterfall.id).url)
      Waterfall.find(waterfall.id, distributor.id.get).get.testMode must beEqualTo(false)
      browser.executeScript("$('#test-mode-switch').click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").areDisplayed()
      Waterfall.find(waterfall.id, distributor.id.get).get.testMode must beEqualTo(true)
      generationNumber(app1.id) must beEqualTo(originalGeneration + 1)
      AppConfig.findLatest(app1.token).get.configuration \ "testMode" must beEqualTo(JsBoolean(true))
    }

    "not set waterfall to live mode when no ad providers are active" in new WithAppBrowser(distributor.id.get) {
      val originalGeneration = generationNumber(currentApp.id)

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      Waterfall.find(currentWaterfall.id, distributor.id.get).get.testMode must beEqualTo(true)
      browser.executeScript("$('#test-mode-switch').click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").containsText("You must activate at least one Ad Provider")
      Waterfall.find(currentWaterfall.id, distributor.id.get).get.testMode must beEqualTo(true)
      generationNumber(currentApp.id) must beEqualTo(originalGeneration)
    }

    "not allow an ad provider to be deactivated if there are no other active ad providers and the waterfall is in live mode" in new WithFakeBrowser {
      val originalGeneration = generationNumber(waterfall.app_id)
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1.id, wap1.waterfallID, wap1.adProviderID, Some(1), wap1.cpm, Some(true), wap1.fillRate, wap1.configurationData, wap1.reportingActive))
      WaterfallAdProvider.update(new WaterfallAdProvider(wap2.id, wap2.waterfallID, wap2.adProviderID, Some(0), wap2.cpm, Some(false), wap2.fillRate, wap2.configurationData, wap1.reportingActive))
      Waterfall.update(waterfall.id, false, false)

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, waterfall.id).url)
      browser.executeScript("$('button[name=status]').last().click()")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").containsText("At least one Ad Provider must be active")
      WaterfallAdProvider.find(wap1.id).get.active.get must beEqualTo(true)
      generationNumber(app1.id) must beEqualTo(originalGeneration)
    }
  }

  "WaterfallsController.edit" should {
    "display default eCPM values for Ad Providers" in new WithAppBrowser(distributor.id.get) {
      val defaultEcpm = "20.00"
      val adProviderWithDefaultEcpmID = AdProvider.create("Test Ad Provider With Default eCPM", adProviderConfigData, None, false, Some(defaultEcpm.toDouble)).get

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      browser.$("div[name=cpm]", 2).getText must contain(defaultEcpm)
    }

    "redirect the distributor user to their own apps index page if they try to edit a Waterfall they do not own" in new WithAppBrowser(distributor.id.get) {
      val (maliciousUser, maliciousDistributor) = newDistributorUser("newuseremail@gmail.com")

      logInUser(maliciousUser.email, password)

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(maliciousDistributor.id.get, currentWaterfall.id).url)
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#flash").hasText("Waterfall could not be found.")
      browser.url() must beEqualTo(controllers.routes.AnalyticsController.show(maliciousDistributor.id.get, None).url)
    }

    "redirect the distributor user to their own apps index page if they try to edit a Waterfall using another distributor ID" in new WithAppBrowser(distributor.id.get) {
      val (maliciousUser, maliciousDistributor) = newDistributorUser("newuseremail2@gmail.com")

      logInUser(maliciousUser.email, password)

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      browser.url() must beEqualTo(controllers.routes.AnalyticsController.show(maliciousDistributor.id.get, None).url)
    }

    "render the updated pending status, on browser refresh, for HyprMarketplace ad provider" in new WithAppBrowser(distributor.id.get, "Some new test app") {
      val wapID = WaterfallAdProvider.create(currentWaterfall.id, adProviderID2.get, None, None, true, false, true).get

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      browser.findFirst(".pending-status").isDisplayed must beTrue
      val wap = WaterfallAdProvider.find(wapID).get
      wap.pending must beEqualTo(true)
      DB.withConnection { implicit connection =>
        SQL(""" UPDATE waterfall_ad_providers SET pending=false WHERE id={id}; """).on("id" -> wapID).executeUpdate()
      }
      val updatedWap = WaterfallAdProvider.find(wapID).get
      updatedWap.pending must beEqualTo(false)

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      browser.findFirst(".pending-status").isDisplayed must beFalse
    }

    "not display the activate button for WaterfallAdProviders that have not yet been configured" in new WithAppBrowser(distributor.id.get) {
      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      browser.find("button[name=status]").getText must not contain "Activate"
    }

    "not display the activate button if the user cancels out of the configuration" in new WithAppBrowser(distributor.id.get) {
      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      browser.findFirst("button[name=status]").getText must not contain "Activate"
      browser.$("button[name=configure-wap]").first().click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      browser.executeScript("$('.close_button').click();")
      browser.find("button[name=status]").getText must not contain "Activate"
    }

    "display the Activate button after a WaterfallAdProvider has been configured" in new WithAppBrowser(distributor.id.get) {
      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      browser.executeScript("$('button[name=configure-wap]').first().click()")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      browser.fill("input").`with`("5.0", "some key")
      browser.executeScript("$('button[name=update-ad-provider]').click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").areDisplayed()
      browser.findFirst("button[name=status]").getText must contain("Activate")
      browser.$("button[name=status]").first().click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").containsText("Waterfall updated!")
      WaterfallAdProvider.findAllOrdered(currentWaterfall.id).size must beEqualTo(1)
    }
  }
}
