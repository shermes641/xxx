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
    "render apps index page when sign up is successful" in new WithFakeBrowser {
      browser.goTo("http://localhost:" + port + "/signup")
      browser.fill("#company").`with`(companyName)
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.fill("#confirmation").`with`(password)
      browser.$("#terms").click()
      browser.find("button").first().isEnabled must beEqualTo(true)
      browser.click("button")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#flash").areDisplayed()
      val user = DistributorUser.findByEmail(email).get
      browser.url() must beEqualTo("/distributors/" + user.distributorID.get + "/apps")
    }

    "disable the submit button if terms are not agreed to" in new WithFakeBrowser {
      browser.goTo("http://localhost:" + port + "/signup")
      browser.fill("#company").`with`(companyName)
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.fill("#confirmation").`with`(password)
      browser.find("button").first().isEnabled must beEqualTo(false)
    }

    "hide the license agreement unless the user clicks on the License Agreement link" in new WithFakeBrowser {
      browser.goTo("http://localhost:" + port + "/signup")
      browser.find("#termsContainer").first().isDisplayed must beEqualTo(false)
      browser.$("#viewTerms").click()
      browser.find("#termsContainer").first().isDisplayed must beEqualTo(true)
    }

    "redirect to the sign up page if the user's email is taken" in new WithFakeBrowser {
      DistributorUser.create(email, password, companyName)
      browser.goTo("http://localhost:" + port + "/signup")
      browser.fill("#company").`with`(companyName)
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.fill("#confirmation").`with`(password)
      browser.$("#terms").click()
      browser.click("button")
      browser.pageSource must contain("Sign Up")
    }

    "respond with a 303 when email is taken" in new WithFakeBrowser {
      DistributorUser.create(email, password, companyName)
      val Some(result) = route(request)
      status(result) must equalTo(303)
    }
  }

  "Authenticated actions" should {
    "redirect to login if the user is not authenticated" in new WithFakeBrowser {
      val baseURL = "http://localhost:" + port
      val email1 = "Email 1"
      val email2 = "Email 2"
      DistributorUser.create(email1, password, companyName)
      DistributorUser.create(email2, password, companyName)
      val user1 = DistributorUser.findByEmail(email1).get
      val user2 = DistributorUser.findByEmail(email2).get
      logInUser()
      browser.goTo(baseURL + "/distributors/" + user2.distributorID.get + "/apps")
      browser.pageSource must contain("Log In")
    }
  }
  step(clean)
}
