import org.junit.runner._
import org.specs2.runner._
import play.api.libs.ws.WSAuthScheme
import play.api.test._
import play.api.test.Helpers._
import models.AppCreationHelper
import resources.SpecificationWithFixtures

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends SpecificationWithFixtures with AppCreationHelper {
  val distributorUser = running(testApplication) {
    distributorUserService.create(email, password, "Company Name")
    distributorUserService.findByEmail(email).get
  }

  "Application.index" should {
    "send 404 on a bad request" in new WithFakeBrowser {
      val Some(result) = route(FakeRequest(GET, "/some-non-existent-endpoint"))
      status(result) must equalTo(404)
    }

    "require user to login" in new WithFakeBrowser {
      val home = route(FakeRequest(GET, "/")).get
      status(home) must equalTo(SEE_OTHER) // Redirect to login?
    }

    "redirect a logged in user to the Analytics index page" in new WithFakeBrowser {
      setUpApp(distributorUser.distributorID.get)
      goToAndWaitForAngular("http://localhost:" + port + "/login")
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.click("button[name=submit]")
      assertUrlEquals("/distributors/" + user.distributorID.get + "/analytics")
    }

    "include a working link to documentation in the navigation bar" in new WithFakeBrowser {
      goToAndWaitForAngular("http://localhost:" + port + "/login")
      val documentationLink = browser.find("#main-documentation-link").getAttribute("href")
      val request = ws.url(documentationLink).withAuth(DocumentationUsername, DocumentationPassword, WSAuthScheme.BASIC)
      Await.result(request.get().map { response =>
        response.status must beEqualTo(200)
        response.body must contain("Welcome")
        response.body must contain("iOS SDK")
        response.body must contain("Administration")
      }, Duration(5000, "millis"))
    }
  }
}