package models

import org.junit.runner._
import org.specs2.runner._
import resources.DistributorUserSetup

@RunWith(classOf[JUnitRunner])
class DistributorUserSpec extends SpecificationWithFixtures {
  val unknownEmail = "SomeUnknownEmail@mail.com"

  "DistributorUser.create" should {
    "add a DistributorUser to the database" in new WithDB {
      DistributorUser.create(email, password, companyName).must(not).beNone
    }

    "create a Distributor" in new WithDB {
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

    "properly save a DistributorUser's information" in new WithDB {
      DistributorUser.create(email, password, companyName)
      val user = DistributorUser.findByEmail(email)
      user match {
        case Some(user) => {
          user.email must beEqualTo(email)
          user.hashedPassword.must(not).beNull
        }
        case _ => user must beSome[DistributorUser]
      }
    }

    "not create another DistributorUser if the email is already taken" in new WithDB {
      DistributorUser.create(email, password, companyName)
      DistributorUser.create(email, password, companyName) must beNone
    }

    "not create a DistributorUser for a duplicate email with alternate capitalization" in new WithDB {
      val newUserEmail = "someNewUser@mail.com"
      DistributorUser.create(newUserEmail, password, companyName) must haveClass[Some[Long]]
      DistributorUser.create(newUserEmail.toUpperCase, password, companyName) must beNone
    }

    "create a Distributor to which the DistributorUser will belong" in new WithDB {
      val distributorUser = {
        val id = DistributorUser.create("newemail3@gmail.com", password, companyName).get
        DistributorUser.find(id).get
      }
      Distributor.find(distributorUser.distributorID.get).get must haveClass[Distributor]
    }
  }

  "DistributorUser.find" should {
    "find a DistributorUser in the database by ID" in new WithDB with DistributorUserSetup {
      val (distributorUser, _) = newDistributorUser("newemail4@gmail.com")
      DistributorUser.find(distributorUser.id.get).get must haveClass[DistributorUser]
    }

    "return None if a DistributorUser is not found" in new WithDB {
      val unknownUserID = 9999
      DistributorUser.find(unknownUserID) must beNone
    }
  }

  "DistributorUser.findByEmail" should {
    "find a DistributorUser in the database by email" in new WithDB with DistributorUserSetup {
      val (distributorUser, _) = newDistributorUser("newemail5@gmail.com")
      DistributorUser.findByEmail(distributorUser.email).get must haveClass[DistributorUser]
    }

    "find a DistributorUser in the database regardless of email case" in new WithDB with DistributorUserSetup {
      val newEmail = "newemail6@gmail.com"
      val (distributorUser, _) = newDistributorUser(newEmail)
      val user = DistributorUser.findByEmail(newEmail.toUpperCase).get
      user.email must beEqualTo(newEmail)
    }

    "return None if a DistributorUser is not found" in new WithDB {
      val unknownEmail = "some-fake-email"
      DistributorUser.findByEmail(unknownEmail) must beNone
    }
  }

  "DistributorUser.checkPassword" should {
    "return false if the email/password combination does not match the password hash stored in the database" in new WithDB with DistributorUserSetup {
      val newPassword = "password"
      val (distributorUser, _) = newDistributorUser("newemail7@gmail.com", newPassword)
      DistributorUser.checkPassword(distributorUser.email, newPassword) must beTrue
    }

    "return true if the email/password combination matches the password hash stored in the database" in new WithDB with DistributorUserSetup {
      val newPassword = "password"
      val wrongPassword = "wrongpassword"
      val (distributorUser, _) = newDistributorUser("newemail8@gmail.com", newPassword)
      DistributorUser.checkPassword(distributorUser.email, wrongPassword) must beFalse
    }
  }

  "DistributorUser.update" should {
    "return 1 and update the DistributorUser record in the database" in new WithDB with DistributorUserSetup {
      val (distributorUser, _) = newDistributorUser("newemail9@gmail.com")
      val newEmail = "newemail10@gmail.com"
      val updatedUser = new DistributorUser(distributorUser.id, newEmail, distributorUser.hashedPassword, distributorUser.distributorID)
      DistributorUser.update(updatedUser) must beEqualTo(1)
      DistributorUser.find(distributorUser.id.get).get.email must beEqualTo(newEmail)
    }
  }

  "DistributorUser.isNotActive" should {
    "return true if the DistributorUser is not active" in new WithDB {
      val distributorUserID = DistributorUser.create("newemail11@gmail.com", "password", "New Company").get
      DistributorUser.isNotActive(distributorUserID) must beTrue
    }

    "return false if the DistributorUser is active" in new WithDB with DistributorUserSetup {
      val (distributorUser, _) = newDistributorUser("newemail12@gmail.com")
      DistributorUser.setActive(distributorUser)
      DistributorUser.isNotActive(distributorUser.id.get) must beFalse
    }
  }

  "DistributorUser.setActive" should {
    "return 1 and set the active attribute to true for the DistributorUser" in new WithDB with DistributorUserSetup {
      val (distributorUser, _) = newDistributorUser("newemail13@gmail.com")
      DistributorUser.setActive(distributorUser) must beEqualTo(1)
      DistributorUser.isNotActive(distributorUser.id.get) must beFalse
    }

    "return 0 and not set the active attribute to true for the DistributorUser" in new WithDB with DistributorUserSetup {
      val (distributorUser, _) = newDistributorUser("newemail14@gmail.com")
      val fakeDistributorID = Some(12345L)
      val fakeDistributorUser = new DistributorUser(fakeDistributorID, "fake-email", "password", None)
      DistributorUser.setActive(fakeDistributorUser) must beEqualTo(0)
    }
  }

  "Welcome Email Actor" should {
    "exist and accept email message to both user and Hypr Team" in new WithDB {
      implicit val actorSystem = ActorSystem("testActorSystem", ConfigFactory.load())
      val emailActor = TestActorRef(new WelcomeEmailActor()).underlyingActor
      emailActor.receive("test@test.com")
      emailActor must haveClass[WelcomeEmailActor]
    }
  }
}
