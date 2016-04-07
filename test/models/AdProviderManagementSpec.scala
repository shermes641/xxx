package models

import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.test.FakeApplication
import play.api.test.Helpers._
import resources.{AdProviderSpecSetup, SpecificationWithFixtures}

class AdProviderManagementSpec extends SpecificationWithFixtures with AdProviderSpecSetup with AdProviderManagement {

  running(FakeApplication(additionalConfiguration = testDB)) {
    AdProvider.loadAll()
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
  val providerList = Platform.Ios.allAdProviders ++ Platform.Android.allAdProviders

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
      val androidProvider = new UpdatableAdProvider("New Ad Provider", config, Platform.AndroidPlatformID, Some(""), true, Some(10))
      createAdProvider(androidProvider.platformID, androidProvider) must beEqualTo(AdProviderResult.CREATED)
      createAdProvider(androidProvider.platformID, androidProvider) must beEqualTo(AdProviderResult.EXISTS)

      val iosProvider = new UpdatableAdProvider("New Ad Provider", config, Platform.IosPlatformID, Some(""), true, Some(10))
      createAdProvider(iosProvider.platformID, iosProvider) must beEqualTo(AdProviderResult.CREATED)
      createAdProvider(iosProvider.platformID, iosProvider) must beEqualTo(AdProviderResult.EXISTS)
    }

    "fail with bad platform ID" in new WithDB {
      var provider = new UpdatableAdProvider("New Ad Provider", config, 0L, Some(""), true, Some(10))
      createAdProvider(provider.platformID, provider) must beEqualTo(AdProviderResult.INVALID_PLATFORM_ID)

      provider = new UpdatableAdProvider("New Ad Provider", config, 12367867823L, Some(""), true, Some(10))
      createAdProvider(provider.platformID, provider) must beEqualTo(AdProviderResult.INVALID_PLATFORM_ID)
    }
  }

  "updateSingleAdProvider" should {
    "fail when ad provider does not exist in the DB" in new WithDB {
      val provider = new UpdatableAdProvider("Newer Ad Provider", config, 5646388L, Some(""), true, Some(10))
      updateSingleAdProvider(provider) must beEqualTo(AdProviderResult.FAILED)
    }

    "succeed when ad provider exists in the DB" in new WithDB {
      val provider = new UpdatableAdProvider("Newer Ad Provider", config, Platform.IosPlatformID, Some(""), true, Some(10))
      createAdProvider(provider.platformID, provider) must beEqualTo(AdProviderResult.CREATED)
      updateSingleAdProvider(provider) must beEqualTo(AdProviderResult.UPDATED)


      val newConfig = config.replace("""displayKey": "Unity Ads App ID""", """displayKey": "Newer Ads App ID""")
      val newProvider = new UpdatableAdProvider("Newer Ad Provider", newConfig, Platform.IosPlatformID, Some("ABCD"), false, Some(5))
      updateSingleAdProvider(newProvider) must beEqualTo(AdProviderResult.UPDATED)

      val adp = AdProvider.findByPlatformAndName(newProvider.platformID, newProvider.name).get
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
      val adProvider = AdProvider.findAllByPlatform(Platform.Ios.PlatformID).head
      val newDefaultEcpm = Some(25.00)
      val newConfigurationData = adProvider.configurationData.as[JsObject]
        .deepMerge(Json.obj("requiredParams" -> JsArray()))
      val newCallbackURLFormat = Some("some callback URL format")
      val newPlatformID = Platform.Android.PlatformID
      val updatableAdProvider = new UpdatableAdProvider(
        adProvider.name,
        newConfigurationData.toString(),
        adProvider.platformID,
        newCallbackURLFormat,
        !adProvider.configurable,
        newDefaultEcpm
      )

      AdProvider.updateSingleAdProvider(updatableAdProvider).toString must beEqualTo(AdProviderResult.UPDATED.toString)
      val updatedAdProvider = AdProvider.findAllByPlatform(updatableAdProvider.platformID)
        .filter(_.name == adProvider.name).head
      updatedAdProvider.configurationData must beEqualTo(newConfigurationData)
      updatedAdProvider.configurable must beEqualTo(!adProvider.configurable)
      updatedAdProvider.defaultEcpm must beEqualTo(newDefaultEcpm)
    }
  }
}

