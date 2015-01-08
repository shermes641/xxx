package functional

import models._
import play.api.test._
import play.api.test.Helpers._

class DistributorUsersControllerSpec extends SpecificationWithFixtures {
  "DistributorUsersController.signup" should {
    "disable the submit button if terms are not agreed to" in new WithFakeBrowser {
      browser.goTo(controllers.routes.DistributorUsersController.signup.url)
      browser.fill("#company").`with`(companyName)
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.fill("#confirmation").`with`(password)
      browser.find("button").first().isEnabled must beEqualTo(false)
    }

    "hide the license agreement unless the user clicks on the License Agreement link" in new WithFakeBrowser {
      browser.goTo(controllers.routes.DistributorUsersController.signup.url)
      browser.find("#termsContainer").first().isDisplayed must beEqualTo(false)
      browser.$("#viewTerms").click()
      browser.find("#termsContainer").first().isDisplayed must beEqualTo(true)
    }
  }

  "DistributorUsersController.create" should {
    "create a pending DistributorUser and render pending page when sign up is successful" in new WithFakeBrowser {
      browser.goTo(controllers.routes.DistributorUsersController.signup.url)
      browser.fill("#company").`with`(companyName)
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.fill("#confirmation").`with`(password)
      browser.$("#terms").click()
      browser.find("button").first().isEnabled must beEqualTo(true)
      browser.click("button")
      browser.pageSource must contain("pending")
      val userID = DistributorUser.findByEmail(email).get.id.get
      DistributorUser.isNotActive(userID) must beEqualTo(true)
    }

    "if the user's email is taken, render the sign up page with all fields filled except the password fields" in new WithFakeBrowser {
      DistributorUser.create(email, password, companyName)
      browser.goTo(controllers.routes.DistributorUsersController.signup.url)
      browser.fill("#company").`with`(companyName)
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.fill("#confirmation").`with`(password)
      browser.$("#terms").click()
      browser.click("button")
      browser.pageSource must contain("This email has been registered already")
      browser.find("#company").getValue must beEqualTo(companyName)
      browser.find("#email").getValue must beEqualTo(email)
      browser.find("#password").getValue must beEqualTo("")
      browser.find("#confirmation").getValue must beEqualTo("")
    }

    "respond with a 303 when email is taken" in new WithFakeBrowser {
      val request = FakeRequest(POST, "/distributor_users").withFormUrlEncodedBody(
        "company" -> companyName,
        "email" -> "user@jungroup.com",
        "password" -> password,
        "confirmation" -> password,
        "terms" -> "true"
      )
      DistributorUser.create(email, password, companyName)
      val Some(result) = route(request)
      status(result) must equalTo(303)
    }
  }

  "DistributorUsersController.logout" should {
    "clear the session and redirect the DistributorUser to the login page" in new WithFakeBrowser {
      val user = DistributorUser.findByEmail(email).get
      logInUser()
      browser.goTo(controllers.routes.DistributorUsersController.logout.url)
      browser.goTo(controllers.routes.AppsController.index(user.distributorID.get).url)
      browser.pageSource must contain("Sign In")
    }
  }

  "Authenticated actions" should {
    "redirect to pending if the user account is not active" in new WithFakeBrowser {
      val user = DistributorUser.findByEmail(email).get
      logInUser()
      browser.goTo(controllers.routes.AppsController.index(user.distributorID.get).url)
      browser.pageSource must contain("pending")
    }

    "redirect to login if the user is not authenticated" in new WithFakeBrowser {
      val email1 = "Email 1"
      val email2 = "Email 2"
      DistributorUser.create(email1, password, companyName)
      DistributorUser.create(email2, password, companyName)
      val user1 = DistributorUser.findByEmail(email1).get
      val user2 = DistributorUser.findByEmail(email2).get
      browser.goTo(controllers.routes.AppsController.index(user2.distributorID.get).url)
      browser.pageSource must contain("Log In")
    }

    "redirect to app index from login if user is authenticated" in new WithFakeBrowser {
      DistributorUser.setActive(DistributorUser.findByEmail(email).get)
      logInUser()
      browser.goTo(controllers.routes.DistributorUsersController.login.url)
      browser.pageSource must contain("Begin by creating an app")
    }

    "redirect to app index from signup if user is authenticated" in new WithFakeBrowser {
      DistributorUser.setActive(DistributorUser.findByEmail(email).get)
      logInUser()
      browser.goTo(controllers.routes.DistributorUsersController.signup.url)
      browser.pageSource must contain("Begin by creating an app")
    }
  }
}
