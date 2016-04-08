package models

import controllers._
import org.junit.runner._
import org.specs2.runner._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.mailer.MailerClient
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import com.typesafe.config.ConfigFactory
import resources.{SpecificationWithFixtures, DistributorUserSetup}

@RunWith(classOf[JUnitRunner])
class DistributorUserSpec extends SpecificationWithFixtures {
  val unknownEmail = "SomeUnknownEmail@mail.com"

  "DistributorUserService.create" should {
    "add a DistributorUser to the database and return the new ID" in new WithDB {
      distributorUserService.create("newemail1@gmail.com", password, companyName) must haveClass[Some[Long]]
    }

    "not create another DistributorUser if the email is already taken" in new WithDB {
      val userEmail = "newemail2@gmail.com"
      distributorUserService.create(userEmail, password, companyName) must haveClass[Some[Long]]
      distributorUserService.create(userEmail, password, companyName) must beNone
    }

      "not create a DistributorUser for a duplicate email with alternate capitalization" in new WithDB {
      val newUserEmail = "someNewUser@mail.com"
      distributorUserService.create(newUserEmail, password, companyName) must haveClass[Some[Long]]
      distributorUserService.create(newUserEmail.toUpperCase, password, companyName) must beNone
    }

    "create a Distributor to which the DistributorUser will belong" in new WithDB {
      val distributorUser = {
        val id = distributorUserService.create("newemail3@gmail.com", password, companyName).get
        distributorUserService.find(id).get
      }
      distributorService.find(distributorUser.distributorID.get).get must haveClass[Distributor]
    }
  }

  "DistributorUserService.find" should {
    "find a DistributorUser in the database by ID" in new WithDB with DistributorUserSetup {
      override val distributorModel = distributorService
      override val userService = distributorUserService
      val (distributorUser, _) = newDistributorUser("newemail4@gmail.com")
      distributorUserService.find(distributorUser.id.get).get must haveClass[DistributorUser]
    }

    "return None if a DistributorUser is not found" in new WithDB {
      val unknownUserID = 9999
      distributorUserService.find(unknownUserID) must beNone
    }
  }

  "DistributorUserService.findByEmail" should {
    "find a DistributorUser in the database by email" in new WithDB with DistributorUserSetup {
      override val distributorModel = distributorService
      override val userService = distributorUserService
      val (distributorUser, _) = newDistributorUser("newemail5@gmail.com")
      distributorUserService.findByEmail(distributorUser.email).get must haveClass[DistributorUser]
    }

    "find a DistributorUser in the database regardless of email case" in new WithDB with DistributorUserSetup {
      override val distributorModel = distributorService
      override val userService = distributorUserService
      val newEmail = "newemail6@gmail.com"
      val (distributorUser, _) = newDistributorUser(newEmail)
      val user = distributorUserService.findByEmail(newEmail.toUpperCase).get
      user.email must beEqualTo(newEmail)
    }

    "return None if a DistributorUser is not found" in new WithDB {
      val unknownEmail = "some-fake-email"
      distributorUserService.findByEmail(unknownEmail) must beNone
    }
  }

  "DistributorUserService.checkPassword" should {
    "return false if the email/password combination does not match the password hash stored in the database" in new WithDB with DistributorUserSetup {
      override val distributorModel = distributorService
      override val userService = distributorUserService
      val newPassword = "password"
      val (distributorUser, _) = newDistributorUser("newemail7@gmail.com", newPassword)
      distributorUserService.checkPassword(distributorUser.email, newPassword) must beTrue
    }

    "return true if the email/password combination matches the password hash stored in the database" in new WithDB with DistributorUserSetup {
      override val distributorModel = distributorService
      override val userService = distributorUserService
      val newPassword = "password"
      val wrongPassword = "wrongpassword"
      val (distributorUser, _) = newDistributorUser("newemail8@gmail.com", newPassword)
      distributorUserService.checkPassword(distributorUser.email, wrongPassword) must beFalse
    }
  }

  "DistributorUserService.update" should {
    "return 1 and update the DistributorUser record in the database" in new WithDB with DistributorUserSetup {
      override val distributorModel = distributorService
      override val userService = distributorUserService
      val (distributorUser, _) = newDistributorUser("newemail9@gmail.com")
      val newEmail = "newemail10@gmail.com"
      val updatedUser = new DistributorUser(distributorUser.id, newEmail, distributorUser.hashedPassword, distributorUser.distributorID)
      distributorUserService.update(updatedUser) must beEqualTo(1)
      distributorUserService.find(distributorUser.id.get).get.email must beEqualTo(newEmail)
    }
  }

  "Welcome Email Actor" should {
    "exist and accept email message to both user and Hypr Team" in new WithDB {
      implicit val actorSystem = ActorSystem("testActorSystem", ConfigFactory.load())
      val injector = new GuiceApplicationBuilder().injector()
      val mailerClient: MailerClient = injector.instanceOf[MailerClient]
      val mailer = new Mailer(mailerClient, appEnvironment)
      val emailActor = TestActorRef(new WelcomeEmailActor(mailer, configVars)).underlyingActor
      emailActor.receive(sendUserCreationEmail("test@test.com", "company name", "some-ip-address"))
      emailActor must haveClass[WelcomeEmailActor]
    }
  }

  "DistributorUserService.updatePassword" should {
    "not update the password if the distributor user ID of the argument does not match the distributor user ID found in the database" in new WithDB with DistributorUserSetup {
      override val distributorModel = distributorService
      override val userService = distributorUserService
      val (user, _) = newDistributorUser("newemail11@gmail.com")
      val newPassword = "some new password"
      val unknownDistributorUserID = 9999
      val passwordUpdate = PasswordUpdate(user.email, unknownDistributorUserID, token = "token", password = newPassword, passwordConfirmation = newPassword)
      distributorUserService.updatePassword(passwordUpdate) must beEqualTo(0)
      distributorUserService.find(user.id.get).get.hashedPassword must beEqualTo(user.hashedPassword)
    }

    "not update the password if no user is found" in new WithDB with DistributorUserSetup {
      override val distributorModel = distributorService
      override val userService = distributorUserService
      val (user, _) = newDistributorUser("newemail12@gmail.com")
      val newPassword = "some new password"
      val unknownEmail = "some unknown email"
      val passwordUpdate = PasswordUpdate(unknownEmail, user.id.get, token = "token", password = newPassword, passwordConfirmation = newPassword)
      distributorUserService.updatePassword(passwordUpdate) must beEqualTo(0)
      distributorUserService.find(user.id.get).get.hashedPassword must beEqualTo(user.hashedPassword)
    }

    "change the hashed password stored in the database" in new WithDB with DistributorUserSetup {
      override val distributorModel = distributorService
      override val userService = distributorUserService
      val (user, _) = newDistributorUser("newemail13@gmail.com")
      val newPassword = "some new password"
      val unknownEmail = "some unknown email"
      val passwordUpdate = PasswordUpdate(user.email, user.id.get, token = "token", password = newPassword, passwordConfirmation = newPassword)
      distributorUserService.updatePassword(passwordUpdate) must beEqualTo(1)
      distributorUserService.find(user.id.get).get.hashedPassword must not equalTo user.hashedPassword
    }
  }
}
