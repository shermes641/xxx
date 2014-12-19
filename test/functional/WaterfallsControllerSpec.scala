package functional

import models._
import org.specs2.runner._
import org.junit.runner._
import play.api.libs.json._
import play.api.Play.current
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
    DistributorUser.setActive(DistributorUser.findByEmail(email).get)
    DistributorUser.findByEmail(email).get
  }

  val distributorID = distributorUser.distributorID.get

  val waterfallID = running(FakeApplication(additionalConfiguration = testDB)) {
    val appID = App.create(distributorID, "App 1").get
    val app = App.find(appID).get
    VirtualCurrency.create(appID, "Coins", 100.toLong, None, None, Some(true))
    DB.withTransaction { implicit connection => createWaterfallWithConfig(appID, "New Waterfall") }
  }

  val waterfallAdProviderID1 = running(FakeApplication(additionalConfiguration = testDB)) {
    WaterfallAdProvider.create(waterfallID, adProviderID1.get, None, None, true, true).get
  }

  val wap1 = running(FakeApplication(additionalConfiguration = testDB)) {
    WaterfallAdProvider.find(waterfallAdProviderID1).get
  }

  val waterfallAdProviderID2 = running(FakeApplication(additionalConfiguration = testDB)) {
    WaterfallAdProvider.create(waterfallID, adProviderID2.get, None, None, true, true).get
  }

  val wap2 = running(FakeApplication(additionalConfiguration = testDB)) {
    WaterfallAdProvider.find(waterfallAdProviderID2).get
  }

  val adProviderConfiguration = "{\"requiredParams\":[{\"description\": \"Your Distributor ID\", \"key\": \"distributorID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"true\"}, " +
    "{\"description\": \"Your App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"true\"}], \"reportingParams\": [{\"description\": \"Your API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}, " +
    "{\"description\": \"Your Placement ID\", \"key\": \"placementID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}, {\"description\": \"Your App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}]}"

  "Waterfall.update" should {
    "respond with a 200 if update is successful" in new WithFakeBrowser {
      val waterfall = Waterfall.find(waterfallID).get
      val appToken = App.find(waterfall.app_id).get.token
      val originalGeneration = generationNumber(waterfall.app_id)
      val configInfo1 = JsObject(
        Seq(
          "id" -> JsString(waterfallAdProviderID1.toString),
          "newRecord" -> JsString("false"),
          "active" -> JsString("true"),
          "waterfallOrder" -> JsString("0"),
          "cpm" -> JsString(""),
          "configurable" -> JsString("true")
        )
      )
      val configInfo2 = JsObject(
        Seq(
          "id" -> JsString(waterfallAdProviderID2.toString),
          "newRecord" -> JsString("false"),
          "active" -> JsString("true"),
          "waterfallOrder" -> JsString("1"),
          "cpm" -> JsString(""),
          "configurable" -> JsString("true")
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
          "testMode" -> JsString("false"),
          "appToken" -> JsString(appToken),
          "waterfallID" -> JsString(waterfallID.toString),
          "generationNumber" -> JsString((generationNumber(waterfall.app_id)).toString)
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
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration + 1)
    }

    "respond with 400 if JSON is not formed properly" in new WithFakeBrowser {
      val originalGeneration = generationNumber(waterfallID)
      val request = FakeRequest(
        POST,
        controllers.routes.WaterfallsController.update(distributorID, waterfallID).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request.withSession("distributorID" -> distributorID.toString(), "username" -> email))
      status(result) must equalTo(400)
      generationNumber(Waterfall.find(waterfallID).get.app_id) must beEqualTo(originalGeneration)
    }

    "reorder the waterfall in the same configuration as the drag and drop list" in new WithFakeBrowser with WaterfallEditSetup {
      clearGeneration(currentWaterfall.app_id)
      val originalGeneration = generationNumber(currentWaterfall.app_id)
      val appToken = App.find(currentWaterfall.app_id).get.token
      val waterfallOrder = DB.withTransaction { implicit connection => Waterfall.order(appToken) }
      val firstProvider = waterfallOrder(0).providerName

      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, waterfallID).url)
      browser.executeScript("$('ul').prepend($('li').last());")
      browser.executeScript("angular.element(\"#waterfall-edit\").scope().postUpdate();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").areDisplayed()
      val newOrder = DB.withTransaction { implicit connection => Waterfall.order(appToken) }
      newOrder(0).providerName must not equalTo(firstProvider)
      generationNumber(currentWaterfall.app_id) must beEqualTo(originalGeneration + 1)
    }

    "remove an ad provider from the order when a user clicks the Deactivate button" in new WithFakeBrowser with WaterfallEditSetup {
      val originalGeneration = generationNumber(currentWaterfall.app_id)
      val appToken = App.find(currentWaterfall.app_id).get.token
      val originalOrder = DB.withTransaction { implicit connection => Waterfall.order(appToken) }

      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, waterfallID).url)
      browser.$("button[name=status]").first().click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").areDisplayed()
      val newOrder = DB.withTransaction { implicit connection => Waterfall.order(appToken) }
      newOrder.filter(adProvider => adProvider.active.get).size must equalTo(originalOrder.size - 1)
      generationNumber(currentWaterfall.app_id) must beEqualTo(originalGeneration + 1)
    }

    "return waterfall edit page with one waterfall" in new WithFakeBrowser with WaterfallEditSetup with JsonTesting {
      logInUser()

      val appID = App.create(distributorID, "App List").get
      VirtualCurrency.create(appID, "Coins", 100.toLong, None, None, Some(true))
      DB.withTransaction { implicit connection => createWaterfallWithConfig(appID, "New Waterfall") }

      browser.goTo(controllers.routes.WaterfallsController.list(distributorID, appID, None).url)
      browser.pageSource must contain("Edit Waterfall")
    }

    "return to app list is multiple waterfalls are found" in new WithFakeBrowser with WaterfallEditSetup with JsonTesting {
      logInUser()

      val appID = App.create(distributorID, "App List").get
      VirtualCurrency.create(appID, "Coins", 100.toLong, None, None, Some(true))
      DB.withTransaction { implicit connection =>
        createWaterfallWithConfig(appID, "New Waterfall")
        createWaterfallWithConfig(appID, "Second Waterfall")
      }

      browser.goTo(controllers.routes.WaterfallsController.list(distributorID, appID, None).url)
      browser.pageSource must contain("Waterfall could not be found.")
    }

    "configure an ad provider from the waterfall edit page" in new WithFakeBrowser with WaterfallEditSetup with JsonTesting {
      val originalGeneration = generationNumber(currentWaterfall.app_id)
      DB.withConnection { implicit connection =>
        SQL("update ad_providers set configuration_data = CAST({configuration_data} AS json) where id = {wap2_id};").on("configuration_data" -> configurationData, "wap2_id" -> wap2.id).executeInsert()
      }
      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, waterfallID).url)
      browser.$("button[name=configure-wap]").first().click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      val configKey = "some key"
      browser.fill("input").`with`("5.0", configKey)
      browser.click("button[name=update-ad-provider]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").isPresent()
      val waterfallAdProviderParams = WaterfallAdProvider.find(wap2.id).get.configurationData \ "requiredParams"
      (waterfallAdProviderParams \ configurationParams(0)).as[String] must beEqualTo(configKey)
      generationNumber(currentWaterfall.app_id) must beEqualTo(originalGeneration + 1)
    }

    "notify the user if the app must be restarted for AppConfig changes to take effect" in new WithFakeBrowser with WaterfallEditSetup with JsonTesting {
      val originalGeneration = generationNumber(currentWaterfall.app_id)
      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, waterfallID).url)
      browser.find("button[name=configure-wap]", 2).click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      val configKey = "some key"
      browser.fill("input").`with`("5.0", configKey)
      browser.executeScript("$('button[name=update-ad-provider]').click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").withText().contains("Your App must be restarted")
      generationNumber(currentWaterfall.app_id) must beEqualTo(originalGeneration + 1)
    }

    "toggle waterfall optimization on and off" in new WithFakeBrowser with WaterfallEditSetup {
      val originalGeneration = generationNumber(currentWaterfall.app_id)
      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, waterfallID).url)
      Waterfall.find(currentWaterfall.id).get.optimizedOrder must beEqualTo(false)
      browser.executeScript("var button = $(':checkbox[id=optimized-mode-switch]'); button.prop('checked', true); angular.element(\"#waterfall-edit\").scope().postUpdate();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").areDisplayed()
      Waterfall.find(currentWaterfall.id).get.optimizedOrder must beEqualTo(true)
      generationNumber(currentWaterfall.app_id) must beEqualTo(originalGeneration + 1)
    }

    "toggle waterfall test mode on and off" in new WithFakeBrowser with WaterfallEditSetup {
      val originalGeneration = generationNumber(currentWaterfall.app_id)
      Waterfall.update(currentWaterfall.id, false, false)
      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, waterfallID).url)
      Waterfall.find(currentWaterfall.id).get.testMode must beEqualTo(false)
      browser.executeScript("var button = $(':checkbox[id=test-mode-switch]'); button.prop('checked', true); angular.element(\"#waterfall-edit\").scope().postUpdate();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").areDisplayed()
      Waterfall.find(currentWaterfall.id).get.testMode must beEqualTo(true)
      generationNumber(currentWaterfall.app_id) must beEqualTo(originalGeneration + 1)
    }

    "not set waterfall to live mode when no ad providers are configured" in new WithFakeBrowser {
      val newAppID = App.create(distributorID, "New App").get
      VirtualCurrency.create(newAppID, "Coins", 100.toLong, None, None, Some(true))
      val newWaterfallID = DB.withTransaction { implicit connection => createWaterfallWithConfig(newAppID, "New App") }
      val originalGeneration = generationNumber(newAppID)

      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, newWaterfallID).url)
      Waterfall.find(newWaterfallID).get.testMode must beEqualTo(true)
      browser.executeScript("var button = $(':checkbox[id=test-mode-switch]'); button.prop('checked', true); button.click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-error").areDisplayed()
      Waterfall.find(newWaterfallID).get.testMode must beEqualTo(true)
      generationNumber(newAppID) must beEqualTo(originalGeneration)
    }

    "update the status of reporting for a waterfall ad provider" in new WithFakeBrowser {
      val newAppID = App.create(distributorID, "New App").get
      VirtualCurrency.create(newAppID, "Coins", 100.toLong, None, None, Some(true))
      val newWaterfallID = DB.withTransaction { implicit connection => createWaterfallWithConfig(newAppID, "New App") }

      logInUser()

      DB.withTransaction { implicit connection => AppConfig.create(newAppID, App.find(newAppID).get.token, generationNumber(newAppID)) }
      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, newWaterfallID).url)
      Waterfall.find(newWaterfallID).get.testMode must beEqualTo(true)
      browser.$(".configure.inactive-button").first().click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      val newWap = WaterfallAdProvider.findAllByWaterfallID(newWaterfallID)(0)
      newWap.reportingActive must beEqualTo(false)
      browser.fill("input").`with`("5.0", "Some key")
      browser.executeScript("var button = $(':checkbox[id=reporting-active-switch]'); button.click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").areDisplayed()
      browser.executeScript("var button = $(':button[name=cancel]'); button.click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areNotDisplayed()
      WaterfallAdProvider.find(newWap.id).get.reportingActive must beEqualTo(true)
    }

    "create an inactive WaterfallAdProvider when an AdProvider is configured before being activated" in new WithFakeBrowser with WaterfallEditSetup with JsonTesting {
      val newAppID = App.create(distributorID, "New App").get
      VirtualCurrency.create(newAppID, "Coins", 100.toLong, None, None, Some(true))
      val newWaterfallID = DB.withTransaction { implicit connection => createWaterfallWithConfig(newAppID, "New App") }
      val configKey = "some key"

      logInUser()

      DB.withTransaction { implicit connection => AppConfig.create(newAppID, Waterfall.find(newWaterfallID).get.token, generationNumber(newAppID)) }
      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, newWaterfallID).url)
      browser.$(".configure.inactive-button").first().click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      browser.fill("input").`with`("5.0", configKey)
      browser.click("button[name=update-ad-provider]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").isPresent()
      val wap = WaterfallAdProvider.findAllByWaterfallID(newWaterfallID)(0)
      wap.active.get must beEqualTo(false)
      (wap.configurationData \ "requiredParams" \ "distributorID").as[String] must beEqualTo(configKey)
      (wap.configurationData \ "requiredParams" \ "appID").as[String] must beEqualTo(configKey)
    }

    "create a WaterfallAdProvider with an eCPM value if an AdProvider with a default eCPM is activated" in new WithFakeBrowser with WaterfallEditSetup {
      val newAppID = App.create(distributorID, "New App").get
      VirtualCurrency.create(newAppID, "Coins", 100.toLong, None, None, Some(true))
      val newWaterfallID = DB.withTransaction { implicit connection => createWaterfallWithConfig(newAppID, "New App") }
      Waterfall.update(newWaterfallID, true, false)
      val originalGeneration = generationNumber(newAppID)

      val defaultEcpm = Some(20.0)
      val adProviderWithDefaultEcpmID = AdProvider.create("Test Ad Provider With Default eCPM", adProviderConfiguration, None, false, defaultEcpm).get
      logInUser()
      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, newWaterfallID).url)
      browser.executeScript("$('li[id=true-" + adProviderWithDefaultEcpmID + "]').children('.inactive-waterfall-content').children('.status.inactive-button').click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").areDisplayed()
      WaterfallAdProvider.findAllByWaterfallID(newWaterfallID).filter(wap => wap.adProviderID == adProviderWithDefaultEcpmID).head.cpm must beEqualTo(defaultEcpm)
      generationNumber(newAppID) must beEqualTo(originalGeneration + 1)
    }
  }

  "Waterfall.edit" should {
    "display default eCPM values for Ad Providers" in new WithFakeBrowser with WaterfallEditSetup {
      val newAppID = App.create(distributorID, "New App").get
      VirtualCurrency.create(newAppID, "Coins", 100.toLong, None, None, Some(true))
      val newWaterfallID = DB.withTransaction { implicit connection => createWaterfallWithConfig(newAppID, "New App") }

      val defaultEcpm = "20.0"
      val adProviderWithDefaultEcpmID = AdProvider.create("Test Ad Provider With Default eCPM", adProviderConfiguration, None, false, Some(defaultEcpm.toDouble)).get
      logInUser()
      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, newWaterfallID).url)
      browser.$("li[id=true-" + adProviderWithDefaultEcpmID + "]").getAttribute("data-cpm") must beEqualTo(defaultEcpm)
    }

    "automatically select the name of the app in the drop down menu" in new WithFakeBrowser with WaterfallEditSetup {
      val appName = "New App"
      val newAppID = App.create(distributorID, "New App").get
      VirtualCurrency.create(newAppID, "Coins", 100.toLong, None, None, Some(true))
      val newWaterfallID = DB.withTransaction { implicit connection => createWaterfallWithConfig(newAppID, "New App") }
      logInUser()
      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, newWaterfallID).url)
      browser.find("#waterfall-selection").getValue.contains(appName)
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

