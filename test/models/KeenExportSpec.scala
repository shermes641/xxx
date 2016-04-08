package models

import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import com.typesafe.config.ConfigFactory
import io.keen.client.java.{JavaKeenClientBuilder, KeenClient, KeenProject}
import org.specs2.mock.Mockito
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import resources.{DistributorUserSetup, SpecificationWithFixtures}

import scala.io._

class KeenExportSpec extends SpecificationWithFixtures with DistributorUserSetup with AppCreationHelper with Mockito {
  implicit val actorSystem = ActorSystem("testActorSystem", ConfigFactory.load())

  val displayFillRate = true

  def getAppsList(distributorID: Long) = {
    val appList = appService.findAllAppsWithWaterfalls(distributorID)
    appList.map(app => app.id.toString)
  }

  val timeframe = JsObject(
    Seq(
      "start" -> JsString("2015-04-30T00:00:00+00:00"),
      "end" -> JsString("2015-05-14T00:00:00+00:00")
    )
  )

  val filters = JsArray(
    Seq(
      JsObject(Seq(
        "property_name" -> JsString("ad_provider"),
        "operator" -> JsString("eq"),
        "property_value" -> JsString("10")
      ))
    )
  )

  val mailer = mock[Mailer]

  "GetDataFromKeen" should {
    "Returns the correct filters" in new WithDB {
      val collection_name = "test_collection"
      val property_name = "property_name"
      val property_value = 123123
      val createdFilters = keenExportService.createFilter(timeframe, filters, collection_name, "2", property_name)
      (createdFilters \ "event_collection").as[String] must beEqualTo(collection_name)
      (createdFilters \ "target_property").as[String] must beEqualTo(property_name)
      ((createdFilters \ "filters").as[JsArray].as[List[JsObject]].head \ "property_value").as[String].toLong must beEqualTo(10)
      ((createdFilters \ "filters").as[JsArray].as[List[JsObject]].head \ "property_name").as[String] must beEqualTo("ad_provider")
      ((createdFilters \ "filters").as[JsArray].as[List[JsObject]].last \ "property_value").as[String].toLong must beEqualTo(2)
      ((createdFilters \ "filters").as[JsArray].as[List[JsObject]].last \ "property_name").as[String] must beEqualTo("app_id")
    }

    "Builds CSV header correctly" in new WithDB {
      val email = "test@jungroup.com"
      val (newUser, newDistributor) = newDistributorUser(email, "password", "company")
      val client = new JavaKeenClientBuilder().build()
      val project = new KeenProject(configVars.ConfigVarsKeen.projectID, configVars.ConfigVarsKeen.writeKey, configVars.ConfigVarsKeen.readKey)
      client.setDefaultProject(project)
      KeenClient.initialize(client)

      lazy val analyticsController = app.injector.instanceOf[controllers.AnalyticsController]
      val scopedReadKey = analyticsController.getScopedReadKey(newDistributor.id.get)
      val keenExportActor = TestActorRef(new KeenExportActor(newDistributor.id.get, displayFillRate, email, filters, timeframe, getAppsList(newDistributor.id.get), adProvidersSelected = true, scopedReadKey, appService, keenExportService, configVars, ws, mailer)).underlyingActor
      setUpApp(newDistributor.id.get)

      val writer = keenExportActor.createCSVFile()
      keenExportActor.createCSVHeader(writer)
      keenExportActor.requestData(writer)
      readFileAsString(keenExportActor.fileName) must beEqualTo("Date,App,DAU,Requests,Fill,Impressions,Completions,Completion Per DAU,eCPM,Estimated Revenue")
    }

    "Parse Keen response and build CSV Row correctly with fillRate" in new WithDB {
      val email = "test2@jungroup.com"
      val (newUser, newDistributor) = newDistributorUser(email, "password", "company")
      val client = new JavaKeenClientBuilder().build()
      val project = new KeenProject(configVars.ConfigVarsKeen.projectID, configVars.ConfigVarsKeen.writeKey, configVars.ConfigVarsKeen.readKey)
      client.setDefaultProject(project)
      KeenClient.initialize(client)

      lazy val analyticsController = app.injector.instanceOf[controllers.AnalyticsController]
      val scopedReadKey = analyticsController.getScopedReadKey(newDistributor.id.get)
      var keenExportActor = TestActorRef(new KeenExportActor(newDistributor.id.get, displayFillRate, email, filters, timeframe, getAppsList(newDistributor.id.get), adProvidersSelected = true, scopedReadKey, appService, keenExportService, configVars, ws, mailer)).underlyingActor
      var writer = keenExportActor.createCSVFile()
      setUpApp(newDistributor.id.get)

      val sampleResult = new KeenResult(Json.toJson(123456), JsObject(Seq(("start", Json.toJson("2015-04-02T00:00:00.000Z")))))
      keenExportActor.parseResponse("{\"result\": [{\"value\": 123456, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}").head must beEqualTo(sampleResult)

      val requestsResponse,
      dauResponse,
      responsesResponse,
      impressionsResponse,
      adCompletedResponse,
      rewardDeliveredResponse,
      adCompletedEcpmResponse,
      rewardDeliveredEcpmResponse,
      adCompletedEarningsResponse,
      rewardDeliveredEarningsResponse = mock[WSResponse]

      def buildAppRows() = {
        keenExportActor.buildAppRows(
          "App Name",
          requestsResponse,
          dauResponse,
          responsesResponse,
          impressionsResponse,
          adCompletedResponse,
          rewardDeliveredResponse,
          adCompletedEcpmResponse,
          rewardDeliveredEcpmResponse,
          adCompletedEarningsResponse,
          rewardDeliveredEarningsResponse,
          writer
        )
      }

      val sampleResult2 = new KeenResult(Json.toJson(3333333), JsObject(Seq(("start", Json.toJson("2015-04-03T00:00:00.000Z")))))
      keenExportActor.parseResponse("{\"result\": [{\"value\": 3333333, \"timeframe\": {\"start\": \"2015-04-03T00:00:00.000Z\"}}]}").head must beEqualTo(sampleResult2)

      // Test multiple days
      keenExportActor.parseResponse("{\"result\": [{\"value\": 123456, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}, {\"value\": 3333333, \"timeframe\": {\"start\": \"2015-04-03T00:00:00.000Z\"}}]}") must beEqualTo(List(sampleResult, sampleResult2))

      requestsResponse.body returns "{\"result\": [{\"value\": 101, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      dauResponse.body returns "{\"result\": [{\"value\": 310, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      responsesResponse.body returns "{\"result\": [{\"value\": 53, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      impressionsResponse.body returns "{\"result\": [{\"value\": 30, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      adCompletedResponse.body returns "{\"result\": [{\"value\": 3, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      rewardDeliveredResponse.body returns "{\"result\": [{\"value\": 6, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      adCompletedEcpmResponse.body returns "{\"result\": [{\"value\": 3.912, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      rewardDeliveredEcpmResponse.body returns "{\"result\": [{\"value\": 17.0775, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      adCompletedEarningsResponse.body returns "{\"result\": [{\"value\": 6000, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      rewardDeliveredEarningsResponse.body returns "{\"result\": [{\"value\": 14013, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"

      buildAppRows()
      readFileAsString(keenExportActor.fileName) must beEqualTo("2015-04-02,App Name,310,101,0.5247525,30,9,0.029032258,12.689,0.3806700110435486")

      keenExportActor = TestActorRef(new KeenExportActor(newDistributor.id.get, displayFillRate, email, filters, timeframe, getAppsList(newDistributor.id.get), adProvidersSelected = false, scopedReadKey, appService, keenExportService, configVars, ws, mailer)).underlyingActor
      writer = keenExportActor.createCSVFile()

      // Verify dividing by 0 does not cause error
      requestsResponse.body returns "{\"result\": [{\"value\": 0, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      dauResponse.body returns "{\"result\": [{\"value\": 0, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      responsesResponse.body returns "{\"result\": [{\"value\": 0, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      impressionsResponse.body returns "{\"result\": [{\"value\": 10, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      adCompletedResponse.body returns "{\"result\": [{\"value\": 1, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      rewardDeliveredResponse.body returns "{\"result\": [{\"value\": 3, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      adCompletedEcpmResponse.body returns "{\"result\": [{\"value\": 6.932, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      rewardDeliveredEcpmResponse.body returns "{\"result\": [{\"value\": 10.00, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      adCompletedEarningsResponse.body returns "{\"result\": [{\"value\": 3000, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      rewardDeliveredEarningsResponse.body returns "{\"result\": [{\"value\": 7002, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"

      buildAppRows()
      readFileAsString(keenExportActor.fileName) must beEqualTo("2015-04-02,App Name,0,0,0.0,10,4,0.0,9.233,0.09233000129461288")

      keenExportActor = TestActorRef(new KeenExportActor(newDistributor.id.get, displayFillRate, email, filters, timeframe, getAppsList(newDistributor.id.get), adProvidersSelected = false, scopedReadKey, appService, keenExportService, configVars, ws, mailer)).underlyingActor
      writer = keenExportActor.createCSVFile()

      // Verify eCPM defaults to 0
      adCompletedEcpmResponse.body returns "{\"result\": [{\"value\": null, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      rewardDeliveredEcpmResponse.body returns "{\"result\": [{\"value\": null, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      buildAppRows()
      readFileAsString(keenExportActor.fileName) must beEqualTo("2015-04-02,App Name,0,0,0.0,10,4,0.0,0.0,0.0")
    }

    "Parse Keen response and build CSV Row correctly without fillRate" in new WithDB {
      val email = "test3@jungroup.com"
      val (newUser, newDistributor) = newDistributorUser(email, "password", "company")
      val client = new JavaKeenClientBuilder().build()
      val project = new KeenProject(configVars.ConfigVarsKeen.projectID, configVars.ConfigVarsKeen.writeKey, configVars.ConfigVarsKeen.readKey)
      client.setDefaultProject(project)
      KeenClient.initialize(client)

      lazy val analyticsController = app.injector.instanceOf[controllers.AnalyticsController]
      val scopedReadKey = analyticsController.getScopedReadKey(newDistributor.id.get)
      var keenExportActor = TestActorRef(new KeenExportActor(
        newDistributor.id.get,
        !displayFillRate,
        email,
        filters,
        timeframe,
        getAppsList(newDistributor.id.get),
        adProvidersSelected = true,
        scopedReadKey,
        appService,
        keenExportService,
        configVars,
        ws,
        mailer)).underlyingActor
      var writer = keenExportActor.createCSVFile()
      setUpApp(newDistributor.id.get)

      val sampleResult = new KeenResult(Json.toJson(123456), JsObject(Seq(("start", Json.toJson("2015-04-02T00:00:00.000Z")))))
      keenExportActor.parseResponse("{\"result\": [{\"value\": 123456, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}").head must beEqualTo(sampleResult)

      val requestsResponse,
      dauResponse,
      responsesResponse,
      impressionsResponse,
      adCompletedResponse,
      rewardDeliveredResponse,
      adCompletedEcpmResponse,
      rewardDeliveredEcpmResponse,
      adCompletedEarningsResponse,
      rewardDeliveredEarningsResponse = mock[WSResponse]

      def buildAppRows() = {
        keenExportActor.buildAppRows(
          "App Name",
          requestsResponse,
          dauResponse,
          responsesResponse,
          impressionsResponse,
          adCompletedResponse,
          rewardDeliveredResponse,
          adCompletedEcpmResponse,
          rewardDeliveredEcpmResponse,
          adCompletedEarningsResponse,
          rewardDeliveredEarningsResponse,
          writer
        )
      }

      val sampleResult2 = new KeenResult(Json.toJson(3333333), JsObject(Seq(("start", Json.toJson("2015-04-03T00:00:00.000Z")))))
      keenExportActor.parseResponse("{\"result\": [{\"value\": 3333333, \"timeframe\": {\"start\": \"2015-04-03T00:00:00.000Z\"}}]}").head must beEqualTo(sampleResult2)

      // Test multiple days
      keenExportActor.parseResponse("{\"result\": [{\"value\": 123456, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}, {\"value\": 3333333, \"timeframe\": {\"start\": \"2015-04-03T00:00:00.000Z\"}}]}") must beEqualTo(List(sampleResult, sampleResult2))

      requestsResponse.body returns "{\"result\": [{\"value\": 101, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      dauResponse.body returns "{\"result\": [{\"value\": 310, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      responsesResponse.body returns "{\"result\": [{\"value\": 53, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      impressionsResponse.body returns "{\"result\": [{\"value\": 30, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      adCompletedResponse.body returns "{\"result\": [{\"value\": 3, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      rewardDeliveredResponse.body returns "{\"result\": [{\"value\": 6, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      adCompletedEcpmResponse.body returns "{\"result\": [{\"value\": 3.912, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      rewardDeliveredEcpmResponse.body returns "{\"result\": [{\"value\": 17.0775, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      adCompletedEarningsResponse.body returns "{\"result\": [{\"value\": 6000, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      rewardDeliveredEarningsResponse.body returns "{\"result\": [{\"value\": 14013, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"

      buildAppRows()
      readFileAsString(keenExportActor.fileName) must
        beEqualTo("2015-04-02,App Name,310,101,N/A,30,9,0.029032258,12.689,0.3806700110435486")

      keenExportActor = TestActorRef(new KeenExportActor(
        newDistributor.id.get,
        !displayFillRate,
        email,
        filters,
        timeframe,
        getAppsList(newDistributor.id.get),
        adProvidersSelected = false,
        scopedReadKey,
        appService,
        keenExportService,
        configVars,
        ws,
        mailer)).underlyingActor
      writer = keenExportActor.createCSVFile()

      // Verify dividing by 0 does not cause error
      requestsResponse.body returns "{\"result\": [{\"value\": 0, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      dauResponse.body returns "{\"result\": [{\"value\": 0, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      responsesResponse.body returns "{\"result\": [{\"value\": 0, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      impressionsResponse.body returns "{\"result\": [{\"value\": 10, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      adCompletedResponse.body returns "{\"result\": [{\"value\": 1, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      rewardDeliveredResponse.body returns "{\"result\": [{\"value\": 3, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      adCompletedEcpmResponse.body returns "{\"result\": [{\"value\": 6.932, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      rewardDeliveredEcpmResponse.body returns "{\"result\": [{\"value\": 10.00, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      adCompletedEarningsResponse.body returns "{\"result\": [{\"value\": 3000, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      rewardDeliveredEarningsResponse.body returns "{\"result\": [{\"value\": 7002, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"

      buildAppRows()
      readFileAsString(keenExportActor.fileName) must
        beEqualTo("2015-04-02,App Name,0,0,N/A,10,4,0.0,9.233,0.09233000129461288")

      keenExportActor = TestActorRef(new KeenExportActor(
        newDistributor.id.get,
        !displayFillRate,
        email,
        filters,
        timeframe,
        getAppsList(newDistributor.id.get),
        adProvidersSelected = false,
        scopedReadKey,
        appService,
        keenExportService,
        configVars,
        ws,
        mailer)).underlyingActor
      writer = keenExportActor.createCSVFile()

      // Verify eCPM defaults to 0
      adCompletedEcpmResponse.body returns "{\"result\": [{\"value\": null, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      rewardDeliveredEcpmResponse.body returns "{\"result\": [{\"value\": null, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      buildAppRows()
      readFileAsString(keenExportActor.fileName) must
        beEqualTo("2015-04-02,App Name,0,0,N/A,10,4,0.0,0.0,0.0")
    }

    "not error when calculating revenue" in new WithDB {
      val email = "test4@jungroup.com"
      val (newUser, newDistributor) = newDistributorUser(email, "password", "company")
      val client = new JavaKeenClientBuilder().build()
      val project = new KeenProject(configVars.ConfigVarsKeen.projectID, configVars.ConfigVarsKeen.writeKey, configVars.ConfigVarsKeen.readKey)
      client.setDefaultProject(project)
      KeenClient.initialize(client)

      val scopedReadKey = {
        lazy val analyticsController = app.injector.instanceOf[controllers.AnalyticsController]
        analyticsController.getScopedReadKey(newDistributor.id.get)
      }
      var keenExportActor = TestActorRef(new KeenExportActor(
        newDistributor.id.get,
        !displayFillRate,
        email,
        filters,
        timeframe,
        getAppsList(newDistributor.id.get),
        adProvidersSelected = true,
        scopedReadKey,
        appService = appService,
        keenExportService = keenExportService,
        configVars = configVars,
        wsClient = ws,
        mailer = mailer)).underlyingActor
      var writer = keenExportActor.createCSVFile()
      setUpApp(newDistributor.id.get)

      val requestsResponse,
      dauResponse,
      responsesResponse,
      impressionsResponse,
      adCompletedResponse,
      rewardDeliveredResponse,
      adCompletedEcpmResponse,
      rewardDeliveredEcpmResponse,
      adCompletedEarningsResponse,
      rewardDeliveredEarningsResponse = mock[WSResponse]

      def buildAppRows() = {
        keenExportActor.buildAppRows(
          "App Name",
          requestsResponse,
          dauResponse,
          responsesResponse,
          impressionsResponse,
          adCompletedResponse,
          rewardDeliveredResponse,
          adCompletedEcpmResponse,
          rewardDeliveredEcpmResponse,
          adCompletedEarningsResponse,
          rewardDeliveredEarningsResponse,
          writer
        )
      }

      // Verify 0 impressions and completions does not cause error
      requestsResponse.body returns "{\"result\": [{\"value\": 0, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      dauResponse.body returns "{\"result\": [{\"value\": 0, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      responsesResponse.body returns "{\"result\": [{\"value\": 0, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      impressionsResponse.body returns "{\"result\": [{\"value\": 0, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      adCompletedResponse.body returns "{\"result\": [{\"value\": 0, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      rewardDeliveredResponse.body returns "{\"result\": [{\"value\": 0, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      adCompletedEcpmResponse.body returns "{\"result\": [{\"value\": 10.00, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      rewardDeliveredEcpmResponse.body returns "{\"result\": [{\"value\": 10.00, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      adCompletedEarningsResponse.body returns "{\"result\": [{\"value\": 0, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      rewardDeliveredEarningsResponse.body returns "{\"result\": [{\"value\": 0, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"

      keenExportActor = TestActorRef(new KeenExportActor(
        newDistributor.id.get,
        !displayFillRate,
        email,
        filters,
        timeframe,
        getAppsList(newDistributor.id.get),
        adProvidersSelected = false,
        scopedReadKey,
        appService = appService,
        keenExportService = keenExportService,
        configVars = configVars,
        wsClient = ws,
        mailer = mailer)).underlyingActor
      writer = keenExportActor.createCSVFile()

      buildAppRows()
      readFileAsString(keenExportActor.fileName) must
        beEqualTo("2015-04-02,App Name,0,0,N/A,0,0,0.0,0.0,0.0")

      // Verify 0 completions does not cause error
      impressionsResponse.body returns "{\"result\": [{\"value\": 100, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      adCompletedResponse.body returns "{\"result\": [{\"value\": 0, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"

      keenExportActor = TestActorRef(new KeenExportActor(
        newDistributor.id.get,
        !displayFillRate,
        email,
        filters,
        timeframe,
        getAppsList(newDistributor.id.get),
        adProvidersSelected = false,
        scopedReadKey,
        appService = appService,
        keenExportService = keenExportService,
        configVars = configVars,
        wsClient = ws,
        mailer = mailer)).underlyingActor
      writer = keenExportActor.createCSVFile()

      buildAppRows()
      readFileAsString(keenExportActor.fileName) must
        beEqualTo("2015-04-02,App Name,0,0,N/A,100,0,0.0,0.0,0.0")

      // Verify 0 impressions does not cause error
      impressionsResponse.body returns "{\"result\": [{\"value\": 0, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"
      adCompletedResponse.body returns "{\"result\": [{\"value\": 100, \"timeframe\": {\"start\": \"2015-04-02T00:00:00.000Z\"}}]}"

      keenExportActor = TestActorRef(new KeenExportActor(
        newDistributor.id.get,
        !displayFillRate,
        email,
        filters,
        timeframe,
        getAppsList(newDistributor.id.get),
        adProvidersSelected = false,
        scopedReadKey,
        appService = appService,
        keenExportService = keenExportService,
        configVars = configVars,
        wsClient = ws,
        mailer = mailer)).underlyingActor
      writer = keenExportActor.createCSVFile()

      buildAppRows()
      readFileAsString(keenExportActor.fileName) must
        beEqualTo("2015-04-02,App Name,0,0,N/A,0,100,0.0,10.0,0.0")
    }
  }
}
