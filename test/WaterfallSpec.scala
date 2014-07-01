import org.specs2.execute.{Result, AsResult}
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.db.DB

import play.api.test._
import play.api.test.Helpers._

import anorm._
import models._
import play.api.libs.json._
import play.api.Play.current

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class WaterfallSpec extends Specification {

  def insertData =
    DB.withConnection { implicit c =>
      SQL("INSERT INTO Distributor (name) values ('TestDistributor')").executeUpdate()
      SQL("INSERT INTO Property (name, distributor_id) values ('TestProperty', 1)").executeUpdate()
      SQL("INSERT INTO Waterfall (name, property_id) values ('TestWaterfall', 1)").executeUpdate()

      SQL("INSERT INTO AdProvider (name) values ('AdProvider 1')").executeUpdate()
      SQL("INSERT INTO AdProvider (name) values ('AdProvider 2')").executeUpdate()

      SQL("INSERT INTO WaterfallAdProvider (waterfall_id, ad_provider_id) values (1, 1)").executeUpdate()
      SQL("INSERT INTO WaterfallAdProvider (waterfall_id, ad_provider_id, waterfall_order) values (1, 2, 0)").executeUpdate()

    }

  abstract class WithFixtures extends WithApplication(FakeApplication(additionalConfiguration = inMemoryDatabase())) {

    override def around[T: AsResult](t: => T): Result = super.around {
      insertData
      t
    }
  }

  "Waterfall" should {

    "there should be One waterfall" in new WithFixtures {
      DB.withConnection { implicit c =>
        val rows = SQL("SELECT count(*) from Waterfall;").apply.map(r => r)
        val row = rows.head
        row.asMap.get(".COUNT(*)") must equalTo(Some(1))
      }
    }

    "should turn into JSON" in new WithFixtures {
      val waterfall = Waterfall(NotAssigned, "Name")
      Json.toJson(waterfall).toString must equalTo("{\"name\":\"Name\"}")
      val waterfall2 = Waterfall(Id(1.toLong), "Name2")
      Json.toJson(waterfall2).toString must equalTo("{\"id\":1,\"name\":\"Name2\"}")
    }

    "should fetch relationships" in new WithFixtures {
    	Waterfall.withWaterfallAdProviders(1) must
    		equalTo(Map(Waterfall(Id(1),"TestWaterfall") ->
    			List(WaterfallAdProvider(Id(1),1,1,None,None,None,None),
    				 WaterfallAdProvider(Id(2),1,2,Some(0),None,None,None)))
)
    }

  }
}
