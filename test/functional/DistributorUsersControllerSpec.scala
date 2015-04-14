package functional

import models._
import play.api.mvc.Session
import play.api.test._
import play.api.test.Helpers._

class DistributorUsersControllerSpec extends SpecificationWithFixtures with AppCreationHelper {
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

    "render an error when the password does not match the confirmation" in new WithFakeBrowser {
      goToAndWaitForAngular(controllers.routes.DistributorUsersController.signup.url)
      browser.fill("#company").`with`("New Test Company")
      browser.fill("#email").`with`("UniqueEmail@gmail.com")
      browser.fill("#password").`with`("password1")
      browser.fill("#confirmation").`with`("password2")
      browser.$("#terms").click()
      browser.find("#confirmation-errors").first().isDisplayed
      browser.find("#confirmation-errors").first().getText must beEqualTo("Password confirmation doesn't match Password.")
    }
  }

  "DistributorUsersController.create" should {
    "if the user's email is taken, render the sign up page with all fields filled" in new WithFakeBrowser {
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
    }

    "respond with a 400 when email is taken" in new WithFakeBrowser {
      val request = FakeRequest(POST, "/distributor_users").withFormUrlEncodedBody(
        "company" -> companyName,
        "email" -> "user@jungroup.com",
        "password" -> password,
        "confirmation" -> password,
        "terms" -> "true"
      )
      DistributorUser.create(email, password, companyName)
      val Some(result) = route(request)
      status(result) must equalTo(400)
    }
  }

  "DistributorUsersController.logout" should {
    "clear the session and redirect the DistributorUser to the login page" in new WithFakeBrowser {
      val user = DistributorUser.findByEmail(email).get
      logInUser()
      browser.goTo(controllers.routes.DistributorUsersController.logout.url)
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(user.distributorID.get, None, None).url)
      browser.pageSource must contain("Welcome Back")
    }
  }

  "Authenticated actions" should {
    "redirect to the Analytics page from login if user is authenticated" in new WithFakeBrowser {
      val user = DistributorUser.findByEmail(email).get
      setUpApp(user.distributorID.get)

      logInUser()

      browser.goTo(controllers.routes.DistributorUsersController.login(None).url)
      browser.url() must beEqualTo(controllers.routes.AnalyticsController.show(user.distributorID.get, None, None).url)
    }

    "redirect to the Analytics page from signup if user is authenticated" in new WithFakeBrowser {
      val user = DistributorUser.findByEmail(email).get
      setUpApp(user.distributorID.get)

      logInUser()

      browser.goTo(controllers.routes.DistributorUsersController.signup.url)
      browser.url() must beEqualTo(controllers.routes.AnalyticsController.show(user.distributorID.get, None, None).url)
    }
  }

  "DistributorUsersController.login" should {
    "clear the session if the email stored in the session is not found in the database" in new WithFakeBrowser {
      val fakeDistributorID = "100"
      val fakeEmail = "somefakeemail"
      val request = FakeRequest(
        GET,
        controllers.routes.DistributorUsersController.login(None).url,
        FakeHeaders(Seq()),
        ""
      )
      val Some(result) = route(request.withSession("distributorID" -> fakeDistributorID, "username" -> fakeEmail))
      val sessionCookie = cookies(result).get("PLAY_SESSION")
      val currentSession = Session.decodeFromCookie(sessionCookie)
      status(result) must beEqualTo(200)
      currentSession.get("username") must beNone
      currentSession.get("distributorID") must beNone
    }
  }
}
