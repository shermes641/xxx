package functional

import models._
import play.api.test._
import play.api.test.Helpers._

class DistributorUsersControllerSpec extends SpecificationWithFixtures {
  val request = FakeRequest(POST, "/distributor_users").withFormUrlEncodedBody(
    "company" -> companyName,
    "email" -> "user@jungroup.com",
    "password" -> password,
    "confirmation" -> password,
    "terms" -> "true"
  )

  "Sign up page" should {
    "render application index page when sign up is successful" in new WithFakeBrowser {
      browser.goTo("http://localhost:" + port + "/signup")
      browser.fill("#company").`with`(companyName)
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.fill("#confirmation").`with`(password)
      browser.$("#terms").click()
      browser.click("button")
      browser.pageSource must contain("My Apps")
    }

    "display an error if terms are not agreed to" in new WithFakeBrowser {
      browser.goTo("http://localhost:" + port + "/signup")
      browser.fill("#company").`with`(companyName)
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.fill("#confirmation").`with`(password)
      browser.click("button")
      browser.pageSource must contain("Please agree to our terms")
    }

    "redirect to the login page if the user's email is taken" in new WithFakeBrowser {
      DistributorUser.create(email, password, companyName)
      browser.goTo("http://localhost:" + port + "/signup")
      browser.fill("#company").`with`(companyName)
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.fill("#confirmation").`with`(password)
      browser.$("#terms").click()
      browser.click("button")
      browser.pageSource must contain("Log In")
    }

    "respond with a 201 when sign up is successful" in new WithFakeBrowser {
      val Some(result) = route(request)
      status(result) must equalTo(201)
    }

    "respond with a 303 when email is taken" in new WithFakeBrowser {
      DistributorUser.create(email, password, companyName)
      val Some(result) = route(request)
      status(result) must equalTo(303)
    }
  }
  step(clean)
}
