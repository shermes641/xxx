package functional

import models._
import play.api.test._
import play.api.test.Helpers._

class DistributorUsersControllerSpec extends SpecificationWithFixtures {
  "DistributorUsersController.signup" should {
    "disable the submit button if terms are not agreed to" in new WithFakeBrowser {
      goToAndWaitForAngular(controllers.routes.DistributorUsersController.signup.url)
      browser.fill("#company").`with`(companyName)
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.fill("#confirmation").`with`(password)
      browser.find("button").first().isEnabled must beEqualTo(false)
    }

    "hide the license agreement unless the user clicks on the License Agreement link" in new WithFakeBrowser {
      goToAndWaitForAngular(controllers.routes.DistributorUsersController.signup.url)
      browser.find("#termsContainer").first().isDisplayed must beEqualTo(false)
      browser.$("#viewTerms").click()
      browser.find("#termsContainer").first().isDisplayed must beEqualTo(true)
    }
  }

  "DistributorUsersController.create" should {
    "if the user's email is taken, render the sign up page with all fields filled except the password fields" in new WithFakeBrowser {
      DistributorUser.create(email, password, companyName)
      goToAndWaitForAngular(controllers.routes.DistributorUsersController.signup.url)
      browser.fill("#company").`with`(companyName)
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.fill("#confirmation").`with`(password)
      browser.$("#terms").click()
      clickAndWaitForAngular("button")
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
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(user.distributorID.get, None).url)
      browser.pageSource must contain("Welcome Back")
    }
  }

  "Authenticated actions" should {
    "redirect to app index from login if user is authenticated" in new WithFakeBrowser {
      logInUser()
      browser.goTo(controllers.routes.DistributorUsersController.login.url)
      browser.pageSource must contain("Analytics")
    }

    "redirect to app index from signup if user is authenticated" in new WithFakeBrowser {
      logInUser()
      browser.goTo(controllers.routes.DistributorUsersController.signup.url)
      browser.pageSource must contain("Analytics")
    }
  }
}
