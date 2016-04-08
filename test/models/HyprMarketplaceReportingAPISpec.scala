package models

import com.github.nscala_time.time.Imports._
import org.junit.runner._
import org.specs2.mock.Mockito
import org.specs2.runner._
import play.api.libs.Codecs
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.test.Helpers._
import resources.{SpecificationWithFixtures, WaterfallSpecSetup}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class HyprMarketplaceReportingAPISpec extends SpecificationWithFixtures with WaterfallSpecSetup with Mockito {
  val waterfallAdProvider1 = running(testApplication) {
    val waterfallAdProviderID1 = waterfallAdProviderService.create(waterfall.id, adProviderID1.get, None, None, true, true).get
    waterfallService.update(waterfall.id, optimizedOrder = true, testMode = false, paused = false)
    waterfallAdProviderService.find(waterfallAdProviderID1).get
  }

  val appID = "Some App ID"
  val placementID = "Some Placement ID"
  val apiKey = "Some API Key"
  val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
  val currentTime = new DateTime(DateTimeZone.UTC)
  val date = currentTime.toString(dateFormat)
  val hashParams = Map("app_id" -> appID, "end_date" -> date, "placement_id" -> placementID, "start_date" -> date)
  val hashableString = hashParams.flatMap((k) => List(k._1 + "=" +  k._2)).mkString("&") + "&" + apiKey
  val hashValue = Codecs.sha1(hashableString)
  val configurationData = JsObject(Seq("requiredParams" -> JsObject(Seq()), "reportingParams" -> JsObject(Seq("APIKey" -> JsString(apiKey), "placementID" -> JsString(placementID), "appID" -> JsString(appID)))))
  val queryString = List("app_id" -> appID, "placement_id" -> placementID, "start_date" -> date, "end_date" -> date, "hash_value" -> hashValue)
  val retrieveAPIDataResponse = mock[WSResponse]
  var retrieveImpressionResponse: Option[String] = Some("")
  val hyprMarketplace = running(testApplication) { spy(new HyprMarketplaceReportingAPI(waterfallAdProvider1.id, configurationData, database, waterfallAdProviderService, testPlatform, configVars, appEnvironment, keenInitialization, ws, appService)) }

  "calculateEcpm" should {
    "return an updated eCPM given revenue and impressions" in new WithDB {
      val revenue = 25.00
      val impressions = 2000
      hyprMarketplace.calculateEcpm(revenue, impressions) must beEqualTo(12.5)
    }

    "return 0.00 if the value of impressions is 0" in new WithDB {
      val revenue = 0.00
      val impressions = 0
      hyprMarketplace.calculateEcpm(revenue, impressions) must beEqualTo(0.00)
    }
  }

  "app" should {
    "return the app by its waterfall ID" in new WithDB {
      hyprMarketplace.app.get must beEqualTo(app1)
    }

    "return None if the app could not be found" in new WithAppDB(distributor.id.get) {
      val fakeWapID = 0
      val hypr = new HyprMarketplaceReportingAPI(
        fakeWapID,
        configurationData,
        database,
        waterfallAdProviderService,
        testPlatform,
        configVars,
        appEnvironment,
        keenInitialization,
        ws,
        appService
      )
      hypr.app must beNone
    }
  }

  "getImpressions" should {
    val fakeWS = mock[WSClient]
    val fakeRequest = mock[WSRequest]
    val fakeResponse = mock[WSResponse]
    val fakeImpressionCount = 50
    val jsonResponse = Json.obj(
      "result" -> JsArray(
        Seq(
          Json.obj("value" -> JsNumber(fakeImpressionCount))
        )
      )
    )
    fakeResponse.json returns jsonResponse
    fakeRequest.withRequestTimeout(300000) returns fakeRequest
    fakeRequest.withQueryString(("api_key", any[String])) returns fakeRequest
    fakeRequest.post(any[JsObject])(any) returns Future { fakeResponse }
    fakeWS.url(any[String]) returns fakeRequest

    "return impression data from keen" in new WithDB {
      val hypr = new HyprMarketplaceReportingAPI(
        waterfallAdProvider1.id,
        configurationData,
        database,
        waterfallAdProviderService,
        testPlatform,
        configVars,
        appEnvironment,
        keenInitialization,
        fakeWS,
        appService
      )
      hypr.getImpressions.get must beEqualTo(fakeImpressionCount.toString)
    }

    "return None if the app is not found" in new WithDB {
      val fakeWapID = 0
      val hypr = new HyprMarketplaceReportingAPI(
        fakeWapID,
        configurationData,
        database,
        waterfallAdProviderService,
        testPlatform,
        configVars,
        appEnvironment,
        keenInitialization,
        ws,
        appService
      )
      hypr.getImpressions must beNone
    }

    "return None if the Keen response cannot be parsed properly" in new WithDB {
      fakeResponse.json returns Json.obj()
      val hypr = new HyprMarketplaceReportingAPI(
        waterfallAdProvider1.id,
        configurationData,
        database,
        waterfallAdProviderService,
        testPlatform,
        configVars,
        appEnvironment,
        keenInitialization,
        fakeWS,
        appService
      )
      hypr.getImpressions must beNone
    }
  }

  "updateRevenueData" should {
    "updates the cpm field of the WaterfallAdProvider if the HyprMarketplace API call is successful" in new WithDB {
      val originalGeneration = generationNumber(waterfall.app_id)
      waterfallAdProviderService.find(waterfallAdProvider1.id).get.cpm must beNone
      waterfallService.update(waterfallAdProvider1.waterfallID, optimizedOrder = true, testMode = false, paused = false)
      waterfallAdProvider1.cpm must beNone
      retrieveImpressionResponse = Some("1000")
      val globalStats = JsObject(Seq("revenue" -> JsString("10.00"), "impressions" -> JsString("1000")))
      val statsJson = JsObject(Seq("results" -> JsArray(Seq(JsObject(Seq("global_stats" -> globalStats))))))
      retrieveAPIDataResponse.body returns statsJson.toString
      retrieveAPIDataResponse.status returns 200
      callAPI
      waterfallAdProviderService.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(10.00)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration + 1)
    }

    "does not update the WaterfallAdProvider if the HyprMarketplace API call is unsuccessful" in new WithDB {
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalCPM = waterfallAdProviderService.find(waterfallAdProvider1.id).get.cpm.get
      val jsonResponse = JsObject(Seq("message" -> JsString("Bad Request."), "details" -> JsString("Some error message")))
      retrieveAPIDataResponse.body returns jsonResponse.toString
      retrieveAPIDataResponse.status returns 401
      callAPI
      waterfallAdProviderService.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(originalCPM)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration)
    }

    "sets the eCPM value of the WaterfallAdProvider to 0 if the HyprMarketplace API returns 0 impressions" in new WithDB {
      val originalGeneration = generationNumber(waterfall.app_id)
      val originalCPM = waterfallAdProviderService.find(waterfallAdProvider1.id).get.cpm
      retrieveImpressionResponse = Some("0")
      val globalStats = JsObject(Seq("revenue" -> JsString("0.00"), "impressions" -> JsString("0")))
      val statsJson = JsObject(Seq("results" -> JsArray(Seq(JsObject(Seq("global_stats" -> globalStats))))))
      retrieveAPIDataResponse.body returns statsJson.toString
      retrieveAPIDataResponse.status returns 200
      callAPI
      waterfallAdProviderService.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(0.00)
      generationNumber(waterfall.app_id) must beEqualTo(originalGeneration + 1)
    }
  }

  /**
   * Helper function to fake HyprMarketplace API call.
   * @return Response from mocked out API call.
   */
  def callAPI = {
    org.mockito.Mockito.doReturn(retrieveImpressionResponse).when(hyprMarketplace).getImpressions
    hyprMarketplace.retrieveAPIData returns Future { retrieveAPIDataResponse }
    Await.result(hyprMarketplace.updateRevenueData(), Duration(10000, "millis"))
  }
}
