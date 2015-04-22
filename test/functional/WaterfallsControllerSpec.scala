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
class WaterfallsControllerSpec extends SpecificationWithFixtures with WaterfallSpecSetup with DistributorUserSetup {
  val wap1 = running(FakeApplication(additionalConfiguration = testDB)) {
    createWaterfallAdProvider(waterfall.id, adProviderID1.get, None, Some(5.0), true, true)
  }

  val wap2 = running(FakeApplication(additionalConfiguration = testDB)) {
    createWaterfallAdProvider(waterfall.id, adProviderID2.get, None, Some(5.0), true, true)
  }

  "WaterfallsController.list" should {
    "return waterfall edit page with one waterfall" in new WithAppBrowser(distributor.id.get) {
      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(distributor.id.get, currentApp.id).url)
      browser.pageSource must contain("Edit Waterfall")
      browser.pageSource must contain(currentWaterfall.name)
    }
  }

  "WaterfallsController.update" should {
    "respond with a 200 and an updated generation number if update is successful" in new WithFakeBrowser {
      val originalGeneration = generationNumber(app1.id)
      val configInfo1 = JsObject(
        Seq(
          "waterfallAdProviderID" -> JsNumber(wap1.id),
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
          "waterfallAdProviderID" -> JsNumber(wap1.id),
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
          "paused" -> JsBoolean(false),
          "appToken" -> JsString(app1.token),
          "waterfallID" -> JsString(waterfall.id.toString),
          "generationNumber" -> JsNumber((generationNumber(app1.id)))
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
      val generationNumberResponse = (Json.parse(contentAsString(result)) \ "newGenerationNumber").as[Long]
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
      Waterfall.update(waterfall.id, optimizedOrder = false, testMode = false, paused = false)
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

    "toggle test mode to off when there is at least one ad provider" in new WithFakeBrowser {
      Waterfall.update(waterfall.id, optimizedOrder = false, testMode = true, paused = false)
      DB.withTransaction { implicit connection => AppConfig.createWithWaterfallIDInTransaction(waterfall.id, None) }
      val originalGeneration = generationNumber(waterfall.app_id)
      AppConfig.findLatest(app1.token).get.configuration \ "testMode" must beEqualTo(JsBoolean(true))

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, waterfall.id).url)
      Waterfall.find(waterfall.id, distributor.id.get).get.testMode must beEqualTo(true)
      browser.executeScript("$('#test-mode-switch').click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").areDisplayed()
      Waterfall.find(waterfall.id, distributor.id.get).get.testMode must beEqualTo(false)
      generationNumber(app1.id) must beEqualTo(originalGeneration + 1)
      AppConfig.findLatest(app1.token).get.configuration \ "testMode" must beEqualTo(JsBoolean(false))
    }

    "toggle test mode to on only when the user confirms the action if waterfall is not paused" in new WithFakeBrowser {
      Waterfall.update(waterfall.id, optimizedOrder = false, testMode = false, paused = false)
      DB.withTransaction { implicit connection => AppConfig.createWithWaterfallIDInTransaction(waterfall.id, None) }
      val originalGeneration = generationNumber(waterfall.app_id)
      AppConfig.findLatest(app1.token).get.configuration \ "testMode" must beEqualTo(JsBoolean(false))

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, waterfall.id).url)
      Waterfall.find(waterfall.id, distributor.id.get).get.testMode must beEqualTo(false)
      browser.executeScript("$('#test-mode-switch').click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#test-mode-confirmation-modal").areDisplayed()
      browser.find("#test_mode_confirmation").click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").areDisplayed()
      Waterfall.find(waterfall.id, distributor.id.get).get.testMode must beEqualTo(true)
      generationNumber(app1.id) must beEqualTo(originalGeneration + 1)
      AppConfig.findLatest(app1.token).get.configuration \ "testMode" must beEqualTo(JsBoolean(true))
    }

    "toggle test mode should not show confirmation if waterfall is paused" in new WithFakeBrowser {
      Waterfall.update(waterfall.id, optimizedOrder = false, testMode = false, paused = true)
      DB.withTransaction { implicit connection => AppConfig.createWithWaterfallIDInTransaction(waterfall.id, None) }
      val originalGeneration = generationNumber(waterfall.app_id)
      AppConfig.findLatest(app1.token).get.configuration \ "message" must beEqualTo(JsString("App Configuration not found or waterfall has been paused."))

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, waterfall.id).url)
      Waterfall.find(waterfall.id, distributor.id.get).get.testMode must beEqualTo(false)
      Waterfall.find(waterfall.id, distributor.id.get).get.paused must beEqualTo(true)
      browser.executeScript("$('#test-mode-switch').click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#test-mode-confirmation-modal").areNotDisplayed()
      Waterfall.find(waterfall.id, distributor.id.get).get.testMode must beEqualTo(true)
      generationNumber(app1.id) must beEqualTo(originalGeneration + 1)
      AppConfig.findLatest(app1.token).get.configuration \ "testMode" must beEqualTo(JsBoolean(true))
    }

    "not toggle test mode to on when the user cancels the action" in new WithFakeBrowser {
      Waterfall.update(waterfall.id, optimizedOrder = false, testMode = false, paused = false)
      DB.withTransaction { implicit connection => AppConfig.createWithWaterfallIDInTransaction(waterfall.id, None) }
      val originalGeneration = generationNumber(waterfall.app_id)
      AppConfig.findLatest(app1.token).get.configuration \ "testMode" must beEqualTo(JsBoolean(false))

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, waterfall.id).url)
      Waterfall.find(waterfall.id, distributor.id.get).get.testMode must beEqualTo(false)
      browser.executeScript("$('#test-mode-switch').click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#test-mode-confirmation-modal").areDisplayed()
      browser.find("#test_mode_cancel").click()
      Waterfall.find(waterfall.id, distributor.id.get).get.testMode must beEqualTo(false)
      generationNumber(app1.id) must beEqualTo(originalGeneration)
      AppConfig.findLatest(app1.token).get.configuration \ "testMode" must beEqualTo(JsBoolean(false))
    }

    "waterfall UI should start paused if waterfall is paused" in new WithFakeBrowser {
      Waterfall.update(waterfall.id, optimizedOrder = false, testMode = false, paused = true)
      DB.withTransaction { implicit connection => AppConfig.createWithWaterfallIDInTransaction(waterfall.id, None) }
      val originalGeneration = generationNumber(waterfall.app_id)
      AppConfig.findLatest(app1.token).get.configuration \ "message" must beEqualTo(JsString("App Configuration not found or waterfall has been paused."))

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, waterfall.id).url)
      Waterfall.find(waterfall.id, distributor.id.get).get.paused must beEqualTo(true)
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(".pause.play").isPresent
    }

    "waterfall UI not should not start paused if waterfall is not paused" in new WithFakeBrowser {
      Waterfall.update(waterfall.id, optimizedOrder = false, testMode = false, paused = false)
      DB.withTransaction { implicit connection => AppConfig.createWithWaterfallIDInTransaction(waterfall.id, None) }
      val originalGeneration = generationNumber(waterfall.app_id)

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, waterfall.id).url)
      Waterfall.find(waterfall.id, distributor.id.get).get.paused must beEqualTo(false)
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(".pause.play").isNotPresent
    }

    "toggle paused mode should show confirmation modal" in new WithFakeBrowser {
      Waterfall.update(waterfall.id, optimizedOrder = false, testMode = false, paused = false)
      DB.withTransaction { implicit connection => AppConfig.createWithWaterfallIDInTransaction(waterfall.id, None) }
      val originalGeneration = generationNumber(waterfall.app_id)

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, waterfall.id).url)
      Waterfall.find(waterfall.id, distributor.id.get).get.paused must beEqualTo(false)
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(".pause.play").isNotPresent
      clickAndWaitForAngular(".pause")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#pause-confirmation-modal").areDisplayed()
      browser.find("#pause_confirmation").click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(".pause.play").isPresent
      Waterfall.find(waterfall.id, distributor.id.get).get.paused must beEqualTo(true)
      clickAndWaitForAngular(".pause")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(".pause.play").isNotPresent
      Waterfall.find(waterfall.id, distributor.id.get).get.paused must beEqualTo(false)
    }


    "leaving test mode, waterfall should be paused if no ad providers are active" in new WithAppBrowser(distributor.id.get) {
      val originalGeneration = generationNumber(currentApp.id)

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      Waterfall.find(currentWaterfall.id, distributor.id.get).get.testMode must beEqualTo(true)
      browser.executeScript("$('#test-mode-switch').click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(".pause.play").areDisplayed()
      Waterfall.find(currentWaterfall.id, distributor.id.get).get.testMode must beEqualTo(false)
    }

    "deactivating the only ad provider should show dialog to user and pause the waterfall if confirmed" in new WithFakeBrowser {
      val originalGeneration = generationNumber(waterfall.app_id)
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1.id, wap1.waterfallID, wap1.adProviderID, Some(1), wap1.cpm, Some(true), wap1.fillRate, wap1.configurationData, wap1.reportingActive))
      WaterfallAdProvider.update(new WaterfallAdProvider(wap2.id, wap2.waterfallID, wap2.adProviderID, Some(0), wap2.cpm, Some(false), wap2.fillRate, wap2.configurationData, wap1.reportingActive))
      Waterfall.update(waterfall.id, optimizedOrder = false, testMode = false, paused = false)

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, waterfall.id).url)
      browser.executeScript("$('button[name=status]').last().click()")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#pause-confirmation-modal").areDisplayed()
      browser.find("#pause_confirmation").click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(".pause.play").isPresent
      WaterfallAdProvider.find(wap1.id).get.active.get must beEqualTo(false)
    }

    "deactivating the only ad provider should show dialog to user and not pause the waterfall if cancelled" in new WithFakeBrowser {
      val originalGeneration = generationNumber(waterfall.app_id)
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1.id, wap1.waterfallID, wap1.adProviderID, Some(1), wap1.cpm, Some(true), wap1.fillRate, wap1.configurationData, wap1.reportingActive))
      WaterfallAdProvider.update(new WaterfallAdProvider(wap2.id, wap2.waterfallID, wap2.adProviderID, Some(0), wap2.cpm, Some(false), wap2.fillRate, wap2.configurationData, wap1.reportingActive))
      Waterfall.update(waterfall.id, optimizedOrder = false, testMode = false, paused = false)

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, waterfall.id).url)
      browser.executeScript("$('button[name=status]').last().click()")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#pause-confirmation-modal").areDisplayed()
      browser.find("#pause_cancel").click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(".pause.play").isNotPresent
      WaterfallAdProvider.find(wap1.id).get.active.get must beEqualTo(true)
    }

    "persist the waterfall ordering when the eCPM is changed for a waterfall ad provider in optimized mode" in new WithAppBrowser(distributor.id.get) {
      createWaterfallAdProvider(currentWaterfall.id, adProviderID1.get, None, Some(5.0), true, true)
      createWaterfallAdProvider(currentWaterfall.id, adProviderID2.get, None, Some(5.0), true, true)
      currentWaterfall.optimizedOrder must beTrue

      logInUser()
      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)

      val topAdProviderText = browser.$(".waterfall-app-info").first().getText
      topAdProviderText must contain(adProviders(0))
      topAdProviderText must contain("$5.00")
      browser.$("button[name=configure-wap]").first().click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      browser.fill("input").`with`("1.0", "some key")
      browser.click("button[name=update-ad-provider]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").isPresent
      browser.$(".waterfall-app-info").first().getText must contain(adProviders(1))
      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      val newTopAdProviderText = browser.$(".waterfall-app-info").first().getText
      newTopAdProviderText must contain(adProviders(1))
      newTopAdProviderText must contain("$5.00")
    }
  }

  "WaterfallsController.edit" should {
    "display default eCPM values for Ad Providers" in new WithAppBrowser(distributor.id.get) {
      val defaultEcpm = "20.00"
      val adProviderWithDefaultEcpmID = AdProvider.create("Test Ad Provider With Default eCPM", adProviderConfigData, None, true, Some(defaultEcpm.toDouble)).get

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      browser.$("div[name=cpm]", 2).getText must contain(defaultEcpm)
    }

    "redirect the distributor user to their own Analytics page if they try to edit a Waterfall they do not own" in new WithAppBrowser(distributor.id.get) {
      val (maliciousUser, maliciousDistributor) = newDistributorUser("newuseremail@gmail.com")
      setUpApp(maliciousDistributor.id.get)

      logInUser(maliciousUser.email, password)

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(maliciousDistributor.id.get, currentWaterfall.id).url)
      browser.url() must beEqualTo(controllers.routes.AnalyticsController.show(maliciousDistributor.id.get, None, Some(false)).url)
    }

    "redirect the distributor user to their own Analytics page if they try to edit a Waterfall using another distributor ID" in new WithAppBrowser(distributor.id.get) {
      val (maliciousUser, maliciousDistributor) = newDistributorUser("newuseremail2@gmail.com")
      setUpApp(maliciousDistributor.id.get)

      logInUser(maliciousUser.email, password)

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      browser.url() must beEqualTo(controllers.routes.AnalyticsController.show(maliciousDistributor.id.get, None, None).url)
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

    "change the name of the App in the header and the sidebar when the App name is edited" in new WithFakeBrowser {
      val newEmail = "somenewuser@gmail.com"
      val (currentDistributorUser, currentDistributor) = newDistributorUser(newEmail)
      val (newApp, newWaterfall, _, _) = setUpApp(currentDistributor.id.get)
      logInUser(newEmail, password)

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(currentDistributor.id.get, newWaterfall.id).url)
      val oldAppName = newApp.name
      val newAppName = "Some Different App Name"
      browser.executeScript("$('button[id=waterfall-app-settings-button]').first().click()")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-app").areDisplayed()
      browser.fill("input[name=appName]").`with`(newAppName)
      browser.executeScript("$('button[name=submit]').click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").areDisplayed()
      browser.find(".left_apps_list").getText must contain(newAppName)
      browser.find(".left_apps_list").getText must not contain(oldAppName)
      browser.find("#edit-top").getText must contain(newAppName)
      browser.find("#edit-top").getText must not contain(oldAppName)
    }

    "persist the last waterfall viewed by the user when switching between the analytics page and the waterfall edit page" in new WithAppBrowser(distributor.id.get) {
      val (newApp, newWaterfall, _, _) = setUpApp(distributor.id.get, "New App 1")

      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      clickAndWaitForAngular("a[name=analytics]")
      clickAndWaitForAngular("a[name=waterfalls]")
      browser.url() must beEqualTo(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, newWaterfall.id).url)
      clickAndWaitForAngular("a[name=analytics]")
      clickAndWaitForAngular("a[name=waterfalls]")
      browser.url() must beEqualTo(controllers.routes.WaterfallsController.edit(distributor.id.get, newWaterfall.id).url)
    }

    "redirect to the new waterfall page when an app is created" in new WithAppBrowser(distributor.id.get) {
      val newAppName = "New Unique App Name"
      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      clickAndWaitForAngular("#create_new_app")
      browser.fill("input").`with`(newAppName, "Coins", "1")
      clickAndWaitForAngular("#create-app")
      val newestApp = App.findAll(distributor.id.get).filter(_.name == newAppName)(0)
      val newestWaterfall = Waterfall.findByAppID(newestApp.id)(0)
      browser.url() must beEqualTo(controllers.routes.WaterfallsController.edit(distributor.id.get, newestWaterfall.id).url)
      browser.pageSource must contain(newAppName + " Waterfall")
    }
  }

  "WaterfallsController.editAll" should {
    "render the appropriate Waterfall page when an App ID is passed in" in new WithAppBrowser(distributor.id.get) {
      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.editAll(distributor.id.get, None, Some(currentApp.id)).url)
      browser.url() must beEqualTo(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      browser.pageSource must contain(currentWaterfall.name)
    }

    "render the appropriate Waterfall page when a Waterfall ID is passed in" in new WithAppBrowser(distributor.id.get) {
      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.editAll(distributor.id.get, Some(currentWaterfall.id), None).url)
      browser.url() must beEqualTo(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      browser.pageSource must contain(currentWaterfall.name)
    }

    "render the page for the first Waterfall found when no Waterfall or App ID is passed in but the Distributor has already created an App" in new WithAppBrowser(distributor.id.get) {
      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.editAll(distributor.id.get, None, None).url)
      val waterfallID = App.findAllAppsWithWaterfalls(distributor.id.get).head.waterfallID
      browser.url() must beEqualTo(controllers.routes.WaterfallsController.edit(distributor.id.get, waterfallID).url)
      browser.pageSource must contain(currentWaterfall.name)
    }

    "redirect to the App creation page if the Distributor has not created any Apps" in new WithFakeBrowser {
      val currentEmail = "newuser@gmail.com"
      val (_, currentDistributor) = newDistributorUser(currentEmail, password, "New Company")

      logInUser(currentEmail, password)

      goToAndWaitForAngular(controllers.routes.WaterfallsController.editAll(currentDistributor.id.get, None, None).url)
      browser.url() must beEqualTo(controllers.routes.AppsController.newApp(currentDistributor.id.get).url)
      browser.pageSource must contain("Begin by creating your first app")
    }
  }
}
