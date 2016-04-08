package models

import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import com.typesafe.config.ConfigFactory
import org.specs2.mock.Mockito
import play.api.Play
import play.api.db.DB
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.test.FakeApplication
import play.api.test.Helpers._
import resources.{SpecificationWithFixtures, WaterfallSpecSetup}

import scala.Int._
import scala.concurrent.Future
import scala.util.Random

class JunGroupAPISpec extends SpecificationWithFixtures with WaterfallSpecSetup with UpdateHyprMarketplace with Mockito {
  implicit val actorSystem = ActorSystem("testActorSystem", ConfigFactory.load())

  val response = mock[WSResponse]
  val junGroup = running(FakeApplication(additionalConfiguration = testDB)) {
    spy(new JunGroupAPI())
  }
  val testApp: App = new App(id = 1,
    active = true,
    distributorID = 1,
    name = "app-name",
    callbackURL = None,
    serverToServerEnabled = true,
    token = "app-token",
    platformID = 1,
    hmacSecret = "")
  val distributorName = "Company Name"

  "updateHyprMarketplaceDistributorID" should {
    "respond correctly" in new WithDB {

      val randomCharacters = Random.alphanumeric.take(5).mkString
      val companyName = "Test Company-" + randomCharacters
      val email = "mediation-testing-" + randomCharacters + "@jungroup.com"
      val password = "testtest"
      val adProviders = AdProvider.findAll
      val distributorID = DistributorUser.create(email, password, companyName).get
      val appID = App.create(distributorID, "12345", 1).get
      val appp = App.find(appID).get
      val adProviderID = 2

      val wap = DB.withTransaction { implicit connection =>
        val waterfallID = Waterfall.create(appID, "12345").get
        VirtualCurrency.createWithTransaction(appID, "Gold", 1, 10, None, Some(true))
      val hyprWaterfallAdProviderID = WaterfallAdProvider
        .createWithTransaction(waterfallID, adProviderID, Option(0), Option(int2double(20)), configurable = false, active = false, pending = true).get
        AppConfig.create(appID, appp.token, 0)
        val hyprWAP = WaterfallAdProvider.findWithTransaction(hyprWaterfallAdProviderID).get
        WaterfallAdProviderWithAppData(hyprWAP.id, waterfallID, adProviderID, hyprWAP.waterfallOrder, hyprWAP.cpm, hyprWAP.active, hyprWAP.fillRate,
          hyprWAP.configurationData, hyprWAP.reportingActive, hyprWAP.pending, appp.token, appp.name, companyName)
      }
      val newApp = App.findByWaterfallID(wap.waterfallID).get
      val adNetwork = junGroup.adNetworkConfiguration(wap.companyName, newApp)
      val response = mock[WSResponse]
      val playerSuccessBody = JsObject(Seq("success" -> JsBoolean(true),
        "ad_network" -> JsObject(Seq("ad_network" -> JsObject(Seq("id" -> JsNumber(adProviderID))))))).toString
      response.body returns playerSuccessBody.toString
      response.status returns 200
      junGroup.createRequest(adNetwork) returns Future(response)
      updateHyprMarketplaceDistributorID(wap, Some(junGroup))
      successfulWaterfallAdProviderIDs.length must_== 1
      unsuccessfulWaterfallAdProviderIDs.length must_== 0

      response.status returns 304
      updateHyprMarketplaceDistributorID(wap, Some(junGroup))
      successfulWaterfallAdProviderIDs.length must_== 2
      unsuccessfulWaterfallAdProviderIDs.length must_== 0

      // fail with good status but bad json
      response.body returns JsUndefined.toString
      response.status returns 200
      updateHyprMarketplaceDistributorID(wap, Some(junGroup))
      successfulWaterfallAdProviderIDs.length must_== 2
      unsuccessfulWaterfallAdProviderIDs.length must_== 1

      // fail with good status but bad json
      val playerFailureBody = JsObject(Seq("success" -> JsBoolean(false),
        "ad_network" -> JsObject(Seq("ad_network" -> JsObject(Seq("id" -> JsNumber(adProviderID))))))).toString
      response.body returns playerFailureBody.toString
      updateHyprMarketplaceDistributorID(wap, Some(junGroup))
      successfulWaterfallAdProviderIDs.length must_== 2
      unsuccessfulWaterfallAdProviderIDs.length must_== 2
    }
  }

  "adNetworkConfiguration" should {
    "Build the correct configuration using the created distributor user" in new WithDB {
      val payoutUrl = Play.current.configuration.getString("jungroup.callbackurl").get.format(testApp.token)
      val config = junGroup.adNetworkConfiguration(distributorName, testApp)
      val networkJson = config \ "ad_network"
      val payoutUrlJson = config \ "payout_url"
      val platformName = Platform.find(testApp.platformID).PlatformName
      val adNetworkName = distributorName + " - " + testApp.name + " - " + platformName
      val createdInContext = Play.current.configuration.getString("app_domain").getOrElse("") + " - " + Environment.mode

      networkJson \ "name" must beEqualTo(JsString(adNetworkName))
      networkJson \ "mobile" must beEqualTo(JsBoolean(true))
      networkJson \ "created_in_context" must beEqualTo(JsString(createdInContext))
      networkJson \ "is_test" must beEqualTo(JsBoolean(true))
      networkJson \ "demographic_targeting_enabled" must beEqualTo(JsBoolean(true))
      networkJson \ "mediation_reporting_api_key" must beEqualTo(JsString(testApp.token))
      networkJson \ "mediation_reporting_placement_id" must beEqualTo(JsString(testApp.token))

      payoutUrlJson \ "url" must beEqualTo(JsString(payoutUrl))
      payoutUrlJson \ "environment" must beEqualTo(JsString(Environment.mode))
      payoutUrlJson \ "signature" must beEqualTo(junGroup.PlayerSignature)
    }
  }

  "createRequest" should {
    "create a request correctly using the ad network configuration" in new WithDB {
      val config = junGroup.adNetworkConfiguration(distributorName, testApp)
      val successResponse = JsObject(Seq("success" -> JsBoolean(true)))
      response.body returns Json.stringify(successResponse)
      response.status returns 200
      junGroup.createRequest(config) returns Future { response }
    }
  }

  "JunGroup API Actor" should {
    val api = mock[JunGroupAPI]
    val playerResponse = mock[WSResponse]
    val hyprWaterfallAdProvider = running(FakeApplication(additionalConfiguration = testDB)) {
      val id = WaterfallAdProvider.create(waterfall.id, adProviderID1.get, None, None, configurable = true, active = false, pending = true).get
      WaterfallAdProvider.find(id).get
    }
    api.createRequest(any[JsObject]) returns Future { playerResponse }
    api.adNetworkConfiguration(any[String], any[App]) returns JsObject(Seq())
    val junActor = running(FakeApplication(additionalConfiguration = testDB)) {
      TestActorRef(new JunGroupAPIActor(waterfall.id, hyprWaterfallAdProvider, testApp, distributor.id.get, api)).underlyingActor
    }

    "exist and accept Create Ad Network message" in new WithDB {
      junActor.receive(CreateAdNetwork(user))
      junActor must haveClass[JunGroupAPIActor]
    }

    "set lastFailure properly when there is an HTTP Auth failure" in new WithDB {
      playerResponse.body returns "HTTP Auth Failure"
      playerResponse.status returns 401
      junActor.receive(CreateAdNetwork(user))
      junActor.lastFailure must contain("Received a JSON parsing error").eventually(3, 1 second)
    }

    "set lastFailure properly when the response code is 200 but the request was not successful" in new WithDB {
      val playerError = "Some player server error"
      val playerErrorBody = JsObject(Seq("error" -> JsString(playerError), "success" -> JsBoolean(false),
        "ad_network" -> JsObject(Seq("ad_network" -> JsObject(Seq("id" -> JsNumber(1))))))).toString
      playerResponse.body returns playerErrorBody
      playerResponse.status returns 200
      junActor.receive(CreateAdNetwork(user))
      junActor.lastFailure must contain (playerError).eventually(3, 1 second)
    }
  }

  "JunGroup Email Actor" should {
    "exist and accept email message" in new WithDB {
      val emailActor = TestActorRef(new JunGroupEmailActor("test@test.com", "subject", "body")).underlyingActor
      emailActor.receive("test@test.com")
      emailActor must haveClass[JunGroupEmailActor]
    }
  }
}
