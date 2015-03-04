import com.google.common.base.Predicate
import org.fluentlenium.core.{FluentPage, Fluent}
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import models._

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends SpecificationWithFixtures with AppCreationHelper {

  "Application.index" should {
    "send 404 on a bad request" in new WithFakeBrowser {
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "require user to login" in new WithFakeBrowser {
      val home = route(FakeRequest(GET, "/")).get
      status(home) must equalTo(SEE_OTHER) // Redirect to login?
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
  }
}