package models

import anorm._
import org.junit.runner._
import org.specs2.runner._
import play.api.db.DB
import play.api.libs.json._

@RunWith(classOf[JUnitRunner])
class WaterfallSpec extends SpecificationWithFixtures {
  /*
  "Waterfall" should {
    "there should be One waterfall" in new WithDB  {
      DB.withConnection { implicit c =>
        val rows = SQL("SELECT count(*) from waterfalls;").apply.map(r => r)
        val row = rows.head
        row.asMap.get(".COUNT(*)") must equalTo(Some(1))
      }
    }

    "should turn into JSON" in new WithDB  {
      val waterfall = Waterfall(NotAssigned, "Name")
      Json.toJson(waterfall).toString must equalTo("{\"name\":\"Name\"}")
      val waterfall2 = Waterfall(Id(1.toLong), "Name2")
      Json.toJson(waterfall2).toString must equalTo("{\"id\":1,\"name\":\"Name2\"}")
    }

    "should fetch relationships" in new WithDB  {
      Waterfall.withWaterfallAdProviders(1) must
        equalTo(Map(Waterfall(Id(1),"TestWaterfall") ->
          List(WaterfallAdProvider(Id(1),1,1,None,None,None,None),
          WaterfallAdProvider(Id(2),1,2,Some(0),None,None,None)))
        )
    }
  }
  */
}
