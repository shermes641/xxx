package models

import anorm.SQL
import org.junit.runner._
import org.specs2.runner._
import play.api.db.DB
import play.api.libs.json._
import play.api.Play.current
import play.api.test.Helpers._
import play.api.test.FakeApplication
import resources.JsonTesting

@RunWith(classOf[JUnitRunner])
class WaterfallAdProviderSpec extends SpecificationWithFixtures with JsonTesting with WaterfallCreationHelper {
  val adProviderID1 = running(FakeApplication(additionalConfiguration = testDB)) {
    DB.withConnection { implicit connection =>
      AdProvider.create(name = "test ad provider 1", configurationData = configurationData, platformID = Platform.Ios.PlatformID, callbackUrlFormat = None)
    }
  }

  val adProviderID2 = running(FakeApplication(additionalConfiguration = testDB)) {
    DB.withConnection { implicit connection =>
      AdProvider.create(name = "test ad provider 2", configurationData = configurationData, platformID = Platform.Ios.PlatformID, callbackUrlFormat = None)
    }
  }

  val currentApp = running(FakeApplication(additionalConfiguration = testDB)) {
    val distributorID = Distributor.create("New Company").get
    val distributor = Distributor.find(distributorID).get
    val id = App.create(distributorID, "New App", Platform.Ios.PlatformID).get
    App.find(id).get
  }

  val waterfall = running(FakeApplication(additionalConfiguration = testDB)) {
    VirtualCurrency.create(currentApp.id, "Coins", 100, 1, None, Some(true))
    val waterfallID = DB.withTransaction { implicit connection => createWaterfallWithConfig(currentApp.id, "New App Waterfall") }
    Waterfall.find(waterfallID, currentApp.distributorID).get
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

    val updatedWaterfallAdProvider2 = new WaterfallAdProvider(waterfallAdProvider2.id, waterfall.id, adProviderID2.get, Some(2), None, Some(true), None, configurationJson, false)
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
      WaterfallAdProvider.update(updatedWaterfallAdProvider) must beEqualTo(1)
      val retrievedWaterfallAdProvider = WaterfallAdProvider.find(waterfallAdProvider1.id).get
      retrievedWaterfallAdProvider.waterfallOrder.get must beEqualTo(newWaterfallOrder.get)
    }

    "return 0 if the WaterfallAdProvider is not update successfully" in new WithDB {
      val unknownID = 0
      val updatedWaterfallAdProvider = new WaterfallAdProvider(unknownID, waterfall.id, adProviderID1.get, Some(1), None, Some(true), None, configurationJson, false)
      WaterfallAdProvider.update(updatedWaterfallAdProvider) must beEqualTo(0)
    }
  }

  "WaterfallAdProvider.find" should {
    "return an instance of the WaterfallAdProvider class if a record is found" in new WithDB {
      WaterfallAdProvider.find(waterfallAdProvider1.id).get must haveClass[WaterfallAdProvider]
    }

    "return None if a WaterfallAdProvider could not be found" in new WithDB {
      val unknownID = 0
      WaterfallAdProvider.find(unknownID) must beNone
    }
  }

  "WaterfallAdProvider.findAllByWaterfallID" should {
    "return a list of WaterfallAdProvider instances if the Waterfall ID is found" in new WithDB {
      val waterfallAdProviders = WaterfallAdProvider.findAllByWaterfallID(waterfall.id)
      waterfallAdProviders.size must beEqualTo(2)
      waterfallAdProviders.map { wap => wap must haveClass[WaterfallAdProvider] }
    }

    "return an empty list if the Waterfall ID is not found" in new WithDB {
      val unknownID = 0
      WaterfallAdProvider.findAllByWaterfallID(unknownID).size must beEqualTo(0)
    }
  }

  "WaterfallAdProvider.findAllOrdered" should {
    "return a list of WaterfallAdProvider instances ordered by waterfallOrder ascending" in new WithDB {
      val currentOrder = WaterfallAdProvider.findAllOrdered(waterfall.id)
      currentOrder.size must beEqualTo(2)
      currentOrder(0).waterfallOrder.get must beEqualTo(1)
      currentOrder(1).waterfallOrder.get must beEqualTo(2)
    }
  }

  "WaterfallAdProvider.findAllReportingEnabled" should {
    "return a list of all waterfall ad providers that have reporting enabled" in new WithDB {
      def verifyReportingWAPs = {
        val reportingEnabledWAPs = WaterfallAdProvider.findAllReportingEnabled
        reportingEnabledWAPs.size must beEqualTo(originalWAPReportingCount + 1)
        reportingEnabledWAPs.map(wap => wap.waterfallAdProviderID) must contain(updatedActiveWaterfallAdProvider.id)
      }

      val originalWAPReportingCount = WaterfallAdProvider.findAllReportingEnabled.size
      val updatedActiveWaterfallAdProvider = new WaterfallAdProvider(waterfallAdProvider1.id, waterfall.id, adProviderID1.get, waterfallOrder = None,
        cpm = None, active = Some(true), fillRate = None, configurationData = configurationJson, reportingActive = true)
      WaterfallAdProvider.update(updatedActiveWaterfallAdProvider)

      verifyReportingWAPs

      val updatedInActiveWaterfallAdProvider = new WaterfallAdProvider(waterfallAdProvider1.id, waterfall.id, adProviderID1.get, waterfallOrder = None,
        cpm = None, active = Some(false), fillRate = None, configurationData = configurationJson, reportingActive = true)
      WaterfallAdProvider.update(updatedInActiveWaterfallAdProvider)

      verifyReportingWAPs
    }
  }

  "WaterfallAdProvider.findConfigurationData" should {
    "return an instance of WaterfallAdProviderConfig containing configuration data for both WaterfallAdProvider and AdProvider" in new WithDB {
      val configData = DB.withConnection { implicit connection => WaterfallAdProvider.findConfigurationData(waterfallAdProvider1.id).get }
      val params = (configData.adProviderConfiguration \ "requiredParams").as[List[JsValue]]
      params.map { param => configurationParams must contain((param \ "key").as[String]) }
      val waterfallAdProviderParams = configData.waterfallAdProviderConfiguration \ "requiredParams"
      (waterfallAdProviderParams \ configurationParams(0)).as[String] must beEqualTo(configurationValues(0))
    }
  }

  "WaterfallAdProviderConfig.mappedFields" should {
    "return a list of RequiredParam instances" in new WithDB {
      val configData = DB.withConnection { implicit connection => WaterfallAdProvider.findConfigurationData(waterfallAdProvider1.id).get }
      val fields = configData.mappedFields("requiredParams")
      for(index <- (0 to fields.size-1)) {
        fields(index).key.get must beEqualTo(configurationParams(index))
        fields(index).value.get must beEqualTo(configurationValues(index))
      }
    }

    "convert WaterfallAdProvider configuration param values to Strings if they are not already" in new WithDB {
      val newAppID = App.create(currentApp.distributorID, "New Test App", Platform.Ios.PlatformID).get
      VirtualCurrency.create(newAppID, "Coins", exchangeRate = 100, rewardMin = 1, rewardMax = None, roundUp = Some(true))
      val newWaterfall = {
        val id = DB.withTransaction { implicit connection => createWaterfallWithConfig(newAppID, "New Waterfall") }
        Waterfall.find(id, currentApp.distributorID).get
      }
      val paramType = "requiredParams"
      val configData = JsObject(
        Seq(
           paramType -> JsObject(
            Seq(
              configurationParams(0) -> JsNumber(5),
              configurationParams(1) -> JsString("some string")
            )
          )
        )
      )
      val wapConfig = {
        val id = WaterfallAdProvider.create(newWaterfall.id, adProviderID1.get, waterfallOrder = None, cpm = None, configurable = true, active = true, pending = false).get
        val wap = WaterfallAdProvider.find(id).get
        WaterfallAdProvider.update(new WaterfallAdProvider(id, newWaterfall.id, adProviderID1.get, waterfallOrder = None, cpm = None, active = Some(true), fillRate = None, configurationData = configData,reportingActive = true, pending = false))
        DB.withTransaction { implicit connection => WaterfallAdProvider.findConfigurationData(id).get }
      }
      val fields = wapConfig.mappedFields(paramType)
      for(index <- (0 to fields.size-1)) {
        fields(index).value.get must haveClass[String]
      }
    }
  }

  "WaterfallAdProvider.findRewardInfo" should {
    "return the configuration data JSON if a record is found" in new WithDB {
      WaterfallAdProvider.findRewardInfo(currentApp.token, "test ad provider 1").get must haveClass[WaterfallAdProvider.AdProviderRewardInfo]
    }

    "return None if the configuration data does not exist" in new WithDB {
      WaterfallAdProvider.findRewardInfo(currentApp.token, "Some fake ad provider name") must beNone
    }
  }

  "WaterfallAdProvider.updateHyprMarketplaceConfig" should {
    val distributionChannelID: Long = 12345

    "set the appropriate JSON configuration for the HyprMarketplace WaterfallAdProvider" in new WithDB {
      DB.withTransaction { implicit connection => WaterfallAdProvider.updateHyprMarketplaceConfig(waterfallAdProvider1, distributionChannelID, currentApp.token, currentApp.name) }
      val updatedWap = WaterfallAdProvider.find(waterfallAdProvider1.id).get
      val hyprDistributionChannelID = (updatedWap.configurationData \ "requiredParams" \ "distributorID").as[String]
      val hyprPropertyID = (updatedWap.configurationData \ "requiredParams" \ "propertyID").as[String]
      hyprDistributionChannelID must beEqualTo(distributionChannelID.toString)
      hyprPropertyID must beEqualTo(currentApp.name)
    }

    "set the reporting JSON configuration for the HyprMarketplace WaterfallAdProvider" in new WithDB {
      DB.withTransaction { implicit connection => WaterfallAdProvider.updateHyprMarketplaceConfig(waterfallAdProvider1, distributionChannelID, currentApp.token, currentApp.name) }
      val updatedWap = WaterfallAdProvider.find(waterfallAdProvider1.id).get
      val reportingParams = (updatedWap.configurationData \ "reportingParams")
      val apiKey = (reportingParams \ "APIKey").as[String]
      val placementID = (reportingParams \ "placementID").as[String]
      val appID = (reportingParams \ "appID").as[String]
      apiKey must beEqualTo(currentApp.token)
      placementID must beEqualTo(currentApp.token)
      appID must beEqualTo(distributionChannelID.toString)
    }

    "set the pending attribute to false" in new WithDB {
      WaterfallAdProvider.update(new WaterfallAdProvider(waterfallAdProvider1.id, waterfallAdProvider1.waterfallID, waterfallAdProvider1.adProviderID, waterfallAdProvider1.waterfallOrder, Some(5.0), Some(false), None, JsObject(Seq()), false, true))
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.pending must beTrue
      DB.withTransaction { implicit connection => WaterfallAdProvider.updateHyprMarketplaceConfig(waterfallAdProvider1, distributionChannelID, currentApp.token, currentApp.name) }
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.pending must beFalse
    }

    "activate the WaterfallAdProvider" in new WithDB {
      WaterfallAdProvider.update(new WaterfallAdProvider(waterfallAdProvider1.id, waterfallAdProvider1.waterfallID, waterfallAdProvider1.adProviderID, waterfallAdProvider1.waterfallOrder, Some(5.0), Some(false), None, JsObject(Seq()), false, true))
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.active.get must beFalse
      DB.withTransaction { implicit connection => WaterfallAdProvider.updateHyprMarketplaceConfig(waterfallAdProvider1, distributionChannelID, currentApp.token, currentApp.name) }
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.active.get must beTrue
    }

    "turn on reporting for the WaterfallAdProvider" in new WithDB {
      WaterfallAdProvider.update(new WaterfallAdProvider(waterfallAdProvider1.id, waterfallAdProvider1.waterfallID, waterfallAdProvider1.adProviderID, waterfallAdProvider1.waterfallOrder, Some(5.0), Some(false), None, JsObject(Seq()), false, true))
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.reportingActive must beFalse
      DB.withTransaction { implicit connection => WaterfallAdProvider.updateHyprMarketplaceConfig(waterfallAdProvider1, distributionChannelID, currentApp.token, currentApp.name) }
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.reportingActive must beTrue
    }
  }

  "WaterfallAdProvider.unconfigured" should {
    "return false if all fields in the JSON argument have corresponding values" in new WithDB {
      val jsonConfig = JsObject(Seq("requiredParams" -> JsObject(Seq("key1" -> JsString("value1"), "key2" -> JsArray(Seq(JsString("element1")))))))
      WaterfallAdProvider.unconfigured(jsonConfig, "requiredParams") must beFalse
    }

    "return true if any field in the JSON argument has an empty string value" in new WithDB {
      val jsonConfig = JsObject(Seq("requiredParams" -> JsObject(Seq("key1" -> JsString("")))))
      WaterfallAdProvider.unconfigured(jsonConfig, "requiredParams") must beTrue
    }

    "return true if any field in the JSON argument has an empty JsArray value" in new WithDB {
      val jsonConfig = JsObject(Seq("requiredParams" -> JsObject(Seq("key1" -> JsArray(Seq())))))
      WaterfallAdProvider.unconfigured(jsonConfig, "requiredParams") must beTrue
    }
  }

  "WaterfallAdProvider.updateEcpm" should {
    "update the eCPM for the WaterfallAdProvider" in new WithDB {
      val newEcpm = 15.0
      val newGeneration = DB.withTransaction { implicit connection =>
        WaterfallAdProvider.updateEcpm(waterfallAdProvider1.id, newEcpm).get
      }
      WaterfallAdProvider.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(newEcpm)
    }

    "return the latest generation number if the WaterfallAdProvider is updated successfully" in new WithDB {
      Waterfall.update(waterfall.id, optimizedOrder = false, testMode = false, paused = false)
      DB.withTransaction { implicit connection => AppConfig.createWithWaterfallIDInTransaction(waterfall.id, None) }
      val originalGeneration = AppConfig.findLatest(currentApp.token).get.generationNumber
      val newGeneration = DB.withTransaction { implicit connection =>
        WaterfallAdProvider.updateEcpm(waterfallAdProvider1.id, 5.0).get
      }
      newGeneration must beEqualTo(originalGeneration + 1)
    }

    "return None if the eCPM is not updated successfully" in new WithDB {
      val unknownWaterfallAdProviderID = 0
      DB.withTransaction { implicit connection =>
        WaterfallAdProvider.updateEcpm(unknownWaterfallAdProviderID, 5.0) must beNone
      }
    }
  }
}
