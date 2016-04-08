package models

import play.api.test.Helpers._
import resources.SpecificationWithFixtures

class PlatformSpec extends SpecificationWithFixtures {
  val adProviderNames = running(testApplication) {
    List(
      testPlatform.Ios.AdColony.name,
      testPlatform.Ios.HyprMarketplace.name,
      testPlatform.Ios.Vungle.name,
      testPlatform.Ios.AppLovin.name,
      testPlatform.Ios.UnityAds.name
    )
  }

  "Platform.find" should {
    "return the correct platform for a given ID" in new WithDB {
      testPlatform.find(testPlatform.Android.PlatformID).PlatformName must beEqualTo(testPlatform.Android.PlatformName)
      testPlatform.find(testPlatform.Ios.PlatformID).PlatformName must beEqualTo(testPlatform.Ios.PlatformName)
    }

    "default to iOS if the platform ID is unknown" in new WithDB {
      val unknownID = 0
      testPlatform.find(unknownID).PlatformName must beEqualTo(testPlatform.Ios.PlatformName)
    }
  }

  "Ios.allAdProviders" should {
    "return the 5 supported ad networks" in new WithDB {
      val allAdProviders = testPlatform.Ios.allAdProviders
      verifyAdProviders(allAdProviders, adProviderNames)
    }
  }

  "Android.allAdProviders" should {
    "return the 5 supported ad networks" in new WithDB {
      val allAdProviders = testPlatform.Android.allAdProviders
      verifyAdProviders(allAdProviders, adProviderNames)
    }
  }

  def verifyAdProviders(allAdProviders: List[UpdatableAdProvider], adProviderNames: List[String]) = {
    allAdProviders.length must beEqualTo(adProviderNames.length)
    adProviderNames.map(adProviderName =>
      allAdProviders.count(_.name == adProviderName) must beEqualTo(1)
    )
  }
}
