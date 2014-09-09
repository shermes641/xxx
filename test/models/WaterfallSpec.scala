package models

import org.junit.runner._
import org.specs2.runner._
import play.api.test.Helpers._
import play.api.test.FakeApplication
import play.api.db.DB
import anorm.SQL
import play.api.Play.current

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

  val adProviderID1 = running(FakeApplication(additionalConfiguration = testDB)) {
    DB.withConnection { implicit connection =>
      SQL("insert into ad_providers (name) values ('test ad provider 1')").executeInsert()
    }
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

  "Waterfall.reconfigureAdProviders" should {
    "create a new WaterfallAdProvider if one doesn't exist and ConfigInfo is a new record" in new WithDB {
      WaterfallAdProvider.findAllByWaterfallID(waterfall.get.id).size must beEqualTo(0)
      val configList: List[controllers.ConfigInfo] = List(new controllers.ConfigInfo(adProviderID1.get, true, true, 0))
      Waterfall.reconfigureAdProviders(waterfall.get.id, configList)
      WaterfallAdProvider.findAllByWaterfallID(waterfall.get.id).size must beEqualTo(1)
    }

    "should not create a new WaterfallAdProvider if one doesn't exist and ConfigInfo is not active" in new WithDB {
      val currentWaterfallID = Waterfall.create(distributor.id.get, "Waterfall").get
      WaterfallAdProvider.findAllByWaterfallID(currentWaterfallID).size must beEqualTo(0)
      val configList: List[controllers.ConfigInfo] = List(new controllers.ConfigInfo(adProviderID1.get, false, true, 0))
      Waterfall.reconfigureAdProviders(currentWaterfallID, configList)
      WaterfallAdProvider.findAllByWaterfallID(currentWaterfallID).size must beEqualTo(0)
    }

    "should set the waterfallOrder property on WaterfallAdProviders correctly" in new WithDB {
      val currentWaterfallID = Waterfall.create(distributor.id.get, "Waterfall").get
      val waterfallOrder = 0
      val configList: List[controllers.ConfigInfo] = List(new controllers.ConfigInfo(adProviderID1.get, true, true, waterfallOrder))
      Waterfall.reconfigureAdProviders(currentWaterfallID, configList)
      WaterfallAdProvider.findAllByWaterfallID(currentWaterfallID)(0).waterfallOrder.get must beEqualTo(waterfallOrder)
    }

    "should deactivate a WaterfallAdProvider correctly" in new WithDB {
      val currentWaterfallID = Waterfall.create(distributor.id.get, "Waterfall").get
      val wapID = WaterfallAdProvider.create(currentWaterfallID, adProviderID1.get, Some(0)).get
      WaterfallAdProvider.findAllOrdered(currentWaterfallID, true)(0).waterfallAdProviderID must beEqualTo(wapID)
      val configList: List[controllers.ConfigInfo] = List(new controllers.ConfigInfo(wapID, false, false, 0))
      Waterfall.reconfigureAdProviders(currentWaterfallID, configList)
      WaterfallAdProvider.findAllOrdered(currentWaterfallID, true).size must beEqualTo(0)
    }

    "should return false if the update is not successful" in new WithDB {
      val currentWaterfallID = Waterfall.create(distributor.id.get, "Waterfall").get
      val someFakeID = 100
      val configList: List[controllers.ConfigInfo] = List(new controllers.ConfigInfo(someFakeID, false, false, 0))
      Waterfall.reconfigureAdProviders(currentWaterfallID, configList) must beEqualTo(false)
    }
  }
  step(clean)
}
