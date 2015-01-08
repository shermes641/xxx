package functional

import models._
import org.specs2.runner._
import org.junit.runner._
import play.api.db.DB
import play.api.libs.json._
import play.api.Play.current
import play.api.test.Helpers._
import play.api.test._
import resources.WaterfallSpecSetup

@RunWith(classOf[JUnitRunner])
class APIControllerSpec extends SpecificationWithFixtures with WaterfallSpecSetup {
  val wap1ID = running(FakeApplication(additionalConfiguration = testDB)) {
    WaterfallAdProvider.create(waterfall.id, adProviderID1.get, None, None, true, true).get
  }

  val wap2ID = running(FakeApplication(additionalConfiguration = testDB)) {
    WaterfallAdProvider.create(waterfall.id, adProviderID2.get, None, None, true, true).get
  }

  val (completionApp, completionWaterfall, _, _) = running(FakeApplication(additionalConfiguration = testDB)) {
    setUpApp(distributor.id.get)
  }

  "APIController.appConfigV1" should {
    "respond with 404 if token is not valid" in new WithFakeBrowser {
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.appConfigV1("some-fake-token").url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(404)
      contentAsString(result) must contain("App Configuration not found.")
    }

    "respond with the HyprMarketplace test distributor configuration when waterfall is in test mode" in new WithFakeBrowser {
      Waterfall.update(waterfall.id, false, true)
      DB.withTransaction { implicit connection => AppConfig.createWithWaterfallIDInTransaction(waterfall.id, None) }
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.appConfigV1(app1.token).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      val appConfig = Json.parse(contentAsString(result))
      val testAdProviderConfig: JsValue = JsObject(Seq("distributorID" -> JsString(AppConfig.TEST_MODE_DISTRIBUTOR_ID), "appID" -> JsString(AppConfig.TEST_MODE_APP_ID),
        "providerName" -> JsString(AppConfig.TEST_MODE_PROVIDER_NAME),"providerID" -> JsNumber(AppConfig.TEST_MODE_PROVIDER_ID), "eCPM" -> JsNumber(5.0)))
      val vcAttributes = AppConfig.TEST_MODE_VIRTUAL_CURRENCY
      val expectedVCJson = JsObject(Seq("name" -> JsString(vcAttributes.name), "exchangeRate" -> JsNumber(vcAttributes.exchangeRate),
        "rewardMin" -> JsNumber(vcAttributes.rewardMin.get), "rewardMax" -> JsNumber(vcAttributes.rewardMax.get), "roundUp" -> JsBoolean(vcAttributes.roundUp)))
      (appConfig \ "adProviderConfigurations") must beEqualTo(JsArray(testAdProviderConfig :: Nil))
      (appConfig \ "analyticsConfiguration") must beEqualTo(JsonBuilder.analyticsConfiguration \ "analyticsConfiguration")
      (appConfig \ "virtualCurrency") must beEqualTo(expectedVCJson)
      (appConfig \ "appName").as[String] must beEqualTo(AppConfig.TEST_MODE_APP_NAME)
      (appConfig \ "appID").as[Long] must beEqualTo(AppConfig.TEST_MODE_HYPRMEDIATE_APP_ID)
      (appConfig \ "distributorName").as[String] must beEqualTo(AppConfig.TEST_MODE_HYPRMEDIATE_DISTRIBUTOR_NAME)
      (appConfig \ "distributorID").as[Long] must beEqualTo(AppConfig.TEST_MODE_HYPRMEDIATE_DISTRIBUTOR_ID)
      (appConfig \ "appConfigRefreshInterval").as[Long] must beEqualTo(AppConfig.TEST_MODE_APP_CONFIG_REFRESH_INTERVAL)
      (appConfig \ "logFullConfig").as[Boolean] must beEqualTo(true)
      (appConfig \ "generationNumber") must haveClass[JsNumber]
    }

    "respond with ad providers ordered by eCPM when the waterfall is in optimized mode" in new WithFakeBrowser {
      Waterfall.update(waterfall.id, true, false)
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1ID, waterfall.id, adProviderID1.get, None, Some(5.0), Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), true))
      WaterfallAdProvider.update(new WaterfallAdProvider(wap2ID, waterfall.id, adProviderID2.get, None, Some(1.0), Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), true))
      DB.withTransaction { implicit connection => AppConfig.create(app1.id, app1.token, generationNumber(app1.id)) }
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.appConfigV1(app1.token).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      val appConfig = Json.parse(contentAsString(result))
      val adProviderConfigs = (appConfig \ "adProviderConfigurations").as[JsArray].as[List[JsValue]]
      adProviderConfigs.zipWithIndex.map { case(provider, index) => (provider \ "providerName").as[String] must beEqualTo(adProviders(index)) }
    }

    "respond with the current waterfall order when waterfall is live and not optimized" in new WithFakeBrowser {
      Waterfall.update(waterfall.id, false, false)
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1ID, waterfall.id, adProviderID1.get, Some(0), Some(1.0), Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), true))
      WaterfallAdProvider.update(new WaterfallAdProvider(wap2ID, waterfall.id, adProviderID2.get, Some(1), Some(5.0), Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), true))
      DB.withTransaction { implicit connection => AppConfig.create(app1.id, app1.token, generationNumber(app1.id)) }
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.appConfigV1(app1.token).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      val appConfig: JsValue = Json.parse(contentAsString(result))
      val adProviderConfigs = (appConfig \ "adProviderConfigurations").as[JsArray].as[List[JsValue]]
      adProviderConfigs.zipWithIndex.map { case(provider, index) => (provider \ "providerName").as[String] must beEqualTo(adProviders(index)) }
    }

    "exclude ad providers from the waterfall order if the virtual currency roundUp option is false and ad provider's current cpm value is less than the calculated reward amount for the virtual currency" in new WithFakeBrowser {
      val roundUp = false
      VirtualCurrency.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, virtualCurrency1.exchangeRate, Some(1.toLong), None, roundUp))
      Waterfall.update(waterfall.id, true, false)
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1ID, waterfall.id, adProviderID1.get, None, Some(5.0), Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), true))
      WaterfallAdProvider.update(new WaterfallAdProvider(wap2ID, waterfall.id, adProviderID2.get, None, Some(50.0), Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), true))
      DB.withTransaction { implicit connection => AppConfig.createWithWaterfallIDInTransaction(waterfall.id, None) }
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.appConfigV1(completionApp.token).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      val appConfig: JsValue = Json.parse(contentAsString(result))
      val adProviderConfigs = (appConfig \ "adProviderConfigurations").as[JsArray].as[List[JsValue]]
      adProviderConfigs.map( provider => (provider \ "providerName").as[String]) must contain(adProviders(0).name)
      adProviderConfigs.map( provider => (provider \ "providerName").as[String]) must not contain(adProviders(1).name)
    }

    "respond with an error when there are no active ad providers that meet the minimum reward threshold" in new WithFakeBrowser {
      val roundUp = false
      VirtualCurrency.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, virtualCurrency1.exchangeRate, Some(100.toLong), None, roundUp))
      Waterfall.update(waterfall.id, true, false)
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1ID, waterfall.id, adProviderID1.get, None, Some(5.0), Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), true))
      DB.withTransaction { implicit connection => AppConfig.create(app1.id, app1.token, generationNumber(app1.id)) }
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.appConfigV1(app1.token).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(400)
      val jsonResponse: JsValue = Json.parse(contentAsString(result))
      (jsonResponse \ "status").as[String] must beEqualTo("error")
      (jsonResponse \ "message").as[String] must beEqualTo("At this time there are no ad providers that are both active and have an eCPM that meets the minimum reward threshold.")
    }
  }

  "APIController.vungleCompletionV1" should {
    val vungleCallbackUrl = Some("http://mediation-staging.herokuapp.com/v1/waterfall/%s/vungle_completion?uid=%%user%%&openudid=%%udid%%&mac=%%mac%%&ifa=%%ifa%%&transaction_id=%%txid%%&digest=%%digest%%")
    val vungleAdProviderID = running(FakeApplication(additionalConfiguration = testDB)) {
      AdProvider.create("Vungle", "{\"requiredParams\":[{\"description\": \"Your Vungle App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}], \"reportingParams\": [{\"description\": \"Your API Key for Fyber\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}], \"callbackParams\": [{\"description\": \"Your Event API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}]}", vungleCallbackUrl).get
    }
    val transactionID = Some("0123456789")
    val wap = running(FakeApplication(additionalConfiguration = testDB)) {
      val id = WaterfallAdProvider.create(completionWaterfall.id, vungleAdProviderID, None, None, true, true).get
      WaterfallAdProvider.find(id).get
    }
    val configuration = JsObject(Seq("callbackParams" -> JsObject(Seq("APIKey" -> JsString("abcdefg"))),
      "requiredParams" -> JsObject(Seq()), "reportingParams" -> JsObject(Seq())))

    "respond with a 200 if all necessary params are present and the signature is valid" in new WithFakeBrowser {
      val completionCount = tableCount("completions")
      WaterfallAdProvider.update(new WaterfallAdProvider(wap.id, wap.waterfallID, wap.adProviderID, None, None, Some(true), None, configuration, false))
      val digest = Some("bf80d53f84df22bb91b48acc7606bc0909876f6fe981b1610a0352433ae16a63")
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.vungleCompletionV1(completionApp.token, transactionID, digest, Some(1), None).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      tableCount("completions") must beEqualTo(completionCount + 1)
    }

    "respond with a 400 if the request signature is not valid" in new WithFakeBrowser {
      val completionCount = tableCount("completions")
      WaterfallAdProvider.update(new WaterfallAdProvider(wap.id, wap.waterfallID, wap.adProviderID, None, None, Some(true), None, configuration, false))
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.vungleCompletionV1(completionWaterfall.token, transactionID, Some("some-fake-digest"), Some(1), None).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(400)
      tableCount("completions") must beEqualTo(completionCount)
    }

    "respond with a 400 if a necessary param is missing" in new WithFakeBrowser {
      val completionCount = tableCount("completions")
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.vungleCompletionV1(completionWaterfall.token, None, None, Some(1), None).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(400)
      tableCount("completions") must beEqualTo(completionCount)
    }
  }

  "APIController.appLovinCompletionV1" should {
    "respond with a 200 if all necessary params are present" in new WithFakeBrowser {
      val completionCount = tableCount("completions")
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.appLovinCompletionV1(completionWaterfall.token, Some("event-id"), Some(1), None, None, None, None).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      tableCount("completions") must beEqualTo(completionCount + 1)
    }

    "respond with a 400 if a necessary param is missing" in new WithFakeBrowser {
      val completionCount = tableCount("completions")
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.appLovinCompletionV1(completionWaterfall.token, None, None, None, None, None, None).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(400)
      tableCount("completions") must beEqualTo(completionCount)
    }
  }

  "APIController.adColonyCompletionV1" should {
    val adColonyCallbackUrl = Some("/v1/reward_callbacks/%s/ad_colony?id=[ID]&uid=[USER_ID]&amount=[AMOUNT]&currency=[CURRENCY]&open_udid=[OpenUDID]&udid=[UDID]&odin1=[ODIN1]&mac_sha1=[MAC_SHA1]&verifier=[VERIFIER]&custom_id=[CUSTOM_ID]")
    val adColonyAdProviderID = running(FakeApplication(additionalConfiguration = testDB)) {
      AdProvider.create("AdColony", "{\"requiredParams\":[{\"description\": \"Your AdColony App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}, {\"description\": \"Your AdColony Zones\", \"key\": \"zoneIds\", \"value\":\"\", \"dataType\": \"Array\"}], \"reportingParams\": [{\"description\": \"Your API Key for Fyber\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}], \"callbackParams\": [{\"description\": \"Your Event API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}]}", adColonyCallbackUrl, true, None).get
    }
    val amount = Some(1)
    val currency = Some("Credits")
    val macSha1 = Some("")
    val odin1 = Some("")
    val openUDID = Some("")
    val udid = Some("")
    val uid = Some("")
    val verifier = Some("15c16e889f382cb60d5b4550380bd5f7")
    val transactionID = Some("0123456789")
    val wap = running(FakeApplication(additionalConfiguration = testDB)) {
      val id = WaterfallAdProvider.create(completionWaterfall.id, adColonyAdProviderID, None, None, true, true).get
      WaterfallAdProvider.find(id).get
    }
    val configuration = JsObject(Seq("callbackParams" -> JsObject(Seq("APIKey" -> JsString("abcdefg"))),
      "requiredParams" -> JsObject(Seq()), "reportingParams" -> JsObject(Seq())))

    "respond with a 200 if all necessary params are present and the signature is valid" in new WithFakeBrowser {
      val completionCount = tableCount("completions")
      WaterfallAdProvider.update(new WaterfallAdProvider(wap.id, wap.waterfallID, wap.adProviderID, None, None, Some(true), None, configuration, false))
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.adColonyCompletionV1(completionApp.token, transactionID, uid, amount, currency, openUDID, udid, odin1, macSha1, verifier).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      tableCount("completions") must beEqualTo(completionCount + 1)
    }

    "respond with a 400 if the request signature is not valid" in new WithFakeBrowser {
      val completionCount = tableCount("completions")
      WaterfallAdProvider.update(new WaterfallAdProvider(wap.id, wap.waterfallID, wap.adProviderID, None, None, Some(true), None, configuration, false))
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.adColonyCompletionV1(completionApp.token, Some("invalid-transaction-id"), uid, amount, currency, openUDID, udid, odin1, macSha1, verifier).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(400)
      tableCount("completions") must beEqualTo(completionCount)
    }

    "respond with a 400 if a necessary param is missing" in new WithFakeBrowser {
      val completionCount = tableCount("completions")
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.adColonyCompletionV1(completionApp.token, None, uid, amount, currency, openUDID, udid, odin1, macSha1, verifier).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(400)
      tableCount("completions") must beEqualTo(completionCount)
    }
  }
}
