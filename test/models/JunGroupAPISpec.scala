package models

import org.specs2.mock.Mockito
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import play.api.test.FakeApplication
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import org.junit.runner._
import org.specs2.runner._
import play.api.Play

@RunWith(classOf[JUnitRunner])
class JunGroupAPISpec extends SpecificationWithFixtures with WaterfallSpecSetup with Mockito {

  val response = mock[WSResponse]
  val junGroup = spy(new JunGroupAPI())

  "adNetworkConfiguration" should {
    "Build the correct configuration using the created distributor user" in new WithDB {
      val config = junGroup.adNetworkConfiguration(user)
      config \ "ad_network" \ "name" must beEqualTo(JsString("tdepplito@jungroup.com"))
      config \ "ad_network" \ "mobile" must beEqualTo(JsBoolean(true))

      config \ "payout_url" \ "url" must beEqualTo(JsString(Play.current.configuration.getString("jungroup.callbackurl").get))
      config \ "payout_url" \ "environment" must beEqualTo(JsString("production"))
    }
  }

  "createRequest" should {
    "create a request correctly using the ad network configuration" in new WithDB {
      val config = junGroup.adNetworkConfiguration(user)
      val successResponse = JsObject(Seq("success" -> JsBoolean(true)))
      response.body returns Json.stringify(successResponse)
      response.status returns 200
      junGroup.createRequest(config) returns Future { response }
    }
  }

  step(clean)
}
