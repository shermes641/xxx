package models

import org.junit.runner._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class DistributorSpec extends SpecificationWithFixtures {
  "Distributor.create" should {
    "properly save a new Distributor in the database" in new WithFakeDB {
      val name = "Some Company Name"
      val id = Distributor.create(name)
      val distributor = Distributor.find(id.get)
      distributor match {
        case Some(distributor) => {
          distributor.name must beEqualTo(name)
        }
        case _ => {
          distributor must beSome[Distributor]
        }
      }
    }
  }
}
