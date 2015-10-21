package functional

import models._
import anorm._
import play.api.db.DB
import play.api.libs.json._
import play.api.libs.ws.{WSAuthScheme, WS}
import play.api.test._
import play.api.test.Helpers._
import resources.DistributorUserSetup
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class AppsControllerSpec extends SpecificationWithFixtures with DistributorUserSetup with AppCreationHelper {
  val appName = "App 1"

  val user = running(FakeApplication(additionalConfiguration = testDB)) {
    DistributorUser.create(email, password, companyName)
    DistributorUser.findByEmail(email).get
  }

  val adProviders = List("test ad provider 1", "test ad provider 2")

  val adProviderID1 = running(FakeApplication(additionalConfiguration = testDB)) {
    AdProvider.create(adProviders(0), "{\"requiredParams\":[{\"description\": \"Your HyprMX Distributor ID\", \"key\": \"distributorID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"true\"}, {\"description\": \"Your HyprMX App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"true\"}], \"reportingParams\": [{\"description\": \"Your Mediation Reporting API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}, {\"description\": \"Your Mediation Reporting Placement ID\", \"key\": \"placementID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}, {\"description\": \"Your App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}], \"callbackParams\": [{\"description\": \"Your Event API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}]}", None)
  }

  val adProviderID2 = running(FakeApplication(additionalConfiguration = testDB)) {
    AdProvider.create(adProviders(1), "{\"requiredParams\":[{\"description\": \"Your HyprMX Distributor ID\", \"key\": \"distributorID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"true\"}, {\"description\": \"Your HyprMX App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"true\"}], \"reportingParams\": [{\"description\": \"Your Mediation Reporting API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}, {\"description\": \"Your Mediation Reporting Placement ID\", \"key\": \"placementID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}, {\"description\": \"Your App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}], \"callbackParams\": [{\"description\": \"Your Event API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": \"false\"}]}", None)
  }

  val distributorID = user.distributorID.get

  "AppsController.newApp" should {
    "not create a new app with a virtual currency Reward Minimum that is greater than the Reward Maximum" in new WithFakeBrowser {
      val appCount = App.findAll(user.distributorID.get).size

      logInUser()
      goToAndWaitForAngular(controllers.routes.AppsController.newApp(user.distributorID.get).url)
      fillInAppValues(appName = appName, currencyName = "Gold", exchangeRate = "100", rewardMin = "100", rewardMax = "1")
      browser.$("button[id=create-app]").first.click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#new-app-reward-max").containsText("Maximum Reward must be greater than or equal to Minimum Reward.")
      App.findAll(user.distributorID.get).size must beEqualTo(appCount)
    }

    "not allow a new app to be created unless all required fields are filled" in new WithFakeBrowser {
      val appCount = App.findAll(user.distributorID.get).size

      logInUser()

      goToAndWaitForAngular(controllers.routes.AppsController.newApp(user.distributorID.get).url)
      browser.$("button[id=create-app]").first.isEnabled must beEqualTo(false)
      fillInAppValues()
      browser.$("button[id=create-app]").first.isEnabled must beEqualTo(true)
      browser.$("button[id=create-app]").first.click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(browser.pageSource.contains("New App Waterfall"))
      App.findAll(user.distributorID.get).size must beEqualTo(appCount + 1)
    }

    "display an error message if reward min is not 1 or greater" in new WithFakeBrowser {
      val appCount = App.findAll(user.distributorID.get).size

      logInUser()

      goToAndWaitForAngular(controllers.routes.AppsController.newApp(user.distributorID.get).url)
      fillInAppValues(appName = appName, currencyName = "Gold", exchangeRate = "100", rewardMin = "0", rewardMax = "10")
      browser.$("button[id=create-app]").first.click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#new-app-reward-min").containsText("Minimum Reward must be a valid integer greater than or equal to 1.")
      App.findAll(user.distributorID.get).size must beEqualTo(appCount)
    }

    "display an error message if exchange rate is not 1 or greater" in new WithFakeBrowser {
      val appCount = App.findAll(user.distributorID.get).size

      logInUser()

      goToAndWaitForAngular(controllers.routes.AppsController.newApp(user.distributorID.get).url)
      fillInAppValues(appName = appName, currencyName = "Gold", exchangeRate = "0", rewardMin = "1", rewardMax = "10")
      browser.$("button[id=create-app]").first.click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#new-app-exchange-rate").containsText("Exchange Rate must be a valid integer greater than or equal to 1.")
      App.findAll(user.distributorID.get).size must beEqualTo(appCount)
    }

    "not allow a new app to be created if the Distributor has already created and enabled an App with the same name" in new WithFakeBrowser {
      val (currentApp, _, _, _) = setUpApp(user.distributorID.get)
      val appCount = App.findAll(user.distributorID.get).size

      logInUser()

      goToAndWaitForAngular(controllers.routes.AppsController.newApp(user.distributorID.get).url)
      browser.$("button[id=create-app]").first.isEnabled must beEqualTo(false)
      val appNames = List(currentApp.name, currentApp.name.toUpperCase, currentApp.name.toLowerCase)
      appNames.map { name =>
        fillInAppValues(appName = name, currencyName = "Gold", exchangeRate = "100", rewardMin = "1", rewardMax = "10")
        clickAndWaitForAngular("button[id=create-app]")
        browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#new-app-app-name-custom-error").containsText("You already have an App with the same name.")
        browser.fill("#newAppName").`with`("")
        browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#new-app-app-name-custom-error").hasText("")
        App.findAll(user.distributorID.get).size must beEqualTo(appCount)
      }
    }

    "allow a new app to be created if the Distributor has already created and deactivated an App with the same name" in new WithFakeBrowser {
      val (currentApp, _, _, _) = setUpApp(user.distributorID.get)
      App.update(new UpdatableApp(currentApp.id, active = false, distributorID = user.distributorID.get, name = currentApp.name, callbackURL = None, serverToServerEnabled = false))
      val appCount = App.findAll(user.distributorID.get).size

      logInUser()

      goToAndWaitForAngular(controllers.routes.AppsController.newApp(user.distributorID.get).url)
      browser.$("button[id=create-app]").first.isEnabled must beEqualTo(false)
      fillInAppValues(appName = currentApp.name, currencyName = "Gold", exchangeRate = "100", rewardMin = "1", rewardMax = "10")
      browser.find("button[id=create-app]").click()
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
      browser.$("button[id=create-app]").first().click()
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
      browser.$("button[id=create-app]").first.click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#new-app-message").areDisplayed()

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
      browser.$("button[id=create-app]").first.click()
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(browser.pageSource.contains(newAppName + " Waterfall"))
      val currentApp = App.findAll(user.distributorID.get).filter { app => app.name == newAppName }(0)
      val currentWaterfall = Waterfall.findByAppID(currentApp.id)(0)
      val waterfallAdProviders = WaterfallAdProvider.findAllOrdered(currentWaterfall.id)
      val hyprMarketplace = waterfallAdProviders(0)
      waterfallAdProviders.size must beEqualTo(1)
      hyprMarketplace.pending must beEqualTo(true)
    }

    "new app on waterfall page should show new app in left column once created" in new WithFakeBrowser {
      logInUser()

      val newAppName = "My left list test app"
      goToAndWaitForAngular(controllers.routes.WaterfallsController.editAll(user.distributorID.get, None, None).url)
      clickAndWaitForAngular("#create-new-app")
      fillInAppValues(appName = newAppName, currencyName = "Gold", exchangeRate = "100", rewardMin = "1", rewardMax = "10")
      clickAndWaitForAngular("button[id=create-app]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(".left-apps-list").containsText("My left list test app")
    }

    "return the new waterfall ID in the JSON if the app is created successfully" in new WithFakeBrowser {
      val body = JsObject(
        Seq(
          "appName" -> JsString("New Unique App"),
          "currencyName" -> JsString("Coins"),
          "exchangeRate" -> JsNumber(100),
          "rewardMin" -> JsNumber(1),
          "rewardMax" -> JsNumber(1),
          "roundUp" -> JsBoolean(true)
        )
      )
      val request = FakeRequest(
        POST,
        controllers.routes.AppsController.create(user.distributorID.get).url,
        FakeHeaders(Seq("Content-type" -> Seq("application/json"))),
        body
      )
      val Some(result) = route(request.withSession("distributorID" -> user.distributorID.get.toString, "username" -> email))
      status(result) must equalTo(200)
      val jsonResponse = Json.parse(contentAsString(result))
      (jsonResponse \ "message").as[String] must beEqualTo("App Created!")
      val waterfallID = jsonResponse \ "waterfallID"
      waterfallID must haveClass[JsNumber]
      Waterfall.find(waterfallID.as[Long], user.distributorID.get).get must haveClass[Waterfall]
    }
  }

  "AppsController.edit" should {
    "find the app with virtual currency and render the edit form" in new WithAppBrowser(user.distributorID.get) {
      logInUser()
      DB.withTransaction { implicit connection => AppConfig.create(currentApp.id, currentApp.token, generationNumber(currentApp.id)) }
      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(user.distributorID.get, currentApp.id).url)
      clickAndWaitForAngular("#waterfall-app-settings-button")
      browser.pageSource must contain("App Configuration")
    }

    "notify the user if server to server callbacks are enabled without a valid callback URL" in new WithAppBrowser(user.distributorID.get) {
      logInUser()
      DB.withTransaction { implicit connection => AppConfig.create(currentApp.id, currentApp.token, generationNumber(currentApp.id)) }
      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(user.distributorID.get, currentApp.id).url)
      clickAndWaitForAngular("#waterfall-app-settings-button")
      currentApp.serverToServerEnabled must beFalse
      browser.find("#callbackURL").first.isEnabled must beFalse
      browser.executeScript("$(':input[id=serverToServerEnabled]').click();")
      browser.find("button[name=submit]").first.isEnabled must beFalse
      browser.find("#callbackURL").first.isEnabled must beTrue
      browser.fill("#callbackURL").`with`("invalid-url")
      browser.clear("#currencyName")
      browser.find("button[name=submit]").first.isEnabled must beFalse
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#valid-callback-url-error").areDisplayed
    }

    "enable the 'Save Changes' button after a callback URL error was encountered, the enable server to server option was toggled to 'off,' the modal was closed, then reopened" in new WithAppBrowser(user.distributorID.get) {
      logInUser()
      DB.withTransaction { implicit connection => AppConfig.create(currentApp.id, currentApp.token, generationNumber(currentApp.id)) }
      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(user.distributorID.get, currentApp.id).url)
      clickAndWaitForAngular("#waterfall-app-settings-button")
      currentApp.serverToServerEnabled must beFalse
      browser.find("#callbackURL").first.isEnabled must beFalse
      browser.executeScript("$(':input[id=serverToServerEnabled]').click();")
      browser.find("#callbackURL").first.isEnabled must beTrue
      browser.fill("#callbackURL").`with`("invalid-url")
      browser.clear("#rewardMax")
      browser.find("button[name=submit]").first.isEnabled must beFalse
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#valid-callback-url-error").areDisplayed
      browser.executeScript("$(':input[id=serverToServerEnabled]').click();")
      browser.executeScript("angular.element($('#waterfall-controller')).scope().hideModal();")
      clickAndWaitForAngular("#waterfall-app-settings-button")
      browser.find("button[name=submit]").first.isEnabled must beTrue
    }

    "redirect the distributor user to their own Analytics page if they try to edit an App they do not own" in new WithAppBrowser(user.distributorID.get) {
      val (maliciousUser, maliciousDistributor) = newDistributorUser("newuseremail2@gmail.com")
      setUpApp(maliciousDistributor.id.get)

      logInUser(maliciousUser.email, password)

      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(maliciousDistributor.id.get, currentApp.id).url)
      browser.url() must beEqualTo(controllers.routes.AnalyticsController.show(maliciousDistributor.id.get, None, Some(false)).url)
    }

    "redirect the distributor user to their own Analytics page if they try to edit an App using another distributor ID" in new WithAppBrowser(user.distributorID.get) {
      val (maliciousUser, maliciousDistributor) = newDistributorUser("newuseremail3@gmail.com")
      setUpApp(maliciousDistributor.id.get)

      logInUser(maliciousUser.email, password)

      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(user.distributorID.get, currentWaterfall.id).url)
      browser.url() must beEqualTo(controllers.routes.AnalyticsController.show(maliciousDistributor.id.get, None, None).url)
    }

    "display an error message if reward min is not 1 or greater" in new WithAppBrowser(user.distributorID.get) {
      logInUser()
      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(user.distributorID.get, currentApp.id).url)
      clickAndWaitForAngular("#waterfall-app-settings-button")
      browser.fill("#rewardMin").`with`("0")
      browser.fill("#appName").`with`(currentApp.name)
      clickAndWaitForAngular("button[name=submit]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#reward-min").containsText("Minimum Reward must be a valid integer greater than or equal to 1.")
    }

    "display an error message if exchange rate is not 1 or greater" in new WithAppBrowser(user.distributorID.get) {
      logInUser()
      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(user.distributorID.get, currentApp.id).url)
      clickAndWaitForAngular("#waterfall-app-settings-button")
      browser.fill("#exchangeRate").`with`("0")
      browser.fill("#appName").`with`(currentApp.name)
      clickAndWaitForAngular("button[name=submit]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#exchange-rate").containsText("Exchange Rate must be a valid integer greater than or equal to 1.")
    }

    "display an error message is exchange rate is too long" in new WithAppBrowser(user.distributorID.get) {
      logInUser()
      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(user.distributorID.get, currentApp.id).url)
      clickAndWaitForAngular("#waterfall-app-settings-button")
      browser.fill("#exchangeRate").`with`("9999999999999999")
      browser.fill("#appName").`with`(currentApp.name)
      clickAndWaitForAngular("button[name=submit]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#exchange-rate").containsText("Exchange Rate must be 15 characters or less.")
    }

    "display an error message is reward min is too long" in new WithAppBrowser(user.distributorID.get) {
      logInUser()
      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(user.distributorID.get, currentApp.id).url)
      clickAndWaitForAngular("#waterfall-app-settings-button")
      browser.fill("#rewardMin").`with`("9999999999999999")
      browser.fill("#appName").`with`(currentApp.name)
      clickAndWaitForAngular("button[name=submit]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#reward-min").containsText("Reward Min must be 15 characters or less.")
    }

    "display an error message is reward max is too long" in new WithAppBrowser(user.distributorID.get) {
      logInUser()
      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(user.distributorID.get, currentApp.id).url)
      clickAndWaitForAngular("#waterfall-app-settings-button")
      browser.fill("#rewardMax").`with`("9999999999999999")
      browser.fill("#appName").`with`(currentApp.name)
      clickAndWaitForAngular("button[name=submit]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#reward-max").containsText("Reward Max must be 15 characters or less.")
    }

    "display an error message when reward min is greater than reward max" in new WithAppBrowser(user.distributorID.get) {
      VirtualCurrency.update(new VirtualCurrency(currentVirtualCurrency.id, currentApp.id, currentVirtualCurrency.name,
        currentVirtualCurrency.exchangeRate, rewardMin = 1, rewardMax = Some(1), roundUp = currentVirtualCurrency.roundUp))
      logInUser()
      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(user.distributorID.get, currentApp.id).url)
      clickAndWaitForAngular("#waterfall-app-settings-button")
      browser.fill("#rewardMin").`with`("5")
      browser.find("button[name=submit]").first.isEnabled must beFalse
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#reward-max-error").areDisplayed
    }

    "render an error if the user tries to change the app name to the same name as one of their other active apps" in new WithAppBrowser(user.distributorID.get) {
      logInUser()
      val (anotherApp, _, _, _) = setUpApp(user.distributorID.get)
      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(user.distributorID.get, currentApp.id).url)
      clickAndWaitForAngular("#waterfall-app-settings-button")
      browser.fill("#appName").`with`(anotherApp.name)
      browser.executeScript("$('#update-app').click()")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-app-app-name-custom-error").containsText("You already have an App with the same name.")
      browser.fill("#appName").`with`("")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#edit-app-app-name-custom-error").hasText("")
    }

    "Callback URL tooltip documentation link is correct" in new WithAppBrowser(user.distributorID.get) {
      logInUser()

      browser.goTo(controllers.routes.WaterfallsController.edit(user.distributorID.get, currentWaterfall.id).url)
      clickAndWaitForAngular("#waterfall-app-settings-button")
      val documentationLinkText = browser.find("#callback-url-documentation-link").getAttribute("ng-bind-html")
      val urlPattern = new scala.util.matching.Regex("""https:\/\/documentation.hyprmx.com(\/|\w|\+)+""")
      val documentationLink = urlPattern findFirstIn documentationLinkText match {
        case Some(url) => url
        case None => ""
      }
      val request = WS.url(documentationLink).withAuth(DocumentationUsername, DocumentationPassword, WSAuthScheme.BASIC)
      Await.result(request.get().map { response =>
        response.status must beEqualTo(200)
        response.body must contain("Server to Server Callbacks")
      }, Duration(5000, "millis"))
    }
  }

  "AppsController.update" should {
    val adProviderConfig = "{\"requiredParams\":[" +
      "{\"description\": \"Your HyprMX Distributor ID\", \"key\": \"distributorID\", \"value\":\"\", \"dataType\": \"String\"}, " +
      "{\"description\": \"Your HyprMX App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}], " +
      "\"reportingParams\": [" +
      "{\"description\": \"Your Mediation Reporting API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}, " +
      "{\"description\": \"Your Mediation Reporting Placement ID\", \"key\": \"placementID\", \"value\":\"\", \"dataType\": \"String\"}, " +
      "{\"description\": \"Your App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}], " +
      "\"callbackParams\": [{\"description\": \"Your Event API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\"}]}"

    val adProviderID = running(FakeApplication(additionalConfiguration = testDB)) {
      AdProvider.create("test ad provider", adProviderConfig, None)
    }

    "update the app record in the database" in new WithAppBrowser(user.distributorID.get) {
      Waterfall.update(currentWaterfall.id, optimizedOrder = true, testMode = false, paused = false)
      WaterfallAdProvider.create(currentWaterfall.id, adProviderID.get, None, Some(5.0), false, true)
      VirtualCurrency.create(currentApp.id, "Gold", 100, 1, None, Some(true))
      val newAppName = "New App Name"

      logInUser()
      DB.withTransaction { implicit connection => AppConfig.create(currentApp.id, currentApp.token, generationNumber(currentApp.id)) }
      val originalGeneration = generationNumber(currentApp.id)
      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(user.distributorID.get, currentApp.id).url)
      clickAndWaitForAngular("#waterfall-app-settings-button")
      browser.fill("#appName").`with`(newAppName)
      browser.executeScript("$('#serverToServerEnabled').click();")
      val longCallbackURL = "http://" + "a" * 2037 + ".com" // This meets the 2048 character limit for the callback_url field
      browser.fill("#callbackURL").`with`(longCallbackURL)
      browser.fill("#rewardMax").`with`("")
      browser.executeScript("$('#update-app').click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").containsText("App updated successfully.")
      val updatedApp = App.find(currentApp.id).get
      updatedApp.name must beEqualTo(newAppName)
      updatedApp.callbackURL.get must beEqualTo(longCallbackURL)
      generationNumber(currentApp.id) must beEqualTo(originalGeneration + 1)
    }

    "update the virtual currency record in the database" in new WithAppBrowser(user.distributorID.get) {
      Waterfall.update(currentWaterfall.id, optimizedOrder = true, testMode = false, paused = false)
      WaterfallAdProvider.create(currentWaterfall.id, adProviderID.get, None, Some(5.0), false, true)
      DB.withTransaction { implicit connection => AppConfig.create(currentApp.id, currentApp.token, generationNumber(currentApp.id)) }
      val originalGeneration = generationNumber(currentApp.id)
      val rewardMin = 1
      val rewardMax = 100

      logInUser()
      goToAndWaitForAngular(controllers.routes.WaterfallsController.list(user.distributorID.get, currentApp.id).url)
      clickAndWaitForAngular("#waterfall-app-settings-button")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until({browser.findFirst("#currencyName").getValue() == currentVirtualCurrency.name})

      browser.fill("#rewardMin").`with`(rewardMin.toString)
      browser.fill("#rewardMax").`with`(rewardMax.toString)
      browser.executeScript("$('#update-app').click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#waterfall-edit-message").containsText("App updated successfully.")

      val updatedVC = VirtualCurrency.find(currentVirtualCurrency.id).get
      updatedVC.rewardMin must beEqualTo(rewardMin)
      updatedVC.rewardMax.get must beEqualTo(rewardMax)
      AppConfig.findLatest(currentApp.token).get.generationNumber must beEqualTo(originalGeneration + 1)
    }
  }
}
