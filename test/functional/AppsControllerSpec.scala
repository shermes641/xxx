package functional

import models._
import play.api.test._
import play.api.test.Helpers._

class AppsControllerSpec extends SpecificationWithFixtures {
  "New App page" should {
    "render application index page when sign up is successful" in new WithFakeBrowser {
      DistributorUser.create(email, password, companyName)
      val user = DistributorUser.findByEmail(email).get
      val appName = "Some new app"
      browser.goTo("http://localhost:" + port + "/distributors/" + user.distributorID.get + "/apps/new")
      browser.fill("#name").`with`(appName)
      browser.click("button")
      browser.pageSource must contain(appName)
    }
  }

  "App Index page" should {
    "display all apps" in new WithFakeBrowser {
      DistributorUser.create(email, password, companyName)
      val user = DistributorUser.findByEmail(email).get
      val appName1 = "App 1"
      val appName2 = "App 2"
      App.create(user.distributorID.get, appName1)
      App.create(user.distributorID.get, appName2)
      val url = "http://localhost:" + port + "/distributors/" + user.distributorID.get + "/apps"
      browser.goTo(url)
      browser.pageSource must contain(appName1)
      browser.pageSource must contain(appName2)
    }
  }

  /*
  "App Delete action" should {
    "remove app from database" in new WithFakeBrowser {
      DistributorUser.create(email, password, companyName)
      val user = DistributorUser.findByEmail(email).get
      val appName1 = "App 1"
      val appName2 = "App 2"
      val app1ID = App.create(user.distributorID.get, appName1)
      App.create(user.distributorID.get, appName2)
      val url = "http://localhost:" + port + "/distributors/" + user.distributorID.get + "/apps/" + app1ID.get + "/delete"
      val request = FakeRequest(POST, url)
      //val Some(result) = route(request)
      //status(result) must equalTo(200)
      val thisApp = App.find(app1ID.get)
      thisApp.getOrElse(Nil) must beNull
    }
  }
  */
}
