package models

import play.api.db.DB
import models.Waterfall.WaterfallCallbackInfo
import org.junit.runner._
import play.api.libs.json.{JsObject}
import resources.WaterfallSpecSetup
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class WaterfallSpec extends SpecificationWithFixtures with WaterfallSpecSetup {
  "Waterfall.create" should {
    "add a new Waterfall record in the database" in new WithDB {
      val appID = App.create(distributor.id.get, "App 1").get
      VirtualCurrency.create(appID, "Gold", 10, 1, None, Some(true)).get
      val waterfallID = DB.withTransaction { implicit connection => createWaterfallWithConfig(appID, "Waterfall") }
      Waterfall.find(waterfallID, distributor.id.get).get must haveClass[Waterfall]
    }
  }

  "Waterfall.update" should {
    "update a Waterfall record in the database" in new WithDB {
      val optimizedOrder = false
      val testMode = true
      Waterfall.update(waterfall.id, optimizedOrder, testMode)
      val currentWaterfall = Waterfall.find(waterfall.id, distributor.id.get).get
      currentWaterfall.optimizedOrder must beEqualTo(optimizedOrder)
      currentWaterfall.testMode must beEqualTo(testMode)
    }
  }

  "Waterfall.findByAppID" should {
    "return a list of Waterfalls if the app ID is found" in new WithDB {
      val appID = App.create(distributor.id.get, "App 1").get
      VirtualCurrency.create(appID, "Gold", 10, 1, None, Some(true)).get
      val waterfallID = DB.withTransaction { implicit connection => createWaterfallWithConfig(appID, "Waterfall") }
      Waterfall.findByAppID(appID)(0).id must beEqualTo(waterfallID)
    }

    "return an empty list if the app ID is not found" in new WithDB {
      val unknownID = 0
      Waterfall.findByAppID(unknownID) must beEqualTo(List())
    }
  }

  "Waterfall.find" should {
    "return an instance of the Waterfall class if the ID is found" in new WithDB {
      val appID = App.create(distributor.id.get, "App 3").get
      VirtualCurrency.create(appID, "Gold", 10, 1, None, Some(true)).get
      val waterfallID = DB.withTransaction { implicit connection => createWaterfallWithConfig(appID, "Waterfall") }
      Waterfall.find(waterfallID, distributor.id.get).get must haveClass[Waterfall]
    }

    "return None if the Waterfall ID is not found" in new WithDB {
      val unknownWaterfallID = 0
      Waterfall.find(unknownWaterfallID, distributor.id.get) must beNone
    }
  }

  "Waterfall.reconfigureAdProviders" should {
    "create a new WaterfallAdProvider if one doesn't exist and ConfigInfo is a new record" in new WithDB {
      WaterfallAdProvider.findAllByWaterfallID(waterfall.id).size must beEqualTo(0)
      val configList: List[controllers.ConfigInfo] = List(new controllers.ConfigInfo(adProviderID1.get, true, true, 0, None, true, false))
      DB.withTransaction { implicit connection => Waterfall.reconfigureAdProviders(waterfall.id, configList) }
      WaterfallAdProvider.findAllByWaterfallID(waterfall.id).size must beEqualTo(1)
    }

    "should not create a new WaterfallAdProvider if one doesn't exist and ConfigInfo is not active" in new WithDB {
      val appID = App.create(distributor.id.get, "App 1").get
      VirtualCurrency.create(appID, "Gold", 10, 1, None, Some(true)).get
      val currentWaterfallID = DB.withTransaction { implicit connection => Waterfall.create(appID, "Waterfall") }.get
      WaterfallAdProvider.findAllByWaterfallID(currentWaterfallID).size must beEqualTo(0)
      val configList: List[controllers.ConfigInfo] = List(new controllers.ConfigInfo(adProviderID1.get, false, true, 0, None, true, false))
      DB.withTransaction { implicit connection => Waterfall.reconfigureAdProviders(currentWaterfallID, configList) }
      WaterfallAdProvider.findAllByWaterfallID(currentWaterfallID).size must beEqualTo(0)
    }

    "should set the waterfallOrder property on WaterfallAdProviders correctly" in new WithDB {
      val appID = App.create(distributor.id.get, "App 1").get
      VirtualCurrency.create(appID, "Gold", 10, 1, None, Some(true)).get
      val currentWaterfallID = DB.withTransaction { implicit connection => Waterfall.create(appID, "Waterfall") }.get
      val waterfallOrder = 0
      val configList: List[controllers.ConfigInfo] = List(new controllers.ConfigInfo(adProviderID1.get, true, true, waterfallOrder, None, true, false))
      DB.withTransaction { implicit connection => Waterfall.reconfigureAdProviders(currentWaterfallID, configList) }
      WaterfallAdProvider.findAllByWaterfallID(currentWaterfallID)(0).waterfallOrder.get must beEqualTo(waterfallOrder)
    }

    "should deactivate a WaterfallAdProvider correctly" in new WithDB {
      val appID = App.create(distributor.id.get, "App 1").get
      VirtualCurrency.create(appID, "Gold", 10, 1, None, Some(true)).get
      val currentWaterfallID = DB.withTransaction { implicit connection => Waterfall.create(appID, "Waterfall") }.get
      val wapID = WaterfallAdProvider.create(currentWaterfallID, adProviderID1.get, Some(0), None, true, true).get
      val wap = WaterfallAdProvider.find(wapID).get
      WaterfallAdProvider.update(new WaterfallAdProvider(wapID, wap.waterfallID, wap.adProviderID, None, None, Some(true), None, JsObject(Seq("requiredParams" -> JsObject(Seq()))), false))
      WaterfallAdProvider.findAllOrdered(currentWaterfallID).filter(_.active)(0).waterfallAdProviderID must beEqualTo(wapID)
      val configList: List[controllers.ConfigInfo] = List(new controllers.ConfigInfo(wapID, false, false, 0, None, true, false))
      DB.withTransaction { implicit connection => Waterfall.reconfigureAdProviders(currentWaterfallID, configList) }
      WaterfallAdProvider.findAllOrdered(currentWaterfallID).count(_.active) must beEqualTo(0)
    }

    "should return false if the update is not successful" in new WithDB {
      val appID = App.create(distributor.id.get, "App 1").get
      VirtualCurrency.create(appID, "Gold", 10, 1, None, Some(true)).get
      val currentWaterfallID = DB.withTransaction { implicit connection => Waterfall.create(appID, "Waterfall") }.get
      val someFakeID = 100
      val configList: List[controllers.ConfigInfo] = List(new controllers.ConfigInfo(someFakeID, false, false, 0, None, true, false))
      DB.withTransaction { implicit connection => Waterfall.reconfigureAdProviders(currentWaterfallID, configList) must beEqualTo(false) }
    }
  }

  "Waterfall.order" should {
    "return a list of ordered ad providers with configuration information if there are active waterfall ad providers" in new WithDB {
      val appID = App.create(distributor.id.get, "App 1").get
      VirtualCurrency.create(appID, "Gold", 10, 1, None, Some(true)).get
      val currentWaterfallID = DB.withTransaction { implicit connection => Waterfall.create(appID, "Waterfall") }.get
      WaterfallAdProvider.create(currentWaterfallID, adProviderID1.get, Some(0), None, true, true)
      val appToken = App.find(appID).get.token
      val waterfallOrder = DB.withTransaction { implicit connection => Waterfall.order(appToken) }
      waterfallOrder.size must beEqualTo(1)
    }

    "return an empty list if no ad providers are active" in new WithDB {
      val waterfallOrder = DB.withTransaction { implicit connection => Waterfall.order("some-fake-token") }
      waterfallOrder.size must beEqualTo(0)
    }
  }

  "Waterfall.findCallbackInfo" should {
    "return an instance of WaterfallCallbackInfo if the token is found" in new WithDB {
      Waterfall.findCallbackInfo(app1.token).get must haveClass[WaterfallCallbackInfo]
    }

    "return None if the token is not found" in new WithDB {
      Waterfall.findCallbackInfo("some fake token") must beNone
    }
  }
}
