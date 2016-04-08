package functional

import controllers.WaterfallAdProvidersController
import models._
import org.fluentlenium.core.filter.FilterConstructor.withId
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test._
import resources._

class WaterfallAdProvidersControllerSpec extends SpecificationWithFixtures with JsonTesting {
  /**
   * Helper function to create a new request to the WaterfallAdProvidersController edit endpoint.
   * @param waterfallAdProviderID The ID of the WaterfallAdProvider to be edited.
   * @return An instance of the GET request.
   */
  def wapEditRequest(waterfallAdProviderID: Long) = {
    FakeRequest(
      GET,
      controllers.routes.WaterfallAdProvidersController.edit(distributor.id.get, waterfallAdProviderID, None).url,
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
        controllers.routes.WaterfallAdProvidersController.create(distributor.id.get).url,
        FakeHeaders(Seq("Content-type" -> "application/json")),
        body
      )
    }

    "create a new WaterfallAdProvider instance" in new WithAppBrowser(distributor.id.get) {
      waterfallService.update(currentWaterfall.id, optimizedOrder = true, testMode = false, paused = false)
      val configurationData = Seq("configurable" -> JsBoolean(true), "adProviderID" -> JsNumber(adProviderID1.get), "appToken" -> JsString(currentApp.token), "cpm" -> JsString("5.00"),
        "waterfallID" -> JsString(currentWaterfall.id.toString), "generationNumber" -> JsNumber(currentAppConfig.generationNumber), "waterfallOrder" -> JsString("0"))
      val body = JsObject(configurationData)
      val Some(result) = route(wapCreateRequest(body).withSession("distributorID" -> distributor.id.get.toString, "username" -> user.email))
      status(result) must beEqualTo(OK)
      val jsonResult = contentAsJson(result)
      val wapID = (jsonResult \ "wapID").as[Long]
      waterfallAdProviderService.find(wapID).get must haveClass[WaterfallAdProvider]
    }

    "not increment the AppConfig generation number when the AppConfig changes as a result of creating a new, inactive WaterfallAdProvider" in new WithAppBrowser(distributor.id.get) {
      waterfallAdProviderService.create(currentWaterfall.id, adProviderID2.get, None, Some(5.0), configurable = true, active = true)
      waterfallService.update(currentWaterfall.id, optimizedOrder = true, testMode = false, paused = false)
      database.withTransaction { implicit connection => appConfigService.createWithWaterfallIDInTransaction(currentWaterfall.id, None) }
      val originalGeneration = generationNumber(currentApp.id)
      val configurationData = Seq("configurable" -> JsBoolean(true), "adProviderID" -> JsNumber(adProviderID1.get), "appToken" -> JsString(currentApp.token), "cpm" -> JsString("5.00"),
        "waterfallID" -> JsString(currentWaterfall.id.toString), "generationNumber" -> JsNumber(originalGeneration), "active" -> JsBoolean(true), "waterfallOrder" -> JsString("0"))
      val body = JsObject(configurationData)
      val Some(result) = route(wapCreateRequest(body).withSession("distributorID" -> distributor.id.get.toString, "username" -> user.email))
      status(result) must beEqualTo(200)
      val jsonResult = contentAsJson(result)
      val wapID = (jsonResult \ "wapID").as[Long]
      val newGeneration = (jsonResult \ "newGenerationNumber").as[Long]
      newGeneration must beEqualTo(originalGeneration)
      waterfallAdProviderService.find(wapID).get must haveClass[WaterfallAdProvider]
    }

    "respond with a 400 if a WaterfallAdProvider cannot be created" in new WithAppBrowser(distributor.id.get) {
      waterfallAdProviderService.create(currentWaterfall.id, adProviderID2.get, None, Some(5.0), configurable = true, active = true)
      waterfallService.update(currentWaterfall.id, optimizedOrder = true, testMode = false, paused = false)
      database.withTransaction { implicit connection => appConfigService.createWithWaterfallIDInTransaction(currentWaterfall.id, None) }
      val originalGeneration = generationNumber(currentApp.id)
      val configurationData = Seq("configurable" -> JsBoolean(true), "adProviderID" -> JsNumber(adProviderID2.get), "appToken" -> JsString(currentApp.token), "cpm" -> JsString("5.00"),
        "waterfallID" -> JsString(currentWaterfall.id.toString), "generationNumber" -> JsNumber(originalGeneration), "active" -> JsBoolean(true), "waterfallOrder" -> JsString("0"))
      val body = JsObject(configurationData)
      val Some(result) = route(wapCreateRequest(body).withSession("distributorID" -> distributor.id.get.toString, "username" -> user.email))
      status(result) must beEqualTo(400)
      generationNumber(currentApp.id) must beEqualTo(originalGeneration)
    }

    "respond with a 400 if the JSON is not valid" in new WithAppBrowser(distributor.id.get) {
      waterfallAdProviderService.create(currentWaterfall.id, adProviderID2.get, None, Some(5.0), configurable = true, active = true)
      waterfallService.update(currentWaterfall.id, optimizedOrder = true, testMode = false, paused = false)
      database.withTransaction { implicit connection => appConfigService.createWithWaterfallIDInTransaction(currentWaterfall.id, None) }
      val requestWithInvalidJson = FakeRequest(
        POST,
        controllers.routes.WaterfallAdProvidersController.create(distributor.id.get).url,
        FakeHeaders(Seq("Content-type" -> "application/json")),
        ""
      )
      val Some(result) = route(requestWithInvalidJson.withSession("distributorID" -> distributor.id.get.toString, "username" -> user.email))
      status(result) must beEqualTo(400)
    }
  }

  "WaterfallAdProvidersController.edit" should {
    "render the appropriate configuration fields for a given ad provider" in new WithAppBrowser(distributor.id.get) {
      val waterfallAdProviderID = waterfallAdProviderService.create(currentWaterfall.id, adProviderID1.get, None, None, configurable = true).get
      val Some(result) = route(wapEditRequest(waterfallAdProviderID).withSession("distributorID" -> distributor.id.get.toString, "username" -> user.email))
      val page = contentAsString(result)
      (requiredKeys ++ reportingKeys).map(page must contain(_))
    }

    "properly fill the values of the fields if data exists" in new WithAppBrowser(distributor.id.get) {
      val waterfallAdProviderID = waterfallAdProviderService.create(currentWaterfall.id, adProviderID1.get, None, None, true).get
      val wap = waterfallAdProviderService.find(waterfallAdProviderID).get
      val configParam = "Some value"
      val updatedValues = new WaterfallAdProvider(wap.id, wap.waterfallID, wap.adProviderID, None, None, Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq(requiredKeys(0) -> JsString(configParam))))), wap.reportingActive)
      waterfallAdProviderService.update(updatedValues)
      val Some(result) = route(wapEditRequest(waterfallAdProviderID).withSession("distributorID" -> distributor.id.get.toString, "username" -> user.email))
      contentAsString(result) must contain(configParam)
    }

    "hide the field if there is no display key present for a given app config param" in new WithAppBrowser(distributor.id.get) {
      val param = "propertyID"
      val hyprMarketplaceConfiguration = {
        "{" +
          "\"requiredParams\":[" +
          "{\"description\": \"Your HyprMX Property ID\", \"displayKey\": \"\", \"key\": \"" + param + "\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
          "], \"reportingParams\": [], \"callbackParams\": []" +
        "}"
      }
      val hyprName = "HyprMarketplace"
      val hyprID = adProviderService.create(
        name = hyprName,
        displayName = hyprName,
        configurationData = hyprMarketplaceConfiguration,
        platformID = testPlatform.Ios.PlatformID,
        callbackUrlFormat = None,
        callbackUrlDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(hyprName),
        configurable = false,
        defaultEcpm = Some(20)
      ).get
      val waterfallAdProviderID = waterfallAdProviderService.create(currentWaterfall.id, hyprID, None, None, configurable = true).get
      logInUser()
      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      browser.executeScript("$('.configure').first().click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#modal").areDisplayed
      browser.find(".edit-waterfall-ad-provider-field").first().isDisplayed must beFalse
    }

    "contain the appropriate configurable callback URL description for the ad network" in new WithAppBrowser(user.distributorID.get) {
      List((adProviderID1, adProvider1CallbackUrlDescription), (adProviderID2, adProvider2CallbackUrlDescription)).map { adProviderInfo =>
        val waterfallAdProviderID = waterfallAdProviderService.create(currentWaterfall.id, adProviderInfo._1.get, None, None, configurable = true).get
        val Some(result) = route(wapEditRequest(waterfallAdProviderID).withSession("distributorID" -> user.distributorID.get.toString, "username" -> user.email))
        val response = contentAsJson(result)
        (response \ "callbackUrlDescription").as[String] must beEqualTo(adProviderInfo._2)
      }
    }
  }

  "WaterfallAdProvidersController.update" should {
    "update the configuration_data field of the waterfall_ad_providers record" in new WithAppBrowser(distributor.id.get) {
      waterfallService.update(currentWaterfall.id, optimizedOrder = true, testMode = false, paused = false)
      val waterfallAdProviderID = waterfallAdProviderService.create(currentWaterfall.id, adProviderID1.get, None, None, true).get
      clearGeneration(currentApp.id)
      val originalGeneration = generationNumber(currentApp.id)
      val updatedParam = "Some new value"
      val requiredParamArray = JsArray(Seq(JsObject(Seq("key" -> JsString(configurationParams.head), "value" -> JsString(updatedParam),
        "displayKey" -> JsString(configurationParams.head), "dataType" -> JsString("String"), "description" -> JsString("Description"), "refreshOnAppRestart" -> JsBoolean(false)))))
      val requiredParamsData = JsObject(Seq("requiredParams" -> requiredParamArray))
      val configurationData = JsObject(Seq("callbackParams" -> JsArray(Seq()), "reportingParams" -> JsArray(Seq()),
        "reportingActive" -> JsBoolean(false), "appToken" -> JsString(currentApp.token), "waterfallID" -> JsString(currentWaterfall.id.toString),
        "generationNumber" -> JsNumber(originalGeneration), "cpm" -> JsString("5.0"))).deepMerge(requiredParamsData)
      val postRequest = FakeRequest(
        POST,
        controllers.routes.WaterfallAdProvidersController.update(distributor.id.get, waterfallAdProviderID).url,
        FakeHeaders(Seq("Content-type" -> "application/json")),
        configurationData
      )
      val Some(result) = route(postRequest.withSession("distributorID" -> distributor.id.get.toString, "username" -> user.email))
      status(result) must equalTo(200)
      val wap = waterfallAdProviderService.find(waterfallAdProviderID).get
      (wap.configurationData \ "requiredParams" \ configurationParams.head).as[String] must beEqualTo(updatedParam)
      generationNumber(currentApp.id) must beEqualTo(originalGeneration + 1)
    }

    "respond with a 400 if proper JSON is not received" in new WithAppBrowser(distributor.id.get) {
      val waterfallAdProviderID = waterfallAdProviderService.create(currentWaterfall.id, adProviderID1.get, None, None, true).get
      val postRequest = FakeRequest(
        POST,
        controllers.routes.WaterfallAdProvidersController.update(distributor.id.get, waterfallAdProviderID).url,
        FakeHeaders(Seq("Content-type" -> "application/json")),
        ""
      )
      val Some(result) = route(postRequest.withSession("distributorID" -> distributor.id.get.toString, "username" -> user.email))
      status(result) must equalTo(400)
    }

    "update the eCPM value for a WaterfallAdProvider if the eCPM is valid" in new WithAppBrowser(distributor.id.get) {
      val validEcpm = "5.0"

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      browser.find(".waterfall li", withId(adProviderDisplayNames(1))).findFirst(".configure").click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      val newWap = waterfallAdProviderService.findAllByWaterfallID(currentWaterfall.id)(0)
      browser.fill("input").`with`(validEcpm, "Some key")
      browser.executeScript("var button = $(':button[name=update-ad-provider]'); button.click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").containsText(adProviderDisplayNames(1) + " updated!")
      val updatedWap = waterfallAdProviderService.find(newWap.id).get
      updatedWap.cpm.get.toString must beEqualTo(validEcpm)
    }

    "not update the WaterfallAdProvider if the eCPM is not valid" in new WithAppBrowser(distributor.id.get) {
      val invalidEcpms = List(" ", "2 0", "2,0", "2e0")

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      waterfallService.find(currentWaterfall.id, distributor.id.get).get.testMode must beEqualTo(true)
      browser.executeScript("$('.configure').first().click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      val newWap = waterfallAdProviderService.findAllByWaterfallID(currentWaterfall.id)(0)
      invalidEcpms.map { eCPM =>
        browser.fill("input").`with`(eCPM, "Some key")
        browser.executeScript("var button = $(':button[name=update-ad-provider]'); button.click();")
        browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#ecpm-input").containsText("eCPM must be a valid number greater than or equal to $0.00")
      }
    }

    "notify the user if the app must be restarted for AppConfig changes to take effect" in new WithAppBrowser(distributor.id.get) {
      val wapID = waterfallAdProviderService.create(currentWaterfall.id, adProviderID1.get, None, None, true, true).get
      val wap = waterfallAdProviderService.find(wapID).get
      waterfallAdProviderService.update(new WaterfallAdProvider(wapID, currentWaterfall.id, wap.adProviderID, None, None, Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), false))
      waterfallService.update(currentWaterfall.id, optimizedOrder = true, testMode = false, paused = false)
      clearGeneration(currentApp.id)
      val originalGeneration = generationNumber(currentApp.id)
      val configKey = "some key"

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      browser.executeScript("$('.configure').first().click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      browser.fill("input").`with`("5.0", configKey)
      browser.click("button[name=update-ad-provider]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").containsText("will require your app to be restarted")
      generationNumber(currentApp.id) must beEqualTo(originalGeneration + 1)
    }

    "update the status of reporting for a waterfall ad provider" in new WithAppBrowser(distributor.id.get) {
      val wap1ID = waterfallAdProviderService.create(currentWaterfall.id, adProviderID1.get, None, None, true, true).get
      val wap = waterfallAdProviderService.find(wap1ID).get
      waterfallAdProviderService.update(new WaterfallAdProvider(wap1ID, currentWaterfall.id, wap.adProviderID, None, None, Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), false))
      waterfallService.update(currentWaterfall.id, optimizedOrder = true, testMode = false, paused = false)
      clearGeneration(currentApp.id)

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      browser.executeScript("$('.configure').first().click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      val newWap = waterfallAdProviderService.find(wap1ID).get
      newWap.reportingActive must beEqualTo(false)
      browser.fill("input").`with`("5.0", "Some key")
      browser.executeScript("var button = $(':checkbox[id=reporting-active-switch]'); button.click();")
      browser.executeScript("$('button[name=update-ad-provider]').click()")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").containsText(adProviderDisplayNames.head + " updated!")
      browser.executeScript("var button = $(':button[name=cancel]'); button.click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areNotDisplayed()
      waterfallAdProviderService.find(newWap.id).get.reportingActive must beEqualTo(true)
    }

    "create an inactive WaterfallAdProvider when an AdProvider is configured before being activated" in new WithAppBrowser(distributor.id.get) with JsonTesting {
      val configKey = "some key"

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      browser.find(".waterfall li", withId(adProviderDisplayNames(1))).findFirst(".configure").click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      browser.fill("input").`with`("5.0", configKey)
      browser.executeScript("$('button[name=update-ad-provider]').click()")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").containsText(adProviderDisplayNames(1) + " updated!")
      val wap = waterfallAdProviderService.findAllByWaterfallID(currentWaterfall.id)(0)
      wap.active.get must beEqualTo(false)
      (wap.configurationData \ "requiredParams" \ requiredKeys(0)).as[String] must beEqualTo(configKey)
      (wap.configurationData \ "requiredParams" \ requiredKeys(1)).as[String] must beEqualTo(configKey)
    }

    "create a WaterfallAdProvider with an eCPM value if an AdProvider with a default eCPM is activated" in new WithAppBrowser(distributor.id.get) {
      waterfallService.update(currentWaterfall.id, optimizedOrder = true, testMode = false, paused = false)
      val originalGeneration = generationNumber(currentApp.id)
      val defaultEcpm = Some(20.0)
      val adProviderName = "Test Ad Provider With Default eCPM"
      val adProviderWithDefaultEcpmID = adProviderService.create(
        name = adProviderName,
        displayName = adProviderName,
        configurationData = configurationData,
        platformID = testPlatform.Ios.PlatformID,
        callbackUrlFormat = None,
        callbackUrlDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(adProviderName),
        configurable = true,
        defaultEcpm
      ).get

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      browser.find(".waterfall li", withId(adProviderName)).findFirst(".configure").click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      browser.findFirst("input[name=eCPM]").getValue must beEqualTo("20.00")
      browser.fill("input").`with`("20.00", "12345")
      browser.executeScript("$('button[name=update-ad-provider]').click()")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").containsText(adProviderName + " updated!")
      waterfallAdProviderService.findAllByWaterfallID(currentWaterfall.id).filter(wap => wap.adProviderID == adProviderWithDefaultEcpmID).head.cpm must beEqualTo(defaultEcpm)
      generationNumber(currentApp.id) must beEqualTo(originalGeneration + 1)
    }

    "flash an error message and not update the WaterfallAdProvider if all required fields are not filled" in new WithAppBrowser(distributor.id.get) {
      waterfallService.update(currentWaterfall.id, optimizedOrder = true, testMode = false, paused = false)

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      browser.executeScript("$('.waterfall li[id=\\'" + adProviderDisplayNames.head + "\\']').children().children('.wap-buttons').children('.configure').click()")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      browser.fill("input").`with`(" ")
      val newEcpm = 25.0
      browser.fill("input[name=eCPM]").`with`(newEcpm.toString)
      browser.find("button[name=update-ad-provider]").first().isEnabled must beEqualTo(false)
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(".required-params-errors").areDisplayed()
      browser.click("button[name=update-ad-provider]")
      val wap = waterfallAdProviderService.find(adProviderID1.get).get
      wap.cpm must not be equalTo(newEcpm)
      wap.configurationData must beEqualTo(JsObject(Seq()))
    }

    "only allow the user to turn on reporting if a valid eCPM is already entered" in new WithAppBrowser(distributor.id.get) {
      val wap1ID = waterfallAdProviderService.create(currentWaterfall.id, adProviderID1.get, None, None, true, true).get
      val wap = waterfallAdProviderService.find(wap1ID).get
      val wapConfig = JsObject(Seq("requiredParams" -> JsObject(Seq()), "callbackParams" -> JsObject(Seq()), "reportingParams" -> JsObject(Seq())))
      waterfallAdProviderService.update(new WaterfallAdProvider(wap1ID, currentWaterfall.id, wap.adProviderID, None, None, Some(true), None, wapConfig, false))
      waterfallService.update(currentWaterfall.id, optimizedOrder = true, testMode = false, paused = false)
      clearGeneration(currentApp.id)

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      browser.executeScript("$('.configure').first().click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      val newWap = waterfallAdProviderService.find(wap1ID).get
      newWap.reportingActive must beEqualTo(false)
      browser.fill("input[name=eCPM]").`with`("")
      browser.executeScript("var button = $(':checkbox[id=reporting-active-switch]'); button.click();")
      browser.click("button[name=update-ad-provider]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#ecpm-input").containsText("eCPM must be a valid number greater than or equal to $0.00")
      waterfallAdProviderService.find(wap1ID).get.reportingActive must beFalse
      browser.executeScript("var button = $(':checkbox[id=reporting-active-switch]'); button.click();")
      browser.fill("input").`with`("5.0", "12345")
      browser.executeScript("var button = $(':checkbox[id=reporting-active-switch]'); button.click();")
      browser.click("button[name=update-ad-provider]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").containsText("will require your app to be restarted")
      waterfallAdProviderService.find(newWap.id).get.reportingActive must beEqualTo(true)
    }

    "not allow a user to enter less than the minimum number of characters for a WaterfallAdProvider configuration field" in new WithAppBrowser(distributor.id.get) {
      val adProviderConfigData = {
        "{" +
          "\"requiredParams\":[" +
            "{\"description\": \"Your Distributor ID\", \"displayKey\": \"Distributor ID\", \"key\": \"distributorID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": true, \"minLength\": 4}" +
          "], " +
          "\"reportingParams\": [], \"callbackParams\": []" +
        "}"
      }
      val adProviderName = "Test Ad Provider 3"
      adProviderService.create(
        name = adProviderName,
        displayName = adProviderName,
        configurationData = adProviderConfigData,
        platformID = testPlatform.Ios.PlatformID,
        callbackUrlFormat = None,
        callbackUrlDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(adProviderName),
        configurable = true,
        None
      )

      logInUser()

      goToAndWaitForAngular(controllers.routes.WaterfallsController.edit(distributor.id.get, currentWaterfall.id).url)
      browser.find(".waterfall li", withId(adProviderName)).findFirst(".configure").click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      browser.fill("input").`with`("1.0", "123")
      browser.fill("input").`with`("2.0")
      browser.find("button[name=update-ad-provider]").first().isEnabled must beEqualTo(false)
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(".required-params-errors").areDisplayed()
      browser.fill("input").`with`("1.0", "1234")
      browser.find("button[name=update-ad-provider]").first().isEnabled must beEqualTo(true)
      browser.click("button[name=update-ad-provider]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").containsText(adProviderName + " updated!")
    }
  }

  "WaterfallAdProvidersController.rollback" should {
    "return a bad request" in new WithFakeBrowser {
      val wapsController = new WaterfallAdProvidersController(modelService, database)
      val result = database.withTransaction { implicit connection =>
        wapsController.rollback(connection)
      }
      result.header.status must beEqualTo(BAD_REQUEST)
    }
  }
}
