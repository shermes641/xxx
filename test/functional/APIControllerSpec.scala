package functional

import anorm._
import controllers.APIController
import hmac.{HmacHashData, Signer}
import models._
import org.specs2.mock.Mockito
import play.api.Play.current
import play.api.db.DB
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test._
import resources.{AdProviderSpecSetup, SpecificationWithFixtures, WaterfallSpecSetup}
import scala.concurrent.Future

class APIControllerSpec extends SpecificationWithFixtures with WaterfallSpecSetup with AdProviderSpecSetup with Mockito {
  val wap1ID = running(FakeApplication(additionalConfiguration = testDB)) {
    WaterfallAdProvider.create(waterfall.id, adProviderID1.get, None, None, configurable = true, active = true).get
  }

  val wap2ID = running(FakeApplication(additionalConfiguration = testDB)) {
    WaterfallAdProvider.create(waterfall.id, adProviderID2.get, None, None, configurable = true, active = true).get
  }

  val (completionApp, completionWaterfall, _, _) = running(FakeApplication(additionalConfiguration = testDB)) {
    val (completionApp, completionWaterfall, _, _) = setUpApp(distributor.id.get)
    WaterfallAdProvider.create(completionWaterfall.id, adProviderID1.get, None, None, configurable = true, active = true).get
    DB.withTransaction { implicit connection => AppConfig.createWithWaterfallIDInTransaction(completionWaterfall.id, None) }
    (completionApp, completionWaterfall, None, None)
  }

  "APIController.appConfigV1" should {
    "respond with 404 if token is not valid" in new WithFakeBrowser {
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.appConfigV1("some-fake-token", None).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(404)
      contentAsString(result) must contain("App Configuration not found.")
    }

    "respond with the HyprMarketplace test distributor configuration when waterfall is in test mode" in new WithFakeBrowser {
      Waterfall.update(waterfall.id, optimizedOrder = false, testMode = true, paused = false)
      DB.withTransaction { implicit connection => AppConfig.createWithWaterfallIDInTransaction(waterfall.id, None) }
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.appConfigV1(app1.token, None).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      val appConfig = Json.parse(contentAsString(result))
      val testAdProviderConfig: JsValue = JsObject(Seq("distributorID" -> JsString(AppConfig.TestModeDistributorID),
        "propertyID" -> JsString(AppConfig.TestModePropertyID), "providerName" -> JsString(AppConfig.TestModeProviderName),
        "providerID" -> JsNumber(AppConfig.TestModeProviderID), "eCPM" -> JsNumber(5.0), "sdkBlacklistRegex" -> JsString(AppConfig.TestModeSdkBlacklistRegex)))
      val vcAttributes = AppConfig.TestModeVirtualCurrency
      val expectedVCJson = JsObject(Seq("name" -> JsString(vcAttributes.name), "exchangeRate" -> JsNumber(vcAttributes.exchangeRate),
        "rewardMin" -> JsNumber(vcAttributes.rewardMin), "rewardMax" -> JsNumber(vcAttributes.rewardMax.get), "roundUp" -> JsBoolean(vcAttributes.roundUp)))
      (appConfig \ "adProviderConfigurations") must beEqualTo(JsArray(testAdProviderConfig :: Nil))
      (appConfig \ "analyticsConfiguration") must beEqualTo(JsonBuilder.analyticsConfiguration \ "analyticsConfiguration")
      (appConfig \ "errorReportingConfiguration") must beEqualTo(JsonBuilder.errorReportingConfiguration \ "errorReportingConfiguration")
      (appConfig \ "virtualCurrency") must beEqualTo(expectedVCJson)
      (appConfig \ "appName").as[String] must beEqualTo(AppConfig.TestModeAppName)
      (appConfig \ "appID").as[Long] must beEqualTo(AppConfig.TestModeHyprMediateAppID)
      (appConfig \ "distributorName").as[String] must beEqualTo(AppConfig.TestModeHyprMediateDistributorName)
      (appConfig \ "distributorID").as[Long] must beEqualTo(AppConfig.TestModeHyprMediateDistributorID)
      (appConfig \ "appConfigRefreshInterval").as[Long] must beEqualTo(AppConfig.TestModeAppConfigRefreshInterval)
      (appConfig \ "logFullConfig").as[Boolean] must beEqualTo(true)
      (appConfig \ "paused").as[Boolean] must beEqualTo(false)
      (appConfig \ "generationNumber") must haveClass[JsNumber]
    }

    "respond with ad providers ordered by eCPM when the waterfall is in optimized mode" in new WithFakeBrowser {
      Waterfall.update(waterfall.id, optimizedOrder = true, testMode = false, paused = false)
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1ID, waterfall.id, adProviderID1.get, None, Some(5.0), Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), true))
      WaterfallAdProvider.update(new WaterfallAdProvider(wap2ID, waterfall.id, adProviderID2.get, None, Some(1.0), Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), true))
      DB.withTransaction { implicit connection => AppConfig.create(app1.id, app1.token, generationNumber(app1.id)) }
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.appConfigV1(app1.token, None).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      val appConfig = Json.parse(contentAsString(result))
      (appConfig \ "logFullConfig").as[Boolean] must beEqualTo(true)
      val adProviderConfigs = (appConfig \ "adProviderConfigurations").as[JsArray].as[List[JsValue]]
      adProviderConfigs.zipWithIndex.map { case (provider, index) => (provider \ "providerName").as[String] must beEqualTo(adProviders(index)) }
    }

    "respond with the current waterfall order when waterfall is live and not optimized" in new WithFakeBrowser {
      Waterfall.update(waterfall.id, optimizedOrder = false, testMode = false, paused = false)
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1ID, waterfall.id, adProviderID1.get, Some(0), Some(1.0), Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), true))
      WaterfallAdProvider.update(new WaterfallAdProvider(wap2ID, waterfall.id, adProviderID2.get, Some(1), Some(5.0), Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), true))
      DB.withTransaction { implicit connection => AppConfig.create(app1.id, app1.token, generationNumber(app1.id)) }
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.appConfigV1(app1.token, None).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      val appConfig: JsValue = Json.parse(contentAsString(result))
      (appConfig \ "logFullConfig").as[Boolean] must beEqualTo(true)
      val adProviderConfigs = (appConfig \ "adProviderConfigurations").as[JsArray].as[List[JsValue]]
      adProviderConfigs.zipWithIndex.map { case (provider, index) => (provider \ "providerName").as[String] must beEqualTo(adProviders(index)) }
    }

    "exclude ad providers from the waterfall order if the virtual currency roundUp option is false and ad provider's current cpm value is less than the calculated reward amount for the virtual currency" in new WithFakeBrowser {
      val roundUp = false
      VirtualCurrency.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, virtualCurrency1.exchangeRate, 1, None, roundUp))
      Waterfall.update(waterfall.id, optimizedOrder = true, testMode = false, paused = false)
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1ID, waterfall.id, adProviderID1.get, None, Some(5.0), Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), true))
      WaterfallAdProvider.update(new WaterfallAdProvider(wap2ID, waterfall.id, adProviderID2.get, None, Some(50.0), Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), true))
      DB.withTransaction { implicit connection => AppConfig.createWithWaterfallIDInTransaction(waterfall.id, None) }
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.appConfigV1(completionApp.token, None).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      val appConfig: JsValue = Json.parse(contentAsString(result))
      (appConfig \ "logFullConfig").as[Boolean] must beEqualTo(true)
      val adProviderConfigs = (appConfig \ "adProviderConfigurations").as[JsArray].as[List[JsValue]]
      adProviderConfigs.map(provider => (provider \ "providerName").as[String]) must contain(adProviders.head.name)
      adProviderConfigs.map(provider => (provider \ "providerName").as[String]) must not contain adProviders(1).name
    }

    "respond with an empty adProviderConfigurations array when there are no active ad providers that meet the minimum reward threshold" in new WithFakeBrowser {
      val roundUp = false
      VirtualCurrency.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, virtualCurrency1.exchangeRate, 100, None, roundUp))
      Waterfall.update(waterfall.id, optimizedOrder = true, testMode = false, paused = false)
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1ID, waterfall.id, adProviderID1.get, None, Some(5.0), Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), true))
      DB.withTransaction { implicit connection => AppConfig.create(app1.id, app1.token, generationNumber(app1.id)) }
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.appConfigV1(app1.token, None).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      val jsonResponse: JsValue = Json.parse(contentAsString(result))
      (jsonResponse \ "logFullConfig").as[Boolean] must beEqualTo(true)
      (jsonResponse \ "adProviderConfigurations").as[JsArray].as[List[JsObject]].size must beEqualTo(0)
    }

    "respond with an error if the platform param conflicts with the platformID found in the AppConfig" in new WithFakeBrowser {
      def verifyBadRequest(appToken: String, correctPlatformName: String, incorrectPlatformName: String) = {
        val request = FakeRequest(
          GET,
          controllers.routes.APIController.appConfigV1(appToken, Some(incorrectPlatformName)).url,
          FakeHeaders(),
          ""
        )
        val Some(result) = route(request)
        status(result) must equalTo(400)
        val jsonResponse: JsValue = Json.parse(contentAsString(result))
        (jsonResponse \ "message").as[String] must beEqualTo(APIController.platformError(correctPlatformName, incorrectPlatformName))
      }

      List((Platform.Android, Platform.Ios), (Platform.Ios, Platform.Android)).map { platforms =>
        val appPlatform = platforms._1
        val incorrectPlatform = platforms._2

        val (currentApp, currentWaterfall, _, _) = setUpApp(
          distributorID = distributor.id.get,
          appName = Some("platform test app" + appPlatform.PlatformName),
          currencyName = "Coins",
          exchangeRate = 100,
          rewardMin = 1,
          rewardMax = None,
          roundUp = true,
          platformID = appPlatform.PlatformID
        )
        WaterfallAdProvider.create(waterfallID = currentWaterfall.id, adProviderID = adProviderID1.get, waterfallOrder = None, cpm = Some(5.0), configurable = true, active = true, pending = false)
        DB.withTransaction { implicit connection => AppConfig.create(currentApp.id, currentApp.token, generationNumber(currentApp.id)) }

        // Verify we get a platform error when in test mode
        Waterfall.find(currentWaterfall.id, distributor.id.get).get.testMode must beTrue
        verifyBadRequest(currentApp.token, appPlatform.PlatformName, incorrectPlatform.PlatformName)

        Waterfall.update(currentWaterfall.id, optimizedOrder = true, testMode = false, paused = false)
        DB.withTransaction { implicit connection => AppConfig.create(currentApp.id, currentApp.token, generationNumber(currentApp.id)) }

        // Verify we get a platform error when in live mode
        Waterfall.find(currentWaterfall.id, distributor.id.get).get.testMode must beFalse
        verifyBadRequest(currentApp.token, appPlatform.PlatformName, incorrectPlatform.PlatformName)
      }
    }
  }

  "APIController.vungleCompletionV1" should {
    val transactionID = Some("0123456789")
    val wap = running(FakeApplication(additionalConfiguration = testDB)) {
      val id = WaterfallAdProvider.create(completionWaterfall.id, vungleID, None, None, configurable = true, active = true).get
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
      verifyNewCompletion(completionApp.token, transactionID.get, Platform.Ios.Vungle.name, completionCount)
    }

    "respond with a 400 if the request signature is not valid" in new WithFakeBrowser {
      val completionCount = tableCount("completions")
      WaterfallAdProvider.update(new WaterfallAdProvider(wap.id, wap.waterfallID, wap.adProviderID, None, None, Some(true), None, configuration, false))
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.vungleCompletionV1(completionApp.token, transactionID, Some("some-fake-digest"), Some(1), None).url,
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
        controllers.routes.APIController.vungleCompletionV1(completionApp.token, None, None, Some(1), None).url,
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
      val transactionID = Some("event-id")
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.appLovinCompletionV1(completionApp.token, transactionID, Some(1), None, None, None, None).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      verifyNewCompletion(completionApp.token, transactionID.get, Platform.Ios.AppLovin.name, completionCount)
    }

    "respond with a 400 if a necessary param is missing" in new WithFakeBrowser {
      val completionCount = tableCount("completions")
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.appLovinCompletionV1(completionApp.token, None, None, None, None, None, None).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(400)
      tableCount("completions") must beEqualTo(completionCount)
    }
  }

  "APIController.adColonyCompletionV1" should {
    val amount = Some(1)
    val currency = Some("Credits")
    val macSha1 = Some("")
    val odin1 = Some("")
    val openUDID = Some("")
    val udid = Some("")
    val uid = Some("")
    val verifier = Some("6c9186f082be09baef312da505114fa2")
    val transactionID = Some("0123456789")
    val customID = Some("testuser")
    val wap = running(FakeApplication(additionalConfiguration = testDB)) {
      val id = WaterfallAdProvider.create(completionWaterfall.id, adColonyID, None, None, configurable = true, active = true).get
      WaterfallAdProvider.find(id).get
    }
    val configuration = JsObject(Seq("callbackParams" -> JsObject(Seq("APIKey" -> JsString("abcdefg"))),
      "requiredParams" -> JsObject(Seq()), "reportingParams" -> JsObject(Seq())))

    "respond with a 200 if all necessary params are present and the signature is valid" in new WithFakeBrowser {
      val completionCount = tableCount("completions")
      WaterfallAdProvider.update(new WaterfallAdProvider(wap.id, wap.waterfallID, wap.adProviderID, None, None, Some(true), None, configuration, false))
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.adColonyCompletionV1(completionApp.token, transactionID, uid, amount, currency, openUDID, udid, odin1, macSha1, verifier, customID).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      contentAsString(result) must contain("vc_success")
      verifyNewCompletion(completionApp.token, transactionID.get, Platform.Ios.AdColony.name, completionCount)
    }

    "respond with a 200 if the request signature is not valid" in new WithFakeBrowser {
      val completionCount = tableCount("completions")
      WaterfallAdProvider.update(new WaterfallAdProvider(wap.id, wap.waterfallID, wap.adProviderID, None, None, Some(true), None, configuration, false))
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.adColonyCompletionV1(completionApp.token, Some("invalid-transaction-id"), uid, amount, currency, openUDID, udid, odin1, macSha1, verifier, customID).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      contentAsString(result) must contain("vc_decline")
      tableCount("completions") must beEqualTo(completionCount)
    }

    "respond with a 200 if a necessary param is missing" in new WithFakeBrowser {
      val completionCount = tableCount("completions")
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.adColonyCompletionV1(completionApp.token, None, uid, amount, currency, openUDID, udid, odin1, macSha1, verifier, customID).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      contentAsString(result) must contain("vc_decline")
      tableCount("completions") must beEqualTo(completionCount)
    }
  }

  "APIController.hyprMarketplaceCompletionV1" should {
    val uid = Some("abc")
    val sig = Some("bb5a5b5a1b0865a355ffa3ae96475753dfc55ba7d1266f24d5f8881e3427060d")
    val time = Some("1419972045")
    val partnerCode = Some("")
    val quantity = Some(1)

    val wap = running(FakeApplication(additionalConfiguration = testDB)) {
      val id = WaterfallAdProvider.create(completionWaterfall.id, hyprMarketplaceID, None, None, configurable = true, active = true).get
      WaterfallAdProvider.find(id).get
    }

    "respond with a 200 when all necessary params are present and transaction_id is not blank" in new WithFakeBrowser {
      val partnerCodeSig = Some("43c038d8f6edda911ef3813fe0c3e86a10437f0fbd78fccf47cca62f61212fdc")
      val nonBlankPartnerCode = Some("partner_code")
      val completionCount = tableCount("completions")
      WaterfallAdProvider.update(new WaterfallAdProvider(wap.id, wap.waterfallID, wap.adProviderID, None, None, Some(true), None, JsObject(Seq()), false))
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.hyprMarketplaceCompletionV1(completionApp.token, time, partnerCodeSig, quantity, None, None, uid, nonBlankPartnerCode).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      verifyNewCompletion(completionApp.token, nonBlankPartnerCode.get, Platform.Ios.HyprMarketplace.name, completionCount)
    }

    "respond with a 200 if all necessary params are present and the signature is valid" in new WithFakeBrowser {
      val completionCount = tableCount("completions")
      WaterfallAdProvider.update(new WaterfallAdProvider(wap.id, wap.waterfallID, wap.adProviderID, None, None, Some(true), None, JsObject(Seq()), false))
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.hyprMarketplaceCompletionV1(completionApp.token, time, sig, quantity, None, None, uid, partnerCode).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      verifyNewCompletion(completionApp.token, partnerCode.get, Platform.Ios.HyprMarketplace.name, completionCount)
    }

    "respond with a 400 if the request signature is not valid" in new WithFakeBrowser {
      val badSignature = Some("123")
      val completionCount = tableCount("completions")
      WaterfallAdProvider.update(new WaterfallAdProvider(wap.id, wap.waterfallID, wap.adProviderID, None, None, Some(true), None, JsObject(Seq()), false))
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.hyprMarketplaceCompletionV1(completionApp.token, time, badSignature, quantity, None, None, uid, partnerCode).url,
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
        controllers.routes.APIController.hyprMarketplaceCompletionV1(completionApp.token, time, sig, quantity, None, None, None, partnerCode).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(400)
      tableCount("completions") must beEqualTo(completionCount)
    }
  }

  "APIController.unityAdsCompletionV1" should {
    val sharedSecret = "ccfbde79b23f0fde7867cb9177b1a15"
    val sid = Some("jcaintic@jungroup.com")
    val oid = Some("546553466")
    val hmac = Some("ec61dfb3f7355aea49a1a81540073f48")
    val productID = Some("1061310")
    val wap = running(FakeApplication(additionalConfiguration = testDB)) {
      val id = WaterfallAdProvider.create(completionWaterfall.id, unityAdsID, None, None, configurable = true, active = true).get
      WaterfallAdProvider.find(id).get
    }
    val configuration = JsObject(Seq("callbackParams" -> JsObject(Seq("APIKey" -> JsString(sharedSecret))),
      "requiredParams" -> JsObject(Seq("APIKey" -> JsString(productID.get))), "reportingParams" -> JsObject(Seq())))

    "respond with a 200 if all necessary params are present and the signature is valid" in new WithFakeBrowser {
      val completionCount = tableCount("completions")
      WaterfallAdProvider.update(new WaterfallAdProvider(wap.id, wap.waterfallID, wap.adProviderID, None, None, Some(true), None, configuration, false))
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.unityAdsCompletionV1(completionApp.token, sid, oid, hmac, productID).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      contentAsString(result) must contain("1")
      verifyNewCompletion(completionApp.token, oid.get, Constants.UnityAdsName, completionCount)
    }

    "respond with a 400 if the request signature is not valid" in new WithFakeBrowser {
      val completionCount = tableCount("completions")
      WaterfallAdProvider.update(new WaterfallAdProvider(wap.id, wap.waterfallID, wap.adProviderID, None, None, Some(true), None, configuration, false))
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.unityAdsCompletionV1(completionApp.token, Some("invalid-transaction-id"), oid, hmac, productID).url,
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
        controllers.routes.APIController.unityAdsCompletionV1(completionApp.token, sid, None, hmac, productID).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(400)
      tableCount("completions") must beEqualTo(completionCount)
    }
  }

  "APIController.requestBuilder" should {
    val time = "some time"
    val sig = "some sig"
    val route = controllers.routes.APIController.hyprMarketplaceCompletionV1(completionApp.token, Some(time), Some(sig), None, None, None, None, None)
    val getRequest = FakeRequest(
      GET,
      route.url,
      FakeHeaders(),
      ""
    )
    val adProviderRequest = APIController.requestToJsonBuilder(getRequest)

    "store the HTTP method in a standardized JSON format" in new WithFakeBrowser {
      (adProviderRequest \ "method").as[String] must beEqualTo(route.method)
    }

    "store the URL path in a standardized JSON format" in new WithFakeBrowser {
      (adProviderRequest \ "path").as[String] must beEqualTo(route.url.split("""\?""")(0))
    }

    "store the query string in a standardized JSON format" in new WithFakeBrowser {
      (adProviderRequest \ "query") must haveClass[JsObject]
      (adProviderRequest \ "query" \ "time").as[String] must beEqualTo(time)
      (adProviderRequest \ "query" \ "sig").as[String] must beEqualTo(sig)
    }
  }

  "APIController.callbackResponse" should {
    val verificationInfo = mock[CallbackVerificationInfo]
    verificationInfo.appToken returns completionApp.token
    verificationInfo.adProviderName returns ""
    verificationInfo.transactionID returns ""
    verificationInfo.offerProfit returns Some(5.0)
    verificationInfo.isValid returns true
    verificationInfo.callbackURL returns Some("http://someUrl.com")

    val callback = mock[CallbackVerificationHelper]
    callback.returnFailure returns APIController.BadRequest
    callback.returnSuccess returns APIController.Ok
    callback.verificationInfo returns verificationInfo
    callback.currencyAmount returns 10
    callback.payout returns Some(10.0)

    val completion = mock[Completion]
    val adProviderRequest = JsObject(Seq())
    val hmacData =
      HmacHashData(uri = callback.verificationInfo.callbackURL.getOrElse(""),
        adProviderName = callback.adProviderName,
        rewardQuantity = callback.currencyAmount,
        estimatedOfferProfit = callback.payout,
        transactionId = callback.verificationInfo.transactionID
      )

    "return the ad provider's default successful response if the server to server callback receives a 200 response" in new WithFakeBrowser {
      val maybeHmacData = Some(
        hmacData.toQueryParamMap(
          timestamp = Some(Signer.timestamp),
          nonce = callback.verificationInfo.transactionID,
          hmacSecret = App.findHmacSecretByToken(callback.verificationInfo.appToken)
        )
      )
      completion.createWithNotification(verificationInfo, adProviderRequest, maybeHmacData) returns Future(true)
      APIController.callbackResponse(callback, adProviderRequest, completion).header.status must beEqualTo(callback.returnSuccess.header.status)
    }

    "return the ad provider's default failure response if the server to server callback does not respond with a 200" in new WithFakeBrowser {
      val maybeHmacData = Some(
        hmacData.toQueryParamMap(
          timestamp = Some(Signer.timestamp),
          nonce = callback.verificationInfo.transactionID,
          hmacSecret = App.findHmacSecretByToken(callback.verificationInfo.appToken)
        )
      )
      completion.createWithNotification(verificationInfo, adProviderRequest, maybeHmacData) returns Future(false)
      APIController.callbackResponse(callback, adProviderRequest, completion).header.status must beEqualTo(callback.returnFailure.header.status)
    }

    "return the ad provider's default failure response if the server to server callback times out" in new WithFakeBrowser {
      val maybeHmacData = Some(
        hmacData.toQueryParamMap(
          timestamp = Some(Signer.timestamp),
          nonce = callback.verificationInfo.transactionID,
          hmacSecret = App.findHmacSecretByToken(callback.verificationInfo.appToken)
        )
      )
      completion.createWithNotification(verificationInfo, adProviderRequest, maybeHmacData) returns Future {
        Thread.sleep(APIController.DefaultTimeout + 1000)
        true
      }
      APIController.callbackResponse(callback, adProviderRequest, completion).header.status must beEqualTo(callback.returnFailure.header.status)
    }

    "return the ad provider's default failure response if the incoming request was not valid" in new WithFakeBrowser {
      verificationInfo.isValid returns false
      val maybeHmacData = Some(
        hmacData.toQueryParamMap(
          timestamp = Some(Signer.timestamp),
          nonce = callback.verificationInfo.transactionID,
          hmacSecret = App.findHmacSecretByToken(callback.verificationInfo.appToken)
        )
      )
      completion.createWithNotification(verificationInfo, adProviderRequest, maybeHmacData) returns Future(true)
      APIController.callbackResponse(callback, adProviderRequest, completion).header.status must beEqualTo(callback.returnFailure.header.status)
    }
  }

  /**
   * Helper function to check that a new completion was inserted with the proper fields
   */
  def verifyNewCompletion(appToken: String,
                          transactionID: String,
                          adProviderName: String, expectedCompletionCount: Long) = {
    val completionID = DB.withConnection { implicit connection =>
      SQL(
        """
          SELECT completions.id FROM completions
          WHERE app_token = {app_token} AND transaction_id = {transaction_id} AND ad_provider_name = {ad_provider_name}
          ORDER BY completions.created_at DESC
          LIMIT 1
        """
      )
        .on("app_token" -> appToken, "transaction_id" -> transactionID, "ad_provider_name" -> adProviderName)()
        .map(row => row[Long]("id")).head
    }
    completionID must beGreaterThan(expectedCompletionCount)
    tableCount("completions") must beEqualTo(expectedCompletionCount + 1)
  }
}
