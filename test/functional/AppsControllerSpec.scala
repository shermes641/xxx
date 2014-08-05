package functional

import models._
import play.api.test._
import play.api.test.Helpers._

class AppsControllerSpec extends SpecificationWithFixtures {
  val appName = "App 1"

  def withUser[A](test: DistributorUser => A): DistributorUser = {
    DistributorUser.create(email, password, companyName)
    DistributorUser.findByEmail(email).get
  }

  "App index action" should {
    "display all apps" in new WithFakeBrowser {
      withUser(user => {
        val app2Name = "App 2"
        App.create(user.distributorID.get, app2Name)
        val url = "http://localhost:" + port + "/distributors/" + user.distributorID.get + "/apps"
        browser.goTo(url)
        browser.pageSource must contain(appName)
        browser.pageSource must contain(app2Name)
      })
    }
  }

  "App newApp action" should {
    "render application index page when sign up is successful" in new WithFakeBrowser {
      withUser(user => {
        browser.goTo("http://localhost:" + port + "/distributors/" + user.distributorID.get + "/apps/new")
        browser.fill("#name").`with`(appName)
        browser.click("button")
        browser.pageSource must contain(appName)
      })
    }
  }

  "App show action" should {
    "display info for app" in new WithFakeBrowser {
      withUser(user => {
        val appID = App.create(user.distributorID.get, appName)
        val url = "http://localhost:" + port + "/distributors/" + user.distributorID.get + "/apps/" + appID.get
        browser.goTo(url)
        browser.pageSource must contain(appName)
      })
    }
  }

  "App destroy action" should {
    "respond with a 200 when an app is deleted successfully" in new WithFakeBrowser {
      withUser(user => {
        val appID = App.create(user.distributorID.get, "App 1")
        val url = "http://localhost:" + port + "/distributors/" + user.distributorID.get + "/apps/" + appID.get + "/delete"
        val result = route(FakeRequest(POST, url)).get
        status(result) must equalTo(OK)
      })
    }
  }

  "App update action" should {
    "update the app record in the database" in new WithFakeBrowser {
      withUser(user => {
        val appID = App.create(user.distributorID.get, "App 1")
        val newAppName = "New App Name"
        val url = "http://localhost:" + port + "/distributors/" + user.distributorID.get + "/apps/" + appID.get + "/edit"
        browser.goTo(url)
        browser.fill("#name").`with`(newAppName)
        browser.$("button[name=submit]").click()
        browser.pageSource must contain(newAppName)
      })
    }
  }
}
