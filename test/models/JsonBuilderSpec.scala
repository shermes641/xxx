package models

import models.Waterfall.AdProviderInfo
import play.api.libs.json._

class JsonBuilderSpec extends SpecificationWithFixtures with JsonTesting with WaterfallSpecSetup {
  "JsonBuilder.waterfallResponse" should {
    "convert a list of AdProviderInfo instances into a proper JSON response" in new WithDB {
      val wapID1 = WaterfallAdProvider.create(waterfall.get.id, adProviderID2.get, Some(0), None, true)
      val wap = WaterfallAdProvider.find(wapID1.get).get
      val configData = JsObject(Seq("requiredParams" -> JsObject(Seq("key1" -> JsString("value1")))))
      WaterfallAdProvider.update(new WaterfallAdProvider(wap.id, wap.waterfallID, wap.adProviderID, wap.waterfallOrder, wap.cpm, wap.active, wap.fillRate, configData, wap.reportingActive))
      val adProviderConfigs = (JsonBuilder.waterfallResponse(Waterfall.order(waterfall.get.token)) \ "adProviderConfigurations").as[List[JsValue]]
      adProviderConfigs.map { config =>
        adProviders must contain((config \ "providerName").as[String])
      }
    }
  }

  "JsonBuilder.virtualCurrencyConfiguration" should {
    "convert an AdProviderInfo instance into a JSON object containing virtual currency information" in new WithDB {
      val virtualCurrency = new VirtualCurrency(0, 0, "Coins", 100, Some(1), Some(100), true)
      val adProviderInfo = new AdProviderInfo(Some("ad provider name"), None, None, None, None, None, None, None, Some(virtualCurrency.name), Some(virtualCurrency.exchangeRate),
        virtualCurrency.rewardMin, virtualCurrency.rewardMax, Some(virtualCurrency.roundUp), false, false, None)
      val expectedVCJson = JsObject(Seq("virtualCurrency" -> JsObject(Seq("name" -> JsString(virtualCurrency.name), "exchangeRate" -> JsNumber(virtualCurrency.exchangeRate),
        "rewardMin" -> JsNumber(virtualCurrency.rewardMin.get), "rewardMax" -> JsNumber(virtualCurrency.rewardMax.get), "roundUp" -> JsBoolean(virtualCurrency.roundUp)))))
      JsonBuilder.virtualCurrencyConfiguration(adProviderInfo) must beEqualTo(expectedVCJson)
    }
  }

  "JsonBuilder.appNameConfiguration" should {
    "convert an AdProviderInfo instance into a JSON object containing the name of an app" in new WithDB {
      val appName = "Test App"
      val appID = 0.toLong
      val adProviderInfo = new AdProviderInfo(None, None, Some(appName), Some(appID), None, None, None, None, None, None, None, None, None, false, false, None)
      val expectedAppNameJson = JsObject(Seq("appName" -> JsString(appName), "appID" -> JsNumber(appID)))
      JsonBuilder.appNameConfiguration(adProviderInfo) must beEqualTo(expectedAppNameJson)
    }
  }

  "JsonBuilder.distributorConfiguration" should {
    "convert an AdProviderInfo instance into a JSON object containing the name and ID of a Distributor" in new WithDB {
      val distributorName = "Test Distributor"
      val distributorID = 10.toLong
      val adProviderInfo = new AdProviderInfo(None, None, None, None, Some(distributorName), Some(distributorID), None, None, None, None, None, None, None, false, false, None)
      val expectedDistributorJson = JsObject(Seq("distributorName" -> JsString(distributorName), "distributorID" -> JsNumber(distributorID)))
      JsonBuilder.distributorConfiguration(adProviderInfo) must beEqualTo(expectedDistributorJson)
    }
  }
  step(clean)
}
