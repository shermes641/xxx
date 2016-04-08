package models

import org.specs2.mock.Mockito
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import play.api.test.FakeApplication
import resources.{SpecificationWithFixtures, WaterfallSpecSetup}
import org.junit.runner._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class ReportingAPISpec extends SpecificationWithFixtures with WaterfallSpecSetup with Mockito {
  val waterfallAdProvider1 = running(testApplication) {
    val waterfallAdProviderID1 = waterfallAdProviderService.create(waterfall.id, adProviderID1.get, None, None, true, true).get
    waterfallService.update(waterfall.id, optimizedOrder = true, testMode = false, paused = false)
    waterfallAdProviderService.find(waterfallAdProviderID1).get
  }

  val api = {
    // Define a test class to extend and test the abstract class, ReportingAPI
    class TestAPI extends ReportingAPI {
      override val wsClient = ws
      override val BaseURL = "some url"
      override val waterfallAdProviderID: Long = 0
      override val queryString = List()
      override val wapService = waterfallAdProviderService
      override val db = database
    }
    new TestAPI
  }

  "updateEcpm" should {
    "return the new generation number if the WaterfallAdProvider is successfully updated" in new WithDB {
      val newEcpm = 10.00
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalCPM = waterfallAdProviderService.find(waterfallAdProvider1.id).get.cpm
      val newGeneration = originalGeneration + 1
      api.updateEcpm(waterfallAdProvider1.id, newEcpm)
      waterfallAdProviderService.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(newEcpm)
      generationNumber(waterfall.app_id) must beEqualTo(newGeneration)
    }
  }

  "parseResponse" should {
    "update the eCPM if a valid JsNumber eCPM is received" in new WithDB {
      val newEcpm = 15.00
      val response = mock[WSResponse]
      val statsJson = JsObject(Seq("results" -> JsArray(Seq(JsObject(Seq("ecpm" -> JsNumber(newEcpm)))))))
      response.body returns statsJson.toString
      response.status returns 200
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalCPM = waterfallAdProviderService.find(waterfallAdProvider1.id).get.cpm
      val newGeneration = originalGeneration + 1
      api.parseResponse(waterfallAdProvider1.id, response)
      waterfallAdProviderService.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(newEcpm)
      generationNumber(waterfall.app_id) must beEqualTo(newGeneration)
    }

    "update the eCPM if a valid JsString eCPM is received" in new WithDB {
      val newEcpm = 20.00
      val response = mock[WSResponse]
      val statsJson = JsObject(Seq("results" -> JsArray(Seq(JsObject(Seq("ecpm" -> JsString(newEcpm.toString)))))))
      response.body returns statsJson.toString
      response.status returns 200
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalCPM = waterfallAdProviderService.find(waterfallAdProvider1.id).get.cpm
      val newGeneration = originalGeneration + 1
      api.parseResponse(waterfallAdProvider1.id, response)
      waterfallAdProviderService.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(newEcpm)
      generationNumber(waterfall.app_id) must beEqualTo(newGeneration)
    }

    "do not update the eCPM if a status code other than 200 or 304 is received" in new WithDB {
      val newEcpm = 25.00
      val response = mock[WSResponse]
      val statsJson = JsObject(Seq("results" -> JsArray(Seq(JsObject(Seq("ecpm" -> JsNumber(newEcpm)))))))
      response.body returns statsJson.toString
      response.status returns 400
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalCPM = waterfallAdProviderService.find(waterfallAdProvider1.id).get.cpm.get
      api.parseResponse(waterfallAdProvider1.id, response)
      waterfallAdProviderService.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(originalCPM)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration)
    }

    "do not update the eCPM if 'results' is not found in the JSON response" in new WithDB {
      val newEcpm = 30.00
      val response = mock[WSResponse]
      val statsJson = JsObject(Seq())
      response.body returns statsJson.toString
      response.status returns 200
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalCPM = waterfallAdProviderService.find(waterfallAdProvider1.id).get.cpm.get
      api.parseResponse(waterfallAdProvider1.id, response)
      waterfallAdProviderService.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(originalCPM)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration)
    }

    "do not update the eCPM if 'ecpm' is not found in the JSON response" in new WithDB {
      val newEcpm = 35.00
      val response = mock[WSResponse]
      val statsJson = JsObject(Seq("results" -> JsArray(Seq(JsObject(Seq())))))
      response.body returns statsJson.toString
      response.status returns 200
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalCPM = waterfallAdProviderService.find(waterfallAdProvider1.id).get.cpm.get
      api.parseResponse(waterfallAdProvider1.id, response)
      waterfallAdProviderService.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(originalCPM)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration)
    }
  }
}
