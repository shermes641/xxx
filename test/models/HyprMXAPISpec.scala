package models

import org.specs2.mock.Mockito
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import play.api.test.FakeApplication
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import org.junit.runner._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class HyprMXAPISpec extends SpecificationWithFixtures with WaterfallSpecSetup with Mockito {
  val waterfallAdProvider1 = running(FakeApplication(additionalConfiguration = testDB)) {
    val waterfallAdProviderID1 = WaterfallAdProvider.create(waterfall.get.id, adProviderID1.get, None, None, true, true).get
    WaterfallAdProvider.find(waterfallAdProviderID1).get
  }

  val configurationData = JsObject(Seq("requiredParams" -> JsObject(Seq()), "reportingParams" -> JsObject(Seq("APIKey" -> JsString("some API Key"), "placementID" -> JsString("some Placement ID"), "appID" -> JsString("some App ID")))))
  val response = mock[WSResponse]
  val hyprMX = spy(new HyprMXAPI(waterfallAdProvider1.id, configurationData))

  "updateRevenueData" should {
    "updates the cpm field of the WaterfallAdProvider if the HyprMX API call is successful" in new WithDB {
      waterfallAdProvider1.cpm must beNone
      val globalStats = JsObject(Seq("revenue" -> JsString("10.00"), "impressions" -> JsString("1000"), "completions" -> JsString("200")))
      val statsJson = JsObject(Seq("results" -> JsArray(Seq(JsObject(Seq("global_stats" -> globalStats))))))
      response.body returns statsJson.toString
      response.status returns 200
      callAPI
      val revenueData = new RevenueData(globalStats)
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(10.00)
    }

    "does not update the WaterfallAdProvider if the HyprMX API call is unsuccessful" in new WithDB {
      val originalCPM = WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get
      val jsonResponse = JsObject(Seq("message" -> JsString("Bad Request."), "details" -> JsString("Some error message")))
      response.body returns jsonResponse.toString
      response.status returns 401
      callAPI
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(originalCPM)
    }
  }

  def callAPI = {
    hyprMX.retrieveHyprMXData(configurationData) returns Future { response }
    Await.result(hyprMX.updateRevenueData, Duration(5000, "millis"))
  }

  step(clean)
}
