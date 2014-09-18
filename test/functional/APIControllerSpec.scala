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
      val testConfigData: JsValue = JsArray(JsObject(Seq("distributorID" -> JsString(APIController.TEST_MODE_DISTRIBUTOR_ID), "providerName" -> JsString(APIController.TEST_MODE_PROVIDER_NAME))) :: Nil)
      val jsonResponse: JsValue = Json.parse(contentAsString(result)) \ "adProviderConfigurations"
      jsonResponse must beEqualTo(testConfigData)
    }

    "respond with ad providers ordered by eCPM when the waterfall is in optimized mode" in new WithFakeBrowser {
      Waterfall.update(waterfall.get.id, true, false)
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1ID, waterfall.get.id, adProviderID1.get, None, Some(5.0), Some(true), None, JsObject(Seq())))
      WaterfallAdProvider.update(new WaterfallAdProvider(wap2ID, waterfall.get.id, adProviderID2.get, None, Some(1.0), Some(true), None, JsObject(Seq())))
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
    }

    "respond with the current waterfall order when waterfall is live and not optimized" in new WithFakeBrowser {
      Waterfall.update(waterfall.get.id, false, false)
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1ID, waterfall.get.id, adProviderID1.get, None, Some(1.0), Some(true), None, JsObject(Seq())))
      WaterfallAdProvider.update(new WaterfallAdProvider(wap2ID, waterfall.get.id, adProviderID2.get, None, Some(5.0), Some(true), None, JsObject(Seq())))
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
    }
  }
  step(clean)
}
