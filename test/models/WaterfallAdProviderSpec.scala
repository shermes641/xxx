package models

import anorm.SQL
import play.api.Play.current
import play.api.db.DB
import org.junit.runner._
import org.specs2.runner._
import play.api.test.Helpers._
import play.api.test.FakeApplication

@RunWith(classOf[JUnitRunner])
class WaterfallAdProviderSpec extends SpecificationWithFixtures {
  val waterfall = running(FakeApplication(additionalConfiguration = testDB)) {
    val distributorID = Distributor.create("New Company").get
    val distributor = Distributor.find(distributorID).get
    val appID = App.create(distributorID, "New App").get
    val waterfallID = Waterfall.create(appID, "New App Waterfall").get
    Waterfall.find(waterfallID).get
  }

  val adProviderID1 = running(FakeApplication(additionalConfiguration = testDB)) {
    DB.withConnection { implicit connection =>
      SQL("insert into ad_providers (name) values ('test ad provider 1')").executeInsert()
    }
  }

  val adProviderID2 = running(FakeApplication(additionalConfiguration = testDB)) {
    DB.withConnection { implicit connection =>
      SQL("insert into ad_providers (name) values ('test ad provider 2')").executeInsert()
    }
  }

  val waterfallAdProvider1 = running(FakeApplication(additionalConfiguration = testDB)) {
    val waterfallAdProviderID1 = WaterfallAdProvider.create(waterfall.id, adProviderID1.get).get
    WaterfallAdProvider.find(waterfallAdProviderID1).get
  }

  val waterfallAdProvider2 = running(FakeApplication(additionalConfiguration = testDB)) {
    val waterfallAdProviderID2 = WaterfallAdProvider.create(waterfall.id, adProviderID2.get).get
    WaterfallAdProvider.find(waterfallAdProviderID2).get
  }

  running(FakeApplication(additionalConfiguration = testDB)) {
    val updatedWaterfallAdProvider1 = new WaterfallAdProvider(waterfallAdProvider1.id, waterfall.id, adProviderID1.get, Some(1), None, Some(true), None)
    WaterfallAdProvider.update(updatedWaterfallAdProvider1)

    val updatedWaterfallAdProvider2 = new WaterfallAdProvider(waterfallAdProvider2.id, waterfall.id, adProviderID2.get, Some(1), None, Some(true), None)
    WaterfallAdProvider.update(updatedWaterfallAdProvider2)
  }

  "WaterfallAdProvider.create" should {
    "create a new record in the waterfall_ad_providers table" in new WithDB {
      waterfallAdProvider1 must beAnInstanceOf[WaterfallAdProvider]
    }

    "should not create a new record if another shares the same ad_provider_id and waterfall_id" in new WithDB {
      WaterfallAdProvider.create(waterfall.id, adProviderID1.get) must beNone
    }
  }

  "WaterfallAdProvider.update" should {
    "update the WaterfallAdProvider record in the database" in new WithDB {
      val newWaterfallOrder = Some(1.toLong)
      val updatedWaterfallAdProvider = new WaterfallAdProvider(waterfallAdProvider1.id, waterfall.id, adProviderID1.get, newWaterfallOrder, None, Some(true), None)
      WaterfallAdProvider.update(updatedWaterfallAdProvider)
      val retrievedWaterfallAdProvider = WaterfallAdProvider.find(waterfallAdProvider1.id).get
      retrievedWaterfallAdProvider.waterfallOrder.get must beEqualTo(newWaterfallOrder.get)
    }
  }

  "WaterfallAdProvider.currentOrder" should {
    "return a list of ad provider names ordered by an ascending waterfall_order number" in new WithDB {
      val currentOrder = WaterfallAdProvider.currentOrder(waterfall.id)
      currentOrder(0).waterfallOrder.get must beEqualTo(1)
    }
  }

  "WaterfallAdProvider.udpateWaterfallOrder" should {
    "update the waterfallOrder number for each ID in the list" in new WithDB {
      running(FakeApplication(additionalConfiguration = testDB)) {
        val updatableList = List(waterfallAdProvider2.id.toString(), waterfallAdProvider1.id.toString())
        WaterfallAdProvider.updateWaterfallOrder(updatableList)
        WaterfallAdProvider.find(waterfallAdProvider2.id).get.waterfallOrder.get must beEqualTo(1)
        WaterfallAdProvider.find(waterfallAdProvider1.id).get.waterfallOrder.get must beEqualTo(2)
      }
    }

    "return false when the update is unsuccessful" in new WithDB {
      val listWithFakeIDs = List("10", "11", "12")
      WaterfallAdProvider.updateWaterfallOrder(listWithFakeIDs) must beEqualTo(false)
    }
  }
  step(clean)
}
