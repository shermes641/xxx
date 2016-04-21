package models

import com.github.nscala_time.time.Imports._
import org.junit.runner._
import org.specs2.mock.Mockito
import org.specs2.runner._
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.test.FakeApplication
import play.api.test.Helpers._
import resources.{SpecificationWithFixtures, WaterfallSpecSetup}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class AdColonyReportingAPISpec extends SpecificationWithFixtures with WaterfallSpecSetup with Mockito {
  def generateConfiguration(zoneIDs: List[String]): JsObject = {
    val zones = JsArray(Seq(
      zoneIDs.map(JsString): _*
    ))
    Json.obj(
      "requiredParams"  -> Json.obj(
        "appID"   -> JsString("Some App ID"),
        "zoneIds" -> zones
      ),
      "reportingParams" -> Json.obj(
        "APIKey"  -> JsString("some API Key")
      )
    )
  }

  def generateResponse(zoneStats: List[JsObject]): JsObject = {
    Json.obj(
      "results" -> JsArray(Seq(
        zoneStats: _*
      ))
    )
  }

  def generateZoneData(id: String, eCPM: Double, impressions: Long) = {
    Json.obj(
      zoneIDKey      -> JsString(id),
      eCPMKey        -> JsNumber(eCPM),
      impressionsKey -> JsNumber(impressions)
    )
  }

  val waterfallAdProvider1 = running(FakeApplication(additionalConfiguration = testDB)) {
    val waterfallAdProviderID1 = WaterfallAdProvider.create(
      waterfallID = waterfall.id,
      adProviderID = adProviderID1.get,
      waterfallOrder = None,
      cpm = Some(10.0),
      configurable = true,
      active = true
    ).get
    Waterfall.update(waterfall.id, optimizedOrder = true, testMode = false, paused = false)
    WaterfallAdProvider.find(waterfallAdProviderID1).get
  }

  val dateFormat = DateTimeFormat.forPattern("MMddyyyy")
  val currentTime = new DateTime(DateTimeZone.UTC)
  val date = currentTime.toString(dateFormat)
  val response = mock[WSResponse]

  val zoneIDKey = "zone_id"
  val eCPMKey = "ecpm"
  val impressionsKey = "impressions"

  val zone1ID = "zone1id"
  val zone1eCPM = 5.25
  val zone1Impressions = 44
  val zone1Stats = generateZoneData(zone1ID, zone1eCPM, zone1Impressions)

  val zone2ID = "zone2id"
  val zone2eCPM = 9.88
  val zone2Impressions = 604
  val zone2Stats = generateZoneData(zone2ID, zone2eCPM, zone2Impressions)

  val zone3ID = "zone3id"
  val zone3eCPM = 11.93
  val zone3Impressions = 1312
  val zone3Stats = generateZoneData(zone3ID, zone3eCPM, zone3Impressions)

  "getZoneData" should {
    "return a list including a single JsValue thats belong to a zone ID" in new WithDB {
      val adColony = spy(new AdColonyReportingAPI(waterfallAdProvider1.id, generateConfiguration(List(zone1ID))))
      val zoneList = List(zone1Stats, zone2Stats, zone3Stats)
      val relevantZones = adColony.getZoneData(zone1ID, zoneList)

      relevantZones.length must beEqualTo(1)
      (relevantZones(0) \ "zone_id").as[String] must beEqualTo(zone1ID)
    }

    "return a list of JsValues that belong to a zone ID" in new WithDB {
      val adColony = spy(new AdColonyReportingAPI(waterfallAdProvider1.id, generateConfiguration(List(zone1ID))))
      val zoneList = List(zone1Stats, zone2Stats, zone3Stats, zone1Stats)
      val relevantZones = adColony.getZoneData(zone1ID, zoneList)

      relevantZones.length must beEqualTo(2)
      relevantZones.map(zone => (zone \ "zone_id").as[String] must beEqualTo(zone1ID))
    }

    "not be case sensitive when matching zone IDs" in new WithDB {
      val adColony = spy(new AdColonyReportingAPI(waterfallAdProvider1.id, generateConfiguration(List(zone1ID))))
      val zoneList = List(zone1Stats, zone2Stats, zone3Stats)
      List(zone1ID.toUpperCase, zone1ID.toLowerCase, zone1ID.capitalize).map { zoneID =>
        adColony.getZoneData(zoneID, zoneList).length must beEqualTo(1)
      }
    }

    "trim whitespace from incoming zone ID data when comparing zone IDs" in new WithDB {
      val adColony = spy(new AdColonyReportingAPI(waterfallAdProvider1.id, generateConfiguration(List(zone1ID))))
      val untrimmedZoneStats = generateZoneData(" " + zone1ID + " ", zone1eCPM, zone1Impressions)
      val zoneList = List(untrimmedZoneStats, zone2Stats, zone3Stats)

      (untrimmedZoneStats \ "zone_id").as[String] must not equalTo zone1ID
      adColony.getZoneData(zone1ID, zoneList).length must beEqualTo(1)
    }

    "not return any JsValues that are missing the zone_id param in AdColony's API response" in new WithDB {
      val adColony = spy(new AdColonyReportingAPI(waterfallAdProvider1.id, generateConfiguration(List(zone1ID))))
      val missingZoneJson = Json.obj()
      val zoneList = List(zone1Stats, missingZoneJson, zone3Stats)
      adColony.getZoneData(zone1ID, zoneList).length must beEqualTo(1)
    }

    "return an empty list if the zone ID is not found" in new WithDB {
      val adColony = spy(new AdColonyReportingAPI(waterfallAdProvider1.id, generateConfiguration(List(zone1ID))))
      val zoneList = List(zone2Stats, zone3Stats)
      val relevantZones = adColony.getZoneData(zone1ID, zoneList)

      relevantZones must beEqualTo(List())
    }
  }

  "calculateTotalImpressions" should {
    "return 0.0 if the list of zones is empty" in new WithDB {
      val adColony = spy(new AdColonyReportingAPI(waterfallAdProvider1.id, generateConfiguration(List(zone1ID))))
      adColony.calculateTotalImpressions(List()) must beEqualTo(0.0)
    }

    "return the sum of all impressions for every zone passed as an argument" in new WithDB {
      val adColony = spy(new AdColonyReportingAPI(waterfallAdProvider1.id, generateConfiguration(List(zone1ID))))
      val expectedImpressionTotal: Double = zone1Impressions + zone2Impressions
      adColony.calculateTotalImpressions(List(zone1Stats, zone2Stats)) must beEqualTo(expectedImpressionTotal)
    }
  }

  "calculateNewEcpm" should {
    "return 0.0 if the list of zones is empty" in new WithDB {
      val adColony = spy(new AdColonyReportingAPI(waterfallAdProvider1.id, generateConfiguration(List(zone1ID))))
      adColony.calculateNewEcpm(zones = List(), totalImpressions = 0.0, response = response) must beEqualTo(0.0)
    }

    "return the correct eCPM for a single zone" in new WithDB {
      val adColony = spy(new AdColonyReportingAPI(waterfallAdProvider1.id, generateConfiguration(List(zone3ID))))
      adColony.calculateNewEcpm(zones = List(zone3Stats), totalImpressions = zone3Impressions, response = response) must beEqualTo(zone3eCPM)
    }

    "return the correct eCPM for multiple zones" in new WithDB {
      val adColony = spy(new AdColonyReportingAPI(waterfallAdProvider1.id, generateConfiguration(List(zone1ID, zone2ID))))
      val totalImpressions = zone1Impressions + zone2Impressions
      val zones = List(zone1Stats, zone2Stats)
      // Calculated using the following formula:
      // zone1eCPM * (zone1Impressions/zone1Impressions + zone2Impressions) + zone2eCPM * (zone2Impressions/zone1Impressions + zone2Impressions)
      val combinedEcpm = 9.56
      val newEcpm = adColony.calculateNewEcpm(zones = zones, totalImpressions = totalImpressions, response = response)

      BigDecimal(newEcpm).setScale(2, BigDecimal.RoundingMode.FLOOR).toDouble must beEqualTo(combinedEcpm)
    }
  }

  "parseResponse" should {
    "not update the eCPM if no zone ID matches the zones from the HyprMediate dashboard" in new WithDB {
      val originalGeneration = generationNumber(waterfall.app_id)
      val body = generateResponse(List(zone1Stats, zone2Stats)).toString()
      response.body returns body
      response.status returns 200

      val originalEcpm = WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get
      callAPI(generateConfiguration(List(zone3ID)))

      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(originalEcpm)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration)
    }

    "update the eCPM for the appropriate zone ID when one zone is entered in our dashboard and multiple zones are returned from AdColony" in new WithDB {
      val originalGeneration = generationNumber(waterfall.app_id)
      val body = generateResponse(List(zone3Stats, zone2Stats)).toString()
      response.body returns body
      response.status returns 200

      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must not equalTo zone3eCPM
      callAPI(generateConfiguration(List(zone3ID)))

      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(zone3eCPM)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration + 1)
    }

    "update the eCPM with a weighted average of the appropriate zones when multiple zones are entered in our dashboard" in new WithDB {
      val originalGeneration = generationNumber(waterfall.app_id)
      val body = generateResponse(List(zone1Stats, zone2Stats, zone3Stats)).toString()
      response.body returns body
      response.status returns 200

      // Calculated using the following formula:
      // zone1eCPM * (zone1Impressions/zone1Impressions + zone3Impressions) + zone3eCPM * (zone3Impressions/zone1Impressions + zone3Impressions)
      val combinedEcpm = 11.71
      callAPI(generateConfiguration(List(zone3ID, zone1ID)))
      val newEcpm = WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get

      BigDecimal(newEcpm).setScale(2, BigDecimal.RoundingMode.FLOOR).toDouble must beEqualTo(combinedEcpm)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration + 1)
    }

    "not update the eCPM if the AdColony API call is unsuccessful" in new WithDB {
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalCPM = WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get
      val jsonResponse = Json.obj(
        "message" -> JsString("Bad Request."),
        "details" -> JsString("Some error message")
      )
      response.body returns jsonResponse.toString
      response.status returns 401
      callAPI(generateConfiguration(List(zone3ID)))

      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(originalCPM)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration)
    }

    "not update the eCPM if the AdColony API call contains no impressions" in new WithDB {
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalCPM = WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get
      val zeroImpressionStats = Json.obj(
        "zone_id"     -> JsString(zone1ID),
        "ecpm"        -> JsNumber(zone1eCPM),
        "impressions" -> JsNumber(0)
      )
      val body = generateResponse(List(zeroImpressionStats)).toString()
      response.body returns body
      response.status returns 200
      callAPI(generateConfiguration(List(zone1ID)))

      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(originalCPM)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration)
    }
  }

  /**
   * Helper function to fake AdColony API call.
   * @param configurationData The JSON configured by users on the HyprMediate dashboard.
   * @return Response from mocked out API call.
   */
  def callAPI(configurationData: JsObject) = {
    val adColony = spy(new AdColonyReportingAPI(waterfallAdProvider1.id, configurationData))
    adColony.retrieveAPIData returns Future { response }
    Await.result(adColony.updateRevenueData(), Duration(5000, "millis"))
  }
}
