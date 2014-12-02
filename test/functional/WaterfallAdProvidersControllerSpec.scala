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
import play.api.Play.current

@RunWith(classOf[JUnitRunner])
class WaterfallAdProvidersControllerSpec extends SpecificationWithFixtures with JsonTesting {
  val adProviderID = running(FakeApplication(additionalConfiguration = testDB)) {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into ad_providers (name, configuration_data) values ('test ad provider', CAST({json_string} as json));
        """
      ).on("json_string" -> configurationData).executeInsert()
    }
  }

  val distributorUser = running(FakeApplication(additionalConfiguration = testDB)) {
    DistributorUser.create(email, password, "Company Name")
    DistributorUser.findByEmail(email).get
  }

  val distributorID = distributorUser.distributorID.get

  val waterfallID = running(FakeApplication(additionalConfiguration = testDB)) {
    val appID = App.create(distributorUser.distributorID.get, "App 1").get
    VirtualCurrency.create(appID, "Coins", 100.toLong, None, None, Some(true))
    DB.withTransaction { implicit connection => createWaterfallWithConfig(appID, "New Waterfall") }
  }

  val waterfallAdProviderID = running(FakeApplication(additionalConfiguration = testDB)) {
    WaterfallAdProvider.create(waterfallID, adProviderID.get, None, None, true).get
  }

  val request = FakeRequest(
    GET,
    controllers.routes.WaterfallAdProvidersController.edit(distributorUser.distributorID.get, waterfallAdProviderID, None).url,
    FakeHeaders(),
    ""
  )

  "WaterfallAdProvidersController.edit" should {
    "render the appropriate configuration fields for a given ad provider" in new WithFakeBrowser {
      val Some(result) = route(request.withSession("distributorID" -> distributorUser.distributorID.get.toString(), "username" -> email))
      val page = contentAsString(result)
      configurationParams.map { param =>
        page must contain(param)
      }
    }

    "properly fill the values of the fields if data exists" in new WithFakeBrowser {
      val wap = WaterfallAdProvider.find(waterfallAdProviderID).get
      val configParam = "Some value"
      val updatedValues = new WaterfallAdProvider(wap.id, wap.waterfallID, wap.adProviderID, None, None, Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq(configurationParams(0) -> JsString(configParam))))), wap.reportingActive)
      WaterfallAdProvider.update(updatedValues)
      val Some(result) = route(request.withSession("distributorID" -> distributorUser.distributorID.get.toString(), "username" -> email))
      contentAsString(result) must contain(configParam)
    }
  }

  "WaterfallAdProvidersController.update" should {
    "update the configuration_data field of the waterfall_ad_providers record" in new WithFakeBrowser {
      val waterfall = Waterfall.find(waterfallID).get
      val currentApp = App.find(waterfall.app_id).get
      clearGeneration(waterfall.app_id)
      val originalGeneration = generationNumber(waterfall.app_id)
      val updatedParam = "Some new value"
      val configurationData = Seq("configurationData" -> JsObject(Seq(configurationParams(0) -> JsString(updatedParam))), "reportingActive" -> JsString("true"),
        "appToken" -> JsString(currentApp.token), "waterfallID" -> JsString(waterfall.id.toString), "generationNumber" -> JsString(originalGeneration.toString), "eCPM" -> JsString("5.0"))
      val body = JsObject(configurationData)
      val postRequest = FakeRequest(
        POST,
        controllers.routes.WaterfallAdProvidersController.update(distributorUser.distributorID.get, waterfallAdProviderID).url,
        FakeHeaders(Seq("Content-type" -> Seq("application/json"))),
        body
      )
      val Some(result) = route(postRequest.withSession("distributorID" -> distributorUser.distributorID.get.toString(), "username" -> email))
      status(result) must equalTo(200)
      val wap = WaterfallAdProvider.find(waterfallAdProviderID).get
      wap.configurationData must beEqualTo(body \ "configurationData")
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration + 1)
    }

    "respond with a 400 if proper JSON is not received" in new WithFakeBrowser {
      val postRequest = FakeRequest(
        POST,
        controllers.routes.WaterfallAdProvidersController.update(distributorUser.distributorID.get, waterfallAdProviderID).url,
        FakeHeaders(Seq("Content-type" -> Seq("application/json"))),
        ""
      )
      val Some(result) = route(postRequest.withSession("distributorID" -> distributorUser.distributorID.get.toString(), "username" -> email))
      status(result) must equalTo(400)
    }

    "update the eCPM value for a WaterfallAdProvider if the eCPM is valid" in new WithFakeBrowser {
      val newAppID = App.create(distributorID, "New App").get
      val validEcpm = "5.0"
      VirtualCurrency.create(newAppID, "Coins", 100.toLong, None, None, Some(true))
      val newWaterfallID = DB.withTransaction { implicit connection => createWaterfallWithConfig(newAppID, "New App") }

      logInUser()

      DB.withTransaction { implicit connection => AppConfig.create(newAppID, App.find(newAppID).get.token, generationNumber(newAppID)) }
      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, newWaterfallID).url)
      Waterfall.find(newWaterfallID).get.testMode must beEqualTo(true)
      browser.$(".configure.inactive-button").first().click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      val newWap = WaterfallAdProvider.findAllByWaterfallID(newWaterfallID)(0)
      browser.fill("input").`with`(validEcpm, "Some key")
      browser.executeScript("var button = $(':button[name=update-ad-provider]'); button.click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-success").areDisplayed()
      val updatedWap = WaterfallAdProvider.find(newWap.id).get
      updatedWap.cpm.get.toString must beEqualTo(validEcpm)
    }

    "not update the WaterfallAdProvider if the eCPM is not valid" in new WithFakeBrowser {
      val newAppID = App.create(distributorID, "New App").get
      val invalidEcpm = " "
      VirtualCurrency.create(newAppID, "Coins", 100.toLong, None, None, Some(true))
      val newWaterfallID = DB.withTransaction { implicit connection => createWaterfallWithConfig(newAppID, "New App") }

      logInUser()

      DB.withTransaction { implicit connection => AppConfig.create(newAppID, App.find(newAppID).get.token, generationNumber(newAppID)) }
      browser.goTo(controllers.routes.WaterfallsController.edit(distributorID, newWaterfallID).url)
      Waterfall.find(newWaterfallID).get.testMode must beEqualTo(true)
      browser.$(".configure.inactive-button").first().click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-waterfall-ad-provider").areDisplayed()
      val newWap = WaterfallAdProvider.findAllByWaterfallID(newWaterfallID)(0)
      browser.fill("input").`with`(invalidEcpm, "Some key")
      browser.executeScript("var button = $(':button[name=update-ad-provider]'); button.click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-error").hasText("eCPM must be a valid number.")
    }
  }
  step(clean)
}
