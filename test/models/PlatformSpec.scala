package models

class PlatformSpec extends SpecificationWithFixtures {
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
}
