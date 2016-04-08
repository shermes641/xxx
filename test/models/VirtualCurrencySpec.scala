package models

import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import resources.{SpecificationWithFixtures, DistributorUserSetup}

@RunWith(classOf[JUnitRunner])
class VirtualCurrencySpec extends SpecificationWithFixtures {
  val currentApp = running(testApplication) {
    val id = appService.create(distributor.id.get, "App 1", testPlatform.Ios.PlatformID).get
    appService.find(id).get
  }

  "VirtualCurrency.create" should {
    "add a record to the virtual_currencies table" in new WithDB {
      virtualCurrencyService.create(currentApp.id, "Coins", 100, 500, Some(100), Some(true)) must not equalTo None
    }
  }

  "VirtualCurrency.find" should {
    "return an instance of VirtualCurrency if one exists" in new WithDB {
      val virtualCurrencyID = virtualCurrencyService.create(currentApp.id, "Coins", 100, 500, Some(100), Some(true)).get
      virtualCurrencyService.find(virtualCurrencyID) must beSome[VirtualCurrency]
    }

    "return None if no record is found" in new WithDB {
      val fakeID = 999
      virtualCurrencyService.find(fakeID) must beNone
    }
  }

  "VirtualCurrency.update" should {
    "update the record in the virtual_currencies table." in new WithDB {
      val virtualCurrencyID = virtualCurrencyService.create(currentApp.id, "Coins", 100, 500, Some(100), Some(true)).get
      val newName = "Energy"
      val updatedVC = new VirtualCurrency(virtualCurrencyID, currentApp.id, newName, 200, 600, Some(200), false)
      virtualCurrencyService.update(updatedVC) must beEqualTo(1)
      val updatedRecord = virtualCurrencyService.find(virtualCurrencyID).get
      updatedRecord.name must beEqualTo(newName)
    }
  }
}
