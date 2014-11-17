package functional

import models._
import org.specs2.runner._
import org.junit.runner._
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test._
import controllers.APIController

@RunWith(classOf[JUnitRunner])
class APIControllerSpec extends SpecificationWithFixtures with WaterfallSpecSetup {
  val wap1ID = running(FakeApplication(additionalConfiguration = testDB)) {
    WaterfallAdProvider.create(waterfall.get.id, adProviderID1.get, None, None, true, true).get
  }

  val wap2ID = running(FakeApplication(additionalConfiguration = testDB)) {
    WaterfallAdProvider.create(waterfall.get.id, adProviderID2.get, None, None, true, true).get
  }

  val vungleCallbackUrl = Some("http://mediation-staging.herokuapp.com/v1/waterfall/%s/vungle_completion?uid=%%user%%&openudid=%%udid%%&mac=%%mac%%&ifa=%%ifa%%&transaction_id=%%txid%%&digest=%%digest%%")
  val vungleAdProviderID = running(FakeApplication(additionalConfiguration = testDB)) {
    AdProvider.create("Vungle", "{\"requiredParams\":[{\"description\": \"Your Vungle App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}], \"reportingParams\": [{\"description\": \"Your API Key for Fyber\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}], \"callbackParams\": [{\"description\": \"Your Event API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}]}", vungleCallbackUrl).get
  }

  val completionWaterfall = running(FakeApplication(additionalConfiguration = testDB)) {
    val waterfallID = Waterfall.create(app1.id, app1.name).get
    Waterfall.find(waterfallID).get
  }

  "APIController.waterfall" should {
    "respond with 400 if token is not valid" in new WithFakeBrowser {
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.waterfallV1("some-fake-token").url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(400)
      contentAsString(result) must contain("Waterfall not found.")
    }

    "respond with the HyprMX test distributor configuration when waterfall is in test mode" in new WithFakeBrowser {
      Waterfall.update(waterfall.get.id, false, true)
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.waterfallV1(waterfall.get.token).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      val requiredParams: JsValue = JsObject(Seq("distributorID" -> JsString(Waterfall.TEST_MODE_DISTRIBUTOR_ID), "appID" -> JsString(Waterfall.TEST_MODE_APP_ID),
        "providerName" -> JsString(Waterfall.TEST_MODE_PROVIDER_NAME),"providerID" -> JsNumber(Waterfall.TEST_MODE_PROVIDER_ID), "eCPM" -> JsNumber(5.0)))
      val testConfigData: JsValue = JsArray(JsObject(Seq("requiredParams" -> requiredParams)) :: Nil)
      val jsonResponse: JsValue = Json.parse(contentAsString(result)) \ "adProviderConfigurations"
      val vcAttributes = Waterfall.TEST_MODE_VIRTUAL_CURRENCY
      val expectedVCJson = JsObject(Seq("name" -> JsString(vcAttributes.name), "exchangeRate" -> JsNumber(vcAttributes.exchangeRate),
        "rewardMin" -> JsNumber(vcAttributes.rewardMin.get), "rewardMax" -> JsNumber(vcAttributes.rewardMax.get), "roundUp" -> JsBoolean(vcAttributes.roundUp)))
      val vcJson = Json.parse(contentAsString(result)) \ "virtualCurrency"
      jsonResponse must beEqualTo(JsArray(requiredParams :: Nil))
      vcJson must beEqualTo(expectedVCJson)
    }

    "respond with ad providers ordered by eCPM when the waterfall is in optimized mode" in new WithFakeBrowser {
      Waterfall.update(waterfall.get.id, true, false)
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1ID, waterfall.get.id, adProviderID1.get, None, Some(5.0), Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), true))
      WaterfallAdProvider.update(new WaterfallAdProvider(wap2ID, waterfall.get.id, adProviderID2.get, None, Some(1.0), Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), true))
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.waterfallV1(waterfall.get.token).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      val jsonResponse: List[JsValue] = (Json.parse(contentAsString(result)) \ "adProviderConfigurations").as[JsArray].as[List[JsValue]]
      jsonResponse.zipWithIndex.map { case(provider, index) => (provider \ "providerName").as[String] must beEqualTo(adProviders(index))}
      Json.parse(contentAsString(result)) \ "virtualCurrency" must haveClass[JsObject]
    }

    "respond with the current waterfall order when waterfall is live and not optimized" in new WithFakeBrowser {
      Waterfall.update(waterfall.get.id, false, false)
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1ID, waterfall.get.id, adProviderID1.get, None, Some(1.0), Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), true))
      WaterfallAdProvider.update(new WaterfallAdProvider(wap2ID, waterfall.get.id, adProviderID2.get, None, Some(5.0), Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), true))
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.waterfallV1(waterfall.get.token).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      val jsonResponse: JsValue = Json.parse(contentAsString(result))
      val adProviderConfigs: List[JsValue] = (jsonResponse \ "adProviderConfigurations").as[JsArray].as[List[JsValue]]
      adProviderConfigs.zipWithIndex.map { case(provider, index) => (provider \ "providerName").as[String] must beEqualTo(adProviders(index))}
      jsonResponse \ "virtualCurrency" must haveClass[JsObject]
    }

    "exclude ad providers from the waterfall order if the virtual currency roundUp option is false and ad provider's current cpm value is less than the calculated reward amount for the virtual currency" in new WithFakeBrowser {
      VirtualCurrency.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, virtualCurrency1.exchangeRate, Some(1.toLong), None, false))
      Waterfall.update(waterfall.get.id, true, false)
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1ID, waterfall.get.id, adProviderID1.get, None, Some(5.0), Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), true))
      WaterfallAdProvider.update(new WaterfallAdProvider(wap2ID, waterfall.get.id, adProviderID2.get, None, Some(50.0), Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), true))
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.waterfallV1(waterfall.get.token).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(200)
      val jsonResponse: JsValue = Json.parse(contentAsString(result))
      val adProviderConfigs: List[JsValue] = (jsonResponse \ "adProviderConfigurations").as[JsArray].as[List[JsValue]]
      adProviderConfigs.map( provider => (provider \ "providerName").as[String]) must contain(adProviders(0).name)
      adProviderConfigs.map( provider => (provider \ "providerName").as[String]) must not contain(adProviders(1).name)
      jsonResponse \ "virtualCurrency" must haveClass[JsObject]
    }

    "respond with an error when there are no active ad providers that meet the minimum reward threshold" in new WithFakeBrowser {
      VirtualCurrency.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, virtualCurrency1.exchangeRate, Some(100.toLong), None, false))
      Waterfall.update(waterfall.get.id, true, false)
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1ID, waterfall.get.id, adProviderID1.get, None, Some(5.0), Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), true))
      val request = FakeRequest(
        GET,
        controllers.routes.APIController.waterfallV1(waterfall.get.token).url,
        FakeHeaders(),
        ""
      )
      val Some(result) = route(request)
      status(result) must equalTo(400)
      val jsonResponse: JsValue = (Json.parse(contentAsString(result)))
      (jsonResponse \ "status").as[String] must beEqualTo("error")
      (jsonResponse \ "message").as[String] must beEqualTo("At this time there are no ad providers that are both active and have an eCPM that meets the minimum reward threshold.")
    }
  }

  "APIController.vungleCompletionV1" should {
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
        controllers.routes.APIController.vungleCompletionV1(completionWaterfall.token, transactionID, digest, Some(1), None).url,
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
  step(clean)
}
