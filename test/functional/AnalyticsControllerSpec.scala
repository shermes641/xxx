package functional

import models._
import play.api.test._
import play.api.test.Helpers._
import com.github.nscala_time.time._
import com.github.nscala_time.time.Imports._
import play.api.test.FakeApplication
import play.api.Play
import io.keen.client.java.ScopedKeys
import resources.DistributorUserSetup
import collection.JavaConversions._
import play.api.libs.json._
import play.api.test.FakeHeaders
import scala.Some
import play.api.test.FakeApplication
import play.api.test.FakeHeaders
import scala.Some
import play.api.test.FakeApplication
import play.api.libs.json.{JsString, JsObject, Json}
import controllers.routes

class AnalyticsControllerSpec extends SpecificationWithFixtures with DistributorUserSetup with AppCreationHelper {

  val distributorUser = running(FakeApplication(additionalConfiguration = testDB)) {
    DistributorUser.create(email, password, "Company Name")
    DistributorUser.findByEmail(email).get
  }

  val distributorID = distributorUser.distributorID.get

  "Analytics show action" should {
    "show analytics for an app" in new WithAppBrowser(distributorID) {
      logInUser()
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)
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

      val adProviderID = AdProvider.create("hyprMX", "{\"required_params\":[{\"description\": \"Your Vungle App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}]}", None)
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)

      // All Ad Providers should be selected
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#ad_providers_filter").containsText("All Ad Providers")

      clickAndWaitForAngular("#ad_providers_filter .add")
      clickAndWaitForAngular("#filter_ad_providers")

      // hyprMX must be part of dropdown
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#ad_providers_filter .add").containsText("hyprMX")
    }

    "country select box should exist and not be empty" in new WithAppBrowser(distributorID) {
      logInUser()

      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)

      // All Countries should be selected
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#countries_filter").containsText("All Countries")

      clickAndWaitForAngular("#countries_filter .add")
      clickAndWaitForAngular("#filter_countries")

      // United States and Ireland must be part of dropdown
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#countries_filter .add").containsText("United States")

      // Test Autocomplete
      fillAndWaitForAngular("#filter_countries", "Ire")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until("#countries_filter .add").containsText("Ireland")
    }

    "date picker should be setup correctly" in new WithAppBrowser(distributorID) {
      logInUser()

      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)

      var date = DateTime.now
      // End date must be todays date
      browser.$("#end_date").getValue() must beEqualTo(date.toString("MMM dd, yyyy"))

      // Start date must be todays date minus 1 month
      browser.$("#start_date").getValue() must beEqualTo(date.minusDays(13).toString("MMM dd, yyyy"))
    }

    "Verify Analytic items have proper labels" in new WithAppBrowser(distributorID) {
      logInUser()
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)

      // eCPM metric
      waitUntilContainsText("#ecpm_metric_container", "Average eCPM")
      // Fill Rate metric
      waitUntilContainsText("#fill_rate_container", "Fill Rate")
      // Average Revenue metric
      waitUntilContainsText("#unique_users_container", "Average Revenue Per Day")
      // Revenue Table
      waitUntilContainsText("#estimated_revenue_chart_container", "Above results use Ad Network eCPMs to estimate revenue.")
    }

    "Check Analytics configuration info JSON" in new WithAppBrowser(distributorID) {
      logInUser()
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)

      val request = FakeRequest(
        GET,
        controllers.routes.AnalyticsController.info(distributorID).url,
        FakeHeaders(),
        ""
      )

      val Some(result) = route(request.withSession("distributorID" -> distributorID.toString, "username" -> email))
      status(result) must equalTo(200)
      val response = Json.parse(contentAsString(result))

      (response \ "distributorID").as[Long] must beEqualTo(distributorID)
      (response \ "keenProject").as[String] must beEqualTo(Play.current.configuration.getString("keen.project").get)

      // Verify scopedKey
      val decrypted = ScopedKeys.decrypt(Play.current.configuration.getString("keen.masterKey").get, (response \ "scopedKey").as[String]).toMap.toString()
      decrypted must contain("property_value="+distributorID.toString)
    }

    "redirect the distributor user to their own Analytics page if they try to access analytics data using another distributor ID" in new WithAppBrowser(distributorUser.distributorID.get) {
      val (maliciousUser, maliciousDistributor) = newDistributorUser("newuseremail2@gmail.com")
      setUpApp(maliciousDistributor.id.get)

      logInUser(maliciousUser.email, password)

      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorUser.distributorID.get, Some(currentApp.id)).url)
      browser.url() must beEqualTo(controllers.routes.AnalyticsController.show(maliciousDistributor.id.get, None).url)
    }

    "export analytics data as csv" in new WithAppBrowser(distributorUser.distributorID.get) {
      logInUser()

      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)
      clickAndWaitForAngular("#export_as_csv")
      browser.fill("#export_email").`with`("test@test.com")
      clickAndWaitForAngular("#export_submit")

      browser.pageSource() must contain("Export Requested")
    }

    "export analytics data as csv must fail with invalid email" in new WithAppBrowser(distributorUser.distributorID.get) {
      logInUser()

      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)
      clickAndWaitForAngular("#export_as_csv")
      browser.fill("#export_email").`with`("test@test.com")
      clickAndWaitForAngular("#export_submit")

      browser.pageSource must contain("The email you entered is invalid.")
    }

    "Send JSON data without email and verify error response" in new WithAppBrowser(distributorUser.distributorID.get) {
      val request = FakeRequest(
        POST,
        controllers.routes.AnalyticsController.export(distributorID).url,
        FakeHeaders(Seq("Content-type" -> Seq("application/json"))),
        JsObject(
          Seq(
            "noEmailParameter" -> JsString("some other parameter")
          )
        )
      )

      val Some(result) = route(request.withSession("distributorID" -> distributorID.toString, "username" -> email))
      status(result) must equalTo(400)
      contentAsString(result) must equalTo("Missing parameter [email]")
    }

    "Send bad JSON data and verify error response" in new WithAppBrowser(distributorUser.distributorID.get) {
      val request = FakeRequest(
        POST,
        controllers.routes.AnalyticsController.export(distributorID).url,
        FakeHeaders(Seq("Content-type" -> Seq("application/json"))),
        "{BAD JSON DATA!@#}"
      )

      val Some(result) = route(request.withSession("distributorID" -> distributorID.toString, "username" -> email))
      status(result) must equalTo(400)
      contentAsString(result) must equalTo("Expecting Json data")
    }

    "Pass Jasmine tests" in new WithAppBrowser(distributorUser.distributorID.get) {
      browser.goTo(routes.Assets.at("/javascripts/test/SpecRunner.html").url)
      browser.pageSource must contain("bar passed")
    }
  }
}
