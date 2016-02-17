package models

import com.github.nscala_time.time.Imports._
import org.specs2.mock.Mockito

import play.api.libs.json._
import play.api.test.FakeApplication
import play.api.test.Helpers._
import resources.WaterfallSpecSetup

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Integration test for Unity Adds Reporting API.
  * This uses the live URL and sometimes timesout, or I've seen 503 errors (Service Unavailable)
  */
//TODO implement some retry logic in the production code or this test to account for server unavailability
class UnityAdsReportingAPI_INTSpec extends SpecificationWithFixtures with WaterfallSpecSetup with Mockito {
  val waterfallAdProvider1 = running(FakeApplication(additionalConfiguration = testDB)) {
    val waterfallAdProviderID1 = WaterfallAdProvider.create(waterfall.id, adProviderID1.get, None, None, configurable = true, active = true).get
    Waterfall.update(waterfall.id, optimizedOrder = true, testMode = false, paused = false)
    WaterfallAdProvider.find(waterfallAdProviderID1).get
  }

  val dateFormat = DateTimeFormat.forPattern("YYYY-MM-d")
  val startDt = new DateTime("2016-02-16", DateTimeZone.UTC)
  val endDate = startDt.plusDays(1).toString(dateFormat)
  val startDate = startDt.toString(dateFormat)
  val configurationData = JsObject(Seq("requiredParams" -> JsObject(Seq("appID" -> JsString("1038305"))),
    "reportingParams" -> JsObject(Seq("APIKey" -> JsString("690f9b49b59ac4551c3d7e8b4c66a578156bb098c289a0c5652e9ade1bf99fdb")))))

  val unityAds = running(FakeApplication(additionalConfiguration = testDB)) {
    new UnityAdsReportingAPI(waterfallAdProvider1.id, configurationData)
  }

  "UnityAdsReportingAPI" should {
    "unityUpdateRevenueData return false on failure and not update eCPM" in new WithDB {
      val queryString = List(
        "apikey" -> (configurationData \ "reportingParams" \ "APIKey").as[String],
        "splitBy" -> "none",
        "start" -> startDate,
        "end" -> endDate,
        "scale" -> "day",
        "sourceIds" -> "103830555")

      running(FakeApplication(additionalConfiguration = testDB)) {
        waterfallAdProvider1.cpm must beNone
        unityAds.updateEcpm(unityAds.waterfallAdProviderID, 5.55)
        WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(5.55)
        val originalGeneration = generationNumber(waterfall.app_id)
        originalGeneration mustEqual 1

        Await.result(unityAds.unityUpdateRevenueData(qs = queryString).map { response =>
          response must beEqualTo(false)
        }, Duration(10000, "millis"))
        WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(5.55)
        generationNumber(waterfall.app_id) must beEqualTo(originalGeneration)
      }
    }

    "unityUpdateRevenueData return false on failure and not update eCPM" in new WithDB {
      val queryString = List(
        "apikey" -> (configurationData \ "reportingParams" \ "APIKey").as[String],
        "splitBy" -> "none",
        "start" -> startDate,
        "end" -> endDate,
        "scale" -> "day",
        "sourceIds" -> "1038305")

      running(FakeApplication(additionalConfiguration = testDB)) {
        waterfallAdProvider1.cpm must beNone
        WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(5.55)
        val originalGeneration = generationNumber(waterfall.app_id)
        originalGeneration mustEqual 1

        Await.result(unityAds.unityUpdateRevenueData(url = "http://localhost/", qs = queryString).map { response =>
          response must beEqualTo(false)
        }, Duration(10000, "millis"))
        WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(5.55)
        generationNumber(waterfall.app_id) must beEqualTo(originalGeneration)
      }
    }

    "unityUpdateRevenueData return false on failure and not update eCPM" in new WithDB {
      val queryString = List(
        "apikey" -> (configurationData \ "reportingParams" \ "APIKey").as[String],
        "splitBy" -> "none",
        "start" -> startDate,
        "end" -> endDate,
        "scale" -> "day",
        "sourceIds" -> "1038305")

      running(FakeApplication(additionalConfiguration = testDB)) {
        waterfallAdProvider1.cpm must beNone
        WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(5.55)
        val originalGeneration = generationNumber(waterfall.app_id)
        originalGeneration mustEqual 1

        Await.result(unityAds.unityUpdateRevenueData(url = "http://google.com/", qs = queryString).map { response =>
          response must beEqualTo(false)
        }, Duration(10000, "millis"))
        WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(5.55)
        generationNumber(waterfall.app_id) must beEqualTo(originalGeneration)
      }
    }

    //tom A unityAds.unityUpdateRevenueData call bumps the generation number
    "unityUpdateRevenueData return true on success, 1 day of stats" in new WithDB {
      val queryString = List(
        "apikey" -> (configurationData \ "reportingParams" \ "APIKey").as[String],
        "splitBy" -> "none",
        "start" -> startDate,
        "end" -> endDate,
        "scale" -> "day",
        "sourceIds" -> "1038305")

      running(FakeApplication(additionalConfiguration = testDB)) {
        val originalGeneration = generationNumber(waterfall.app_id)
        originalGeneration mustEqual 1
        waterfallAdProvider1.cpm must beNone

        Await.result(unityAds.unityUpdateRevenueData(qs = queryString).map { response =>
          response must beEqualTo(true)
        }, Duration(10000, "millis"))
        WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(0.0)
        generationNumber(waterfall.app_id) must beEqualTo(originalGeneration + 1)
      }
    }

    //tom B the unityAds.updateEcpm call bumps the generation number and the unityAds.unityUpdateRevenueData call bumps the generation number
    "unityUpdateRevenueData return true on success, 10 days of stats" in new WithDB {
      val queryString = List(
        "apikey" -> (configurationData \ "reportingParams" \ "APIKey").as[String],
        "splitBy" -> "none",
        "start" -> startDate,
        "end" -> startDt.plusDays(10).toString(dateFormat),
        "scale" -> "day",
        "sourceIds" -> "1038305")

      running(FakeApplication(additionalConfiguration = testDB)) {
        val originalGeneration = generationNumber(waterfall.app_id)
        waterfallAdProvider1.cpm must beNone
        originalGeneration must beEqualTo(2)
        //TODO //tom this bumps the generation number --- remove it and the Await.result call below does not bump
        unityAds.updateEcpm(unityAds.waterfallAdProviderID, 5.55)
        WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(5.55)
        generationNumber(waterfall.app_id) must beEqualTo(3)

        Await.result(unityAds.unityUpdateRevenueData(qs = queryString).map { response =>
          response must beEqualTo(true)
        }, Duration(10000, "millis"))
        WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(0.0)
        generationNumber(waterfall.app_id) must beEqualTo(originalGeneration + 2)
      }
    }

    //tom C unityAds.unityUpdateRevenueData call DOES NOT bump the generation number
    //tom this test is the same as test A above
    "unityUpdateRevenueData return true on success, 1 day of stats" in new WithDB {
      val queryString = List(
        "apikey" -> (configurationData \ "reportingParams" \ "APIKey").as[String],
        "splitBy" -> "none",
        "start" -> startDate,
        "end" -> endDate,
        "scale" -> "day",
        "sourceIds" -> "1038305")

      running(FakeApplication(additionalConfiguration = testDB)) {
        val originalGeneration = generationNumber(waterfall.app_id)
        originalGeneration mustEqual 4
        waterfallAdProvider1.cpm must beNone

        Await.result(unityAds.unityUpdateRevenueData(qs = queryString).map { response =>
          response must beEqualTo(true)
        }, Duration(10000, "millis"))
        WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(0.0)
        generationNumber(waterfall.app_id) must beEqualTo(originalGeneration + 1)
      }
    }
  }
}
