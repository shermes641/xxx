package models

import org.specs2.runner._
import org.junit.runner._
import play.api.db.DB
import play.api.libs.json.{JsArray, JsNumber, JsObject}
import play.api.test.Helpers._
import play.api.test._
import resources.WaterfallSpecSetup

@RunWith(classOf[JUnitRunner])
class AppConfigSpec extends SpecificationWithFixtures with WaterfallSpecSetup with JsonConversion {
  val virtualCurrency = running(FakeApplication(additionalConfiguration = testDB)) {
    VirtualCurrency.create(app1.id, "Coins", 100, None, None, Some(true))
  }

  "AppConfig.create" should {
    "store the proper app config response for a given waterfall ID" in new WithDB {
      val response = DB.withTransaction { implicit connection =>
        val generation = AppConfig.create(app1.id, app1.token, generationNumber(app1.id)).get
        AppConfig.responseV1(app1.token).as[JsObject].deepMerge(JsObject(Seq("generationNumber" -> JsNumber(generation))))
      }
      AppConfig.findLatest(app1.token).get.configuration must beEqualTo(response)
    }

    "increment the generation number for an existing waterfall ID each time the configuration has changed" in new WithDB {
      val originalGeneration = generationNumber(app1.id)
      Waterfall.update(waterfall.id, true, false)
      WaterfallAdProvider.create(waterfall.id, adProviderID1.get, None, Some(5.0), false, true)
      DB.withTransaction { implicit connection => AppConfig.create(app1.id, app1.token, generationNumber(app1.id)) }
      generationNumber(app1.id) must beEqualTo(originalGeneration + 1)
    }

    "not increment the generation number if the configuration has not changed" in new WithDB {
      val originalGeneration = generationNumber(app1.id)
      DB.withTransaction { implicit connection => AppConfig.create(app1.id, app1.token, generationNumber(app1.id)) }
      generationNumber(app1.id) must beEqualTo(originalGeneration)
    }

    "throw an IllegalArgumentException if the generation number passed as an argument does not match the latest generation number stored in the database" in new WithDB {
      val originalGeneration = generationNumber(app1.id)
      DB.withTransaction { implicit connection =>
        AppConfig.create(app1.id, app1.token, generationNumber(app1.id) - 1) must throwA[IllegalArgumentException]
      }
    }
  }

  "AppConfig.createWithWaterfallIDInTransaction" should {
    "create a record in the app_configs table when given a Waterfall ID" in new WithDB {
      clearGeneration(app1.id)
      val originalGeneration = generationNumber(app1.id)
      DB.withTransaction { implicit connection =>
        val newGeneration = AppConfig.createWithWaterfallIDInTransaction(waterfall.id, Some(originalGeneration)).get
        newGeneration must beEqualTo(originalGeneration + 1)
      }
      generationNumber(app1.id) must beEqualTo(originalGeneration + 1)
    }

    "not create a record if the generation number is old" in new WithDB {
      clearGeneration(app1.id)
      val originalGeneration = generationNumber(app1.id)
      DB.withTransaction { implicit connection =>
        AppConfig.createWithWaterfallIDInTransaction(waterfall.id, Some(originalGeneration - 1)) must throwA[IllegalArgumentException]
      }
      generationNumber(app1.id) must beEqualTo(originalGeneration)
    }

    "find the latest generation number if None is passed as an argument" in new WithDB {
      clearGeneration(app1.id)
      val originalGeneration = generationNumber(app1.id)
      DB.withTransaction { implicit connection =>
        val newGeneration = AppConfig.createWithWaterfallIDInTransaction(waterfall.id, None).get
        newGeneration must beEqualTo(originalGeneration + 1)
      }
      generationNumber(app1.id) must beEqualTo(originalGeneration + 1)
    }
  }

  "AppConfig.responseV1" should {
    "return the ad provider configuration info" in new WithDB {
      val appID = App.create(distributor.id.get, "New App").get
      val currentApp = App.find(appID).get
      VirtualCurrency.create(appID, "Coins", 100, None, None, Some(true))
      val currentWaterfallID = DB.withTransaction { implicit connection =>
        createWaterfallWithConfig(appID, "New Waterfall")
      }
      val wap1ID = WaterfallAdProvider.create(currentWaterfallID, adProviderID1.get, None, None, true, true).get
      val wap1 = WaterfallAdProvider.find(wap1ID).get
      Waterfall.update(currentWaterfallID, false, false)
      DB.withTransaction { implicit connection =>
        AppConfig.create(appID, currentApp.token, generationNumber(appID))
        val configs = (AppConfig.responseV1(currentApp.token) \ "adProviderConfigurations").as[JsArray]
        (configs(0) \ "providerID").as[JsNumber].toString.toLong must beEqualTo(wap1.adProviderID)
      }
    }

    "return an error message when no ad providers are found" in new WithDB {
      DB.withTransaction { implicit connection =>
        (AppConfig.responseV1("some-fake-app-token") \ "message").as[String] must beEqualTo("App Configuration not found.")
      }
    }

    "return the test mode response when the Waterfall is in test mode" in new WithDB {
      val appID = App.create(distributor.id.get, "New App").get
      val currentApp = App.find(appID).get
      VirtualCurrency.create(appID, "Coins", 100, None, None, Some(true))
      val currentWaterfallID = DB.withTransaction { implicit connection =>
        createWaterfallWithConfig(appID, "New Waterfall")
      }
      DB.withTransaction { implicit connection =>
        AppConfig.responseV1(currentApp.token) must beEqualTo(AppConfig.testResponseV1)
      }
    }

    "return an error message indicating that no ad providers are active or meet the reward threshold when all ad providers are either deactivated or below the minimum eCPM" in new WithDB {
      val appID = App.create(distributor.id.get, "New App").get
      val currentApp = App.find(appID).get
      VirtualCurrency.create(appID, "Coins", 100, None, None, Some(true))
      val currentWaterfallID = DB.withTransaction { implicit connection =>
        createWaterfallWithConfig(appID, "New Waterfall")
      }
      val wap1ID = WaterfallAdProvider.create(currentWaterfallID, adProviderID1.get, None, None, true, false).get
      val wap1 = WaterfallAdProvider.find(wap1ID).get
      Waterfall.update(currentWaterfallID, false, false)
      DB.withTransaction { implicit connection =>
        AppConfig.create(appID, currentApp.token, generationNumber(appID))
        (AppConfig.responseV1(currentApp.token) \ "message").as[String] must beEqualTo("At this time there are no ad providers that are both active and have an eCPM that meets the minimum reward threshold.")
      }
    }
  }
  step(clean)
}
