import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import models._

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends SpecificationWithFixtures {

  "Application.index" should {
    "send 404 on a bad request" in new WithFakeBrowser {
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "require user to login" in new WithFakeBrowser {
      val home = route(FakeRequest(GET, "/")).get
      status(home) must equalTo(SEE_OTHER) // Redirect to login?
    }

    "redirect a logged in user to the Apps index page" in new WithFakeBrowser {
      DistributorUser.create(email, password, companyName)
      DistributorUser.setActive(DistributorUser.findByEmail(email).get)

      val user = DistributorUser.findByEmail(email).get
      browser.goTo("http://localhost:" + port + "/login")
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.click("button")
      browser.url() must beEqualTo("/distributors/" + user.distributorID.get + "/apps")
    }
  }
}
