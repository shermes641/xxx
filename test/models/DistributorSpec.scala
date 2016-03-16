package models

import org.junit.runner._
import org.specs2.runner._
import resources.{SpecificationWithFixtures, DistributorUserSetup}

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
}
