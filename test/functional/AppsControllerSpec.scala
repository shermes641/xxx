package functional

import models._
import anorm._
import play.api.db.DB
import play.api.test._
import play.api.test.Helpers._
import resources.DistributorUserSetup

class AppsControllerSpec extends SpecificationWithFixtures with DistributorUserSetup with AppCreationHelper {
  val appName = "App 1"

  val user = running(FakeApplication(additionalConfiguration = testDB)) {
    DistributorUser.create(email, password, companyName)
    DistributorUser.findByEmail(email).get
  }

  val adProviders = List("test ad provider 1", "test ad provider 2")

  val adProviderID1 = running(FakeApplication(additionalConfiguration = testDB)) {
    AdProvider.create(adProviders(0), "{\"requiredParams\":[{\"description\": \"Your HyprMX Distributor ID\", \"key\": \"distributorID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"true\"}, {\"description\": \"Your HyprMX App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"true\"}], \"reportingParams\": [{\"description\": \"Your API Key for Fyber\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}, {\"description\": \"Your Placement ID\", \"key\": \"placementID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}, {\"description\": \"Your App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}], \"callbackParams\": [{\"description\": \"Your Event API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}]}", None)
  }

  val adProviderID2 = running(FakeApplication(additionalConfiguration = testDB)) {
    AdProvider.create(adProviders(1), "{\"requiredParams\":[{\"description\": \"Your HyprMX Distributor ID\", \"key\": \"distributorID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"true\"}, {\"description\": \"Your HyprMX App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"true\"}], \"reportingParams\": [{\"description\": \"Your API Key for Fyber\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}, {\"description\": \"Your Placement ID\", \"key\": \"placementID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}, {\"description\": \"Your App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}], \"callbackParams\": [{\"description\": \"Your Event API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}]}", None)
  }

  val distributorID = user.distributorID.get

  "AppsController.newApp" should {
    "not create a new app with a virtual currency Reward Minimum that is greater than the Reward Maximum" in new WithFakeBrowser {
      val appCount = App.findAll(user.distributorID.get).size

      logInUser()
      goToAndWaitForAngular(controllers.routes.AppsController.newApp(user.distributorID.get).url)
      fillInAppValues(appName = appName, currencyName = "Gold", exchangeRate = "100", rewardMin = "100", rewardMax = "1")
      browser.$("button[name=new-app-form]").first.click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#new_app_reward_max").containsText("Reward Maximum must be greater than or equal to Reward Minimum.")
      App.findAll(user.distributorID.get).size must beEqualTo(appCount)
    }

    "not allow a new app to be created unless all required fields are filled" in new WithFakeBrowser {
      val appCount = App.findAll(user.distributorID.get).size

      logInUser()

      goToAndWaitForAngular(controllers.routes.AppsController.newApp(user.distributorID.get).url)
      browser.$("button[name=new-app-form]").first.isEnabled must beEqualTo(false)
      fillInAppValues()
      browser.$("button[name=new-app-form]").first.isEnabled must beEqualTo(true)
      browser.$("button[name=new-app-form]").first.click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(browser.pageSource.contains("New App Waterfall"))
      App.findAll(user.distributorID.get).size must beEqualTo(appCount + 1)
    }

    "flash an error message if reward min is not 1 or greater" in new WithFakeBrowser {
      val appCount = App.findAll(user.distributorID.get).size

      logInUser()

      goToAndWaitForAngular(controllers.routes.AppsController.newApp(user.distributorID.get).url)
      fillInAppValues(appName = appName, currencyName = "Gold", exchangeRate = "100", rewardMin = "0", rewardMax = "10")
      browser.$("button[name=new-app-form]").first.click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#new_app_reward_min").containsText("Reward Minimum must be 1 or greater.")
      App.findAll(user.distributorID.get).size must beEqualTo(appCount)
    }

    "flash an error message if exchange rate is not 1 or greater" in new WithFakeBrowser {
      val appCount = App.findAll(user.distributorID.get).size

      logInUser()

      goToAndWaitForAngular(controllers.routes.AppsController.newApp(user.distributorID.get).url)
      fillInAppValues(appName = appName, currencyName = "Gold", exchangeRate = "0", rewardMin = "1", rewardMax = "10")
      browser.$("button[name=new-app-form]").first.click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#new_app_exchange_rate").containsText("Exchange Rate must be 1 or greater.")
      App.findAll(user.distributorID.get).size must beEqualTo(appCount)
    }

    "not allow a new app to be created if the Distributor has already created and enabled an App with the same name" in new WithFakeBrowser {
      val (currentApp, _, _, _) = setUpApp(user.distributorID.get)
      val appCount = App.findAll(user.distributorID.get).size

      logInUser()

      goToAndWaitForAngular(controllers.routes.AppsController.newApp(user.distributorID.get).url)
      browser.$("button[name=new-app-form]").first.isEnabled must beEqualTo(false)
      fillInAppValues(appName = currentApp.name, currencyName = "Gold", exchangeRate = "100", rewardMin = "1", rewardMax = "10")
      clickAndWaitForAngular("button[name=new-app-form]")
      browser.pageSource must contain("You already have an active App with the same name.")
      App.findAll(user.distributorID.get).size must beEqualTo(appCount)
    }

    "allow a new app to be created if the Distributor has already created and deactivated an App with the same name" in new WithFakeBrowser {
      val (currentApp, _, _, _) = setUpApp(user.distributorID.get, "Some unique app name")
      App.update(new UpdatableApp(currentApp.id, active = false, distributorID = user.distributorID.get, name = currentApp.name, callbackURL = None, serverToServerEnabled = false))
      val appCount = App.findAll(user.distributorID.get).size

      logInUser()

      goToAndWaitForAngular(controllers.routes.AppsController.newApp(user.distributorID.get).url)
      browser.$("button[name=new-app-form]").first.isEnabled must beEqualTo(false)
      fillInAppValues(appName = currentApp.name, currencyName = "Gold", exchangeRate = "100", rewardMin = "1", rewardMax = "10")
      browser.$("button[name=new-app-form]").first.click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(browser.pageSource.contains(currentApp.name + " Waterfall"))
      App.findAll(user.distributorID.get).size must beEqualTo(appCount + 1)
    }
  }

  "AppsController.create" should {
    "create a new app with a corresponding waterfall, virtual currency, and app config" in new WithFakeBrowser {
      val appCount = App.findAll(user.distributorID.get).size

      logInUser()

      goToAndWaitForAngular(controllers.routes.AppsController.newApp(user.distributorID.get).url)
      fillInAppValues(appName = "Some new unique app name", currencyName = "Gold", exchangeRate = "100", rewardMin = "1", rewardMax = "10")
      browser.$("button[name=new-app-form]").first().click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(browser.pageSource.contains("Some new unique app name Waterfall"))

      val apps = App.findAll(user.distributorID.get)
      val firstApp = apps(0)

      apps.size must beEqualTo(appCount + 1)
      Waterfall.findByAppID(firstApp.id).size must beEqualTo(1)
      AppConfig.findLatest(firstApp.token).must(not).beNone
      VirtualCurrency.findByAppID(firstApp.id).must(not).beNone
    }

    "rollback the database if there is an error creating a new app, waterfall, virtual currency, or app config" in new WithFakeBrowser {
      val newUserEmail = "test@gmail.com"
      val newUserPassword = "password"
      DistributorUser.create(newUserEmail, newUserPassword, companyName)
      val newUser = DistributorUser.findByEmail(newUserEmail).get
      val appsCount = tableCount("apps")
      val waterfallsCount = tableCount("waterfalls")
      val currenciesCount = tableCount("virtual_currencies")
      val appConfigsCount = tableCount("app_configs")

      logInUser(newUserEmail, newUserPassword)
      goToAndWaitForAngular(controllers.routes.AppsController.newApp(newUser.distributorID.get).url)

      // Remove the distributor from the database just before attempting to create a new app.  This will cause a SQL error in AppsController.create
      DB.withConnection { implicit connection =>
        SQL("DELETE FROM distributor_users WHERE id = {id}").on("id" -> newUser.id.get).execute()
        SQL("DELETE FROM distributors WHERE id = {id}").on("id" -> newUser.distributorID.get).execute()
      }

      fillInAppValues(appName = appName, currencyName = "Gold", exchangeRate = "100", rewardMin = "1")
      browser.$("button[name=new-app-form]").first.click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#error-message").areDisplayed()

      appsCount must beEqualTo(tableCount("apps"))
      waterfallsCount must beEqualTo(tableCount("waterfalls"))
      currenciesCount must beEqualTo(tableCount("virtual_currencies"))
      appConfigsCount must beEqualTo(tableCount("app_configs"))
    }

    "should create a pending HyprMarketplace WaterfallAdProvider instance" in new WithFakeBrowser {
      logInUser()

      val newAppName = "My new test app"
      goToAndWaitForAngular(controllers.routes.AppsController.newApp(user.distributorID.get).url)
      fillInAppValues(appName = newAppName, currencyName = "Gold", exchangeRate = "100", rewardMin = "1", rewardMax = "10")
      browser.$("button[name=new-app-form]").first.click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(browser.pageSource.contains(newAppName + " Waterfall"))
      val currentApp = App.findAll(user.distributorID.get).filter { app => app.name == newAppName }(0)
      val currentWaterfall = Waterfall.findByAppID(currentApp.id)(0)
      val waterfallAdProviders = WaterfallAdProvider.findAllOrdered(currentWaterfall.id)
      val hyprMarketplace = waterfallAdProviders(0)
      waterfallAdProviders.size must beEqualTo(1)
      hyprMarketplace.pending must beEqualTo(true)
    }
  }

  "AppsController.edit" should {
    "find the app with virtual currency and render the edit form" in new WithAppBrowser(user.distributorID.get) {
      logInUser()
      DB.withTransaction { implicit connection => AppConfig.create(currentApp.id, currentApp.token, generationNumber(currentApp.id)) }
      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(user.distributorID.get, currentApp.id, None).url)
      clickAndWaitForAngular("#waterfall-app-settings-button")
      browser.pageSource must contain("App Configuration")
    }

    "notify the user if server to server callbacks are enabled without a valid callback URL" in new WithAppBrowser(user.distributorID.get) {
      logInUser()
      DB.withTransaction { implicit connection => AppConfig.create(currentApp.id, currentApp.token, generationNumber(currentApp.id)) }
      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(user.distributorID.get, currentApp.id, None).url)
      clickAndWaitForAngular("#waterfall-app-settings-button")
      browser.executeScript("$(':input[id=serverToServerEnabled]').click();")
      clickAndWaitForAngular("button[name=submit]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#callback_url").containsText("A valid HTTP or HTTPS callback URL is required.")
    }

    "redirect the distributor user to their own apps index page if they try to edit an App they do not own" in new WithAppBrowser(user.distributorID.get) {
      val (maliciousUser, maliciousDistributor) = newDistributorUser("newuseremail2@gmail.com")

      logInUser(maliciousUser.email, password)

      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(maliciousDistributor.id.get, currentApp.id, None).url)
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#flash").hasText("Waterfall could not be found.")
      browser.url() must beEqualTo(controllers.routes.AnalyticsController.show(maliciousDistributor.id.get, None).url)
    }

    "redirect the distributor user to their own apps index page if they try to edit an App using another distributor ID" in new WithAppBrowser(user.distributorID.get) {
      val (maliciousUser, maliciousDistributor) = newDistributorUser("newuseremail3@gmail.com")

      logInUser(maliciousUser.email, password)

      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(user.distributorID.get, currentWaterfall.id, None).url)
      browser.url() must beEqualTo(controllers.routes.AnalyticsController.show(maliciousDistributor.id.get, None).url)
    }

    "flash an error message if reward min is not 1 or greater" in new WithAppBrowser(user.distributorID.get) {
      logInUser()
      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(user.distributorID.get, currentApp.id, None).url)
      clickAndWaitForAngular("#waterfall-app-settings-button")
      browser.fill("#rewardMin").`with`("0")
      clickAndWaitForAngular("button[name=submit]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#reward_min").containsText("Reward Minimum must be 1 or greater.")
    }

    "flash an error message if exchange rate is not 1 or greater" in new WithAppBrowser(user.distributorID.get) {
      logInUser()
      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(user.distributorID.get, currentApp.id, None).url)
      clickAndWaitForAngular("#waterfall-app-settings-button")
      browser.fill("#exchangeRate").`with`("0")
      clickAndWaitForAngular("button[name=submit]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#exchange_rate").containsText("Exchange Rate must be 1 or greater.")
    }
  }

  "AppsController.update" should {
    val adProviderConfig = "{\"requiredParams\":[" +
      "{\"description\": \"Your HyprMX Distributor ID\", \"key\": \"distributorID\", \"value\":\"\", \"dataType\": \"String\"}, " +
      "{\"description\": \"Your HyprMX App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}], " +
      "\"reportingParams\": [" +
      "{\"description\": \"Your API Key for Fyber\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}, " +
      "{\"description\": \"Your Placement ID\", \"key\": \"placementID\", \"value\":\"\", \"dataType\": \"String\"}, " +
      "{\"description\": \"Your App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}], " +
      "\"callbackParams\": [{\"description\": \"Your Event API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}]}"

    val adProviderID = running(FakeApplication(additionalConfiguration = testDB)) {
      AdProvider.create("test ad provider", adProviderConfig, None)
    }

    "update the app record in the database" in new WithAppBrowser(user.distributorID.get) {
      Waterfall.update(currentWaterfall.id, true, false)
      WaterfallAdProvider.create(currentWaterfall.id, adProviderID.get, None, Some(5.0), false, true)
      VirtualCurrency.create(currentApp.id, "Gold", 100, 1, None, Some(true))
      val newAppName = "New App Name"

      logInUser()
      DB.withTransaction { implicit connection => AppConfig.create(currentApp.id, currentApp.token, generationNumber(currentApp.id)) }
      val originalGeneration = generationNumber(currentApp.id)
      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(user.distributorID.get, currentApp.id, None).url)
      clickAndWaitForAngular("#waterfall-app-settings-button")
      browser.fill("#appName").`with`(newAppName)
      clickAndWaitForAngular("button[name=submit]")
      browser.pageSource must contain(newAppName)
      App.find(currentApp.id).get.name must beEqualTo(newAppName)
      generationNumber(currentApp.id) must beEqualTo(originalGeneration + 1)
    }

    "update the virtual currency record in the database" in new WithAppBrowser(user.distributorID.get) {
      Waterfall.update(currentWaterfall.id, true, false)
      WaterfallAdProvider.create(currentWaterfall.id, adProviderID.get, None, Some(5.0), false, true)
      DB.withTransaction { implicit connection => AppConfig.create(currentApp.id, currentApp.token, generationNumber(currentApp.id)) }
      val originalGeneration = generationNumber(currentApp.id)
      val rewardMin = 1
      val rewardMax = 100

      logInUser()
      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(user.distributorID.get, currentApp.id, None).url)
      clickAndWaitForAngular("#waterfall-app-settings-button")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until({browser.findFirst("#currencyName").getValue() == currentVirtualCurrency.name})

      browser.fill("#rewardMin").`with`(rewardMin.toString)
      browser.fill("#rewardMax").`with`(rewardMax.toString)
      clickAndWaitForAngular("button[name=submit]")

      val updatedVC = VirtualCurrency.find(currentVirtualCurrency.id).get
      updatedVC.rewardMin must beEqualTo(rewardMin)
      updatedVC.rewardMax.get must beEqualTo(rewardMax)
      AppConfig.findLatest(currentApp.token).get.generationNumber must beEqualTo(originalGeneration + 1)
    }
  }
}
