import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

import anorm._
import models._
import play.api.libs.json._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class WaterfallSpec extends Specification {

  "Waterfall" should {

    "should turn into JSON" in new WithApplication {
      val waterfall = Waterfall(NotAssigned, "Name")
      Json.toJson(waterfall).toString must equalTo("{\"name\":\"Name\"}")
      val waterfall2 = Waterfall(Id(1.toLong), "Name2")
      Json.toJson(waterfall2).toString must equalTo("{\"id\":1,\"name\":\"Name2\"}")
    }

    "should fetch relationships" in new WithApplication {
    	Waterfall.withWaterfallAdProviders(1) must
    		equalTo(Map(Waterfall(Id(1),"TestWaterfall") ->
    			List(WaterfallAdProvider(Id(1),1,1,None,None,None,None),
    				 WaterfallAdProvider(Id(2),1,2,Some(0),None,None,None)))
)
    }

  }
}
