import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._
import models._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends SpecificationWithFixtures {



  "Application" should {

    "send 404 on a bad request" in new WithFakeBrowser {
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "require user to login" in new WithFakeBrowser {
      val home = route(FakeRequest(GET, "/")).get

      status(home) must equalTo(SEE_OTHER) // Redirect to login?
    }
  }
}
