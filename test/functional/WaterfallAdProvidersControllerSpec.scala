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
      Waterfall.update(currentWaterfall.id, optimizedOrder = true, testMode = false, paused = false)
      val configurationData = Seq("configurable" -> JsBoolean(true), "adProviderID" -> JsNumber(adProvider1ID), "appToken" -> JsString(currentApp.token), "cpm" -> JsString("5.00"),
        "waterfallID" -> JsString(currentWaterfall.id.toString), "generationNumber" -> JsNumber(currentAppConfig.generationNumber), "waterfallOrder" -> JsString("0"))
      val body = JsObject(configurationData)
      val Some(result) = route(wapCreateRequest(body).withSession("distributorID" -> distributorUser.distributorID.get.toString, "username" -> distributorUser.email))
      status(result) must beEqualTo(200)
      val jsonResult = Json.parse(contentAsString(result))
      val wapID = (jsonResult \ "wapID").as[Long]
      WaterfallAdProvider.find(wapID).get must haveClass[WaterfallAdProvider]
    }

    "not increment the AppConfig generation number when the AppConfig changes as a result of creating a new, inactive WaterfallAdProvider" in new WithAppBrowser(distributorUser.distributorID.get) {
      WaterfallAdProvider.create(currentWaterfall.id, adProvider2ID, None, Some(5.0), true, true)
      Waterfall.update(currentWaterfall.id, optimizedOrder = true, testMode = false, paused = false)
      DB.withTransaction { implicit connection => AppConfig.createWithWaterfallIDInTransaction(currentWaterfall.id, None) }
      val originalGeneration = generationNumber(currentApp.id)
      val configurationData = Seq("configurable" -> JsBoolean(true), "adProviderID" -> JsNumber(adProvider1ID), "appToken" -> JsString(currentApp.token), "cpm" -> JsString("5.00"),
        "waterfallID" -> JsString(currentWaterfall.id.toString), "generationNumber" -> JsNumber(originalGeneration), "active" -> JsBoolean(true), "waterfallOrder" -> JsString("0"))
      val body = JsObject(configurationData)
      val Some(result) = route(wapCreateRequest(body).withSession("distributorID" -> distributorUser.distributorID.get.toString, "username" -> distributorUser.email))
      status(result) must beEqualTo(200)
      val jsonResult = Json.parse(contentAsString(result))
      val wapID = (jsonResult \ "wapID").as[Long]
      val newGeneration = (jsonResult \ "newGenerationNumber").as[Long]
      newGeneration must beEqualTo(originalGeneration)
      WaterfallAdProvider.find(wapID).get must haveClass[WaterfallAdProvider]
    }

    "respond with a 400 if a WaterfallAdProvider cannot be created" in new WithAppBrowser(distributorUser.distributorID.get) {
      WaterfallAdProvider.create(currentWaterfall.id, adProvider2ID, None, Some(5.0), true, true)
      Waterfall.update(currentWaterfall.id, optimizedOrder = true, testMode = false, paused = false)
      DB.withTransaction { implicit connection => AppConfig.createWithWaterfallIDInTransaction(currentWaterfall.id, None) }
      val originalGeneration = generationNumber(currentApp.id)
      val configurationData = Seq("configurable" -> JsBoolean(true), "adProviderID" -> JsNumber(adProvider2ID), "appToken" -> JsString(currentApp.token), "cpm" -> JsString("5.00"),
        "waterfallID" -> JsString(currentWaterfall.id.toString), "generationNumber" -> JsNumber(originalGeneration), "active" -> JsBoolean(true), "waterfallOrder" -> JsString("0"))
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

    "hide the field if there is no display key present for a given app config param" in new WithAppBrowser(distributorUser.distributorID.get) {
      val param = "propertyID"
      val hyprMarketplaceConfiguration = {
        "{" +
          "\"requiredParams\":[" +
          "{\"description\": \"Your HyprMX Property ID\", \"displayKey\": \"\", \"key\": \"" + param + "\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
          "], \"reportingParams\": [], \"callbackParams\": []" +
        "}"
      }
      val hyprID = AdProvider.create("HyprMarketplace", hyprMarketplaceConfiguration, None, false, Some(20)).get
      val waterfallAdProviderID = WaterfallAdProvider.create(currentWaterfall.id, hyprID, None, None, true).get
      logInUser()
      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributorUser.distributorID.get, currentWaterfall.id).url)
      browser.executeScript("$('button[name=configure-wap]').first().click();")
      browser.find(".edit-waterfall-ad-provider-field").first().isDisplayed must beFalse
    }
  }

  "WaterfallAdProvidersController.update" should {
    "update the configuration_data field of the waterfall_ad_providers record" in new WithAppBrowser(distributorUser.distributorID.get) {
      Waterfall.update(currentWaterfall.id, optimizedOrder = true, testMode = false, paused = false)
      val waterfallAdProviderID = WaterfallAdProvider.create(currentWaterfall.id, adProvider1ID, None, None, true).get
      clearGeneration(currentApp.id)
      val originalGeneration = generationNumber(currentApp.id)
      val updatedParam = "Some new value"
      val requiredParamArray = JsArray(Seq(JsObject(Seq("key" -> JsString(configurationParams(0)), "value" -> JsString(updatedParam),
        "displayKey" -> JsString(configurationParams(0)), "dataType" -> JsString("String"), "description" -> JsString("Description"), "refreshOnAppRestart" -> JsBoolean(false)))))
      val requiredParamsData = JsObject(Seq("requiredParams" -> requiredParamArray))
      val configurationData = JsObject(Seq("callbackParams" -> JsArray(Seq()), "reportingParams" -> JsArray(Seq()),
        "reportingActive" -> JsBoolean(false), "appToken" -> JsString(currentApp.token), "waterfallID" -> JsString(currentWaterfall.id.toString),
        "generationNumber" -> JsNumber(originalGeneration), "cpm" -> JsString("5.0"))).deepMerge(requiredParamsData)
      val postRequest = FakeRequest(
        POST,
        controllers.routes.WaterfallAdProvidersController.update(distributorUser.distributorID.get, waterfallAdProviderID).url,
        FakeHeaders(Seq("Content-type" -> Seq("application/json"))),
        configurationData
      )
      val Some(result) = route(postRequest.withSession("distributorID" -> distributorUser.distributorID.get.toString, "username" -> distributorUser.email))
      status(result) must equalTo(200)
      val wap = WaterfallAdProvider.find(waterfallAdProviderID).get
      (wap.configurationData \ "requiredParams" \ configurationParams(0)).as[String] must beEqualTo(updatedParam)
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

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributorUser.distributorID.get, currentWaterfall.id).url)
      browser.executeScript("$('button[name=configure-wap]').first().click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      val newWap = WaterfallAdProvider.findAllByWaterfallID(currentWaterfall.id)(0)
      browser.fill("input").`with`(validEcpm, "Some key")
      browser.executeScript("var button = $(':button[name=update-ad-provider]'); button.click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").areDisplayed()
      val updatedWap = WaterfallAdProvider.find(newWap.id).get
      updatedWap.cpm.get.toString must beEqualTo(validEcpm)
    }

    "not update the WaterfallAdProvider if the eCPM is not valid" in new WithAppBrowser(distributorUser.distributorID.get) {
      val invalidEcpms = List(" ", "2 0", "2,0", "2e0")

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributorUser.distributorID.get, currentWaterfall.id).url)
      Waterfall.find(currentWaterfall.id, distributorUser.distributorID.get).get.testMode must beEqualTo(true)
      browser.executeScript("$('button[name=configure-wap]').first().click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      val newWap = WaterfallAdProvider.findAllByWaterfallID(currentWaterfall.id)(0)
      invalidEcpms.map { eCPM =>
        browser.fill("input").`with`(eCPM, "Some key")
        browser.executeScript("var button = $(':button[name=update-ad-provider]'); button.click();")
        browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#ecpm-input").containsText("eCPM must be a valid number greater than or equal to $0.00")
      }
    }

    "notify the user if the app must be restarted for AppConfig changes to take effect" in new WithAppBrowser(distributorUser.distributorID.get) {
      val wapID = WaterfallAdProvider.create(currentWaterfall.id, adProvider1ID, None, None, true, true).get
      val wap = WaterfallAdProvider.find(wapID).get
      WaterfallAdProvider.update(new WaterfallAdProvider(wapID, currentWaterfall.id, wap.adProviderID, None, None, Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), false))
      Waterfall.update(currentWaterfall.id, optimizedOrder = true, testMode = false, paused = false)
      clearGeneration(currentApp.id)
      val originalGeneration = generationNumber(currentApp.id)
      val configKey = "some key"

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributorUser.distributorID.get, currentWaterfall.id).url)
      browser.executeScript("$('button[name=configure-wap]').first().click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      browser.fill("input").`with`("5.0", configKey)
      browser.click("button[name=update-ad-provider]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").areDisplayed()
      generationNumber(currentApp.id) must beEqualTo(originalGeneration + 1)
    }

    "update the status of reporting for a waterfall ad provider" in new WithAppBrowser(distributorUser.distributorID.get) {
      val wap1ID = WaterfallAdProvider.create(currentWaterfall.id, adProvider1ID, None, None, true, true).get
      val wap = WaterfallAdProvider.find(wap1ID).get
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1ID, currentWaterfall.id, wap.adProviderID, None, None, Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), false))
      Waterfall.update(currentWaterfall.id, optimizedOrder = true, testMode = false, paused = false)
      clearGeneration(currentApp.id)

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributorUser.distributorID.get, currentWaterfall.id).url)
      browser.executeScript("$('button[name=configure-wap]').first().click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      val newWap = WaterfallAdProvider.find(wap1ID).get
      newWap.reportingActive must beEqualTo(false)
      browser.fill("input").`with`("5.0", "Some key")
      browser.executeScript("var button = $(':checkbox[id=reporting-active-switch]'); button.click();")
      browser.executeScript("$('button[name=update-ad-provider]').click()")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").areDisplayed()
      browser.executeScript("var button = $(':button[name=cancel]'); button.click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areNotDisplayed()
      WaterfallAdProvider.find(newWap.id).get.reportingActive must beEqualTo(true)
    }

    "create an inactive WaterfallAdProvider when an AdProvider is configured before being activated" in new WithAppBrowser(distributorUser.distributorID.get) with JsonTesting {
      val configKey = "some key"

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributorUser.distributorID.get, currentWaterfall.id).url)
      browser.executeScript("$('button[name=configure-wap]').first().click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      browser.fill("input").`with`("5.0", configKey)
      browser.executeScript("$('button[name=update-ad-provider]').click()")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").isPresent()
      val wap = WaterfallAdProvider.findAllByWaterfallID(currentWaterfall.id)(0)
      wap.active.get must beEqualTo(false)
      (wap.configurationData \ "requiredParams" \ configurationParams(0)).as[String] must beEqualTo(configKey)
      (wap.configurationData \ "requiredParams" \ configurationParams(1)).as[String] must beEqualTo(configKey)
    }

    "create a WaterfallAdProvider with an eCPM value if an AdProvider with a default eCPM is activated" in new WithAppBrowser(distributorUser.distributorID.get) {
      Waterfall.update(currentWaterfall.id, optimizedOrder = true, testMode = false, paused = false)
      val originalGeneration = generationNumber(currentApp.id)
      val defaultEcpm = Some(20.0)
      val adProviderName = "Test Ad Provider With Default eCPM"
      val adProviderWithDefaultEcpmID = AdProvider.create(adProviderName, configurationData, None, false, defaultEcpm).get

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributorUser.distributorID.get, currentWaterfall.id).url)
      browser.executeScript("$('.waterfall li').last().children().children('.wap-buttons').children('button[name=configure-wap]').click()")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      browser.findFirst("input[name=eCPM]").getValue must beEqualTo("20.00")
      browser.fill("input").`with`("20.00", "12345")
      browser.executeScript("$('button[name=update-ad-provider]').click()")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").areDisplayed()
      WaterfallAdProvider.findAllByWaterfallID(currentWaterfall.id).filter(wap => wap.adProviderID == adProviderWithDefaultEcpmID).head.cpm must beEqualTo(defaultEcpm)
      generationNumber(currentApp.id) must beEqualTo(originalGeneration + 1)
    }

    "flash an error message and not update the WaterfallAdProvider if all required fields are not filled" in new WithAppBrowser(distributorUser.distributorID.get) {
      Waterfall.update(currentWaterfall.id, optimizedOrder = true, testMode = false, paused = false)

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributorUser.distributorID.get, currentWaterfall.id).url)
      browser.executeScript("$('button[name=configure-wap]').first().click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      browser.fill("input[name=eCPM]").`with`("5.0")
      browser.click("button[name=update-ad-provider]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#requiredParams-key1-input").containsText("Field is required")
      val wap = WaterfallAdProvider.findAllByWaterfallID(currentWaterfall.id)(0)
      wap.cpm must beNone
      wap.configurationData must beEqualTo(JsObject(Seq()))
    }

    "only allow the user to turn on reporting if a valid eCPM is already entered" in new WithAppBrowser(distributorUser.distributorID.get) {
      val wap1ID = WaterfallAdProvider.create(currentWaterfall.id, adProvider1ID, None, None, true, true).get
      val wap = WaterfallAdProvider.find(wap1ID).get
      val wapConfig = JsObject(Seq("requiredParams" -> JsObject(Seq()), "callbackParams" -> JsObject(Seq()), "reportingParams" -> JsObject(Seq())))
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1ID, currentWaterfall.id, wap.adProviderID, None, None, Some(true), None, wapConfig, false))
      Waterfall.update(currentWaterfall.id, optimizedOrder = true, testMode = false, paused = false)
      clearGeneration(currentApp.id)

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributorUser.distributorID.get, currentWaterfall.id).url)
      browser.executeScript("$('button[name=configure-wap]').first().click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      val newWap = WaterfallAdProvider.find(wap1ID).get
      newWap.reportingActive must beEqualTo(false)
      browser.fill("input[name=eCPM]").`with`("")
      browser.executeScript("var button = $(':checkbox[id=reporting-active-switch]'); button.click();")
      browser.click("button[name=update-ad-provider]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#ecpm-input").containsText("eCPM must be a valid number greater than or equal to $0.00")
      WaterfallAdProvider.find(wap1ID).get.reportingActive must beFalse
      browser.executeScript("var button = $(':checkbox[id=reporting-active-switch]'); button.click();")
      browser.fill("input").`with`("5.0", "12345")
      browser.executeScript("var button = $(':checkbox[id=reporting-active-switch]'); button.click();")
      browser.click("button[name=update-ad-provider]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").areDisplayed()
      WaterfallAdProvider.find(newWap.id).get.reportingActive must beEqualTo(true)
    }

    "not allow a user to enter less than the minimum number of characters for a WaterfallAdProvider configuration field" in new WithAppBrowser(distributorUser.distributorID.get) {
      val adProviderConfigData = {
        "{" +
          "\"requiredParams\":[" +
            "{\"description\": \"Your Distributor ID\", \"displayKey\": \"Distributor ID\", \"key\": \"distributorID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": true, \"minLength\": 4}" +
          "], " +
          "\"reportingParams\": [], \"callbackParams\": []" +
        "}"
      }
      val adProviderName = "Test Ad Provider 3"
      AdProvider.create(adProviderName, adProviderConfigData, None, true, None)

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributorUser.distributorID.get, currentWaterfall.id).url)
      browser.executeScript("$('button[name=configure-wap]').last().click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      browser.fill("input").`with`("1.0", "123")
      browser.click("button[name=update-ad-provider]")
      browser.find(".modal-text", 2).getText must contain("This field requires at least 4 characters")
      browser.fill("input").`with`("1.0", "1234")
      browser.click("button[name=update-ad-provider]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").containsText(adProviderName + " updated!")
    }
  }
}
