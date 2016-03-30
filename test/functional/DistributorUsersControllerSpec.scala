package functional

import anorm._
import com.github.nscala_time.time.Imports._
import models._
import play.api.db.DB
import play.api.mvc.Session
import play.api.test._
import play.api.test.Helpers._
import java.security.MessageDigest
import resources.{AppCreationHelper, SpecificationWithFixtures, DistributorUserSetup}

import scala.concurrent.duration.Duration

class DistributorUsersControllerSpec extends SpecificationWithFixtures with AppCreationHelper {
  "DistributorUsersController.sendPasswordResetEmail" should {
    "create a new password reset record and alert the user of a password reset email" in new WithFakeBrowser {
      val originalResetCount = tableCount("password_resets")
      val newUser = {
        val id = DistributorUser.create("UniqueUser4@gmail.com", password = "password", company = "new company").get
        DistributorUser.find(id).get
      }
      goToAndWaitForAngular(controllers.routes.DistributorUsersController.login(None).url)
      clickAndWaitForAngular("#forgot-password-link")
      browser.fill("#email").`with`(newUser.email)
      browser.find("button").first.click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#login-message").containsText("Password reset email sent!")
      tableCount("password_resets") must beEqualTo(originalResetCount+1).eventually(5, Duration(1000, "millis"))
    }
  }

  "DistributorUsersController.signup" should {
    "disable the submit button if terms are not agreed to" in new WithFakeBrowser {
      goToAndWaitForAngular(controllers.routes.DistributorUsersController.signup().url)
      browser.fill("#company").`with`(companyName)
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.fill("#confirmation").`with`(password)
      browser.find("button").first().isEnabled must beEqualTo(false)
    }

    "hide the license agreement unless the user clicks on the License Agreement link" in new WithFakeBrowser {
      goToAndWaitForAngular(controllers.routes.DistributorUsersController.signup().url)
      browser.find("#termsContainer").first().isDisplayed must beEqualTo(false)
      browser.$("#viewTerms").click()
      browser.find("#termsContainer").first().isDisplayed must beEqualTo(true)
    }

    "license agreement must be correct" in new WithFakeBrowser {
      goToAndWaitForAngular(controllers.routes.DistributorUsersController.signup().url)
      browser.$("#viewTerms").click()
      val terms = browser.find("#termsContainer").first().getText
      // Gets MD5 of agreements and compares to last approved version
      MessageDigest.getInstance("MD5").digest(terms.getBytes).map("%02x".format(_)).mkString must beEqualTo("399d0be6511845c449c1418b3df26be2")
      terms must contain("Updated: 03/11/2016 Revision: 5.1")
    }

    "render an error when the password does not match the confirmation" in new WithFakeBrowser {
      goToAndWaitForAngular(controllers.routes.DistributorUsersController.signup().url)
      browser.fill("#company").`with`("New Test Company")
      browser.fill("#email").`with`("UniqueEmail@gmail.com")
      browser.fill("#password").`with`("password1")
      browser.fill("#confirmation").`with`("password2")
      browser.$("#terms").click()
      browser.find("#confirmation-errors").first().isDisplayed
      browser.find("#confirmation-errors").first().getText must beEqualTo("Passwords do not match")
    }

    "not display the welcome email flash message unless a user has just signed up" in new WithFakeBrowser {
      goToAndWaitForAngular(controllers.routes.DistributorUsersController.signup().url)
      browser.fill("#company").`with`(companyName)
      browser.fill("#email").`with`("UniqueUser1@gmail.com")
      browser.fill("#password").`with`(password)
      browser.fill("#confirmation").`with`(password)
      browser.$("#terms").click()
      browser.find("button").first.click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#new-app-message").containsText("Your confirmation email will arrive shortly.")
      browser.goTo(controllers.routes.Application.index().url)
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#new-app-message").areNotDisplayed
    }

    "display the latest email error" in new WithFakeBrowser {
      val distributorUserEmail = "UniqueUser2@gmail.com"
      DistributorUser.create(distributorUserEmail, password = "password", company = "new company")
      goToAndWaitForAngular(controllers.routes.DistributorUsersController.signup().url)
      browser.fill("#company").`with`(companyName)
      browser.fill("#email").`with`(distributorUserEmail)
      browser.fill("#password").`with`(password)
      browser.fill("#confirmation").`with`(password)
      browser.$("#terms").click()
      browser.find("button").first.click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#email-custom-error").containsText("This email has been registered already.")
      browser.fill("#email").`with`("")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#email-custom-error").hasText("")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#email-required-error").areDisplayed
    }
  }

  "DistributorUsersController.create" should {
    "if the user's email is taken, render the sign up page with all fields filled" in new WithFakeBrowser {
      DistributorUser.create(email, password, companyName)
      goToAndWaitForAngular(controllers.routes.DistributorUsersController.signup().url)
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
      browser.goTo(controllers.routes.DistributorUsersController.logout().url)
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

      browser.goTo(controllers.routes.DistributorUsersController.signup().url)
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

    "display the latest password error" in new WithFakeBrowser {
      val distributorUserEmail = "UniqueUser3@gmail.com"
      DistributorUser.create(distributorUserEmail, password = "password", company = "new company")
      goToAndWaitForAngular(controllers.routes.DistributorUsersController.login(None).url)
      browser.fill("#email").`with`(distributorUserEmail)
      browser.fill("#password").`with`("incorrect password")
      browser.find("button").first.click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#password-custom-error").containsText("Invalid Password.")
      browser.fill("#password").`with`("")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#password-custom-error").hasText("")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#password-required-error").areDisplayed
    }
  }

  "DistributorUsersController.updatePassword" should {
    "update the password for a distributor user if the token is valid" in new WithFakeBrowser with DistributorUserSetup {
      val (newUser, _) = newDistributorUser("UniqueUser5@gmail.com")
      setUpApp(newUser.distributorID.get)
      val resetToken = PasswordReset.create(newUser.id.get)
      val newPassword = "new password"
      PasswordReset.isValid(newUser.id.get, resetToken.get) must beEqualTo(true).eventually(3, Duration(1000, "millis"))
      goToAndWaitForAngular(controllers.routes.DistributorUsersController.resetPassword(Some(newUser.email), resetToken, newUser.id).url)
      browser.fill("#password").`with`(newPassword)
      browser.fill("#confirmation").`with`(newPassword)
      browser.find("button").first.click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#analytics-controller").isPresent
      browser.url() must beEqualTo(controllers.routes.AnalyticsController.show(newUser.distributorID.get, None, None).url)
      DistributorUser.find(newUser.id.get).get.hashedPassword must not equalTo newUser.hashedPassword
    }

    "respond with a 400 if the token is not valid" in new WithFakeBrowser with DistributorUserSetup {
      val (newUser, _) = newDistributorUser("UniqueUser6@gmail.com")
      val invalidToken = Some("some-invalid-token")
      val Some(result) = passwordResetRequest(Some(newUser.email), invalidToken, newUser.id, POST)
      status(result) must beEqualTo(400)
    }

    "respond with a 400 if the token has expired" in new WithFakeBrowser with DistributorUserSetup {
      val (newUser, _) = newDistributorUser("UniqueUser7@gmail.com")
      val resetToken = PasswordReset.create(newUser.id.get)
      val newCreatedAt = (new DateTime(DateTimeZone.UTC) - PasswordReset.ResetPasswordWindow).toString(PasswordReset.dateFormatGeneration)
      DB.withConnection { implicit connection =>
        SQL(
          """
          UPDATE password_resets SET created_at = to_timestamp({new_created_at}, 'YYYY-MM-DD HH24:MI:SS')
          WHERE token = {token};
          """
        ).on(
            "token" -> resetToken,
            "new_created_at" -> newCreatedAt
          ).executeUpdate()
      }
      val Some(result) = passwordResetRequest(Some(newUser.email), resetToken, newUser.id, POST)
      status(result) must beEqualTo(400)
    }

    "respond with a 400 if the token has already been used" in new WithFakeBrowser with DistributorUserSetup {
      val (newUser, _) = newDistributorUser("UniqueUser8@gmail.com")
      val resetToken = PasswordReset.create(newUser.id.get)
      PasswordReset.isValid(newUser.id.get, resetToken.get) must beEqualTo(true).eventually(3, Duration(1000, "millis"))
      PasswordReset.complete(newUser.id.get, resetToken.get)
      PasswordReset.isValid(newUser.id.get, resetToken.get) must beEqualTo(false).eventually(3, Duration(1000, "millis"))
      val Some(result) = passwordResetRequest(Some(newUser.email), resetToken, newUser.id, POST)
      status(result) must beEqualTo(400)
    }

    "allow a user to log in with the new password" in new WithFakeBrowser with DistributorUserSetup {
      val (newUser, _) = newDistributorUser("UniqueUser9@gmail.com")
      setUpApp(newUser.distributorID.get)
      val resetToken = PasswordReset.create(newUser.id.get)
      val newPassword = "new password"
      PasswordReset.isValid(newUser.id.get, resetToken.get) must beEqualTo(true).eventually(3, Duration(1000, "millis"))
      goToAndWaitForAngular(controllers.routes.DistributorUsersController.resetPassword(Some(newUser.email), resetToken, newUser.id).url)
      browser.fill("#password").`with`(newPassword)
      browser.fill("#confirmation").`with`(newPassword)
      browser.find("button").first.click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#analytics-controller").isPresent
      browser.goTo(controllers.routes.DistributorUsersController.logout().url)

      goToAndWaitForAngular(controllers.routes.DistributorUsersController.login(None).url)
      browser.fill("#email").`with`(newUser.email)
      browser.fill("#password").`with`(newPassword)
      browser.find("button").first.click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#analytics-controller").isPresent
    }
  }

  "DistributorUsers.resetPassword" should {
    "return a 200 if all query params are valid" in new WithFakeBrowser with DistributorUserSetup {
      val (newUser, _) = newDistributorUser("UniqueUser10@gmail.com")
      val resetToken = PasswordReset.create(newUser.id.get)
      PasswordReset.isValid(newUser.id.get, resetToken.get) must beEqualTo(true).eventually(3, Duration(1000, "millis"))
      val Some(result) = passwordResetRequest(Some(newUser.email), resetToken, newUser.id)
      status(result) must beEqualTo(200)
    }

    "redirect to 404 if the token is not valid" in new WithFakeBrowser with DistributorUserSetup {
      val (newUser, _) = newDistributorUser("UniqueUser11@gmail.com")
      val resetToken = PasswordReset.create(newUser.id.get)
      PasswordReset.isValid(newUser.id.get, resetToken.get) must beEqualTo(true).eventually(3, Duration(1000, "millis"))
      PasswordReset.complete(newUser.id.get, resetToken.get)
      PasswordReset.isValid(newUser.id.get, resetToken.get) must beEqualTo(false).eventually(3, Duration(1000, "millis"))
      val Some(result) = passwordResetRequest(Some(newUser.email), resetToken, newUser.id)
      redirectLocation(result).get must beEqualTo("/404")
    }

    "redirect to 404 if the distributor ID is invalid" in new WithFakeBrowser with DistributorUserSetup {
      val (newUser, _) = newDistributorUser("UniqueUser12@gmail.com")
      val resetToken = PasswordReset.create(newUser.id.get)
      val unknownDistributorUserID = Some(9999L)
      PasswordReset.isValid(newUser.id.get, resetToken.get) must beEqualTo(true).eventually(3, Duration(1000, "millis"))
      val Some(result) = passwordResetRequest(Some(newUser.email), resetToken, unknownDistributorUserID)
      redirectLocation(result).get must beEqualTo("/404")
    }

    "redirect to a 404 if the email is invalid" in new WithFakeBrowser with DistributorUserSetup {
      val (newUser, _) = newDistributorUser("UniqueUser13@gmail.com")
      val resetToken = PasswordReset.create(newUser.id.get)
      val unknownEmail = Some("some unknown email")
      PasswordReset.isValid(newUser.id.get, resetToken.get) must beEqualTo(true).eventually(3, Duration(1000, "millis"))
      val Some(result) = passwordResetRequest(unknownEmail, resetToken, newUser.id)
      redirectLocation(result).get must beEqualTo("/404")
    }
  }

  def passwordResetRequest(email: Option[String], token: Option[String], userID: Option[Long], requestType: String = GET) = {
    val request = FakeRequest(
      requestType,
      controllers.routes.DistributorUsersController.resetPassword(email, token, userID).url,
      FakeHeaders(Seq()),
      ""
    )
    route(request)
  }
}
