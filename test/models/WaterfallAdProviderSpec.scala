package models

import anorm.SQL
import play.api.Play.current
import play.api.db.DB
import org.junit.runner._
import org.specs2.runner._
import play.api.libs.json.{JsString, JsObject, JsValue}
import play.api.Play.current
import play.api.test.Helpers._
import play.api.test.FakeApplication
import play.libs.Json

@RunWith(classOf[JUnitRunner])
class WaterfallAdProviderSpec extends SpecificationWithFixtures with JsonTesting {
  val currentApp = running(FakeApplication(additionalConfiguration = testDB)) {
    val distributorID = Distributor.create("New Company").get
    val distributor = Distributor.find(distributorID).get
    val id = App.create(distributorID, "New App").get
    App.find(id).get
  }

  val waterfall = running(FakeApplication(additionalConfiguration = testDB)) {
    VirtualCurrency.create(currentApp.id, "Coins", 100, None, None, Some(true))
    val waterfallID = DB.withTransaction { implicit connection => createWaterfallWithConfig(currentApp.id, "New App Waterfall") }
    Waterfall.find(waterfallID).get
  }

  val adProviderID1 = running(FakeApplication(additionalConfiguration = testDB)) {
    DB.withConnection { implicit connection =>
      SQL("insert into ad_providers (name, configuration_data) values ('test ad provider 1', cast({configuration_data} as json))").on("configuration_data" -> configurationData).executeInsert()
    }
  }

  val adProviderID2 = running(FakeApplication(additionalConfiguration = testDB)) {
    DB.withConnection { implicit connection =>
      SQL("insert into ad_providers (name) values ('test ad provider 2')").executeInsert()
    }
  }

  val waterfallAdProvider1 = running(FakeApplication(additionalConfiguration = testDB)) {
    val waterfallAdProviderID1 = WaterfallAdProvider.create(waterfall.id, adProviderID1.get, None, None, true, true).get
    WaterfallAdProvider.find(waterfallAdProviderID1).get
  }

  val waterfallAdProvider2 = running(FakeApplication(additionalConfiguration = testDB)) {
    val waterfallAdProviderID2 = WaterfallAdProvider.create(waterfall.id, adProviderID2.get, None, None, true, true).get
    WaterfallAdProvider.find(waterfallAdProviderID2).get
  }

  running(FakeApplication(additionalConfiguration = testDB)) {
    val updatedWaterfallAdProvider1 = new WaterfallAdProvider(waterfallAdProvider1.id, waterfall.id, adProviderID1.get, Some(1), None, Some(true), None, configurationJson, false)
    WaterfallAdProvider.update(updatedWaterfallAdProvider1)

    val updatedWaterfallAdProvider2 = new WaterfallAdProvider(waterfallAdProvider2.id, waterfall.id, adProviderID2.get, Some(1), None, Some(true), None, configurationJson, false)
    WaterfallAdProvider.update(updatedWaterfallAdProvider2)
  }

  "WaterfallAdProvider.create" should {
    "create a new record in the waterfall_ad_providers table" in new WithDB {
      waterfallAdProvider1 must beAnInstanceOf[WaterfallAdProvider]
    }

    "should not create a new record if another shares the same ad_provider_id and waterfall_id" in new WithDB {
      WaterfallAdProvider.create(waterfall.id, adProviderID1.get, None, None, true, true) must throwA[org.postgresql.util.PSQLException]
    }
  }

  "WaterfallAdProvider.update" should {
    "update the WaterfallAdProvider record in the database" in new WithDB {
      val newWaterfallOrder = Some(1.toLong)
      val updatedWaterfallAdProvider = new WaterfallAdProvider(waterfallAdProvider1.id, waterfall.id, adProviderID1.get, newWaterfallOrder, None, Some(true), None, configurationJson, false)
      WaterfallAdProvider.update(updatedWaterfallAdProvider)
      val retrievedWaterfallAdProvider = WaterfallAdProvider.find(waterfallAdProvider1.id).get
      retrievedWaterfallAdProvider.waterfallOrder.get must beEqualTo(newWaterfallOrder.get)
    }
  }

  "WaterfallAdProvider.findAllOrdered" should {
    "return a list of ad provider names ordered by an ascending waterfall_order number" in new WithDB {
      val currentOrder = WaterfallAdProvider.findAllOrdered(waterfall.id, true)
      currentOrder(0).waterfallOrder.get must beEqualTo(1)
    }
  }

  "WaterfallAdProvider.findAllReportingEnabled" should {
    "return a list of all active waterfall ad providers that have reporting enabled" in new WithDB {
      val wapReportingCount = WaterfallAdProvider.findAllReportingEnabled.size
      val updatedWaterfallAdProvider = new WaterfallAdProvider(waterfallAdProvider1.id, waterfall.id, adProviderID1.get, None, None, Some(true), None, configurationJson, true)
      WaterfallAdProvider.update(updatedWaterfallAdProvider)
      val reportingEnabledWAPs = WaterfallAdProvider.findAllReportingEnabled
      reportingEnabledWAPs.size must beEqualTo(wapReportingCount + 1)
      reportingEnabledWAPs.map( wap => wap.waterfallAdProviderID) must contain(updatedWaterfallAdProvider.id)
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

  "WaterfallAdProvider.findConfigurationData" should {
    "return an instance of WaterfallAdProviderConfig containing configuration data for both WaterfallAdProvider and AdProvider" in new WithDB {
      val configData = WaterfallAdProvider.findConfigurationData(waterfallAdProvider1.id).get
      val params = (configData.adProviderConfiguration \ "requiredParams").as[List[Map[String, String]]]
      params.map { param =>
        configurationParams must contain(param.get("key").get)
      }
      val waterfallAdProviderParams = configData.waterfallAdProviderConfiguration \ "requiredParams"
      (waterfallAdProviderParams \ configurationParams(0)).as[String] must beEqualTo(configurationValues(0))
    }
  }

  "WaterfallAdProviderConfig.mappedFields" should {
    "return a list of RequiredParam instances" in new WithDB{
      val configData = WaterfallAdProvider.findConfigurationData(waterfallAdProvider1.id).get
      val fields = configData.mappedFields("requiredParams")
      for(index <- (0 to fields.size-1)) {
        fields(index).key.get must beEqualTo(configurationParams(index))
        fields(index).value.get must beEqualTo(configurationValues(index))
      }
    }
  }

  "WaterfallAdProvider.findByAdProvider" should {
    "return the configuration data JSON if a record is found" in new WithDB {
      WaterfallAdProvider.findByAdProvider(currentApp.token, "test ad provider 1").get must haveClass[WaterfallAdProvider.WaterfallAdProviderCallbackInfo]
    }

    "return None if the configuration data does not exist" in new WithDB {
      WaterfallAdProvider.findByAdProvider(currentApp.token, "Some fake ad provider name") must beNone
    }
  }
  step(clean)
}
