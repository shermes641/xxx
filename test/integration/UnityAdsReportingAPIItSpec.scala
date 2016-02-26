package integration

import com.github.nscala_time.time.Imports._
import models._
import play.api.libs.json._
import play.api.test.FakeApplication
import play.api.test.Helpers._
import resources.WaterfallSpecSetup

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Integration test for Unity Adds Reporting API.
  * This uses the live URL and sometimes times out, or I've seen 503 errors (Service Unavailable)
  * The existing data for this app can be seen using the following curl command (this data should never change)
  * curl -v -L "http://gameads-admin.applifier.com/stats/monetization-api?apikey=690f9b49b59ac4551c3d7e8b4c66a578156bb098c289a0c5652e9ade1bf99fdb&splitBy=none&start=2016-02-16&end=2016-02-18&scale=day&sourceIds=1038305"
  *
  * Note that the eCPM in the date range tested is always 0.0 & there is reporting data on 2016-02-16 & 2016-02-17
  */
//TODO implement some retry logic in the production code or the test code to account for server unavailability
//TODO move this to the integration test directory when we enable integration tests in sbt
class UnityAdsReportingAPIItSpec extends SpecificationWithFixtures with WaterfallSpecSetup {

  val ShouldNotHappen = "This should not happen"
  val liveAppId: String = "1038305"
  val liveEcpm = 0.0
  val awaitTimeoutMs = Constants.DefaultReportingTimeoutMs
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
    "unityUpdateRevenueData bad application ID, do not bump generation number or update eCPM" in new WithDB {
      val queryString = List(
        "apikey" -> (configurationData \ "reportingParams" \ "APIKey").as[String],
        "splitBy" -> "none",
        "start" -> startDate,
        "end" -> endDate,
        "scale" -> "day",
        "sourceIds" -> (liveAppId + "44"))

      unityAds.updateEcpm(unityAds.waterfallAdProviderID, liveEcpm + 5.55)
      val originalEcpm = WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get
      originalEcpm must beEqualTo(liveEcpm + 5.55)
      val originalGeneration = generationNumber(waterfall.app_id)

      Await.result(unityAds.unityUpdateRevenueData(qs = queryString).map { response =>
        response must beEqualTo(false)
      }, Duration(awaitTimeoutMs, "millis"))
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.getOrElse(ShouldNotHappen) must beEqualTo(originalEcpm)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration)
    }

    "unityUpdateRevenueData bad URL, do not bump generation number or update eCPM" in new WithDB {
      val queryString = List(
        "apikey" -> (configurationData \ "reportingParams" \ "APIKey").as[String],
        "splitBy" -> "none",
        "start" -> startDate,
        "end" -> endDate,
        "scale" -> "day",
        "sourceIds" -> liveAppId)
      val originalEcpm = WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get
      val originalGeneration = generationNumber(waterfall.app_id)

      Await.result(unityAds.unityUpdateRevenueData(url = "http://localhost/", qs = queryString).map { response =>
        response must beEqualTo(false)
      }, Duration(awaitTimeoutMs, "millis"))
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.getOrElse(ShouldNotHappen) must beEqualTo(originalEcpm)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration)
    }

    "unityUpdateRevenueData wrong URL, do not bump generation number or update eCPM" in new WithDB {
      val queryString = List(
        "apikey" -> (configurationData \ "reportingParams" \ "APIKey").as[String],
        "splitBy" -> "none",
        "start" -> startDate,
        "end" -> endDate,
        "scale" -> "day",
        "sourceIds" -> liveAppId)
      val originalEcpm = WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get
      val originalGeneration = generationNumber(waterfall.app_id)

      Await.result(unityAds.unityUpdateRevenueData(url = "http://google.com/", qs = queryString).map { response =>
        response must beEqualTo(false)
      }, Duration(awaitTimeoutMs, "millis"))
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.getOrElse(ShouldNotHappen) must beEqualTo(originalEcpm)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration)
    }

    "unityUpdateRevenueData update changed eCPM and bump generation number 1 day of stats" in new WithDB {
      val queryString = List(
        "apikey" -> (configurationData \ "reportingParams" \ "APIKey").as[String],
        "splitBy" -> "none",
        "start" -> startDate,
        "end" -> endDate,
        "scale" -> "day",
        "sourceIds" -> liveAppId)

      unityAds.updateEcpm(unityAds.waterfallAdProviderID, liveEcpm + 9.99)
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalEcpm = WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get
      // ensure we should update the eCPM and bump the generation number
      originalEcpm mustNotEqual liveEcpm

      Await.result(unityAds.unityUpdateRevenueData(qs = queryString).map { response =>
        response must beEqualTo(true)
      }, Duration(awaitTimeoutMs, "millis"))
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.getOrElse(ShouldNotHappen) must beEqualTo(liveEcpm)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration + 1)
    }

    "unityUpdateRevenueData update changed eCPM and bump generation number with 10 days of stats" in new WithDB {
      val queryString = List(
        "apikey" -> (configurationData \ "reportingParams" \ "APIKey").as[String],
        "splitBy" -> "none",
        "start" -> startDate,
        "end" -> startDt.plusDays(10).toString(dateFormat),
        "scale" -> "day",
        "sourceIds" -> liveAppId)

      unityAds.updateEcpm(unityAds.waterfallAdProviderID, 2.99)
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalEcpm = WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get
      // ensure we should update the eCPM and bump the generation number
      originalEcpm mustNotEqual liveEcpm

      Await.result(unityAds.unityUpdateRevenueData(qs = queryString).map { response =>
        response must beEqualTo(true)
      }, Duration(awaitTimeoutMs, "millis"))
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.getOrElse(ShouldNotHappen) must beEqualTo(liveEcpm)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration + 1)
    }

    "unityUpdateRevenueData not bump generation number when eCPM does not change" in new WithDB {
      val queryString = List(
        "apikey" -> (configurationData \ "reportingParams" \ "APIKey").as[String],
        "splitBy" -> "none",
        "start" -> startDate,
        "end" -> endDate,
        "scale" -> "day",
        "sourceIds" -> liveAppId)
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalEcpm = WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get

      Await.result(unityAds.unityUpdateRevenueData(qs = queryString).map { response =>
        response must beEqualTo(true)
      }, Duration(awaitTimeoutMs, "millis"))
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration)
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.getOrElse(ShouldNotHappen) must beEqualTo(originalEcpm)
    }

    "unityUpdateRevenueData force timeout" in new WithDB {
      val queryString = List(
        "apikey" -> (configurationData \ "reportingParams" \ "APIKey").as[String],
        "splitBy" -> "none",
        "start" -> startDate,
        "end" -> endDate,
        "scale" -> "day",
        "sourceIds" -> liveAppId)
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalEcpm = WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get

      Await.result(unityAds.unityUpdateRevenueData(qs = queryString, timeOut = 1).map { response =>
        response must beEqualTo(false)
      }, Duration(awaitTimeoutMs, "millis"))
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration)
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.getOrElse(ShouldNotHappen) must beEqualTo(originalEcpm)
    }

    "unityUpdateRevenueData no reporting data" in new WithDB {
      val queryString = List(
        "apikey" -> (configurationData \ "reportingParams" \ "APIKey").as[String],
        "splitBy" -> "none",
        "start" -> startDt.minusDays(10).toString(dateFormat),
        "end" -> startDt.minusDays(9).toString(dateFormat),
        "scale" -> "day",
        "sourceIds" -> liveAppId)
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalEcpm = WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get

      Await.result(unityAds.unityUpdateRevenueData(qs = queryString, timeOut = 1).map { response =>
        response must beEqualTo(false)
      }, Duration(awaitTimeoutMs, "millis"))
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration)
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.getOrElse(ShouldNotHappen) must beEqualTo(originalEcpm)
    }

    "unityUpdateRevenueData reporting end date before start date" in new WithDB {
      val queryString = List(
        "apikey" -> (configurationData \ "reportingParams" \ "APIKey").as[String],
        "splitBy" -> "none",
        "start" -> startDt.minusDays(9).toString(dateFormat),
        "end" -> startDt.minusDays(10).toString(dateFormat),
        "scale" -> "day",
        "sourceIds" -> liveAppId)
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalEcpm = WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get

      Await.result(unityAds.unityUpdateRevenueData(qs = queryString, timeOut = 1).map { response =>
        response must beEqualTo(false)
      }, Duration(awaitTimeoutMs, "millis"))
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration)
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.getOrElse(ShouldNotHappen) must beEqualTo(originalEcpm)
    }

    "unityUpdateRevenueData no query string" in new WithDB {
      val queryString = List()
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalEcpm = WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get

      Await.result(unityAds.unityUpdateRevenueData(qs = queryString).map { response =>
        response must beEqualTo(false)
      }, Duration(awaitTimeoutMs, "millis"))
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration)
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.getOrElse(ShouldNotHappen) must beEqualTo(originalEcpm)
    }
  }
}
