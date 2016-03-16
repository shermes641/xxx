package models

import play.api.libs.json.{JsObject, JsArray, Json}
import play.api.test.FakeApplication
import play.api.test.Helpers._
import resources.SpecificationWithFixtures

class AdProviderManagementSpec extends SpecificationWithFixtures with AdProviderManagement {
  running(FakeApplication(additionalConfiguration = testDB)) {
    AdProvider.loadAll()
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

      AdProvider.update(updatableAdProvider) must beEqualTo(1)
      val updatedAdProvider = AdProvider.findAllByPlatform(updatableAdProvider.platformID)
        .filter(_.name == adProvider.name).head
      updatedAdProvider.configurationData must beEqualTo(newConfigurationData)
      updatedAdProvider.configurable must beEqualTo(!adProvider.configurable)
      updatedAdProvider.defaultEcpm must beEqualTo(newDefaultEcpm)
    }
  }

  "updateAll" should {
    "return the number of ad providers updated successfully" in new WithDB {
      AdProvider.updateAll() must beEqualTo(AdProvider.findAll.length)
    }
  }
}
