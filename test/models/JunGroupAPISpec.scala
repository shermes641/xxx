package models

import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import com.typesafe.config.ConfigFactory
import org.junit.runner._
import org.specs2.mock.Mockito
import org.specs2.runner._
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.Play
import play.api.test.FakeApplication
import play.api.test.Helpers._
import resources.WaterfallSpecSetup
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class JunGroupAPISpec extends SpecificationWithFixtures with WaterfallSpecSetup with Mockito {
  implicit val actorSystem = ActorSystem("testActorSystem", ConfigFactory.load())

  val response = mock[WSResponse]
  val junGroup = running(FakeApplication(additionalConfiguration = testDB)) {
    spy(new JunGroupAPI())
  }
  val appToken = "app-token"
  val appName = "app-name"
  val distributorName = "Company Name"

  "adNetworkConfiguration" should {
    "Build the correct configuration using the created distributor user" in new WithDB {
      val payoutUrl = Play.current.configuration.getString("jungroup.callbackurl").get.format(appToken)
      val config = junGroup.adNetworkConfiguration(distributorName, appName, appToken)
      val networkJson = config \ "ad_network"
      val payoutUrlJson = config \ "payout_url"
      val adNetworkName = distributorName + " - " + appName
      val createdInContext = Play.current.configuration.getString("app_domain").getOrElse("") + " - " + Environment.mode

      networkJson \ "name" must beEqualTo(JsString(adNetworkName))
      networkJson \ "mobile" must beEqualTo(JsBoolean(true))
      networkJson \ "created_in_context" must beEqualTo(JsString(createdInContext))
      networkJson \ "is_test" must beEqualTo(JsBoolean(true))
      networkJson \ "demographic_targeting_enabled" must beEqualTo(JsBoolean(true))
      networkJson \ "mediation_reporting_api_key" must beEqualTo(JsString(appToken))
      networkJson \ "mediation_reporting_placement_id" must beEqualTo(JsString(appToken))

      payoutUrlJson \ "url" must beEqualTo(JsString(payoutUrl))
      payoutUrlJson \ "environment" must beEqualTo(JsString(Environment.mode))
      payoutUrlJson \ "signature" must beEqualTo(junGroup.PlayerSignature)
    }
  }

  "createRequest" should {
    "create a request correctly using the ad network configuration" in new WithDB {
      val config = junGroup.adNetworkConfiguration(distributorName, appName, appToken)
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
      val id = WaterfallAdProvider.create(waterfall.id, adProviderID1.get, None, None, true, false, true).get
      WaterfallAdProvider.find(id).get
    }
    api.createRequest(any[JsObject]) returns Future { playerResponse }
    api.adNetworkConfiguration(any[String], any[String], any[String]) returns JsObject(Seq())
    val junActor = running(FakeApplication(additionalConfiguration = testDB)) {
      TestActorRef(new JunGroupAPIActor(waterfall.id, hyprWaterfallAdProvider, appToken, appName, distributor.id.get, api)).underlyingActor
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
      val playerErrorBody = JsObject(Seq("error" -> JsString(playerError), "success" -> JsBoolean(false), "ad_network" -> JsObject(Seq("ad_network" -> JsObject(Seq("id" -> JsNumber(1))))))).toString
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
