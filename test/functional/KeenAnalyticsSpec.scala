package functional

import models._
import play.api.test.Helpers._
import resources.DistributorUserSetup
import scala.Some
import play.api.test.FakeApplication
import io.keen.client.scala.Client
import play.api.Play
import com.github.nscala_time.time._
import com.github.nscala_time.time.Imports._
import play.api.test.FakeApplication
import scala.Some
import play.api.libs.json.{JsValue, Json}

class KeenAnalyticsSpec extends SpecificationWithFixtures {

  val distributorUser = running(FakeApplication(additionalConfiguration = testDB)) {
    DistributorUser.create(email, password, "Company Name")
    DistributorUser.findByEmail(email).get
  }

  val distributorID = distributorUser.distributorID.get

  "Analytics keen tests" should {

    tag("keen")
    "Verify Keen analytics are loading" in new WithAppBrowser(distributorID) {
      logInUser()
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)

      // Verify analytics data has been loaded
      waitUntilContainsText("#analytics_loading_status", "Waiting...")

      // eCPM metric
      verifyAnalyticsHaveLoaded
    }

    tag("keen")
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

    tag("keen")
    "Update Ad Provider drop down and verify content begins updating" in new WithAppBrowser(distributorID) {
      logInUser()
      AdProvider.create("hyprMX", "{\"required_params\":[{\"description\": \"Your Vungle App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}]}", None)
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

    tag("keen")
    "Add both HyprMX and Vungle to the filter list" in new WithAppBrowser(distributorID) {
      logInUser()
      AdProvider.create("hyprMX", "{\"required_params\":[{\"description\": \"Your Vungle App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}]}", None)
      AdProvider.create("Vungle", "{\"required_params\":[{\"description\": \"Your Vungle App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}]}", None)
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)

      verifyAnalyticsHaveLoaded

      clickAndWaitForAngular("#ad_providers_filter .add")
      clickAndWaitForAngular("#filter_ad_providers")

      fillAndWaitForAngular("#filter_ad_providers", "Vungl")
      waitUntilContainsText("#ad_providers_filter .add", "Vungle")
      clickAndWaitForAngular("#ad_providers_filter .add .dropdown-menu .active")

      fillAndWaitForAngular("#filter_ad_providers", "hyprMX")
      waitUntilContainsText("#ad_providers_filter .add", "hyprMX")
      clickAndWaitForAngular("#ad_providers_filter .add .dropdown-menu .active")

      // Verify analytics data has been loaded
      waitUntilContainsText("#analytics_loading_status", "Waiting...")
      verifyAnalyticsHaveLoaded
    }

    tag("keen")
    "Clear all filters list" in new WithAppBrowser(distributorID) {
      logInUser()
      AdProvider.create("hyprMX", "{\"required_params\":[{\"description\": \"Your Vungle App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}]}", None)
      AdProvider.create("Vungle", "{\"required_params\":[{\"description\": \"Your Vungle App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}]}", None)
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)

      verifyAnalyticsHaveLoaded

      clickAndWaitForAngular("#ad_providers_filter .add")
      clickAndWaitForAngular("#filter_ad_providers")

      fillAndWaitForAngular("#filter_ad_providers", "Vungl")
      waitUntilContainsText("#ad_providers_filter .add", "Vungle")
      clickAndWaitForAngular("#ad_providers_filter .add .dropdown-menu .active")

      fillAndWaitForAngular("#filter_ad_providers", "hyprMX")
      waitUntilContainsText("#ad_providers_filter .add", "hyprMX")
      clickAndWaitForAngular("#ad_providers_filter .add .dropdown-menu .active")
      // Verify analytics data has been loaded
      waitUntilContainsText("#analytics_loading_status", "Waiting...")
      verifyAnalyticsHaveLoaded

      clickAndWaitForAngular(".reset_filters")

      // Verify analytics data has been loaded
      waitUntilContainsText("#analytics_loading_status", "Waiting...")
      verifyAnalyticsHaveLoaded

    }
  }
}
