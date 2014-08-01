package models

import org.junit.runner._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class DistributorUserSpec extends SpecificationWithFixtures {
  "DistributorUser.create" should {
    "add a DistributorUser to the database" in new WithFakeDB {
      DistributorUser.create(email, password, companyName)
      DistributorUser.findByEmail(email) must beSome[DistributorUser]
    }

    "create a Distributor" in new WithFakeDB {
      DistributorUser.create(email, password, companyName)
      val user = DistributorUser.findByEmail(email)
      user match {
        case Some(user) => {
          val distributor = Distributor.find(user.distributorID.get)
          distributor match {
            case Some(distributor) => distributor.name must beEqualTo(companyName)
            case _ => distributor must beSome[Distributor]
          }
        }
        case _ => user must beSome[DistributorUser]
      }
    }

    "properly save a DistributorUser's information" in new WithFakeDB {
      DistributorUser.create(email, password, companyName)
      val user = DistributorUser.findByEmail(email)
      user match {
        case Some(user) => {
          user.email must beEqualTo(email)
          user.hashedPassword must not beNull
        }
        case _ => user must beSome[DistributorUser]
      }
    }

    "not create another DistributionUser if the email is already taken" in new WithFakeDB {
      DistributorUser.create(email, password, companyName)
      DistributorUser.create(email, password, companyName) must equalTo(false)
    }
  }
}
