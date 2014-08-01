package models

import org.junit.runner._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class DistributorUserSpec extends SpecificationWithFixtures {
  "DistributorUser.create" should {
    val email = "tdepplito@gmail.com"

    "add a DistributorUser to the database" in new WithFakeDB {
      DistributorUser.create(email, "password")
      DistributorUser.findByEmail(email) must beSome[DistributorUser]
    }

    "properly save a DistributorUser's information" in new WithFakeDB {
      DistributorUser.create(email, "password")
      val user = DistributorUser.findByEmail(email)
      user match {
        case Some(user) => {
          user.email must beEqualTo(email)
          user.hashed_password must not beNull
        }
        case _ => user must beSome[DistributorUser]
      }
    }
  }
}
