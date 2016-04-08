package models

import play.api.libs.json._
import play.api.test.Helpers._
import resources._

class JsonBuilderSpec extends SpecificationWithFixtures with JsonTesting with WaterfallSpecSetup {
  "jsonBuilder.appConfigResponseV1" should {
    val appConfig = running(testApplication) {
      val wapID1 = waterfallAdProviderService.create(waterfall.id, adProviderID2.get, Some(0), None, true, true)
      val wap = waterfallAdProviderService.find(wapID1.get).get
      val configData = JsObject(Seq("requiredParams" -> JsObject(Seq("key1" -> JsString("value1")))))
      waterfallAdProviderService.update(new WaterfallAdProvider(wap.id, wap.waterfallID, wap.adProviderID, wap.waterfallOrder, Some(5.0), wap.active, wap.fillRate, configData, wap.reportingActive))
      val appToken = appService.find(waterfall.app_id).get.token
      val waterfallOrder = database.withTransaction { implicit connection => waterfallService.order(appToken) }
      jsonBuilder.appConfigResponseV1(adProviderList = waterfallOrder, adProviderBelowRewardThresholdList = waterfallOrder, configInfo = waterfallOrder.head)
    }

    "convert a list of AdProviderInfo instances into a proper JSON response" in new WithDB {
      val adProviderConfigs = (appConfig \ "adProviderConfigurations").as[List[JsValue]]
      adProviderConfigs.length must beEqualTo(1)
      adProviderConfigs.map { config =>
        adProviders must contain((config \ "providerName").as[String])
        (config \ "providerID").get must haveClass[JsNumber]
        (config \ "eCPM").get must haveClass[JsNumber]
        (config \ "sdkBlacklistRegex").get.as[String] must beEqualTo(".^")
      }
    }

    "convert a list of ad providers below threshold to JSON response" in new WithDB {
      val adProviderConfigs = (appConfig \ "adProviderBelowRewardThreshold").as[List[JsValue]]
      adProviderConfigs.length must beEqualTo(1)
      adProviderConfigs.map { config =>
        adProviders must contain((config \ "providerName").as[String])
        (config \ "providerID").get must haveClass[JsNumber]
        (config \ "eCPM").get must haveClass[JsNumber]
        (config \ "sdkBlacklistRegex").get.as[String] must beEqualTo(".^")
      }
    }

    "contain appName" in new WithDB {
      (appConfig \ "appName").get.as[String] must beEqualTo(app1.name)
    }

    "contain appID" in new WithDB {
      (appConfig \ "appID").get must haveClass[JsNumber]
    }

    "contain distributorName" in new WithDB {
      (appConfig \ "distributorName").get.as[String] must beEqualTo(distributor.name)
    }

    "contain distributorID" in new WithDB {
      (appConfig \ "distributorID").get.as[Long] must beEqualTo(distributor.id.get)
    }

    "contain appConfigRefreshInterval" in new WithDB {
      (appConfig \ "appConfigRefreshInterval").get.as[Long] must beEqualTo(0)
    }

    "contain logFullConfig" in new WithDB {
      (appConfig \ "logFullConfig").get.as[Boolean] must beEqualTo(true)
    }

    "contain testMode" in new WithDB {
      (appConfig \ "testMode").get.as[Boolean] must beEqualTo(false)
    }

    "contain canShowAdTimeout" in new WithDB {
      (appConfig \ "canShowAdTimeout").get.as[Long] must beEqualTo(jsonBuilder.DefaultCanShowAdTimeout)
    }

    "contain rewardTimeout" in new WithDB {
      (appConfig \ "rewardTimeout").get.as[Long] must beEqualTo(jsonBuilder.DefaultRewardTimeout)
    }

    "contain paused" in new WithDB {
      (appConfig \ "paused").get.as[Boolean] must beEqualTo(false)
    }
  }

  "jsonBuilder.virtualCurrencyConfiguration" should {
    "convert an AdProviderInfo instance into a JSON object containing virtual currency information" in new WithDB {
      val virtualCurrency = new VirtualCurrency(0, 0, "Coins", 100, 1, Some(100), true)
      val adProviderInfo = new AdProviderInfo(providerName=Some("ad provider name"), providerID=None, platformID=Some(1), sdkBlacklistRegex=None, appName=None, appID=None, appConfigRefreshInterval=0,
        distributorName=None, distributorID=None, configurationData=None, cpm=None, Some(virtualCurrency.name), Some(virtualCurrency.exchangeRate),
        virtualCurrency.rewardMin, virtualCurrency.rewardMax, Some(virtualCurrency.roundUp), testMode=false, paused=false, optimizedOrder=false, active=None)
      val expectedVCJson = JsObject(Seq("virtualCurrency" -> JsObject(Seq("name" -> JsString(virtualCurrency.name), "exchangeRate" -> JsNumber(virtualCurrency.exchangeRate),
        "rewardMin" -> JsNumber(virtualCurrency.rewardMin), "rewardMax" -> JsNumber(virtualCurrency.rewardMax.get), "roundUp" -> JsBoolean(virtualCurrency.roundUp)))))
      jsonBuilder.virtualCurrencyConfiguration(adProviderInfo) must beEqualTo(expectedVCJson)
    }
  }

  "jsonBuilder.appConfiguration" should {
    "convert an AdProviderInfo instance into a JSON object containing the name of an app" in new WithDB {
      val appName = "Test App"
      val appID = 0L
      val platformID = 1L
      val adProviderInfo = new AdProviderInfo(providerName=None, providerID=None, platformID=Some(1), sdkBlacklistRegex=None, Some(appName), Some(appID), appConfigRefreshInterval=0,
        distributorName=None, distributorID=None, configurationData=None, cpm=None, virtualCurrencyName=None, exchangeRate=None, rewardMin=1, rewardMax=None,
        roundUp=None, testMode=false, paused=false, optimizedOrder=false, active=None)
      val expectedAppNameJson = JsObject(Seq("appName" -> JsString(appName), "appID" -> JsNumber(appID), "platformID" -> JsNumber(platformID)))
      jsonBuilder.appConfiguration(adProviderInfo) must beEqualTo(expectedAppNameJson)
    }
  }

  "jsonBuilder.distributorConfiguration" should {
    "convert an AdProviderInfo instance into a JSON object containing the name and ID of a Distributor" in new WithDB {
      val distributorName = "Test Distributor"
      val distributorID = 10.toLong
      val adProviderInfo = new AdProviderInfo(providerName=None, providerID=None, platformID=Some(1), sdkBlacklistRegex=None, appName=None, appID=None, appConfigRefreshInterval=0,
        Some(distributorName), Some(distributorID), configurationData=None, cpm=None, virtualCurrencyName=None, exchangeRate=None, rewardMin=1, rewardMax=None,
        roundUp=None, testMode=false, paused=false, optimizedOrder=false, active=None)
      val expectedDistributorJson = JsObject(Seq("distributorName" -> JsString(distributorName), "distributorID" -> JsNumber(distributorID)))
      jsonBuilder.distributorConfiguration(adProviderInfo) must beEqualTo(expectedDistributorJson)
    }
  }

  "jsonBuilder.sdkConfiguration" should {
    "create a JSON object containing the appropriate SDK configuration info" in new WithDB {
      val expectedAppConfigRefreshInterval = 1800
      val expectedSdkConfigurationJson = JsObject(
        Seq(
          "appConfigRefreshInterval" -> JsNumber(expectedAppConfigRefreshInterval),
          "logFullConfig" -> JsBoolean(jsonBuilder.LogFullConfig)
        )
      )
      jsonBuilder.sdkConfiguration(expectedAppConfigRefreshInterval) must beEqualTo(expectedSdkConfigurationJson)
    }
  }

  "jsonBuilder.timeoutConfigurations" should {
    "create a JSON object containing the canShowAdTimeout and rewardTimeout" in new WithDB {
      val expectedCanShowAdTimeout = 10
      val expectedRewardTimeout = 10
      val expectedTimeoutConfigurationJson = JsObject(
        Seq(
          "canShowAdTimeout" -> JsNumber(expectedCanShowAdTimeout),
          "rewardTimeout" -> JsNumber(expectedRewardTimeout)
        )
      )
      jsonBuilder.timeoutConfigurations must beEqualTo(expectedTimeoutConfigurationJson)
    }
  }

  "jsonBuilder.testModeConfiguration" should {
    "create a JSON object containing the testMode boolean, which is false by default" in new WithDB {
      (jsonBuilder.testModeConfiguration  \ "testMode").get must beEqualTo(JsBoolean(false))
    }
  }
}
