package functional

import models._
import play.api.test.Helpers._
import resources.DistributorUserSetup
import scala.Some
import play.api.test.FakeApplication

class KeenAnalyticsSpec extends SpecificationWithFixtures with DistributorUserSetup with AppCreationHelper {

  val distributorUser = running(FakeApplication(additionalConfiguration = testDB)) {
    DistributorUser.create(email, password, "Company Name")
    DistributorUser.findByEmail(email).get
  }

  val distributorID = distributorUser.distributorID.get

  "Analytics keen tests" should {

    "Verify Keen analytics are loading" in new WithAppBrowser(distributorID) {
      logInUser()
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)

      // Verify analytics data has been loaded
      waitUntilContainsText("#analytics_loading_status", "Waiting...")

      // eCPM metric
      verifyAnalyticsHaveLoaded
    }

    "Update Countries drop down and verify content begins updating" in new WithAppBrowser(distributorID) {
      logInUser()
      val adProviderID = AdProvider.create("hyprMX", "{\"required_params\":[{\"description\": \"Your Vungle App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}]}", None)
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)

      verifyAnalyticsHaveLoaded

      clickAndWaitForAngular("#countries_filter .add")
      clickAndWaitForAngular("#filter_countries")

      fillAndWaitForAngular("#filter_countries", "Ire")
      waitUntilContainsText("#countries_filter .add", "Ireland")
      clickAndWaitForAngular("#countries_filter .add .dropdown-menu .active")

      waitUntilContainsText("#analytics_loading_status", "Waiting...")
      verifyAnalyticsHaveLoaded
    }

    "Update Apps drop down and verify content begins updating" in new WithAppBrowser(distributorID) {
      logInUser()
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)

      verifyAnalyticsHaveLoaded

      clickAndWaitForAngular("#apps_filter .add")
      clickAndWaitForAngular("#filter_apps")

      // hyprMX must be part of dropdown
      waitUntilContainsText("#apps_filter .add", "New App")
      clickAndWaitForAngular("#apps_filter .add .dropdown-menu .active")

      // Verify analytics data has been loaded
      waitUntilContainsText("#analytics_loading_status", "Waiting...")
      verifyAnalyticsHaveLoaded
    }

    "Update Ad Provider drop down and verify content begins updating" in new WithAppBrowser(distributorID) {
      logInUser()
      val adProviderID = AdProvider.create("hyprMX", "{\"required_params\":[{\"description\": \"Your Vungle App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}]}", None)
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)

      verifyAnalyticsHaveLoaded

      clickAndWaitForAngular("#ad_providers_filter .add")
      clickAndWaitForAngular("#filter_ad_providers")

      // hyprMX must be part of dropdown
      waitUntilContainsText("#ad_providers_filter .add", "hyprMX")
      clickAndWaitForAngular("#ad_providers_filter .add .dropdown-menu .active")

      // Verify analytics data has been loaded
      waitUntilContainsText("#analytics_loading_status", "Waiting...")
      verifyAnalyticsHaveLoaded
    }
  }
}
