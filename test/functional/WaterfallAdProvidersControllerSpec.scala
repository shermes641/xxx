package functional

import models._
import org.junit.runner._
import org.specs2.runner._
import play.api.db.DB
import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers._
import resources._

@RunWith(classOf[JUnitRunner])
class WaterfallAdProvidersControllerSpec extends SpecificationWithFixtures with JsonTesting with DistributorUserSetup {
  val adProvider1ID = running(FakeApplication(additionalConfiguration = testDB)) {
    AdProvider.create("test ad provider 1", configurationData, None, true, None).get
  }

  val adProvider2ID = running(FakeApplication(additionalConfiguration = testDB)) {
    AdProvider.create("test ad provider 2", configurationData, None, true, None).get
  }

  val (distributorUser, _) = running(FakeApplication(additionalConfiguration = testDB)) {
    newDistributorUser()
  }

  /**
   * Helper function to create a new request to the WaterfallAdProvidersController edit endpoint.
   * @param waterfallAdProviderID The ID of the WaterfallAdProvider to be edited.
   * @return An instance of the GET request.
   */
  def wapEditRequest(waterfallAdProviderID: Long) = {
    FakeRequest(
      GET,
      controllers.routes.WaterfallAdProvidersController.edit(distributorUser.distributorID.get, waterfallAdProviderID, None).url,
      FakeHeaders(),
      ""
    )
  }

  "WaterfallAdProvidersController.create" should {
    /**
     * Helper function to create a POST request to the WaterfallAdProvidersController create endpoint.
     * @param body The JSON body of the request.
     * @return An instance of the POST request.
     */
    def wapCreateRequest(body: JsObject) = {
      FakeRequest(
        POST,
        controllers.routes.WaterfallAdProvidersController.create(distributorUser.distributorID.get).url,
        FakeHeaders(Seq("Content-type" -> Seq("application/json"))),
        body
      )
    }

    "create a new WaterfallAdProvider instance" in new WithAppBrowser(distributorUser.distributorID.get) {
      Waterfall.update(currentWaterfall.id, true, false)
      val configurationData = Seq("configurable" -> JsString("true"), "adProviderID" -> JsString(adProvider1ID.toString), "appToken" -> JsString(currentApp.token),
        "waterfallID" -> JsString(currentWaterfall.id.toString), "generationNumber" -> JsString(currentAppConfig.generationNumber.toString), "cpm" -> JsString("5.0"),
        "active" -> JsBoolean(true), "waterfallOrder" -> JsString("0"))
      val body = JsObject(configurationData)
      val Some(result) = route(wapCreateRequest(body).withSession("distributorID" -> distributorUser.distributorID.get.toString, "username" -> distributorUser.email))
      status(result) must beEqualTo(200)
      val jsonResult = Json.parse(contentAsString(result))
      val wapID = (jsonResult \ "wapID").as[Long]
      WaterfallAdProvider.find(wapID).get must haveClass[WaterfallAdProvider]
    }

    "increment the AppConfig generation number when the AppConfig changes as a result of creating a new WaterfallAdProvider" in new WithAppBrowser(distributorUser.distributorID.get) {
      WaterfallAdProvider.create(currentWaterfall.id, adProvider2ID, None, Some(5.0), true, true)
      Waterfall.update(currentWaterfall.id, true, false)
      DB.withTransaction { implicit connection => AppConfig.createWithWaterfallIDInTransaction(currentWaterfall.id, None) }
      val originalGeneration = generationNumber(currentApp.id)
      val configurationData = Seq("configurable" -> JsString("true"), "adProviderID" -> JsString(adProvider1ID.toString), "appToken" -> JsString(currentApp.token),
        "waterfallID" -> JsString(currentWaterfall.id.toString), "generationNumber" -> JsString(originalGeneration.toString), "cpm" -> JsString("5.0"),
        "active" -> JsBoolean(true), "waterfallOrder" -> JsString("0"))
      val body = JsObject(configurationData)
      val Some(result) = route(wapCreateRequest(body).withSession("distributorID" -> distributorUser.distributorID.get.toString, "username" -> distributorUser.email))
      status(result) must beEqualTo(200)
      val jsonResult = Json.parse(contentAsString(result))
      val wapID = (jsonResult \ "wapID").as[Long]
      val newGeneration = (jsonResult \ "newGenerationNumber").as[Long]
      newGeneration must beEqualTo(originalGeneration + 1)
      WaterfallAdProvider.find(wapID).get must haveClass[WaterfallAdProvider]
    }

    "respond with a 400 if a WaterfallAdProvider cannot be created" in new WithAppBrowser(distributorUser.distributorID.get) {
      WaterfallAdProvider.create(currentWaterfall.id, adProvider2ID, None, Some(5.0), true, true)
      Waterfall.update(currentWaterfall.id, true, false)
      DB.withTransaction { implicit connection => AppConfig.createWithWaterfallIDInTransaction(currentWaterfall.id, None) }
      val originalGeneration = generationNumber(currentApp.id)
      val configurationData = Seq("configurable" -> JsString("true"), "adProviderID" -> JsString(adProvider2ID.toString), "appToken" -> JsString(currentApp.token),
        "waterfallID" -> JsString(currentWaterfall.id.toString), "generationNumber" -> JsString(originalGeneration.toString), "cpm" -> JsString("5.0"),
        "active" -> JsBoolean(true), "waterfallOrder" -> JsString("0"))
      val body = JsObject(configurationData)
      val Some(result) = route(wapCreateRequest(body).withSession("distributorID" -> distributorUser.distributorID.get.toString, "username" -> distributorUser.email))
      status(result) must beEqualTo(400)
      generationNumber(currentApp.id) must beEqualTo(originalGeneration)
    }
  }

  "WaterfallAdProvidersController.edit" should {
    "render the appropriate configuration fields for a given ad provider" in new WithAppBrowser(distributorUser.distributorID.get) {
      val waterfallAdProviderID = WaterfallAdProvider.create(currentWaterfall.id, adProvider1ID, None, None, true).get
      val Some(result) = route(wapEditRequest(waterfallAdProviderID).withSession("distributorID" -> distributorUser.distributorID.get.toString, "username" -> distributorUser.email))
      val page = contentAsString(result)
      configurationParams.map { param =>
        page must contain(param)
      }
    }

    "properly fill the values of the fields if data exists" in new WithAppBrowser(distributorUser.distributorID.get) {
      val waterfallAdProviderID = WaterfallAdProvider.create(currentWaterfall.id, adProvider1ID, None, None, true).get
      val wap = WaterfallAdProvider.find(waterfallAdProviderID).get
      val configParam = "Some value"
      val updatedValues = new WaterfallAdProvider(wap.id, wap.waterfallID, wap.adProviderID, None, None, Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq(configurationParams(0) -> JsString(configParam))))), wap.reportingActive)
      WaterfallAdProvider.update(updatedValues)
      val Some(result) = route(wapEditRequest(waterfallAdProviderID).withSession("distributorID" -> distributorUser.distributorID.get.toString, "username" -> distributorUser.email))
      contentAsString(result) must contain(configParam)
    }
  }

  "WaterfallAdProvidersController.update" should {
    "update the configuration_data field of the waterfall_ad_providers record" in new WithAppBrowser(distributorUser.distributorID.get) {
      Waterfall.update(currentWaterfall.id, true, false)
      val waterfallAdProviderID = WaterfallAdProvider.create(currentWaterfall.id, adProvider1ID, None, None, true).get
      clearGeneration(currentApp.id)
      val originalGeneration = generationNumber(currentApp.id)
      val updatedParam = "Some new value"
      val configurationData = Seq("configurationData" -> JsObject(Seq(configurationParams(0) -> JsString(updatedParam))), "reportingActive" -> JsString("false"),
        "appToken" -> JsString(currentApp.token), "waterfallID" -> JsString(currentWaterfall.id.toString), "generationNumber" -> JsString(originalGeneration.toString), "eCPM" -> JsString("5.0"))
      val body = JsObject(configurationData)
      val postRequest = FakeRequest(
        POST,
        controllers.routes.WaterfallAdProvidersController.update(distributorUser.distributorID.get, waterfallAdProviderID).url,
        FakeHeaders(Seq("Content-type" -> Seq("application/json"))),
        body
      )
      val Some(result) = route(postRequest.withSession("distributorID" -> distributorUser.distributorID.get.toString, "username" -> distributorUser.email))
      status(result) must equalTo(200)
      val wap = WaterfallAdProvider.find(waterfallAdProviderID).get
      wap.configurationData must beEqualTo(body \ "configurationData")
      generationNumber(currentApp.id) must beEqualTo(originalGeneration + 1)
    }

    "respond with a 400 if proper JSON is not received" in new WithAppBrowser(distributorUser.distributorID.get) {
      val waterfallAdProviderID = WaterfallAdProvider.create(currentWaterfall.id, adProvider1ID, None, None, true).get
      val postRequest = FakeRequest(
        POST,
        controllers.routes.WaterfallAdProvidersController.update(distributorUser.distributorID.get, waterfallAdProviderID).url,
        FakeHeaders(Seq("Content-type" -> Seq("application/json"))),
        ""
      )
      val Some(result) = route(postRequest.withSession("distributorID" -> distributorUser.distributorID.get.toString, "username" -> distributorUser.email))
      status(result) must equalTo(400)
    }

    "update the eCPM value for a WaterfallAdProvider if the eCPM is valid" in new WithAppBrowser(distributorUser.distributorID.get) {
      val validEcpm = "5.0"

      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorUser.distributorID.get, currentWaterfall.id).url)
      browser.$(".configure.inactive-button").first().click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      val newWap = WaterfallAdProvider.findAllByWaterfallID(currentWaterfall.id)(0)
      browser.fill("input").`with`(validEcpm, "Some key")
      browser.executeScript("var button = $(':button[name=update-ad-provider]'); button.click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").areDisplayed()
      val updatedWap = WaterfallAdProvider.find(newWap.id).get
      updatedWap.cpm.get.toString must beEqualTo(validEcpm)
    }

    "not update the WaterfallAdProvider if the eCPM is not valid" in new WithAppBrowser(distributorUser.distributorID.get) {
      val invalidEcpm = " "

      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorUser.distributorID.get, currentWaterfall.id).url)
      Waterfall.find(currentWaterfall.id, distributorUser.distributorID.get).get.testMode must beEqualTo(true)
      browser.$(".configure.inactive-button").first().click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      val newWap = WaterfallAdProvider.findAllByWaterfallID(currentWaterfall.id)(0)
      browser.fill("input").`with`(invalidEcpm, "Some key")
      browser.executeScript("var button = $(':button[name=update-ad-provider]'); button.click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-error").hasText("eCPM must be a valid number.")
    }

    "notify the user if the app must be restarted for AppConfig changes to take effect" in new WithAppBrowser(distributorUser.distributorID.get) {
      WaterfallAdProvider.create(currentWaterfall.id, adProvider1ID, None, None, true, true).get
      Waterfall.update(currentWaterfall.id, true, false)
      clearGeneration(currentApp.id)
      val originalGeneration = generationNumber(currentApp.id)
      val configKey = "some key"

      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorUser.distributorID.get, currentWaterfall.id).url)
      browser.find("button[name=configure-wap]").first.click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      browser.fill("input").`with`("5.0", configKey)
      browser.executeScript("$('button[name=update-ad-provider]').click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").withText().contains("Your App must be restarted")
      generationNumber(currentApp.id) must beEqualTo(originalGeneration + 1)
    }

    "update the status of reporting for a waterfall ad provider" in new WithAppBrowser(distributorUser.distributorID.get) {
      val wap1ID = WaterfallAdProvider.create(currentWaterfall.id, adProvider1ID, None, None, true, true).get
      Waterfall.update(currentWaterfall.id, true, false)
      clearGeneration(currentApp.id)

      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorUser.distributorID.get, currentWaterfall.id).url)
      browser.$(".configure.active-button").first.click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      val newWap = WaterfallAdProvider.find(wap1ID).get
      newWap.reportingActive must beEqualTo(false)
      browser.fill("input").`with`("5.0", "Some key")
      browser.executeScript("var button = $(':checkbox[id=reporting-active-switch]'); button.click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").areDisplayed()
      browser.executeScript("var button = $(':button[name=cancel]'); button.click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areNotDisplayed()
      WaterfallAdProvider.find(newWap.id).get.reportingActive must beEqualTo(true)
    }

    "create an inactive WaterfallAdProvider when an AdProvider is configured before being activated" in new WithAppBrowser(distributorUser.distributorID.get) with JsonTesting {
      val configKey = "some key"

      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorUser.distributorID.get, currentWaterfall.id).url)
      browser.$(".configure.inactive-button").first().click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      browser.fill("input").`with`("5.0", configKey)
      browser.click("button[name=update-ad-provider]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").isPresent()
      val wap = WaterfallAdProvider.findAllByWaterfallID(currentWaterfall.id)(0)
      wap.active.get must beEqualTo(false)
      (wap.configurationData \ "requiredParams" \ configurationParams(0)).as[String] must beEqualTo(configKey)
      (wap.configurationData \ "requiredParams" \ configurationParams(1)).as[String] must beEqualTo(configKey)
    }

    "create a WaterfallAdProvider with an eCPM value if an AdProvider with a default eCPM is activated" in new WithAppBrowser(distributorUser.distributorID.get) {
      Waterfall.update(currentWaterfall.id, true, false)
      val originalGeneration = generationNumber(currentApp.id)
      val defaultEcpm = Some(20.0)
      val adProviderWithDefaultEcpmID = AdProvider.create("Test Ad Provider With Default eCPM", configurationData, None, false, defaultEcpm).get

      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(distributorUser.distributorID.get, currentWaterfall.id).url)
      browser.executeScript("$('li[id=true-" + adProviderWithDefaultEcpmID + "]').children('.inactive-waterfall-content').children('.status.inactive-button').click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").areDisplayed()
      WaterfallAdProvider.findAllByWaterfallID(currentWaterfall.id).filter(wap => wap.adProviderID == adProviderWithDefaultEcpmID).head.cpm must beEqualTo(defaultEcpm)
      generationNumber(currentApp.id) must beEqualTo(originalGeneration + 1)
    }
  }
}
