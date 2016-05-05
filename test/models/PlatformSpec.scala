package models

import models.Platform.{Android, Ios}
import play.api.test._
import play.api.test.Helpers._
import resources.SpecificationWithFixtures

class PlatformSpec extends SpecificationWithFixtures {
  val adProviderNames = running(FakeApplication(additionalConfiguration = testDB)) {
    List(
      Ios.AdColony.name,
      Ios.HyprMarketplace.name,
      Ios.Vungle.name,
      Ios.AppLovin.name,
      Ios.UnityAds.name
    )
  }

  "Platform.find" should {
    "return the correct platform for a given ID" in new WithDB {
      Platform.find(Platform.Android.PlatformID).PlatformName must beEqualTo(Platform.Android.PlatformName)
      Platform.find(Platform.Ios.PlatformID).PlatformName must beEqualTo(Platform.Ios.PlatformName)
    }

    "default to iOS if the platform ID is unknown" in new WithDB {
      val unknownID = 0
      Platform.find(unknownID).PlatformName must beEqualTo(Platform.Ios.PlatformName)
    }
  }

  "Ios.allAdProviders" should {
    "return the 5 supported ad networks" in new WithDB {
      val allAdProviders = Ios.allAdProviders
      verifyAdProviders(allAdProviders, adProviderNames)
    }
  }

  "Android.allAdProviders" should {
    "return the 4 supported ad networks" in new WithDB {
      val androidAdProviderNames = List(
        Ios.AdColony.name,
        Ios.HyprMarketplace.name,
        Ios.AppLovin.name,
        Ios.UnityAds.name
      )
      val allAdProviders = Android.allAdProviders
      verifyAdProviders(allAdProviders, androidAdProviderNames)
    }

    "should not include Vungle" in new WithDB {
      Android.allAdProviders.filter(provider =>
        provider.platformID == Platform.Android.PlatformID && provider.name == Platform.Android.Vungle.name
      ) must beEmpty
    }
  }

  def verifyAdProviders(allAdProviders: List[UpdatableAdProvider], adProviderNames: List[String]) = {
    allAdProviders.length must beEqualTo(adProviderNames.length)
    adProviderNames.map(adProviderName =>
      allAdProviders.count(_.name == adProviderName) must beEqualTo(1)
    )
  }
}
