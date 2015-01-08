package models

import org.junit.runner._
import org.specs2.runner._
import resources.DistributorUserSetup

@RunWith(classOf[JUnitRunner])
class DistributorSpec extends SpecificationWithFixtures {
  "Distributor.create" should {
    "properly save a new Distributor in the database" in new WithDB {
      Distributor.create("Some Company Name") must haveClass[Some[Long]]
    }
  }

  "Distributor.find" should {
    "return an instance of the Distributor class if the ID is found" in new WithDB with DistributorUserSetup {
      val (_, distributor) = newDistributorUser("newemail1@gmail.com")
      Distributor.find(distributor.id.get).get must haveClass[Distributor]
    }

    "return None if the ID is not found" in new WithDB {
      val fakeDistributorID = 12345
      Distributor.find(fakeDistributorID) must beNone
    }
  }

  "Distributor.setHyprMarketplaceID" should {
    "return 1 and set the hypr_marketplace_id field for the Distributor if the table is updated successfully" in new WithDB with DistributorUserSetup {
      val (_, distributor) = newDistributorUser("newemail2@gmail.com")
      val hyprMarketplaceID = 2
      Distributor.setHyprMarketplaceID(distributor, hyprMarketplaceID) must beEqualTo(1)
      val updatedDistributor = Distributor.find(distributor.id.get).get
      updatedDistributor.hyprMarketplaceID.get must beEqualTo(hyprMarketplaceID)
    }

    "return 0 and not update the distributors table if update is not successful" in new WithDB with DistributorUserSetup {
      val fakeDistributorID = Some(12345L)
      val hyprMarketplaceID = 2
      val fakeDistributor = new Distributor(fakeDistributorID, "Distributor name", Some(hyprMarketplaceID))
      Distributor.setHyprMarketplaceID(fakeDistributor, hyprMarketplaceID) must beEqualTo(0)
    }
  }
}
