package models

import anorm._
import play.api.libs.json.{JsObject, JsArray, Json}
import play.api.test.Helpers._
import resources.SpecificationWithFixtures

class AdProviderManagementSpec extends SpecificationWithFixtures with AdProviderManagement {
  override lazy val adProvider = adProviderService
  override lazy val platform = testPlatform
  override lazy val db = database
  running(testApplication) {
    database.withConnection { implicit connection =>
      SQL("""DELETE FROM ad_providers;""").execute()
    }
    adProviderService.loadAll()
  }

  val config = {
    """{ "requiredParams":[{"description": "",
      |  "displayKey": "Unity Ads App ID",
      |  "key": "appID",
      |  "value": "",
      |  "dataType": "String",
      |  "refreshOnAppRestart": true,
      |  "minLength": 1
      |  }],
      |  "reportingParams": [{"description": "",
      |  "displayKey": "API Key",
      |  "key": "APIKey",
      |  "value": "",
      |  "dataType": "String",
      |  "refreshOnAppRestart": false
      |  }],
      |  "callbackParams": [{"description": "",
      |  "displayKey": "Secret Key",
      |  "key": "APIKey",
      |  "value": "",
      |  "dataType": "String",
      |  "refreshOnAppRestart": false
      |  }]}""".stripMargin
  }
  val providerList = testPlatform.Ios.allAdProviders ++ testPlatform.Android.allAdProviders

  "createAdProvider" should {
    "find or create ad providers" in new WithDB {
      providerList.foreach { adProvider =>
        val res = createAdProvider(adProvider.platformID, adProvider) == AdProviderResult.CREATED ||
          createAdProvider(adProvider.platformID, adProvider) == AdProviderResult.EXISTS
        res must beEqualTo(true)
      }

      providerList.foreach { adProvider =>
        createAdProvider(adProvider.platformID, adProvider) must beEqualTo(AdProviderResult.EXISTS)
      }
    }

    "create new ad provider" in new WithDB {
      val adProviderName = "NewAdProvider"
      val adProviderDisplayName = "New Ad Provider"
      val androidProvider = new UpdatableAdProvider(
        name = adProviderName,
        displayName = adProviderDisplayName,
        configurationData = config,
        platformID = testPlatform.AndroidPlatformID,
        callbackURLFormat = Some(""),
        callbackURLDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(adProviderDisplayName),
        configurable = true,
        defaultEcpm = Some(10)
      )
      createAdProvider(androidProvider.platformID, androidProvider) must beEqualTo(AdProviderResult.CREATED)
      createAdProvider(androidProvider.platformID, androidProvider) must beEqualTo(AdProviderResult.EXISTS)

      val iosProvider = new UpdatableAdProvider(
        name = adProviderName,
        displayName = adProviderName,
        configurationData = config,
        platformID = testPlatform.IosPlatformID,
        callbackURLFormat = Some(""),
        callbackURLDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(adProviderDisplayName),
        configurable = true,
        defaultEcpm = Some(10)
      )
      createAdProvider(iosProvider.platformID, iosProvider) must beEqualTo(AdProviderResult.CREATED)
      createAdProvider(iosProvider.platformID, iosProvider) must beEqualTo(AdProviderResult.EXISTS)
    }

    "throw an exception if the ad provider name contains spaces or punctuation" in new WithDB {
      val badAdProviderNames = List("New Ad Provider", "NewAdProvider!", "New-Ad-Provider")
      badAdProviderNames.map { name =>
        new UpdatableAdProvider(
          name = name,
          displayName = name,
          configurationData = config,
          platformID = testPlatform.AndroidPlatformID,
          callbackURLFormat = Some(""),
          callbackURLDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(name),
          configurable = true,
          defaultEcpm = Some(10)
        ) must throwA[IllegalArgumentException]
      }
    }

    "fail with bad platform ID" in new WithDB {
      val adProviderName = "NewAdProvider"
      val adProviderDisplayName = "New Ad Provider"
      var provider = new UpdatableAdProvider(
        name = adProviderName,
        displayName = adProviderDisplayName,
        configurationData = config,
        platformID = 0L,
        callbackURLFormat = Some(""),
        callbackURLDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(adProviderDisplayName),
        configurable = true,
        defaultEcpm = Some(10)
      )
      createAdProvider(provider.platformID, provider) must beEqualTo(AdProviderResult.INVALID_PLATFORM_ID)

      provider = new UpdatableAdProvider(
        name = adProviderName,
        displayName = adProviderName,
        configurationData = config,
        platformID = 12367867823L,
        callbackURLFormat = Some(""),
        callbackURLDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(adProviderName),
        configurable = true,
        defaultEcpm = Some(10)
      )
      createAdProvider(provider.platformID, provider) must beEqualTo(AdProviderResult.INVALID_PLATFORM_ID)
    }
  }

  "updateSingleAdProvider" should {
    "fail when ad provider does not exist in the DB" in new WithDB {
      val adProviderName = "SomeNewAdProvider"
      val adProviderDisplayName = "Some New Ad Provider"
      val provider = new UpdatableAdProvider(
        name = adProviderName,
        displayName = adProviderDisplayName,
        configurationData = config,
        platformID = 1L,
        callbackURLFormat = Some(""),
        callbackURLDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(adProviderDisplayName),
        configurable = true,
        defaultEcpm = Some(10)
      )
      updateSingleAdProvider(provider) must beEqualTo(AdProviderResult.FAILED)
    }

    "succeed when ad provider exists in the DB" in new WithDB {
      val adProviderName = "SomeNewAdProvider"
      val adProviderDisplayName = "Some New Ad Provider"
      val provider = new UpdatableAdProvider(
        name = adProviderName,
        displayName = adProviderDisplayName,
        configurationData = config,
        platformID = testPlatform.IosPlatformID,
        callbackURLFormat = Some(""),
        callbackURLDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(adProviderDisplayName),
        configurable = true,
        defaultEcpm = Some(10)
      )
      createAdProvider(provider.platformID, provider) must beEqualTo(AdProviderResult.CREATED)
      updateSingleAdProvider(provider) must beEqualTo(AdProviderResult.UPDATED)


      val newConfig = config.replace("""displayKey": "Unity Ads App ID""", """displayKey": "Newer Ads App ID""")
      val newProvider = new UpdatableAdProvider(
        name = adProviderName,
        displayName = adProviderName,
        configurationData = newConfig,
        platformID = testPlatform.IosPlatformID,
        callbackURLFormat = Some("ABCD"),
        callbackURLDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(adProviderName),
        configurable = false,
        defaultEcpm = Some(5)
      )
      updateSingleAdProvider(newProvider) must beEqualTo(AdProviderResult.UPDATED)

      val adp = adProviderService.findByPlatformAndName(newProvider.platformID, newProvider.name).get
      adp.name must beEqualTo(newProvider.name)
      adp.platformID must beEqualTo(newProvider.platformID)
      (adp.configurationData + "").contains("Newer Ads App ID")
      (adp.configurationData + "").contains("ABCD")
      adp.configurable must beEqualTo(false)
      adp.defaultEcpm must beEqualTo(Some(5))
    }
  }

  "loadAll" should {
    "match the total count of ad providers and not fail" in new WithDB {
      val res = loadAll()
      var result = !res.contains(AdProviderResult.FAILED) &&
        !res.contains(AdProviderResult.INVALID_PLATFORM_ID) &&
        !res.contains(AdProviderResult.UPDATED)

      res.size must beEqualTo(providerList.size)
      result must beEqualTo(true)
    }
  }

  "updateAll" should {
    "match the total count of ad providers" in new WithDB {
      loadAll()
      val res = updateAll()
      res must beEqualTo(providerList.size)
    }
  }

  "update" should {
    "update values for only the ad provider passed as an argument" in new WithDB {
      val adProvider = adProviderService.findAllByPlatform(testPlatform.Ios.PlatformID).head
      val newDefaultEcpm = Some(25.00)
      val newConfigurationData = adProvider.configurationData.as[JsObject]
        .deepMerge(Json.obj("requiredParams" -> JsArray()))
      val newCallbackURLFormat = Some("some callback URL format")
      val newPlatformID = testPlatform.Android.PlatformID
      val newCallbackUrlDescription = "Some new callback URL description"
      val updatableAdProvider = new UpdatableAdProvider(
        name = adProvider.name,
        displayName = adProvider.name,
        configurationData = newConfigurationData.toString(),
        platformID = adProvider.platformID,
        callbackURLFormat = newCallbackURLFormat,
        callbackURLDescription = newCallbackUrlDescription,
        configurable = !adProvider.configurable,
        defaultEcpm = newDefaultEcpm
      )

      adProviderService.updateSingleAdProvider(updatableAdProvider).toString must beEqualTo(AdProviderResult.UPDATED.toString)
      val updatedAdProvider = adProviderService.findAllByPlatform(updatableAdProvider.platformID)
        .filter(_.name == adProvider.name).head
      updatedAdProvider.configurationData must beEqualTo(newConfigurationData)
      updatedAdProvider.configurable must beEqualTo(!adProvider.configurable)
      updatedAdProvider.defaultEcpm must beEqualTo(newDefaultEcpm)
      updatedAdProvider.callbackUrlDescription must beEqualTo(newCallbackUrlDescription)
    }
  }
}

