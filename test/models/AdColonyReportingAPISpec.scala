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
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class AdColonyReportingAPISpec extends SpecificationWithFixtures with WaterfallSpecSetup with Mockito {
  val waterfallAdProvider1 = running(FakeApplication(additionalConfiguration = testDB)) {
    val waterfallAdProviderID1 = WaterfallAdProvider.create(waterfall.id, adProviderID1.get, None, None, true, true).get
    Waterfall.update(waterfall.id, true, false, false)
    WaterfallAdProvider.find(waterfallAdProviderID1).get
  }

  val dateFormat = DateTimeFormat.forPattern("MMddyyyy")
  val currentTime = new DateTime(DateTimeZone.UTC)
  val date = currentTime.toString(dateFormat)
  val configurationData = JsObject(Seq("requiredParams" -> JsObject(Seq("appID" -> JsString("Some App ID"))), "reportingParams" -> JsObject(Seq("APIKey" -> JsString("some API Key")))))
  val queryString = List("user_credentials" -> (configurationData \ "reportingParams" \ "APIKey").as[String], "date" -> date, "app_id" -> (configurationData \ "requiredParams" \ "appID").as[String])
  val response = mock[WSResponse]
  val adColony = running(FakeApplication(additionalConfiguration = testDB)) { spy(new AdColonyReportingAPI(waterfallAdProvider1.id, configurationData)) }

  "updateRevenueData" should {
    "updates the cpm field of the WaterfallAdProvider if the AdColony API call is successful" in new WithDB {
      val originalGeneration = generationNumber(waterfall.app_id)
      waterfallAdProvider1.cpm must beNone
      val newEcpm: Double = 5.0
      val statsJson = JsObject(Seq("results" -> JsArray(Seq(JsObject(Seq("ecpm" -> JsNumber(newEcpm)))))))
      response.body returns statsJson.toString
      response.status returns 200
      callAPI
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(newEcpm)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration + 1)
    }

    "does not update the WaterfallAdProvider if the AdColony API call is unsuccessful" in new WithDB {
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalCPM = WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get
      val jsonResponse = JsObject(Seq("message" -> JsString("Bad Request."), "details" -> JsString("Some error message")))
      response.body returns jsonResponse.toString
      response.status returns 401
      callAPI
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(originalCPM)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration)
    }
  }

  /**
   * Helper function to fake AdColony API call.
   * @return Response from mocked out API call.
   */
  def callAPI = {
    adColony.retrieveAPIData(queryString) returns Future { response }
    Await.result(adColony.updateRevenueData, Duration(5000, "millis"))
  }
}
