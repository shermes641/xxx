package models

import org.junit.runner._
import org.specs2.runner._
import play.api.libs.json._
import play.api.test.Helpers._
import resources.{SpecificationWithFixtures, JsonTesting}

@RunWith(classOf[JUnitRunner])
class WaterfallAdProviderSpec extends SpecificationWithFixtures with JsonTesting with WaterfallCreationHelper {
  val currentAdProviderID1 = running(testApplication) {
    val name = "newTestAdProvider1"
    val displayName = "new test ad provider 1"
    database.withConnection { implicit connection =>
      adProviderService.create(
        name = name,
        displayName = displayName,
        configurationData = configurationData,
        platformID = testPlatform.Ios.PlatformID,
        callbackUrlFormat = None,
        callbackUrlDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(displayName)
      )
    }
  }

  val currentAdProviderID2 = running(testApplication) {
    val name = "new test ad provider 2"
    val displayName = "newTestAdProvider2"
    database.withConnection { implicit connection =>
      adProviderService.create(
        name = name,
        displayName = displayName,
        configurationData = configurationData,
        platformID = testPlatform.Ios.PlatformID,
        callbackUrlFormat = None,
        callbackUrlDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(displayName)
      )
    }
  }

  val currentTestApp = running(testApplication) {
    val distributorID = distributorService.create("New Company").get
    val distributor = distributorService.find(distributorID).get
    val id = appService.create(distributorID, "New App", testPlatform.Ios.PlatformID).get
    appService.find(id).get
  }

  val currentWaterfall = running(testApplication) {
    virtualCurrencyService.create(currentTestApp.id, "Coins", 100, 1, None, Some(true))
    val waterfallID = database.withTransaction { implicit connection => createWaterfallWithConfig(currentTestApp.id, "New App Waterfall") }
    waterfallService.find(waterfallID, currentTestApp.distributorID).get
  }

  val waterfallAdProvider1 = running(testApplication) {
    val waterfallAdProviderID1 = waterfallAdProviderService.create(currentWaterfall.id, currentAdProviderID1.get, None, None, true, true).get
    waterfallAdProviderService.find(waterfallAdProviderID1).get
  }

  val waterfallAdProvider2 = running(testApplication) {
    val waterfallAdProviderID2 = waterfallAdProviderService.create(currentWaterfall.id, currentAdProviderID2.get, None, None, true, true).get
    waterfallAdProviderService.find(waterfallAdProviderID2).get
  }

  running(testApplication) {
    val updatedWaterfallAdProvider1 = new WaterfallAdProvider(waterfallAdProvider1.id, currentWaterfall.id, currentAdProviderID1.get, Some(1), None, Some(true), None, configurationJson, false)
    waterfallAdProviderService.update(updatedWaterfallAdProvider1)

    val updatedWaterfallAdProvider2 = new WaterfallAdProvider(waterfallAdProvider2.id, currentWaterfall.id, currentAdProviderID2.get, Some(2), None, Some(true), None, configurationJson, false)
    waterfallAdProviderService.update(updatedWaterfallAdProvider2)
  }

  "WaterfallAdProvider.create" should {
    "create a new record in the waterfall_ad_providers table" in new WithDB {
      waterfallAdProvider1 must beAnInstanceOf[WaterfallAdProvider]
    }

    "should not create a new record if another shares the same ad_provider_id and waterfall_id" in new WithDB {
      waterfallAdProviderService.create(currentWaterfall.id, currentAdProviderID1.get, None, None, true, true) must throwA[org.postgresql.util.PSQLException]
    }
  }

  "WaterfallAdProvider.update" should {
    "update the WaterfallAdProvider record in the database" in new WithDB {
      val newWaterfallOrder = Some(1.toLong)
      val updatedWaterfallAdProvider = new WaterfallAdProvider(waterfallAdProvider1.id, currentWaterfall.id, currentAdProviderID1.get, newWaterfallOrder, None, Some(true), None, configurationJson, false)
      waterfallAdProviderService.update(updatedWaterfallAdProvider) must beEqualTo(1)
      val retrievedWaterfallAdProvider = waterfallAdProviderService.find(waterfallAdProvider1.id).get
      retrievedWaterfallAdProvider.waterfallOrder.get must beEqualTo(newWaterfallOrder.get)
    }

    "return 0 if the WaterfallAdProvider is not update successfully" in new WithDB {
      val unknownID = 0
      val updatedWaterfallAdProvider = new WaterfallAdProvider(unknownID, currentWaterfall.id, currentAdProviderID1.get, Some(1), None, Some(true), None, configurationJson, false)
      waterfallAdProviderService.update(updatedWaterfallAdProvider) must beEqualTo(0)
    }
  }

  "WaterfallAdProvider.find" should {
    "return an instance of the WaterfallAdProvider class if a record is found" in new WithDB {
      waterfallAdProviderService.find(waterfallAdProvider1.id).get must haveClass[WaterfallAdProvider]
    }

    "return None if a WaterfallAdProvider could not be found" in new WithDB {
      val unknownID = 0
      waterfallAdProviderService.find(unknownID) must beNone
    }
  }

  "WaterfallAdProvider.findAllByWaterfallID" should {
    "return a list of WaterfallAdProvider instances if the Waterfall ID is found" in new WithDB {
      val waterfallAdProviders = waterfallAdProviderService.findAllByWaterfallID(currentWaterfall.id)
      waterfallAdProviders.size must beEqualTo(2)
      waterfallAdProviders.map { wap => wap must haveClass[WaterfallAdProvider] }
    }

    "return an empty list if the Waterfall ID is not found" in new WithDB {
      val unknownID = 0
      waterfallAdProviderService.findAllByWaterfallID(unknownID).size must beEqualTo(0)
    }
  }

  "WaterfallAdProvider.findAllOrdered" should {
    "return a list of WaterfallAdProvider instances ordered by waterfallOrder ascending" in new WithDB {
      val currentOrder = waterfallAdProviderService.findAllOrdered(currentWaterfall.id)
      currentOrder.size must beEqualTo(2)
      currentOrder(0).waterfallOrder.get must beEqualTo(1)
      currentOrder(1).waterfallOrder.get must beEqualTo(2)
    }
  }

  "WaterfallAdProvider.findAllReportingEnabled" should {
    "return a list of all waterfall ad providers that have reporting enabled" in new WithDB {
      def verifyReportingWAPs = {
        val reportingEnabledWAPs = waterfallAdProviderService.findAllReportingEnabled
        reportingEnabledWAPs.size must beEqualTo(originalWAPReportingCount + 1)
        reportingEnabledWAPs.map(wap => wap.waterfallAdProviderID) must contain(updatedActiveWaterfallAdProvider.id)
      }

      val originalWAPReportingCount = waterfallAdProviderService.findAllReportingEnabled.size
      val updatedActiveWaterfallAdProvider = new WaterfallAdProvider(waterfallAdProvider1.id, currentWaterfall.id, currentAdProviderID1.get, waterfallOrder = None,
        cpm = None, active = Some(true), fillRate = None, configurationData = configurationJson, reportingActive = true)
      waterfallAdProviderService.update(updatedActiveWaterfallAdProvider)

      verifyReportingWAPs

      val updatedInActiveWaterfallAdProvider = new WaterfallAdProvider(waterfallAdProvider1.id, currentWaterfall.id, currentAdProviderID1.get, waterfallOrder = None,
        cpm = None, active = Some(false), fillRate = None, configurationData = configurationJson, reportingActive = true)
      waterfallAdProviderService.update(updatedInActiveWaterfallAdProvider)

      verifyReportingWAPs
    }
  }

  "WaterfallAdProvider.findConfigurationData" should {
    "return an instance of WaterfallAdProviderConfig containing configuration data for both WaterfallAdProvider and AdProvider" in new WithDB {
      val configData = database.withConnection { implicit connection => waterfallAdProviderService.findConfigurationData(waterfallAdProvider1.id).get }
      val params = (configData.adProviderConfiguration \ "requiredParams").as[List[JsValue]]
      params.map { param => configurationParams must contain((param \ "key").as[String]) }
      val waterfallAdProviderParams = configData.waterfallAdProviderConfiguration \ "requiredParams"
      (waterfallAdProviderParams \ configurationParams(0)).as[String] must beEqualTo(configurationValues(0))
    }
  }

  "WaterfallAdProviderConfig.mappedFields" should {
    "return a list of RequiredParam instances" in new WithDB {
      val configData = database.withConnection { implicit connection => waterfallAdProviderService.findConfigurationData(waterfallAdProvider1.id).get }
      val fields = configData.mappedFields("requiredParams")
      for(index <- (0 to fields.size-1)) {
        fields(index).key.get must beEqualTo(configurationParams(index))
        fields(index).value.get must beEqualTo(configurationValues(index))
      }
    }

    "convert WaterfallAdProvider configuration param values to Strings if they are not already" in new WithDB {
      val newAppID = appService.create(currentTestApp.distributorID, "New Test App", testPlatform.Ios.PlatformID).get
      virtualCurrencyService.create(newAppID, "Coins", exchangeRate = 100, rewardMin = 1, rewardMax = None, roundUp = Some(true))
      val newWaterfall = {
        val id = database.withTransaction { implicit connection => createWaterfallWithConfig(newAppID, "New Waterfall") }
        waterfallService.find(id, currentTestApp.distributorID).get
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
        val id = waterfallAdProviderService.create(newWaterfall.id, currentAdProviderID1.get, waterfallOrder = None, cpm = None, configurable = true, active = true, pending = false).get
        val wap = waterfallAdProviderService.find(id).get
        waterfallAdProviderService.update(new WaterfallAdProvider(id, newWaterfall.id, currentAdProviderID1.get, waterfallOrder = None, cpm = None, active = Some(true), fillRate = None, configurationData = configData,reportingActive = true, pending = false))
        database.withTransaction { implicit connection => waterfallAdProviderService.findConfigurationData(id).get }
      }
      val fields = wapConfig.mappedFields(paramType)
      for(index <- (0 to fields.size-1)) {
        fields(index).value.get must haveClass[String]
      }
    }

    "convert an array param to a comma separated string" in new WithDB {
      val paramType = "requiredParams"
      val arrayParams = List("param1", "param2")
      val arrayParamsJson: Seq[JsString] = Seq(JsString(arrayParams.head), JsString(arrayParams(1)))
      val adConfig = {
        Json.obj(
          paramType -> JsArray(
            Seq(
              Json.obj(
                "description" -> JsString(""),
                "displayKey" -> JsString(""),
                "key" -> configurationParams.head,
                "dataType" -> "Array",
                "value" -> "",
                "minLength" -> JsNumber(1),
                "refreshOnAppRestart" -> JsBoolean(false)
              )
            )
          )
        )
      }
      val wapConfig = Json.obj(
        paramType -> Json.obj(
          configurationParams.head -> JsArray(arrayParamsJson)
        )
      )
      val wap = WaterfallAdProviderConfig(
        name = "name",
        platformID = 1L,
        rewardMin = 0,
        cpm = None,
        adProviderConfiguration = adConfig,
        callbackUrlFormat = None,
        callbackUrlDescription = "",
        waterfallAdProviderConfiguration = wapConfig,
        reportingActive = false,
        pending = false
      )
      val arrayField = wap.mappedFields(paramType).head
      arrayField.value.get must beEqualTo(arrayParams.mkString(","))
    }

    "return the correct defaults if the waterfallAdProvider's configuration is empty" in new WithDB {
      val paramType = "requiredParams"
      val refresh = false
      val minLength = 3
      val typeOfData = "String"
      val display = "some display key"
      val description = "some description"
      val adConfig = {
        Json.obj(
          paramType -> JsArray(
            Seq(
              Json.obj(
                "description" -> JsString(description),
                "displayKey" -> JsString(display),
                "key" -> configurationParams.head,
                "dataType" -> typeOfData,
                "value" -> "",
                "minLength" -> JsNumber(minLength),
                "refreshOnAppRestart" -> JsBoolean(refresh)
              )
            )
          )
        )
      }
      val wap = WaterfallAdProviderConfig(
        name = "name",
        platformID = 1L,
        rewardMin = 0,
        cpm = None,
        adProviderConfiguration = adConfig,
        callbackUrlFormat = None,
        callbackUrlDescription = "",
        waterfallAdProviderConfiguration = Json.obj(),
        reportingActive = false,
        pending = false
      )
      val field = wap.mappedFields(paramType).head
      field.key.get must beEqualTo(configurationParams.head)
      field.refreshOnAppRestart must beEqualTo(refresh)
      field.minLength must beEqualTo(minLength)
      field.dataType.get must beEqualTo(typeOfData)
      field.displayKey.get must beEqualTo(display)
      field.description.get must beEqualTo(description)
      field.value must beNone
    }
  }

  "WaterfallAdProvider.findRewardInfo" should {
    "return the configuration data JSON if a record is found" in new WithDB {
      waterfallAdProviderService.findRewardInfo(currentTestApp.token, "newTestAdProvider1").get must haveClass[AdProviderRewardInfo]
    }

    "return None if the configuration data does not exist" in new WithDB {
      waterfallAdProviderService.findRewardInfo(currentTestApp.token, "Some fake ad provider name") must beNone
    }
  }

  "WaterfallAdProvider.updateHyprMarketplaceConfig" should {
    val distributionChannelID: Long = 12345

    "set the appropriate JSON configuration for the HyprMarketplace WaterfallAdProvider" in new WithDB {
      database.withTransaction { implicit connection => waterfallAdProviderService.updateHyprMarketplaceConfig(waterfallAdProvider1, distributionChannelID, currentTestApp.token, currentTestApp.name) }
      val updatedWap = waterfallAdProviderService.find(waterfallAdProvider1.id).get
      val hyprDistributionChannelID = (updatedWap.configurationData \ "requiredParams" \ "distributorID").as[String]
      val hyprPropertyID = (updatedWap.configurationData \ "requiredParams" \ "propertyID").as[String]
      hyprDistributionChannelID must beEqualTo(distributionChannelID.toString)
      hyprPropertyID must beEqualTo(currentTestApp.name)
    }

    "set the reporting JSON configuration for the HyprMarketplace WaterfallAdProvider" in new WithDB {
      database.withTransaction { implicit connection => waterfallAdProviderService.updateHyprMarketplaceConfig(waterfallAdProvider1, distributionChannelID, currentTestApp.token, currentTestApp.name) }
      val updatedWap = waterfallAdProviderService.find(waterfallAdProvider1.id).get
      val reportingParams = (updatedWap.configurationData \ "reportingParams")
      val apiKey = (reportingParams \ "APIKey").as[String]
      val placementID = (reportingParams \ "placementID").as[String]
      val appID = (reportingParams \ "appID").as[String]
      apiKey must beEqualTo(currentTestApp.token)
      placementID must beEqualTo(currentTestApp.token)
      appID must beEqualTo(distributionChannelID.toString)
    }

    "set the pending attribute to false" in new WithDB {
      waterfallAdProviderService.update(new WaterfallAdProvider(waterfallAdProvider1.id, waterfallAdProvider1.waterfallID, waterfallAdProvider1.adProviderID, waterfallAdProvider1.waterfallOrder, Some(5.0), Some(false), None, JsObject(Seq()), false, true))
      waterfallAdProviderService.find(waterfallAdProvider1.id).get.pending must beTrue
      database.withTransaction { implicit connection => waterfallAdProviderService.updateHyprMarketplaceConfig(waterfallAdProvider1, distributionChannelID, currentTestApp.token, currentTestApp.name) }
      waterfallAdProviderService.find(waterfallAdProvider1.id).get.pending must beFalse
    }

    "activate the WaterfallAdProvider" in new WithDB {
      waterfallAdProviderService.update(new WaterfallAdProvider(waterfallAdProvider1.id, waterfallAdProvider1.waterfallID, waterfallAdProvider1.adProviderID, waterfallAdProvider1.waterfallOrder, Some(5.0), Some(false), None, JsObject(Seq()), false, true))
      waterfallAdProviderService.find(waterfallAdProvider1.id).get.active.get must beFalse
      database.withTransaction { implicit connection => waterfallAdProviderService.updateHyprMarketplaceConfig(waterfallAdProvider1, distributionChannelID, currentTestApp.token, currentTestApp.name) }
      waterfallAdProviderService.find(waterfallAdProvider1.id).get.active.get must beTrue
    }

    "turn on reporting for the WaterfallAdProvider" in new WithDB {
      waterfallAdProviderService.update(new WaterfallAdProvider(waterfallAdProvider1.id, waterfallAdProvider1.waterfallID, waterfallAdProvider1.adProviderID, waterfallAdProvider1.waterfallOrder, Some(5.0), Some(false), None, JsObject(Seq()), false, true))
      waterfallAdProviderService.find(waterfallAdProvider1.id).get.reportingActive must beFalse
      database.withTransaction { implicit connection => waterfallAdProviderService.updateHyprMarketplaceConfig(waterfallAdProvider1, distributionChannelID, currentTestApp.token, currentTestApp.name) }
      waterfallAdProviderService.find(waterfallAdProvider1.id).get.reportingActive must beTrue
    }
  }

  "WaterfallAdProvider.unconfigured" should {
    "return false if all fields in the JSON argument have corresponding values" in new WithDB {
      val jsonConfig = JsObject(Seq("requiredParams" -> JsObject(Seq("key1" -> JsString("value1"), "key2" -> JsArray(Seq(JsString("element1")))))))
      waterfallAdProviderService.unconfigured(jsonConfig, "requiredParams") must beFalse
    }

    "return true if any field in the JSON argument has an empty string value" in new WithDB {
      val jsonConfig = JsObject(Seq("requiredParams" -> JsObject(Seq("key1" -> JsString("")))))
      waterfallAdProviderService.unconfigured(jsonConfig, "requiredParams") must beTrue
    }

    "return true if any field in the JSON argument has an empty JsArray value" in new WithDB {
      val jsonConfig = JsObject(Seq("requiredParams" -> JsObject(Seq("key1" -> JsArray(Seq())))))
      waterfallAdProviderService.unconfigured(jsonConfig, "requiredParams") must beTrue
    }
  }

  "WaterfallAdProvider.updateEcpm" should {
    "update the eCPM for the WaterfallAdProvider" in new WithDB {
      val newEcpm = 15.0
      val newGeneration = database.withTransaction { implicit connection =>
        waterfallAdProviderService.updateEcpm(waterfallAdProvider1.id, newEcpm).get
      }
      waterfallAdProviderService.find(waterfallAdProvider1.id).get.cpm.get must beEqualTo(newEcpm)
    }

    "return the latest generation number if the WaterfallAdProvider is updated successfully" in new WithDB {
      waterfallService.update(currentWaterfall.id, optimizedOrder = false, testMode = false, paused = false)
      database.withTransaction { implicit connection => appConfigService.createWithWaterfallIDInTransaction(currentWaterfall.id, None) }
      val originalGeneration = appConfigService.findLatest(currentTestApp.token).get.generationNumber
      val newGeneration = database.withTransaction { implicit connection =>
        waterfallAdProviderService.updateEcpm(waterfallAdProvider1.id, 5.0).get
      }
      newGeneration must beEqualTo(originalGeneration + 1)
    }

    "return None if the eCPM is not updated successfully" in new WithDB {
      val unknownWaterfallAdProviderID = 0
      database.withTransaction { implicit connection =>
        waterfallAdProviderService.updateEcpm(unknownWaterfallAdProviderID, 5.0) must beNone
      }
    }
  }
}
