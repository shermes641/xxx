package models

import play.api.libs.json.{JsValue, JsObject, JsString}

class JsonBuilderSpec extends SpecificationWithFixtures with JsonTesting with WaterfallSpecSetup {
  "JsonBuilder.waterfallResponse" should {
    "convert a list of AdProviderInfo instances into a proper JSON response" in new WithDB {
      val wapID1 = WaterfallAdProvider.create(waterfall.get.id, adProviderID2.get, Some(0))
      val wap = WaterfallAdProvider.find(wapID1.get).get
      val configData = JsObject(
        Seq("key1" -> JsString("value1"))
      )
      WaterfallAdProvider.update(new WaterfallAdProvider(wap.id, wap.waterfallID, wap.adProviderID, wap.waterfallOrder, wap.cpm, wap.active, wap.fillRate, configData))
      val adProviderConfigs = (JsonBuilder.waterfallResponse(Waterfall.order(waterfall.get.token)) \ "adProviderConfigurations").as[List[JsValue]]
      adProviderConfigs.map { config =>
        adProviders must contain((config \ "providerName").as[String])
      }
    }
  }
  step(clean)
}
