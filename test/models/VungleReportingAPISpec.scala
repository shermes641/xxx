package models

import com.github.nscala_time.time.Imports._
import org.junit.runner._
import org.specs2.mock.Mockito
import org.specs2.runner._
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.test.FakeApplication
import play.api.test.Helpers._
import resources.WaterfallSpecSetup
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

@RunWith(classOf[JUnitRunner])
class VungleReportingAPISpec extends SpecificationWithFixtures with WaterfallSpecSetup with Mockito {
  val waterfallAdProvider1 = running(FakeApplication(additionalConfiguration = testDB)) {
    val waterfallAdProviderID1 = WaterfallAdProvider.create(waterfall.id, adProviderID1.get, waterfallOrder = None, cpm = None, configurable = true, active = true).get
    Waterfall.update(waterfall.id, optimizedOrder = true, testMode = false, paused = false)
    WaterfallAdProvider.find(waterfallAdProviderID1).get
  }

  val apiKey = "VungleAPIKey"
  val apiID = "VungleAPIID"
  val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
  val currentTime = new DateTime(DateTimeZone.UTC)
  val date = currentTime.toString(dateFormat)
  val configurationData = JsObject(Seq("requiredParams" -> JsObject(Seq()), "reportingParams" -> JsObject(Seq("APIKey" -> JsString(apiKey), "APIID" -> JsString(apiID)))))
  val queryString = List("key" -> apiKey, "date" -> date)
  val response = mock[WSResponse]
  val vungle = running(FakeApplication(additionalConfiguration = testDB)) { spy(new VungleReportingAPI(waterfallAdProvider1.id, configurationData)) }

  "parseResponse" should {
    "update the eCPM if Vungle responds with a status code of 200 or 304" in new WithDB {
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalEcpm = WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm
      val newEcpm = 5.0
      originalEcpm must not(beEqualTo(newEcpm))
      val jsonResponse = JsArray(Seq(JsObject(Seq("eCPM" -> JsNumber(newEcpm)))))
      response.body returns jsonResponse.toString
      response.status returns 200
      callAPI
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(newEcpm)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration + 1)
    }

    "not update the eCPM if Vungle responds with a status code other than 200 or 304" in new WithDB {
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalEcpm = WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get
      val newEcpm = 10.0
      val jsonResponse = JsArray(Seq(JsObject(Seq("eCPM" -> JsNumber(newEcpm)))))
      response.body returns jsonResponse.toString
      response.status returns 400
      callAPI
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(originalEcpm)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration)
    }

    "not update the eCPM if the JSON response is malformed" in new WithDB {
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalEcpm = WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get
      val newEcpm = 10.0
      val jsonResponse = JsArray(Seq(JsObject(Seq("stats" -> JsObject(Seq("eCPM" -> JsNumber(newEcpm)))))))
      response.body returns jsonResponse.toString
      response.status returns 200
      callAPI
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(originalEcpm)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration)
    }

    "not update the eCPM if there are no events available" in new WithDB {
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalEcpm = WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get
      val jsonResponse = JsArray(Seq())
      response.body returns jsonResponse.toString
      response.status returns 200
      callAPI
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(originalEcpm)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration)
    }
  }

  /**
   * Helper function to fake Vungle API call.
   * @return Response from mocked out API call.
   */
  def callAPI = {
    vungle.retrieveAPIData(queryString) returns Future { response }
    Await.result(vungle.updateRevenueData, Duration(5000, "millis"))
  }
}
