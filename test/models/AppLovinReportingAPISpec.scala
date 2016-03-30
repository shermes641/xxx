package models

import com.github.nscala_time.time.Imports._
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.test.FakeApplication
import play.api.test.Helpers._
import org.junit.runner._
import org.specs2.mock.Mockito
import org.specs2.runner._
import resources.{SpecificationWithFixtures, WaterfallSpecSetup}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class AppLovinReportingAPISpec extends SpecificationWithFixtures with WaterfallSpecSetup with Mockito {
  val waterfallAdProvider1 = running(FakeApplication(additionalConfiguration = testDB)) {
    val id = WaterfallAdProvider.create(waterfall.id, adProviderID1.get, None, None, configurable = true, active = true).get
    Waterfall.update(waterfall.id, optimizedOrder = true, testMode = false, paused = false)
    WaterfallAdProvider.find(id).get
  }

  val configurationData = JsObject(Seq("requiredParams" -> JsObject(Seq()), "reportingParams" -> JsObject(Seq("APIKey" -> JsString("some API Key"), "appName" -> JsString("some App Name")))))
  val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
  val currentTime = new DateTime(DateTimeZone.UTC)
  val date = currentTime.toString(dateFormat)
  val queryString = List("api_key" -> (configurationData \ "reportingParams" \ "APIKey").as[String], "start" -> date, "end" -> date,
    "format" -> "json", "columns" -> "application,impressions,clicks,ctr,revenue,ecpm", "filter_application" -> (configurationData \ "reportingParams" \ "appName").as[String])
  val response = mock[WSResponse]
  val appLovin = running(FakeApplication(additionalConfiguration = testDB)) { spy(new AppLovinReportingAPI(waterfallAdProvider1.id, configurationData)) }

  "updateRevenueData" should {
    "updates the cpm field of the WaterfallAdProvider if the AppLovin API call is successful" in new WithDB {
      val originalGeneration = generationNumber(waterfall.app_id)
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm must beNone
      Waterfall.update(waterfallAdProvider1.waterfallID, optimizedOrder = true, testMode = false, paused = false)
      waterfallAdProvider1.cpm must beNone
      val stats = JsObject(Seq("ecpm" -> JsString("10.00")))
      val statsJson = JsObject(Seq("results" -> JsArray(Seq(stats))))
      response.body returns statsJson.toString
      response.status returns 200
      callAPI
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(10)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration + 1)
    }

    "does not update the WaterfallAdProvider if the AppLovin API call is unsuccessful" in new WithDB {
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
   * Helper function to fake AppLovin API call.
   * @return Response from mocked out API call.
   */
  def callAPI = {
    appLovin.retrieveAPIData returns Future { response }
    Await.result(appLovin.updateRevenueData(), Duration(5000, "millis"))
  }
}
