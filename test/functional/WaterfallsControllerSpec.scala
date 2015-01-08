package functional

import anorm.SQL
import models._
import org.specs2.runner._
import org.junit.runner._
import play.api.db.DB
import play.api.libs.json._
import play.api.Play.current
import play.api.test._
import play.api.test.Helpers._
import resources._

@RunWith(classOf[JUnitRunner])
class WaterfallsControllerSpec extends SpecificationWithFixtures with WaterfallSpecSetup {
  val wap1 = running(FakeApplication(additionalConfiguration = testDB)) {
    val id = WaterfallAdProvider.create(waterfall.id, adProviderID1.get, None, None, true, true).get
    WaterfallAdProvider.find(id).get
  }

  val wap2 = running(FakeApplication(additionalConfiguration = testDB)) {
    val id = WaterfallAdProvider.create(waterfall.id, adProviderID2.get, None, None, true, true).get
    WaterfallAdProvider.find(id).get
  }

  "WaterfallsController.list" should {
    "return waterfall edit page with one waterfall" in new WithAppBrowser(distributor.id.get) {
      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.list(distributor.id.get, currentApp.id, None).url)
      browser.pageSource must contain("Edit Waterfall")
      browser.pageSource must contain(currentWaterfall.name)
    }

    "return to app list if multiple waterfalls are found" in new WithAppBrowser(distributor.id.get) {
      logInUser()

      DB.withTransaction { implicit connection => createWaterfallWithConfig(currentApp.id, "New Waterfall") }
      browser.goTo(controllers.routes.WaterfallsController.list(distributor.id.get, currentApp.id, None).url)
      browser.pageSource must contain("Waterfall could not be found.")
    }
  }

  "WaterfallsController.editAll" should {
    "redirect to the edit page for the appropriate Waterfall if a Waterfall ID is passed" in new WithFakeBrowser {
      val targetUrl = controllers.routes.WaterfallsController.edit(distributor.id.get, waterfall.id).url

      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.editAll(distributor.id.get, Some(waterfall.id), None).url)
      browser.find("select[id=waterfall-selection]").getValue must beEqualTo(targetUrl)
      browser.url() must beEqualTo(targetUrl)
    }

    "redirect to the edit page for the appropriate Waterfall if an App ID is passed" in new WithFakeBrowser {
      val appID = Some(app1.id)
      val targetUrl = controllers.routes.WaterfallsController.edit(distributor.id.get, waterfall.id).url

      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.editAll(distributor.id.get, None, appID).url)
      browser.find("select[id=waterfall-selection]").getValue must beEqualTo(targetUrl)
      browser.url() must beEqualTo(targetUrl)
    }

    "render the edit page with no Waterfall selected if no Waterfall ID or App ID is passed" in new WithFakeBrowser {
      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.editAll(distributor.id.get, None, None).url)
      browser.$("#waterfall-selection").getValue must beEqualTo("")
    }
  }

  "WaterfallsController.update" should {
    "respond with a 200 and an updated generation number if update is successful" in new WithFakeBrowser {
      val originalGeneration = generationNumber(app1.id)
      val configInfo1 = JsObject(
        Seq(
          "id" -> JsString(wap1.id.toString),
          "newRecord" -> JsString("false"),
          "active" -> JsString("true"),
          "waterfallOrder" -> JsString("0"),
          "cpm" -> JsString(""),
          "configurable" -> JsString("true")
        )
      )
      val configInfo2 = JsObject(
        Seq(
          "id" -> JsString(wap2.id.toString),
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

      browser.goTo(controllers.routes.WaterfallsController.edit(distributor.id.get, waterfall.id).url)
      browser.executeScript("$('ul').prepend($('li').last());")
      browser.executeScript("postUpdate();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").areDisplayed()
      val newOrder = DB.withTransaction { implicit connection => Waterfall.order(app1.token) }
      newOrder(0).providerName must not equalTo(firstProvider)
      generationNumber(app1.id) must beEqualTo(originalGeneration + 1)
    }

    "remove an ad provider from the order when a user clicks the Deactivate button" in new WithFakeBrowser {
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalOrder = DB.withTransaction { implicit connection => Waterfall.order(app1.token) }

      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributor.id.get, waterfall.id).url)
      browser.$("button[name=status]").first().click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").areDisplayed()
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

      browser.goTo(controllers.routes.WaterfallsController.edit(distributor.id.get, waterfall.id).url)
      browser.$("button[name=configure-wap]").first().click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      val configKey = "some key"
      browser.fill("input").`with`("5.0", configKey)
      browser.click("button[name=update-ad-provider]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").isPresent()
      val waterfallAdProviderParams = WaterfallAdProvider.find(wap2.id).get.configurationData \ "requiredParams"
      (waterfallAdProviderParams \ configurationParams(0)).as[String] must beEqualTo(configKey)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration + 1)
    }

    "toggle waterfall optimization on and off" in new WithFakeBrowser {
      clearGeneration(app1.id)
      val originalGeneration = generationNumber(app1.id)

      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributor.id.get, waterfall.id).url)
      Waterfall.find(waterfall.id, distributor.id.get).get.optimizedOrder must beEqualTo(false)
      browser.executeScript("var button = $(':checkbox[id=optimized-mode-switch]'); button.prop('checked', true); postUpdate();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").areDisplayed()
      Waterfall.find(waterfall.id, distributor.id.get).get.optimizedOrder must beEqualTo(true)
      generationNumber(app1.id) must beEqualTo(originalGeneration + 1)
    }

    "toggle waterfall test mode on and off" in new WithFakeBrowser {
      val originalGeneration = generationNumber(waterfall.app_id)
      Waterfall.update(waterfall.id, false, false)

      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributor.id.get, waterfall.id).url)
      Waterfall.find(waterfall.id, distributor.id.get).get.testMode must beEqualTo(false)
      browser.executeScript("var button = $(':checkbox[id=test-mode-switch]'); button.prop('checked', true); postUpdate();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").areDisplayed()
      Waterfall.find(waterfall.id, distributor.id.get).get.testMode must beEqualTo(true)
      generationNumber(app1.id) must beEqualTo(originalGeneration + 1)
    }

    "not set waterfall to live mode when no ad providers are configured" in new WithAppBrowser(distributor.id.get) {
      val originalGeneration = generationNumber(currentApp.id)

      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      Waterfall.find(currentWaterfall.id, distributor.id.get).get.testMode must beEqualTo(true)
      browser.executeScript("var button = $(':checkbox[id=test-mode-switch]'); button.prop('checked', true); button.click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-error").areDisplayed()
      Waterfall.find(currentWaterfall.id, distributor.id.get).get.testMode must beEqualTo(true)
      generationNumber(currentApp.id) must beEqualTo(originalGeneration)
    }
  }

  "WaterfallsController.edit" should {
    "display default eCPM values for Ad Providers" in new WithAppBrowser(distributor.id.get) {
      val defaultEcpm = "20.0"
      val adProviderWithDefaultEcpmID = AdProvider.create("Test Ad Provider With Default eCPM", adProviderConfigData, None, false, Some(defaultEcpm.toDouble)).get

      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      browser.$("li[id=true-" + adProviderWithDefaultEcpmID + "]").getAttribute("data-cpm") must beEqualTo(defaultEcpm)
    }

    "redirect the distributor user to their own apps index page if they try to edit a Waterfall they do not own" in new WithAppBrowser(distributor.id.get) {
      val (maliciousUser, maliciousDistributor) = newDistributorUser("newuseremail@gmail.com")

      logInUser(maliciousUser.email, password)

      browser.goTo(controllers.routes.WaterfallsController.edit(maliciousDistributor.id.get, currentWaterfall.id).url)
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#flash").hasText("Waterfall could not be found.")
      browser.url() must beEqualTo(controllers.routes.AppsController.index(maliciousDistributor.id.get).url)
    }

    "redirect the distributor user to their own apps index page if they try to edit a Waterfall using another distributor ID" in new WithAppBrowser(distributor.id.get) {
      val (maliciousUser, maliciousDistributor) = newDistributorUser("newuseremail2@gmail.com")

      logInUser(maliciousUser.email, password)

      browser.goTo(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      browser.url() must beEqualTo(controllers.routes.AppsController.index(maliciousDistributor.id.get).url)
    }

    "automatically select the name of the app in the drop down menu" in new WithFakeBrowser {
      val appName = "New App"
      val (_, newWaterfall, _, _) = setUpApp(distributor.id.get, appName)

      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributor.id.get, newWaterfall.id).url)
      browser.find("#waterfall-selection").getValue.contains(appName)
    }
  }
}
