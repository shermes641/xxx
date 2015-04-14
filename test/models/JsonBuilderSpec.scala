package models

import models.Waterfall.AdProviderInfo
import play.api.db.DB
import play.api.libs.json._
import play.api.Play.current
import play.api.test.Helpers._
import play.api.test.FakeApplication
import resources._

class JsonBuilderSpec extends SpecificationWithFixtures with JsonTesting with WaterfallSpecSetup {
  "JsonBuilder.appConfigResponseV1" should {
    val appConfig = running(FakeApplication(additionalConfiguration = testDB)) {
      val wapID1 = WaterfallAdProvider.create(waterfall.id, adProviderID2.get, Some(0), None, true, true)
      val wap = WaterfallAdProvider.find(wapID1.get).get
      val configData = JsObject(Seq("requiredParams" -> JsObject(Seq("key1" -> JsString("value1")))))
      WaterfallAdProvider.update(new WaterfallAdProvider(wap.id, wap.waterfallID, wap.adProviderID, wap.waterfallOrder, wap.cpm, wap.active, wap.fillRate, configData, wap.reportingActive))
      val appToken = App.find(waterfall.app_id).get.token
      val waterfallOrder = DB.withTransaction { implicit connection => Waterfall.order(appToken) }
      JsonBuilder.appConfigResponseV1(waterfallOrder, waterfallOrder(0))
    }

    "convert a list of AdProviderInfo instances into a proper JSON response" in new WithDB {
      val adProviderConfigs = (appConfig \ "adProviderConfigurations").as[List[JsValue]]
      adProviderConfigs.map { config =>
        adProviders must contain((config \ "providerName").as[String])
      }
    }

    "contain appName" in new WithDB {
      (appConfig \ "appName").as[String] must beEqualTo(app1.name)
    }

    "contain appID" in new WithDB {
      (appConfig \ "appID") must haveClass[JsNumber]
    }

    "contain distributorName" in new WithDB {
      (appConfig \ "distributorName").as[String] must beEqualTo(distributor.name)
    }

    "contain distributorID" in new WithDB {
      (appConfig \ "distributorID").as[Long] must beEqualTo(distributor.id.get)
    }

    "contain appConfigRefreshInterval" in new WithDB {
      (appConfig \ "appConfigRefreshInterval").as[Long] must beEqualTo(0)
    }

    "contain logFullConfig" in new WithDB {
      (appConfig \ "logFullConfig").as[Boolean] must beEqualTo(true)
    }

    "contain testMode" in new WithDB {
      (appConfig \ "testMode").as[Boolean] must beEqualTo(false)
    }

    "contain canShowAdTimeout" in new WithDB {
      (appConfig \ "canShowAdTimeout").as[Long] must beEqualTo(JsonBuilder.DefaultCanShowAdTimeout)
    }

    "contain rewardTimeout" in new WithDB {
      (appConfig \ "rewardTimeout").as[Long] must beEqualTo(JsonBuilder.DefaultRewardTimeout)
    }
  }

  "JsonBuilder.virtualCurrencyConfiguration" should {
    "convert an AdProviderInfo instance into a JSON object containing virtual currency information" in new WithDB {
      val virtualCurrency = new VirtualCurrency(0, 0, "Coins", 100, 1, Some(100), true)
      val adProviderInfo = new AdProviderInfo(Some("ad provider name"), None, None, None, 0, None, None, None, None, Some(virtualCurrency.name), Some(virtualCurrency.exchangeRate),
        virtualCurrency.rewardMin, virtualCurrency.rewardMax, Some(virtualCurrency.roundUp), false, false, None)
      val expectedVCJson = JsObject(Seq("virtualCurrency" -> JsObject(Seq("name" -> JsString(virtualCurrency.name), "exchangeRate" -> JsNumber(virtualCurrency.exchangeRate),
        "rewardMin" -> JsNumber(virtualCurrency.rewardMin), "rewardMax" -> JsNumber(virtualCurrency.rewardMax.get), "roundUp" -> JsBoolean(virtualCurrency.roundUp)))))
      JsonBuilder.virtualCurrencyConfiguration(adProviderInfo) must beEqualTo(expectedVCJson)
    }
  }

  "JsonBuilder.appNameConfiguration" should {
    "convert an AdProviderInfo instance into a JSON object containing the name of an app" in new WithDB {
      val appName = "Test App"
      val appID = 0.toLong
      val adProviderInfo = new AdProviderInfo(None, None, Some(appName), Some(appID), 0, None, None, None, None, None, None, 1, None, None, false, false, None)
      val expectedAppNameJson = JsObject(Seq("appName" -> JsString(appName), "appID" -> JsNumber(appID)))
      JsonBuilder.appNameConfiguration(adProviderInfo) must beEqualTo(expectedAppNameJson)
    }
  }

  "JsonBuilder.distributorConfiguration" should {
    "convert an AdProviderInfo instance into a JSON object containing the name and ID of a Distributor" in new WithDB {
      val distributorName = "Test Distributor"
      val distributorID = 10.toLong
      val adProviderInfo = new AdProviderInfo(None, None, None, None, 0, Some(distributorName), Some(distributorID), None, None, None, None, 1, None, None, false, false, None)
      val expectedDistributorJson = JsObject(Seq("distributorName" -> JsString(distributorName), "distributorID" -> JsNumber(distributorID)))
      JsonBuilder.distributorConfiguration(adProviderInfo) must beEqualTo(expectedDistributorJson)
    }
  }

  "JsonBuilder.sdkConfiguration" should {
    "create a JSON object containing the appropriate SDK configuration info" in new WithDB {
      val expectedAppConfigRefreshInterval = 1800
      val expectedSdkConfigurationJson = JsObject(
        Seq(
          "appConfigRefreshInterval" -> JsNumber(expectedAppConfigRefreshInterval),
          "logFullConfig" -> JsBoolean(JsonBuilder.LogFullConfig)
        )
      )
      JsonBuilder.sdkConfiguration(expectedAppConfigRefreshInterval) must beEqualTo(expectedSdkConfigurationJson)
    }
  }

  "JsonBuilder.timeoutConfigurations" should {
    "create a JSON object containing the canShowAdTimeout and rewardTimeout" in new WithDB {
      val expectedCanShowAdTimeout = 10
      val expectedRewardTimeout = 10
      val expectedTimeoutConfigurationJson = JsObject(
        Seq(
          "canShowAdTimeout" -> JsNumber(expectedCanShowAdTimeout),
          "rewardTimeout" -> JsNumber(expectedRewardTimeout)
        )
      )
      JsonBuilder.timeoutConfigurations must beEqualTo(expectedTimeoutConfigurationJson)
    }
  }

  "JsonBuilder.testModeConfiguration" should {
    "create a JSON object containing the testMode boolean, which is false by default" in new WithDB {
      JsonBuilder.testModeConfiguration  \ "testMode" must beEqualTo(JsBoolean(false))
    }
  }
}
