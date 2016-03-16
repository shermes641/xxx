package functional

import models._
import play.api.libs.json.{JsBoolean, JsString, Json}
import play.api.libs.ws.{WS, WSAuthScheme}
import play.api.test.Helpers._
import play.api.test._
import resources.{AppCreationHelper, SpecificationWithFixtures}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ApplicationSpec extends SpecificationWithFixtures with AppCreationHelper {
  "Checking page loads" should {
    "include a working link to documentation in the navigation bar" in new WithFakeBrowser {
      goToAndWaitForAngular("http://localhost:" + port + "/login")
      val documentationLink = browser.find("#main-documentation-link").getAttribute("href")
      val request = WS.url(documentationLink).withAuth(DocumentationUsername, DocumentationPassword, WSAuthScheme.BASIC)
      Await.result(request.get().map { response =>
        response.status must beEqualTo(OK)
        response.body must contain("Welcome")
        response.body must contain("iOS SDK")
        response.body must contain("Administration")
      }, Duration(5000, "millis"))
    }

    "redirect a logged in user to the Analytics index page" in new WithFakeBrowser {
      val distributorID = DistributorUser.create(email, password, companyName).get
      val user = DistributorUser.findByEmail(email).get
      setUpApp(distributorID)
      goToAndWaitForAngular("http://localhost:" + port + "/login")
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.click("button")
      assertUrlEquals("/distributors/" + user.distributorID.get + "/analytics")
    }

    "redirect a logged out user to the login page" in new WithFakeBrowser {
      val user = DistributorUser.findByEmail(email).get
      val distributorID = user.id.get
      setUpApp(distributorID)
      goToAndWaitForAngular("http://localhost:" + port + "/login")
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.click("button")
      assertUrlEquals("/distributors/" + user.distributorID.get + "/analytics")

      val logout = route(FakeRequest(GET, "/logout")).get
      status(logout) must equalTo(SEE_OTHER)
      redirectLocation(logout).get must contain("/login?recently_logged_out=true")
    }

    "display signup page" in new WithFakeBrowser {
      val user = DistributorUser.findByEmail(email).get
      setUpApp(user.id.get)
      val signup = route(FakeRequest(GET, "/signup")).get
      status(signup) must equalTo(OK)
      contentType(signup) must equalTo(Some("text/html"))
      contentAsString(signup).contains("<title>Sign up - hyprMediate</title>") must equalTo(true)
    }
  }
  "Through Router /distributor_users" should {
    "create user fails with missing terms" in new WithFakeBrowser {
      val createUser = route(FakeRequest(POST, "/distributor_users")
        .withJsonBody(Json.obj(
          "company" -> JsString("My Company"),
          "email" -> JsString("steve@gmail.com"),
          "password" -> JsString("Flash12345"),
          "confirmation" -> JsString("Flash12345")
        ))).get
      status(createUser) must equalTo(BAD_REQUEST)
    }

    "create user fails with missing confirmation" in new WithFakeBrowser {
      val createUser = route(FakeRequest(POST, "/distributor_users").withJsonBody(Json.obj(
        "company" -> JsString("My Company"),
        "email" -> JsString("steve@gmail.com"),
        "password" -> JsString("Flash12345"),
        "terms" -> JsBoolean(true)
      ))).get
      status(createUser) must equalTo(BAD_REQUEST)
    }

    "create user fails with not agreeing to terms" in new WithFakeBrowser {
      val createUser = route(FakeRequest(POST, "/distributor_users").withJsonBody(Json.obj(
        "company" -> JsString("My Company"),
        "email" -> JsString("steve@gmail.com"),
        "password" -> JsString("Flash12345"),
        "terms" -> JsBoolean(false)
      ))).get
      status(createUser) must equalTo(BAD_REQUEST)
    }

    "create user succeeds" in new WithFakeBrowser {
      val createUser = route(FakeRequest(POST, "/distributor_users").withJsonBody(Json.obj(
        "company" -> JsString("My Company"),
        "email" -> JsString("steve@gmail.com"),
        "password" -> JsString("Flash12345"),
        "confirmation" -> JsString("Flash12345"),
        "terms" -> JsBoolean(true)
      ))).get
      status(createUser) must equalTo(OK)
      contentType(createUser) must equalTo(Some("application/json"))
      contentAsJson(createUser).toString contains """{"status":"success","distributorID":""" must equalTo(true)
    }
  }

  "Through Router /distributor_users/forgot_password" should {
    "display password reset page" in new WithFakeBrowser {
      val pw = route(FakeRequest(GET, "/distributor_users/forgot_password")).get
      status(pw) must equalTo(OK)
      contentType(pw) must equalTo(Some("text/html"))
      contentAsString(pw).contains("<title>Forgot Password - hyprMediate</title>") must equalTo(true)
    }
  }

  "Through Router /distributor_users/send_password_reset_email" should {
    "fail with no json" in new WithFakeBrowser {
      val pw = route(FakeRequest(POST, "/distributor_users/send_password_reset_email")).get
      status(pw) must equalTo(BAD_REQUEST)
      contentType(pw) must equalTo(Some("application/json"))
      contentAsString(pw).contains("""{"status":"error","message":"Invalid Request."}""") must equalTo(true)
    }

    "fail with unexpected json" in new WithFakeBrowser {
      val pw = route(FakeRequest(POST, "/distributor_users/send_password_reset_email")
        .withJsonBody(Json.obj("ddddddd" -> JsString("steve@gmail.com")))).get
      status(pw) must equalTo(BAD_REQUEST)
      contentType(pw) must equalTo(Some("application/json"))
      contentAsString(pw).contains("""{"status":"error","message":"Unkown JsError"}""") must equalTo(true)
    }

    "fail with email not found" in new WithFakeBrowser {
      val pw = route(FakeRequest(POST, "/distributor_users/send_password_reset_email")
        .withJsonBody(Json.obj("email" -> JsString("steveee@gmail.com")))).get
      status(pw) must equalTo(BAD_REQUEST)
      contentType(pw) must equalTo(Some("application/json"))
      contentAsString(pw).contains("""{"status":"error","message":"Email not found. Please Sign up.","fieldName":"email"}""") must equalTo(true)
    }

    "succeed with good email" in new WithFakeBrowser {
      val pw = route(FakeRequest(POST, "/distributor_users/send_password_reset_email")
        .withJsonBody(Json.obj("email" -> JsString("steve@gmail.com")))).get
      status(pw) must equalTo(OK)
      contentType(pw) must equalTo(Some("application/json"))
      contentAsString(pw).contains("""{"status":"success","message":"Password reset email sent!"}""") must equalTo(true)
    }
  }
}