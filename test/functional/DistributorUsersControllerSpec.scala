package functional

import models._

class DistributorUsersControllerSpec extends SpecificationWithFixtures {
  "Sign up page" should {
    "redirect to the log in page when sign up is successful" in new WithFakeBrowser {
      browser.goTo("http://localhost:" + port + "/signup")
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.fill("#confirmation").`with`(password)
      browser.$("#terms").click()
      browser.click("button")
      browser.pageSource must contain("Log In")
    }

    "display an error if terms are not agreed to" in new WithFakeBrowser {
      browser.goTo("http://localhost:" + port + "/signup")
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.fill("#confirmation").`with`(password)
      browser.click("button")
      browser.pageSource must contain("Please agree to our terms")
    }
  }
}
