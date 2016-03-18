package models

import controllers.DistributorUsersController.PasswordUpdate
import org.junit.runner._
import org.specs2.runner._
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import com.typesafe.config.ConfigFactory
import resources.{SpecificationWithFixtures, DistributorUserSetup}

@RunWith(classOf[JUnitRunner])
class DistributorUserSpec extends SpecificationWithFixtures {
  val unknownEmail = "SomeUnknownEmail@mail.com"

  "DistributorUser.create" should {
    "add a DistributorUser to the database and return the new ID" in new WithDB {
      DistributorUser.create("newemail1@gmail.com", password, companyName) must haveClass[Some[Long]]
    }

    "not create another DistributorUser if the email is already taken" in new WithDB {
      val userEmail = "newemail2@gmail.com"
      DistributorUser.create(userEmail, password, companyName) must haveClass[Some[Long]]
      DistributorUser.create(userEmail, password, companyName) must beNone
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

  "Welcome Email Actor" should {
    "exist and accept email message to both user and Hypr Team" in new WithDB {
      implicit val actorSystem = ActorSystem("testActorSystem", ConfigFactory.load())
      val emailActor = TestActorRef(new WelcomeEmailActor()).underlyingActor
      emailActor.receive(sendUserCreationEmail("test@test.com", "company name", "some-ip-address"))
      emailActor must haveClass[WelcomeEmailActor]
    }
  }

  "DistributorUser.updatePassword" should {
    "not update the password if the distributor user ID of the argument does not match the distributor user ID found in the database" in new WithDB with DistributorUserSetup {
      val (user, _) = newDistributorUser("newemail11@gmail.com")
      val newPassword = "some new password"
      val unknownDistributorUserID = 9999
      val passwordUpdate = PasswordUpdate(user.email, unknownDistributorUserID, token = "token", password = newPassword, passwordConfirmation = newPassword)
      DistributorUser.updatePassword(passwordUpdate) must beEqualTo(0)
      DistributorUser.find(user.id.get).get.hashedPassword must beEqualTo(user.hashedPassword)
    }

    "not update the password if no user is found" in new WithDB with DistributorUserSetup {
      val (user, _) = newDistributorUser("newemail12@gmail.com")
      val newPassword = "some new password"
      val unknownEmail = "some unknown email"
      val passwordUpdate = PasswordUpdate(unknownEmail, user.id.get, token = "token", password = newPassword, passwordConfirmation = newPassword)
      DistributorUser.updatePassword(passwordUpdate) must beEqualTo(0)
      DistributorUser.find(user.id.get).get.hashedPassword must beEqualTo(user.hashedPassword)
    }

    "change the hashed password stored in the database" in new WithDB with DistributorUserSetup {
      val (user, _) = newDistributorUser("newemail13@gmail.com")
      val newPassword = "some new password"
      val unknownEmail = "some unknown email"
      val passwordUpdate = PasswordUpdate(user.email, user.id.get, token = "token", password = newPassword, passwordConfirmation = newPassword)
      DistributorUser.updatePassword(passwordUpdate) must beEqualTo(1)
      DistributorUser.find(user.id.get).get.hashedPassword must not equalTo user.hashedPassword
    }
  }
}
