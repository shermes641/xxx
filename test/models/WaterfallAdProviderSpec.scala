package models

import anorm.SQL
import play.api.Play.current
import play.api.db.DB
import org.junit.runner._
import org.specs2.runner._
import play.api.libs.json.{JsString, JsObject, JsValue}
import play.api.test.Helpers._
import play.api.test.FakeApplication
import play.libs.Json

@RunWith(classOf[JUnitRunner])
class WaterfallAdProviderSpec extends SpecificationWithFixtures {
  val waterfall = running(FakeApplication(additionalConfiguration = testDB)) {
    val distributorID = Distributor.create("New Company").get
    val distributor = Distributor.find(distributorID).get
    val appID = App.create(distributorID, "New App").get
    val waterfallID = Waterfall.create(appID, "New App Waterfall").get
    Waterfall.find(waterfallID).get
  }

  val configurationParams = List("key1", "key2")
  val configurationValues = List("value1", "value2")
  val configurationData = "{\"required_params\": [\"" + configurationParams(0) + "\", \"" + configurationParams(1) + "\"]}"

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
    val waterfallAdProviderID1 = WaterfallAdProvider.create(waterfall.id, adProviderID1.get).get
    WaterfallAdProvider.find(waterfallAdProviderID1).get
  }

  val waterfallAdProvider2 = running(FakeApplication(additionalConfiguration = testDB)) {
    val waterfallAdProviderID2 = WaterfallAdProvider.create(waterfall.id, adProviderID2.get).get
    WaterfallAdProvider.find(waterfallAdProviderID2).get
  }

  running(FakeApplication(additionalConfiguration = testDB)) {
    val updatedWaterfallAdProvider1 = new WaterfallAdProvider(waterfallAdProvider1.id, waterfall.id, adProviderID1.get, Some(1), None, Some(true), None, JsObject(Seq(configurationParams(0) -> JsString(configurationValues(0)))))
    WaterfallAdProvider.update(updatedWaterfallAdProvider1)

    val updatedWaterfallAdProvider2 = new WaterfallAdProvider(waterfallAdProvider2.id, waterfall.id, adProviderID2.get, Some(1), None, Some(true), None, JsObject(Seq(configurationParams(1) -> JsString(configurationValues(1)))))
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
      val updatedWaterfallAdProvider = new WaterfallAdProvider(waterfallAdProvider1.id, waterfall.id, adProviderID1.get, newWaterfallOrder, None, Some(true), None, JsObject(Seq(configurationParams(0) -> JsString(configurationValues(0)))))
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

  "WaterfallAdProvider.createFromAdProviderList" should {
    "return true when all inserts are successful" in new WithDB {
      val adProviderID = {
        DB.withConnection { implicit connection =>
          SQL("insert into ad_providers (name) values ('test ad provider 1')").executeInsert()
        }
      }
      val adProviderList = List(adProviderID.get.toString())
      WaterfallAdProvider.createFromAdProviderList(waterfall.id, adProviderList) must beEqualTo(true)
    }

    "return false when an insert fails" in new WithDB {
      val unknownAdProviderIDs = List("10", "11", "12")
      WaterfallAdProvider.createFromAdProviderList(waterfall.id, unknownAdProviderIDs) must beEqualTo(false)
    }
  }

  "WaterfallAdProvider.findConfigurationData" should {
    "return an instance of WaterfallAdProviderConfig containing configuration data for both WaterfallAdProvider and AdProvider" in new WithDB {
      val configData = WaterfallAdProvider.findConfigurationData(waterfallAdProvider1.id).get
      val params = (configData.adProviderConfiguration \ "required_params").as[List[String]]
      params.map { param =>
        configurationParams must contain(param)
      }
      (configData.waterfallAdProviderConfiguration \ configurationParams(0)).as[String] must beEqualTo(configurationValues(0))
    }
  }

  "WaterfallAdProviderConfig.mappedFields" should {
    "return a list of tuples where the first element is the name of the required key and the second element is the value for that key" in new WithDB{
      val configData = WaterfallAdProvider.findConfigurationData(waterfallAdProvider1.id).get
      val fields = configData.mappedFields
      fields(0) must beEqualTo((configurationParams(0), configurationValues(0)))
      fields(1) must beEqualTo((configurationParams(1), ""))
    }
  }
  step(clean)
}
