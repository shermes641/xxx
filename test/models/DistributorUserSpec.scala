package models

import org.junit.runner._
import org.specs2.runner._
import play.api.test.Helpers._
import play.api.test._
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

@RunWith(classOf[JUnitRunner])
class DistributorUserSpec extends SpecificationWithFixtures {
  val unknownEmail = "SomeUnknownEmail@mail.com"

  "DistributorUser.create" should {
    "add a DistributorUser to the database" in new WithDB {
      DistributorUser.create(email, password, companyName) must not equalTo(false)
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
          user.hashedPassword must not beNull
        }
        case _ => user must beSome[DistributorUser]
      }
    }

    "not create another DistributorUser if the email is already taken" in new WithDB {
      DistributorUser.create(email, password, companyName)
      DistributorUser.create(email, password, companyName) must equalTo(false)
    }

    "not create a DistributorUser for a duplicate email with alternate capitalization" in new WithDB {
      val newUserEmail = "someNewUser@mail.com"
      DistributorUser.create(newUserEmail, password, companyName)
      DistributorUser.create(newUserEmail.toUpperCase, password, companyName) must equalTo(false)
    }
  }

  "DistributorUser.find" should {
    "find a DistributorUser in the database by ID" in new WithDB {
      val newUserEmail = "anotherNewUser@mail.com"
      DistributorUser.create(newUserEmail, password, companyName)
      val user = DistributorUser.findByEmail(newUserEmail).get
      DistributorUser.find(user.id.get).get must haveClass[DistributorUser]
    }

    "return None if a DistributorUser is not found" in new WithDB {
      val unknownUserID = 9999
      DistributorUser.find(unknownUserID) must beNone
    }
  }

  "DistributorUser.findByEmail" should {
    val newUserEmail = "NewUser@mail.com"

    running(FakeApplication(additionalConfiguration = testDB)) {
      DistributorUser.create(newUserEmail, password, companyName)
    }

    "find a DistributorUser in the database by email" in new WithDB {
      DistributorUser.findByEmail(newUserEmail).get must haveClass[DistributorUser]
    }

    "find a DistributorUser in the database regardless of email case" in new WithDB {
      val user = DistributorUser.findByEmail(newUserEmail.toUpperCase).get
      user.email must beEqualTo(newUserEmail)
    }

    "return None if a DistributorUser is not found" in new WithDB {
      DistributorUser.findByEmail(unknownEmail) must beNone
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
  step(clean)
}
