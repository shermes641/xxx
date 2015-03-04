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
import play.api.libs.json.{JsString, JsObject, Json}

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

    "populate ad networks for show page" in new WithAppBrowser(distributorID) {
      logInUser()

      val adProviderID = AdProvider.create("hyprMX", "{\"required_params\":[{\"description\": \"Your Vungle App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}]}", None)
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)

      // Verify first option defaults to "all"
      browser.$("#ad_providers").getValue() must beEqualTo("all")
    }

    "country select box should exist and not be empty" in new WithAppBrowser(distributorID) {
      logInUser()

      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)

      // Verify first option defaults to "all"
      browser.$("#countries").getValue() must beEqualTo("all")
    }

    "date picker should be setup correctly" in new WithAppBrowser(distributorID) {
      logInUser()

      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)

      var date = DateTime.now
      // End date must be todays date
      browser.$("#end_date").getValue() must beEqualTo(date.toString("MM/dd/yyyy"))

      // Start date must be todays date minus 1 month
      browser.$("#start_date").getValue() must beEqualTo(date.minusMonths(1).toString("MM/dd/yyyy"))
    }

    "Keen project should be set correctly in hidden field" in new WithAppBrowser(distributorID) {
      logInUser()
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)

      // eCPM must be set correctly (placeholder for now)
      browser.$("#keen_project").getValue() must beEqualTo(Play.current.configuration.getString("keen.project").get)
    }

    "Scoped key has been created correctly" in new WithAppBrowser(distributorID) {
      logInUser()

      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)

      val decrypted = ScopedKeys.decrypt(Play.current.configuration.getString("keen.masterKey").get, browser.$("#scoped_key").getValue()).toMap.toString()
      decrypted must contain("property_value="+distributorID.toString)
    }

    "not display analytics data for an App ID not owned by the current Distributor User" in new WithAppBrowser(distributorUser.distributorID.get) {
      val (maliciousUser, maliciousDistributor) = newDistributorUser("newuseremail@gmail.com")
      setUpApp(maliciousDistributor.id.get)

      logInUser(maliciousUser.email, password)

      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(maliciousDistributor.id.get, Some(currentApp.id)).url)
      browser.pageSource() must not contain currentApp.name
    }

    "redirect the distributor user to their own Analytics page if they try to access analytics data using another distributor ID" in new WithAppBrowser(distributorUser.distributorID.get) {
      val (maliciousUser, maliciousDistributor) = newDistributorUser("newuseremail2@gmail.com")
      setUpApp(maliciousDistributor.id.get)

      logInUser(maliciousUser.email, password)

      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorUser.distributorID.get, Some(currentApp.id)).url)
      browser.url() must beEqualTo(controllers.routes.AnalyticsController.show(maliciousDistributor.id.get, None).url)
    }

    "export analytics data as csv" in new WithAppBrowser(distributorUser.distributorID.get) {
      val app2Name = "App 2"
      val appID = App.create(distributorID, app2Name).get
      logInUser()

      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(appID)).url)
      clickAndWaitForAngular("#export_as_csv")
      browser.fill("#export_email").`with`("test@test.com")
      clickAndWaitForAngular("#export_submit")

      browser.pageSource() must contain("Export Requested")
    }

    "export analytics data as csv must fail with invalid email" in new WithAppBrowser(distributorUser.distributorID.get) {
      val app2Name = "App 2"
      val appID = App.create(distributorID, app2Name).get

      logInUser()

      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(appID)).url)
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
  }
}
