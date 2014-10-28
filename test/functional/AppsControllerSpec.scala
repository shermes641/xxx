package functional

import models._
import anorm._
import play.api.db.DB
import play.api.test._
import play.api.test.Helpers._
import play.api.Play.current

class AppsControllerSpec extends SpecificationWithFixtures {
  val appName = "App 1"

  val user = running(FakeApplication(additionalConfiguration = testDB)) {
    DistributorUser.create(email, password, companyName)
    DistributorUser.findByEmail(email).get
  }

  "AppsController.index" should {
    "display all apps" in new WithFakeBrowser {
      val app2Name = "App 2"
      val appID = App.create(user.distributorID.get, app2Name).get
      Waterfall.create(appID, app2Name)
      val url = "http://localhost:" + port + "/distributors/" + user.distributorID.get + "/apps"

      logInUser()

      browser.goTo(url)
      browser.pageSource must contain(app2Name)
    }
  }

  "AppsController.newApp" should {
    "create a new app with a corresponding waterfall and virtual currency" in new WithFakeBrowser {
      val appCount = App.findAll(user.distributorID.get).size
      val currencyName = "Gold"

      logInUser()

      browser.goTo("http://localhost:" + port + "/distributors/" + user.distributorID.get + "/apps/new")
      browser.fill("#appName").`with`(appName)
      browser.$("button[name=app-form-next]").click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#new-virtual-currency").areDisplayed()
      browser.pageSource must contain("New Virtual Currency")
      browser.fill("#currencyName").`with`(currencyName)
      browser.fill("#exchangeRate").`with`("100")
      browser.$("#roundUp").click()
      browser.$("button[name=virtual-currency-form]").click()
      browser.pageSource must contain(appName)

      val apps = App.findAll(user.distributorID.get)
      val firstApp = apps(0)

      apps.size must beEqualTo(appCount + 1)
      Waterfall.findByAppID(firstApp.id).size must beEqualTo(1)
      VirtualCurrency.findByAppID(firstApp.id) must not beNone
    }

    "rollback the database if there is an error creating a new app, waterfall, or virtual currency" in new WithFakeBrowser {
      val appCount = App.findAll(user.distributorID.get).size
      val newUserEmail = "test@gmail.com"
      val newUserPassword = "password"
      DistributorUser.create(newUserEmail, newUserPassword, companyName)
      val newUser = DistributorUser.findByEmail(newUserEmail).get
      val appsCount = tableCount("apps")
      val waterfallsCount = tableCount("waterfalls")
      val currenciesCount = tableCount("virtual_currencies")

      logInUser(newUserEmail, newUserPassword)
      browser.goTo("http://localhost:" + port + "/distributors/" + newUser.distributorID.get + "/apps/new")

      // Remove the distributor from the database just before attempting to create a new app.  This will cause a SQL error in AppsController.create
      DB.withConnection { implicit connection =>
        SQL("DELETE FROM distributor_users WHERE id = {id}").on("id" -> newUser.id.get).execute()
        SQL("DELETE FROM distributors WHERE id = {id}").on("id" -> newUser.distributorID.get).execute()
      }

      browser.fill("#appName").`with`(appName)
      browser.$("button[name=app-form-next]").click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#new-virtual-currency").areDisplayed()
      browser.pageSource must contain("New Virtual Currency")
      browser.fill("#currencyName").`with`("Gold")
      browser.fill("#exchangeRate").`with`("100")
      browser.$("#roundUp").click()
      browser.$("button[name=virtual-currency-form]").click()
      browser.pageSource must not contain(appName)

      appsCount must beEqualTo(tableCount("apps"))
      waterfallsCount must beEqualTo(tableCount("waterfalls"))
      currenciesCount must beEqualTo(tableCount("virtual_currencies"))
    }

    "not create a new app with a virtual currency Reward Minimum that is greater than the Reward Maximum" in new WithFakeBrowser {
      val appCount = App.findAll(user.distributorID.get).size

      logInUser()

      browser.goTo("http://localhost:" + port + "/distributors/" + user.distributorID.get + "/apps/new")
      browser.fill("#appName").`with`(appName)
      browser.$("button[name=app-form-next]").click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#new-virtual-currency").areDisplayed()
      browser.pageSource must contain("New Virtual Currency")
      browser.fill("#currencyName").`with`("Gold")
      browser.fill("#exchangeRate").`with`("100")
      browser.fill("#rewardMin").`with`("100")
      browser.fill("#rewardMax").`with`("1")
      browser.$("#roundUp").click()
      browser.$("button[name=virtual-currency-form]").click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#error-message").areDisplayed()
      App.findAll(user.distributorID.get).size must beEqualTo(appCount)
    }

    "not create a new app without all required fields" in new WithFakeBrowser {
      val appCount = App.findAll(user.distributorID.get).size

      logInUser()

      browser.goTo("http://localhost:" + port + "/distributors/" + user.distributorID.get + "/apps/new")
      browser.$("button[name=app-form-next]").click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#error-message").areDisplayed()
      App.findAll(user.distributorID.get).size must beEqualTo(appCount)
    }
  }

  "AppsController.edit" should {
    "update the app record in the database" in new WithFakeBrowser {
      val appID = App.create(user.distributorID.get, "App 1").get
      Waterfall.create(appID, "App 1")
      VirtualCurrency.create(appID, "Gold", 100, None, None, Some(true))
      val newAppName = "New App Name"
      val url = "http://localhost:" + port + "/distributors/" + user.distributorID.get + "/apps/" + appID + "/edit"

      logInUser()
      browser.goTo(url)
      browser.fill("#appName").`with`(newAppName)
      browser.$("button[name=submit]").first.click()
      browser.pageSource must contain(newAppName)
    }

    "update the virtual currency record in the database" in new WithFakeBrowser {
      val appID = App.create(user.distributorID.get, "App 1").get
      val vcID = VirtualCurrency.create(appID, "App 1", 100, None, None, Some(true)).get
      val virtualCurrency = VirtualCurrency.find(vcID).get
      val rewardMin = 1
      val rewardMax = 100

      logInUser()
      browser.goTo("http://localhost:" + port + "/distributors/" + user.distributorID.get + "/apps/" + appID + "/edit")
      browser.pageSource must contain(virtualCurrency.name)
      browser.fill("#rewardMin").`with`(rewardMin.toString())
      browser.fill("#rewardMax").`with`(rewardMax.toString())
      browser.$("button[name=submit]").first.click()

      val updatedVC = VirtualCurrency.find(virtualCurrency.id).get
      updatedVC.rewardMin.get must beEqualTo(rewardMin)
      updatedVC.rewardMax.get must beEqualTo(rewardMax)
    }
  }
  step(clean)
}

