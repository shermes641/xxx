package functional

import com.github.nscala_time.time.Imports._
import controllers.routes
import io.keen.client.java.ScopedKeys
import models._
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test.{FakeApplication, FakeHeaders, _}
import resources.SpecificationWithFixtures
import resources.DistributorUserSetup
import scala.collection.JavaConversions._

class AnalyticsControllerSpec extends SpecificationWithFixtures with DistributorUserSetup with AppCreationHelper {
  val distributorUser = running(FakeApplication(additionalConfiguration = testDB)) {
    distributorUserService.create(email, password, "Company Name")
    distributorUserService.findByEmail(email).get
  }

  val distributorID = distributorUser.distributorID.get

  "Analytics show action" should {
    "show analytics for an app" in new WithFakeBrowser {
      logInUser()
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(app1.id), None).url)
      browser.pageSource must contain("Analytics")
    }

    "redirect to the App creation page if the Distributor has not created any Apps" in new WithFakeBrowser {
      val currentEmail = "newuser@gmail.com"
      val (_, currentDistributor) = newDistributorUser(currentEmail, password, "New Company")

      logInUser(currentEmail, password)

      goToAndWaitForAngular(controllers.routes.WaterfallsController.editAll(currentDistributor.id.get, None, None).url)
      browser.url.contains(controllers.routes.AppsController.newApp(currentDistributor.id.get).url)
      browser.pageSource must contain("Begin by creating your first app")
    }

    "populate ad networks and verify that All ad providers is default" in new WithAppBrowser(distributorID) {
      logInUser()

      val adProviderDisplayName = "Unity Ads"
      val adProviderID = {
        val name = "UnityAds"
        val configuration = "{\"required_params\":[{\"description\": \"Your Unity Ads GAME Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}]}"
        adProviderService.create(
          name = name,
          displayName = adProviderDisplayName,
          configurationData = configuration,
          platformID = testPlatform.Ios.PlatformID,
          callbackUrlFormat = None,
          callbackUrlDescription = testPlatform.Ios.UnityAds.callbackURLDescription.format(name)
        )
      }
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id), None).url)

      // All Ad Providers should be selected
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#ad-providers-filter").containsText("All Ad Providers")

      clickAndWaitForAngular("#ad-providers-filter .add")
      clickAndWaitForAngular("#filter-ad_providers")

      // Unity must be part of dropdown
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#ad-providers-filter .add").containsText(adProviderDisplayName)
    }

    "country select box should exist and not be empty" in new WithFakeBrowser {
      logInUser()

      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(app1.id), None).url)

      // All Countries should be selected
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#countries-filter").containsText("All Countries")

      clickAndWaitForAngular("#countries-filter .add")
      clickAndWaitForAngular("#filter-countries")

      // United States and Ireland must be part of dropdown
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#countries-filter .add").containsText("United States")

      // Test Autocomplete
      fillAndWaitForAngular("#filter-countries", "Ire")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#countries-filter .add").containsText("Ireland")
    }

    "date picker should be setup correctly" in new WithFakeBrowser {
      logInUser()

      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(app1.id), None).url)

      var date = new DateTime(DateTimeZone.UTC)
      browser.$("#start-date").getValue must beEqualTo(date.minusDays(13).toString("MMM dd yyyy") + " - " + date.toString("MMM dd yyyy"))
    }

    "Verify Analytic items have proper labels" in new WithFakeBrowser {
      logInUser()
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(app1.id), None).url)

      // eCPM metric
      waitUntilContainsText("#ecpm-metric-container", "Average eCPM")
      // Fill Rate metric
      waitUntilContainsText("#fill-rate-container", "Fill Rate")
      // Average Revenue metric
      waitUntilContainsText("#unique-users-container", "Average Revenue Per Day")
      // Revenue Table
      waitUntilContainsText("#estimated-revenue-chart-container", "Above results use Ad Network eCPMs to estimate revenue.")
    }

    "Check Analytics configuration info JSON" in new WithFakeBrowser {
      logInUser()
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(app1.id), None).url)

      val request = FakeRequest(
        GET,
        controllers.routes.AnalyticsController.info(distributorID).url,
        FakeHeaders(),
        ""
      )

      val Some(result) = route(request.withSession("distributorID" -> distributorID.toString, "username" -> email))
      status(result) must equalTo(200)
      val response = contentAsJson(result)

      (response \ "distributorID").as[Long] must beEqualTo(distributorID)
      (response \ "keenProject").as[String] must beEqualTo(configVars.ConfigVarsKeen.projectID)

      // Verify scopedKey
      val decrypted = ScopedKeys.decrypt(configVars.ConfigVarsKeen.masterKey, (response \ "scopedKey").as[String]).toMap.toString()
      decrypted must contain("property_value=" + distributorID.toString)
    }

    "redirect the distributor user to their own Analytics page if they try to access analytics data using another distributor ID" in new WithFakeBrowser {
      val (maliciousUser, maliciousDistributor) = newDistributorUser("newuseremail2@gmail.com")
      setUpApp(maliciousDistributor.id.get)

      logInUser(maliciousUser.email, password)

      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorUser.distributorID.get, Some(app1.id), None).url)
      browser.url() must beEqualTo(controllers.routes.AnalyticsController.show(maliciousDistributor.id.get, None, None).url)
    }

    "export analytics data as csv" in new WithFakeBrowser {
      logInUser()

      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(app1.id), None).url)
      clickAndWaitForAngular("#export-as-csv")
      browser.fill("#export-email").`with`("test@test.com")
      clickAndWaitForAngular("#export-submit")

      browser.pageSource() must contain("Export Requested")
    }

    "export analytics data as csv must fail with invalid email" in new WithFakeBrowser {
      logInUser()

      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(app1.id), None).url)
      clickAndWaitForAngular("#export-as-csv")
      browser.fill("#export-email").`with`("test.com")
      clickAndWaitForAngular("#export-submit")

      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#csv-email-form").containsText("The email you entered is invalid.")
    }

    "export analytics data as csv should not allow field to be empty" in new WithFakeBrowser {
      logInUser()

      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(app1.id), None).url)
      clickAndWaitForAngular("#export-as-csv")
      browser.fill("#export-email").`with`("")
      clickAndWaitForAngular("#export-submit")

      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#csv-email-form").containsText("Email address is required")
    }

    "export analytics data as csv should not allow field to be empty after already successfully exporting" in new WithFakeBrowser {
      logInUser()

      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(app1.id), None).url)
      clickAndWaitForAngular("#export-as-csv")
      browser.fill("#export-email").`with`("test@test.com")
      browser.click("#export-submit")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#csv-requested").areDisplayed
      browser.executeScript("$('#export-requested-close').click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#csv-requested").areNotDisplayed

      browser.executeScript("$('#export-as-csv').click();")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#csv-email-form").areDisplayed
      browser.fill("#export-email").`with`("")
      browser.click("#export-submit")

      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#csv-email-form").containsText("Email address is required")
    }

    "Send JSON data without email and verify error response" in new WithFakeBrowser {
      val request = FakeRequest(
        POST,
        controllers.routes.AnalyticsController.export(distributorID).url,
        FakeHeaders(Seq("Content-type" -> "application/json")),
        JsObject(
          Seq(
            "noEmailParameter" -> JsString("some other parameter")
          )
        )
      )

      val Some(result) = route(request.withSession("distributorID" -> distributorID.toString, "username" -> email))
      status(result) must equalTo(400)
      contentAsString(result) must equalTo("{\"status\":\"error\",\"message\":\"Missing parameters\"}")
    }

    "Send bad JSON data and verify error response" in new WithFakeBrowser {
      val request = FakeRequest(
        POST,
        controllers.routes.AnalyticsController.export(distributorID).url,
        FakeHeaders(Seq("Content-type" -> "application/json")),
        "{BAD JSON DATA!@#}"
      )

      val Some(result) = route(request.withSession("distributorID" -> distributorID.toString, "username" -> email))
      status(result) must equalTo(400)
    }

    "Refresh analytics page when clicking on the HyprMediate logo without a persisted app_id" in new WithFakeBrowser {
      setUpApp(distributorID)
      logInUser(distributorUser.email, password)
      browser.goTo(controllers.routes.AnalyticsController.show(distributorUser.distributorID.get, None, None).url)
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#logo").isPresent
      clickAndWaitForAngular("#logo")
      browser.url must beEqualTo(controllers.routes.AnalyticsController.show(distributorUser.distributorID.get, None, None).url)
    }

    "include the platform logo in the 'Filter By App' list" in new WithFakeBrowser {
      val (currentDistributorUser, currentDistributor) = newDistributorUser("NewUser2@gmail.com")
      val (androidApp, _, _, _) = setUpApp(
        distributorID = currentDistributor.id.get,
        appName = Some("Android Test App"),
        currencyName = "Coins",
        exchangeRate = 100,
        rewardMin = 1,
        rewardMax = None,
        roundUp = true,
        platformID = testPlatform.Android.PlatformID
      )
      val (iosApp, _, _, _) = setUpApp(
        distributorID = currentDistributor.id.get,
        appName = Some("iOS Test App"),
        currencyName = "Coins",
        exchangeRate = 100,
        rewardMin = 1,
        rewardMax = None,
        roundUp = true,
        platformID = testPlatform.Ios.PlatformID
      )

      logInUser(currentDistributorUser.email, password)
      browser.goTo(controllers.routes.AnalyticsController.show(currentDistributor.id.get, None, None).url)
      clickAndWaitForAngular("#apps-filter .add")
      clickAndWaitForAngular("#filter-apps")
      fillAndWaitForAngular("#filter-apps", "App")

      List(testPlatform.Ios.PlatformName, testPlatform.Android.PlatformName)
        .map(platformName =>
          browser.find(
            "#apps-filter li a span",
            org.fluentlenium.core.filter.FilterConstructor.withClass().contains(platformName + "-app-list-logo")
          ).length must beEqualTo(1)
        )
    }

    "Pass Jasmine tests" in new WithAppBrowser(distributorUser.distributorID.get) {
      browser.goTo(routes.Assets.at("""/javascripts/test/SpecRunner.html""").url)
      browser.await().atMost(30, java.util.concurrent.TimeUnit.SECONDS).until(".bar.passed").isPresent
    }
  }

  "AnalyticsController.info" should {
    "include only unique ad provider names" in new WithFakeBrowser {
      adProviderService.loadAll()

      val request = FakeRequest(
        GET,
        controllers.routes.AnalyticsController.info(distributorID).url,
        FakeHeaders(),
        ""
      )

      val Some(result) = route(request.withSession("distributorID" -> distributorID.toString, "username" -> email))
      status(result) must equalTo(200)
      val response = Json.parse(contentAsString(result))

      val adProviders = (response \ "adProviders").as[JsArray].as[List[JsValue]]
      adProviders
        .foldLeft(Set[String]())((providerNames, provider) => providerNames + (provider \ "id").as[String])
        .toList.length must beEqualTo(adProviders.length)
    }

    "must include a platform name for each app" in new WithFakeBrowser {
      val (_, _, _, _) = setUpApp(
        distributorID = distributorID,
        appName = Some("Android Test App"),
        currencyName = "Coins",
        exchangeRate = 100,
        rewardMin = 1,
        rewardMax = None,
        roundUp = true,
        platformID = testPlatform.Android.PlatformID
      )

      val request = FakeRequest(
        GET,
        controllers.routes.AnalyticsController.info(distributorID).url,
        FakeHeaders(),
        ""
      )

      val Some(result) = route(request.withSession("distributorID" -> distributorID.toString, "username" -> email))
      status(result) must equalTo(200)
      val response = Json.parse(contentAsString(result))

      val platformNames = List(testPlatform.Android.PlatformName, testPlatform.Ios.PlatformName)
      val apps = (response \ "apps").as[JsArray].as[List[JsValue]]
      apps.map(appInfo =>
        platformNames must contain((appInfo \ "platformName").as[String])
      )
    }
  }
}
