package models

import org.specs2.runner._
import org.junit.runner._
import play.api.db.DB
import play.api.libs.json._
import resources.{AdProviderSpecSetup, WaterfallSpecSetup}

@RunWith(classOf[JUnitRunner])
class AppConfigSpec extends SpecificationWithFixtures with WaterfallSpecSetup with JsonConversion with AdProviderSpecSetup {
  "AppConfig.configurationDiffers" should {
    "return true if the old configuration does not match the new configuration" in new WithDB {
      val oldConfig = Json.parse(buildAdProviderConfig(Array(("appID", Some("appID1"), None, Some("true"))), Array(("APIKey", None, None, None)), Array(("APIKey", None, None, None))))
      val newConfig = Json.parse(buildAdProviderConfig(Array(("appID", Some("appID2"), None, Some("true"))), Array(("APIKey", None, None, None)), Array(("APIKey", None, None, None))))
      AppConfig.configurationDiffers(oldConfig, newConfig) must beTrue
    }

    "return false if the old configuration matches the new configuration" in new WithDB {
      val oldConfig = Json.parse(buildAdProviderConfig(Array(("appID", Some("appID1"), None, Some("true"))), Array(("APIKey", None, None, None)), Array(("APIKey", None, None, None))))
      val newConfig = Json.parse(buildAdProviderConfig(Array(("appID", Some("appID1"), None, Some("true"))), Array(("APIKey", None, None, None)), Array(("APIKey", None, None, None))))
      AppConfig.configurationDiffers(oldConfig, newConfig) must beFalse
    }
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

  "AppConfig.findLatest" should {
    "return the latest instance of AppConfig for a given App token" in new WithAppDB(distributor.id.get) {
      WaterfallAdProvider.create(currentWaterfall.id, adProviderID1.get, None, None, true, true)
      val originalGeneration = generationNumber(currentApp.id)
      clearGeneration(currentApp.id)
      val latestConfig = AppConfig.findLatest(currentApp.token).get
      latestConfig must haveClass[AppConfig]
      latestConfig.generationNumber must beEqualTo(originalGeneration + 1)
    }

    "return None if the App token is not found" in new WithDB {
      val fakeAppToken = "some-fake-token"
      AppConfig.findLatest(fakeAppToken) must beNone
    }
  }

  "AppConfig.responseV1" should {
    "return the ad provider configuration info" in new WithAppDB(distributor.id.get) {
      val wap1ID = WaterfallAdProvider.create(currentWaterfall.id, adProviderID1.get, None, None, true, true).get
      val wap1 = WaterfallAdProvider.find(wap1ID).get
      Waterfall.update(currentWaterfall.id, false, false)
      DB.withTransaction { implicit connection =>
        AppConfig.create(currentApp.id, currentApp.token, generationNumber(currentApp.id))
        val configs = (AppConfig.responseV1(currentApp.token) \ "adProviderConfigurations").as[JsArray]
        (configs(0) \ "providerID").as[JsNumber].toString.toLong must beEqualTo(wap1.adProviderID)
      }
    }

    "return an error message when no ad providers are found" in new WithDB {
      DB.withTransaction { implicit connection =>
        (AppConfig.responseV1("some-fake-app-token") \ "message").as[String] must beEqualTo("App Configuration not found.")
      }
    }

    "return the test mode response when the Waterfall is in test mode" in new WithAppDB(distributor.id.get) {
      DB.withTransaction { implicit connection =>
        AppConfig.responseV1(currentApp.token) must beEqualTo(AppConfig.testResponseV1)
      }
    }

    "return an error message indicating that no ad providers are active when all ad providers are deactivated" in new WithAppDB(distributor.id.get) {
      val wap1ID = WaterfallAdProvider.create(currentWaterfall.id, adProviderID1.get, None, None, true, false).get
      val wap1 = WaterfallAdProvider.find(wap1ID).get
      Waterfall.update(currentWaterfall.id, false, false)
      DB.withTransaction { implicit connection =>
        AppConfig.create(currentApp.id, currentApp.token, generationNumber(currentApp.id))
        (AppConfig.responseV1(currentApp.token) \ "message").as[String] must beEqualTo("At this time there are no ad providers that are both active and have an eCPM that meets the minimum reward threshold.")
      }
    }

    "return an error message indicating that no ad providers meet the reward threshold when all ad providers are below the minimum eCPM" in new WithDB {
      val (currentApp, currentWaterfall, _, _) = setUpApp(distributor.id.get, "New App", "Coins", exchangeRate = 25, rewardMin = 1, rewardMax = None, roundUp = false)
      val wap1ID = WaterfallAdProvider.create(currentWaterfall.id, adProviderID1.get, None, None, configurable = true, active = true).get
      WaterfallAdProvider.update(new WaterfallAdProvider(wap1ID, currentWaterfall.id, adProviderID1.get, waterfallOrder = Some(0), cpm = Some(25.0), active = Some(true), fillRate = None, configurationData = JsObject(Seq("requiredParams" -> JsObject(Seq()))), reportingActive = false))
      Waterfall.update(currentWaterfall.id, optimizedOrder = true, testMode = false)
      DB.withTransaction { implicit connection =>
        AppConfig.create(currentApp.id, currentApp.token, generationNumber(currentApp.id))
        (AppConfig.responseV1(currentApp.token) \ "message").as[String] must beEqualTo("At this time there are no ad providers that are both active and have an eCPM that meets the minimum reward threshold.")
      }
    }

    "return a response with testMode equal to true when a Waterfall is in test mode" in new WithAppDB(distributor.id.get) {
      currentAppConfig.configuration \ "testMode" must beEqualTo(JsBoolean(true))
    }
  }
}
