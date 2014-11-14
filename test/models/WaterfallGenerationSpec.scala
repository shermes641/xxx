package models

import org.specs2.runner._
import org.junit.runner._
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test._
import anorm.SQL
import play.api.db.DB
import play.api.Play.current

@RunWith(classOf[JUnitRunner])
class WaterfallGenerationSpec extends SpecificationWithFixtures with WaterfallSpecSetup with JsonConversion {
  val virtualCurrency = running(FakeApplication(additionalConfiguration = testDB)) {
    VirtualCurrency.create(app1.id, "Coins", 100, None, None, Some(true))
  }

  /**
   * Helper function to check the latest stored configuration data for a given waterfall ID.
   * @param waterfallID The ID of the Waterfall for which to check the configuration data.
   * @return The JSON object containing configuration data.
   */
  def latestGenerationConfig(waterfallID: Long): JsValue = {
    (DB.withConnection { implicit connection =>
      SQL("""SELECT configuration FROM waterfall_generations where waterfall_id={waterfall_id} order by generation_number DESC LIMIT 1""").on("waterfall_id" -> waterfallID).apply()
    }.head)[JsValue]("configuration")
  }

  "WaterfallGeneration.create" should {
    "store the proper waterfall response for a given waterfall ID" in new WithDB {
      WaterfallGeneration.create(waterfall.get.id, waterfall.get.token)
      latestGenerationConfig(waterfall.get.id) must beEqualTo(JsonBuilder.waterfallResponse(Waterfall.order(waterfall.get.token)))
    }

    "increment the generation number for an existing waterfall ID each time it is called" in new WithDB {
      val originalGeneration = generationNumber(waterfall.get.id)
      WaterfallGeneration.create(waterfall.get.id, waterfall.get.token)
      generationNumber(waterfall.get.id) must beEqualTo(originalGeneration + 1)
    }
  }
  step(clean)
}
