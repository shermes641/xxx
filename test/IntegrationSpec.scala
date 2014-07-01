import org.fluentlenium.core.action.FillConstructor
import org.specs2.runner._
import org.junit.runner._
import org.fluentlenium.core.action.FillConstructor._
/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
@RunWith(classOf[JUnitRunner])
class IntegrationSpec extends SpecificationWithFixtures {

  "Application" should {

    "work from within a browser" in new WithBrowserAndFixtures {

      browser.goTo("http://localhost:" + port)

      browser.pageSource must contain("Log In")
    }

    "login" in new WithBrowserAndFixtures {
      browser.goTo("http://localhost:" + port + "/login")
      browser.pageSource must contain("Log In")

      browser.fill("#password").`with`(password)

      browser.fill("#email").`with`(email);

      browser.click("button");

      browser.pageSource must not contain("Invalid email or password")
      browser.pageSource must contain(email)
    }
  }
}
