package models

import org.junit.runner._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class VirtualCurrencySpec extends SpecificationWithFixtures with AppSpecSetup {
  "VirtualCurrency.create" should {
    "add a record to the virtual_currencies table" in new WithDB {
      VirtualCurrency.create(currentApp.id, "Coins", 100, Some(500), Some(100), Some(true)) must not beNone
    }
  }

  "VirtualCurrency.find" should {
    "return an instance of VirtualCurrency if one exists" in new WithDB {
      val virtualCurrencyID = VirtualCurrency.create(currentApp.id, "Coins", 100, Some(500), Some(100), Some(true)).get
      VirtualCurrency.find(virtualCurrencyID) must beSome[VirtualCurrency]
    }

    "return None if no record is found" in new WithDB {
      val fakeID = 999
      VirtualCurrency.find(fakeID) must beNone
    }
  }

  "VirtualCurrency.update" should {
    "update the record in the virtual_currencies table." in new WithDB {
      val virtualCurrencyID = VirtualCurrency.create(currentApp.id, "Coins", 100, Some(500), Some(100), Some(true)).get
      val newName = "Energy"
      val updatedVC = new VirtualCurrency(virtualCurrencyID, currentApp.id, newName, 200, Some(600), Some(200), false)
      VirtualCurrency.update(updatedVC) must beEqualTo(1)
      val updatedRecord = VirtualCurrency.find(virtualCurrencyID).get
      updatedRecord.name must beEqualTo(newName)
    }
  }
  step(clean)
}
