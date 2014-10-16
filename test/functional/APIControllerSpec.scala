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
    WaterfallAdProvider.create(waterfall.get.id, adProviderID1.get, None).get
  }

  val wap2ID = running(FakeApplication(additionalConfiguration = testDB)) {
    WaterfallAdProvider.create(waterfall.get.id, adProviderID2.get, None).get
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
      val requiredParams: JsValue = JsObject(Seq("distributorID" -> JsString(APIController.TEST_MODE_DISTRIBUTOR_ID), "appID" -> JsString(APIController.TEST_MODE_APP_ID), "providerName" -> JsString(APIController.TEST_MODE_PROVIDER_NAME), "eCPM" -> JsNumber(5.0)))
      val testConfigData: JsValue = JsArray(JsObject(Seq("requiredParams" -> requiredParams)) :: Nil)
      val jsonResponse: JsValue = Json.parse(contentAsString(result)) \ "adProviderConfigurations"
      val vcAttributes = APIController.TEST_MODE_VIRTUAL_CURRENCY
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
  step(clean)
}
