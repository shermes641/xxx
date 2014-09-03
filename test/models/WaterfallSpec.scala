package models

import org.junit.runner._
import org.specs2.runner._
import play.api.test.Helpers._
import play.api.test.FakeApplication

@RunWith(classOf[JUnitRunner])
class WaterfallSpec extends SpecificationWithFixtures {
  val distributor = running(FakeApplication(additionalConfiguration = testDB)) {
    val distributorID = Distributor.create("Company Name").get
    Distributor.find(distributorID).get
  }

  val app1 = running(FakeApplication(additionalConfiguration = testDB)) {
    val appID = App.create(distributor.id.get, "App 1").get
    App.find(appID).get
  }

  val waterfall = running(FakeApplication(additionalConfiguration = testDB)) {
    val waterfallID = Waterfall.create(app1.id, app1.name).get
    Waterfall.find(waterfallID)
  }

  "Waterfall.create" should {
    "add a new Waterfall record in the database" in new WithDB {
      val waterfallID = Waterfall.create(distributor.id.get, "Waterfall").get
      Waterfall.find(waterfallID).get must haveClass[Waterfall]
    }
  }

  "Waterfall.update" should {
    "update a Waterfall record in the database" in new WithDB {
      val newName = "Some new name"
      Waterfall.update(new Waterfall(waterfall.get.id, newName))
      Waterfall.find(waterfall.get.id).get.name must beEqualTo(newName)
    }
  }
  step(clean)
}
