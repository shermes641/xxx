package functional

import controllers.APIController
import models._
import org.specs2.mock.Mockito
import org.specs2.runner._
import org.junit.runner._
import play.api.db.DB
import play.api.libs.json._
import play.api.Play.current
import play.api.test.Helpers._
import play.api.test._
import resources.{AdProviderSpecSetup, WaterfallSpecSetup}
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class APIControllerSpec extends SpecificationWithFixtures with WaterfallSpecSetup with AdProviderSpecSetup with Mockito {
  val wap1ID = running(FakeApplication(additionalConfiguration = testDB)) {
    WaterfallAdProvider.create(waterfall.id, adProviderID1.get, None, None, true, true).get
  }

  val wap2ID = running(FakeApplication(additionalConfiguration = testDB)) {
    WaterfallAdProvider.create(waterfall.id, adProviderID2.get, None, None, true, true).get
  }

  val (completionApp, completionWaterfall, _, _) = running(FakeApplication(additionalConfiguration = testDB)) {
    val (completionApp, completionWaterfall, _, _) = setUpApp(distributor.id.get)
    WaterfallAdProvider.create(completionWaterfall.id, adProviderID1.get, None, None, true, true).get
    DB.withTransaction { implicit connection => AppConfig.createWithWaterfallIDInTransaction(completionWaterfall.id, None)}
    (completionApp, completionWaterfall, None, None)
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
      Waterfall.update(waterfall.id, optimizedOrder = false, testMode = true, paused = false)
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
      Waterfall.update(waterfall.id, optimizedOrder = false, testMode = false, paused = false)
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
      VirtualCurrency.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, virtualCurrency1.exchangeRate, 1, None, roundUp))
      Waterfall.update(waterfall.id, optimizedOrder = true, testMode = false, paused = false)
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

    "respond with an empty adProviderConfigurations array when there are no active ad providers that meet the minimum reward threshold" in new WithFakeBrowser {
      val roundUp = false
      VirtualCurrency.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, virtualCurrency1.exchangeRate, 100, None, roundUp))
      Waterfall.update(waterfall.id, optimizedOrder = true, testMode = false, paused = false)
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1ID, waterfall.id, adProviderID1.get, None, Some(5.0), Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), true))
      DB.withTransaction { implicit connection => AppConfig.create(app1.id, app1.token, generationNumber(app1.id)) }
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.appConfigV1(app1.token).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      val jsonResponse: JsValue = Json.parse(contentAsString(result))
      (jsonResponse \ "adProviderConfigurations").as[JsArray].as[List[JsObject]].size must beEqualTo(0)
    }
  }

  "APIController.vungleCompletionV1" should {
    val transactionID = Some("0123456789")
    val wap = running(FakeApplication(additionalConfiguration = testDB)) {
      val id = WaterfallAdProvider.create(completionWaterfall.id, vungleID, None, None, true, true).get
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
      val id = WaterfallAdProvider.create(completionWaterfall.id, adColonyID, None, None, true, true).get
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
      tableCount("completions") must beEqualTo(completionCount + 1)
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
    val sig = Some("b6125341cbd0393e5b5ab67169964c63aba583982cb44f0bc75a48f2587ab870")
    val time = Some("1419972045")
    val subID = Some("1111")
    val partnerCode = Some("")
    val quantity = Some(1)

    val wap = running(FakeApplication(additionalConfiguration = testDB)) {
      val id = WaterfallAdProvider.create(completionWaterfall.id, hyprMarketplaceID, None, None, true, true).get
      WaterfallAdProvider.find(id).get
    }

    "respond with a 200 when all necessary params are present and transaction_id is not blank" in new WithFakeBrowser {
      val partnerCodeSig = Some("8d611bff0d42df4e9e194181fe22c1798013f126ce961eef6aaeb326468d8a64")
      val nonBlankPartnerCode = Some("partner_code")
      val completionCount = tableCount("completions")
      WaterfallAdProvider.update(new WaterfallAdProvider(wap.id, wap.waterfallID, wap.adProviderID, None, None, Some(true), None, JsObject(Seq()), false))
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.hyprMarketplaceCompletionV1(completionApp.token, time, partnerCodeSig, quantity, None, None, uid, subID, nonBlankPartnerCode).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      tableCount("completions") must beEqualTo(completionCount + 1)
    }

    "respond with a 200 if all necessary params are present and the signature is valid" in new WithFakeBrowser {
      val completionCount = tableCount("completions")
      WaterfallAdProvider.update(new WaterfallAdProvider(wap.id, wap.waterfallID, wap.adProviderID, None, None, Some(true), None, JsObject(Seq()), false))
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.hyprMarketplaceCompletionV1(completionApp.token, time, sig, quantity, None, None, uid, subID, partnerCode).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      tableCount("completions") must beEqualTo(completionCount + 1)
    }

    "respond with a 400 if the request signature is not valid" in new WithFakeBrowser {
      val badSignature = Some("123")
      val completionCount = tableCount("completions")
      WaterfallAdProvider.update(new WaterfallAdProvider(wap.id, wap.waterfallID, wap.adProviderID, None, None, Some(true), None, JsObject(Seq()), false))
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.hyprMarketplaceCompletionV1(completionApp.token, time, badSignature, quantity, None, None, uid, subID, partnerCode).url,
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
        controllers.routes.APIController.hyprMarketplaceCompletionV1(completionApp.token, time, sig, quantity, None, None, uid, None, partnerCode).url,
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
    val route = controllers.routes.APIController.hyprMarketplaceCompletionV1(completionApp.token, Some(time), Some(sig), None, None, None, None, None, None)
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

    val callback = mock[CallbackVerificationHelper]
    callback.returnFailure returns APIController.BadRequest
    callback.returnSuccess returns APIController.Ok
    callback.verificationInfo returns verificationInfo

    val completion = mock[Completion]
    val adProviderRequest = JsObject(Seq())

    "return the ad provider's default successful response if the server to server callback receives a 200 response" in new WithFakeBrowser {
      completion.createWithNotification(verificationInfo, adProviderRequest) returns Future { true }
      APIController.callbackResponse(callback, adProviderRequest, completion).header.status must beEqualTo(callback.returnSuccess.header.status)
    }

    "return the ad provider's default failure response if the server to server callback does not respond with a 200" in new WithFakeBrowser {
      completion.createWithNotification(verificationInfo, adProviderRequest) returns Future { false }
      APIController.callbackResponse(callback, adProviderRequest, completion).header.status must beEqualTo(callback.returnFailure.header.status)
    }

    "return the ad provider's default failure response if the server to server callback times out" in new WithFakeBrowser {
      completion.createWithNotification(verificationInfo, adProviderRequest) returns Future { Thread.sleep(APIController.DefaultTimeout + 1000); true }
      APIController.callbackResponse(callback, adProviderRequest, completion).header.status must beEqualTo(callback.returnFailure.header.status)
    }

    "return the ad provider's default failure response if the incoming request was not valid" in new WithFakeBrowser {
      verificationInfo.isValid returns false
      completion.createWithNotification(verificationInfo, adProviderRequest) returns Future { true }
      APIController.callbackResponse(callback, adProviderRequest, completion).header.status must beEqualTo(callback.returnFailure.header.status)
    }
  }
}
