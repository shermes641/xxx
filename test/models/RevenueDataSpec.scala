package models

import play.api.libs.json.{JsString, JsObject}
import org.junit.runner._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class RevenueDataSpec extends SpecificationWithFixtures {
  val revenue = 10.00
  val impressions = 1000
  val globalStats = JsObject(Seq("revenue" -> JsString(revenue.toString), "impressions" -> JsString(impressions.toString), "completions" -> JsString("200")))
  val revenueData = new RevenueData(globalStats)

  "RevenueData" should {
    "store revenue value" in {
      revenueData.revenue must beEqualTo(revenue)
    }

    "store impression count" in {
      revenueData.impressionCount must beEqualTo(impressions)
    }
  }

  "eCPM" should {
    "correctly calculate the eCPM based on revenue and impression values" in {
      revenueData.eCPM must beEqualTo(10.00)
    }
  }
}
