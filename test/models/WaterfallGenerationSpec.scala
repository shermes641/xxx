package models

import org.specs2.runner._
import org.junit.runner._
import play.api.test.Helpers._
import play.api.test._

@RunWith(classOf[JUnitRunner])
class WaterfallGenerationSpec extends SpecificationWithFixtures with WaterfallSpecSetup with JsonConversion {
  val virtualCurrency = running(FakeApplication(additionalConfiguration = testDB)) {
    VirtualCurrency.create(app1.id, "Coins", 100, None, None, Some(true))
  }

  "WaterfallGeneration.create" should {
    "store the proper waterfall response for a given waterfall ID" in new WithDB {
      WaterfallGeneration.create(waterfall.get.id, waterfall.get.token)
      WaterfallGeneration.findLatest(waterfall.get.token).get.configuration must beEqualTo(Waterfall.responseV1(waterfall.get.token))
    }

    "increment the generation number for an existing waterfall ID each time it is called" in new WithDB {
      val originalGeneration = generationNumber(waterfall.get.id)
      WaterfallGeneration.create(waterfall.get.id, waterfall.get.token)
      generationNumber(waterfall.get.id) must beEqualTo(originalGeneration + 1)
    }
  }
  step(clean)
}
