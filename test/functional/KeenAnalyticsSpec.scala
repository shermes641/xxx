package functional

import models._
import play.api.test.Helpers._
import resources.SpecificationWithFixtures

class KeenAnalyticsSpec extends SpecificationWithFixtures {
  // Load AdProviders required for tests
  running(testApplication) {
    val unityAdsName = "UnityAds"
    val unityAdsDisplayName = "Unity Ads"
    adProviderService.create(
      name = Constants.UnityAds.Name,
      displayName = Constants.UnityAds.DisplayName,
      configurationData = "{\"required_params\":[{\"description\": \"Your Unity Ads GAME Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}]}",
      platformID = testPlatform.Ios.PlatformID,
      callbackUrlFormat = None,
      callbackUrlDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(Constants.UnityAds.DisplayName)
    )

    val vungleName = "Vungle"
    adProviderService.create(
      name = "Vungle",
      displayName = vungleName,
      configurationData = "{\"required_params\":[{\"description\": \"Your Vungle App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}]}",
      platformID = testPlatform.Ios.PlatformID,
      callbackUrlFormat = None,
      callbackUrlDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(vungleName)
    )
  }

  val distributorUser = running(testApplication) {
    distributorUserService.create(email, password, "Company Name")
    distributorUserService.findByEmail(email).get
  }

  val distributorID = distributorUser.distributorID.get

  "Analytics keen tests" should {

    tag("keen")
    "Verify Keen analytics are loading" in new WithAppBrowser(distributorID) {
      logInUser()
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id), None).url)

      // Verify analytics data has been loaded
      waitUntilContainsText("#analytics-loading-status", "Waiting...")

      // eCPM metric
      verifyAnalyticsHaveLoaded
    }

    tag("keen")
    "Update Apps drop down and verify content begins updating" in new WithAppBrowser(distributorID) {
      logInUser()
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id), None).url)

      verifyAnalyticsHaveLoaded

      clickAndWaitForAngular("#apps-filter .add")
      clickAndWaitForAngular("#filter-apps")

      // Unity Ads must be part of dropdown
      waitUntilContainsText("#apps-filter .add", currentApp.name)
      clickAndWaitForAngular("#apps-filter .add .dropdown-menu .active")

      // Verify analytics data has been loaded
      waitUntilContainsText("#analytics-loading-status", "Waiting...")
      verifyAnalyticsHaveLoaded
    }

    tag("keen")
    "Update Ad Provider drop down and verify content begins updating" in new WithAppBrowser(distributorID) {
      logInUser()
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id), None).url)

      verifyAnalyticsHaveLoaded

      clickAndWaitForAngular("#ad-providers-filter .add")
      clickAndWaitForAngular("#filter-ad_providers")

      // Unity Ads must be part of dropdown
      waitUntilContainsText("#ad-providers-filter .add", "Unity Ads")
      clickAndWaitForAngular("#ad-providers-filter .add .dropdown-menu .active")

      // Verify analytics data has been loaded
      waitUntilContainsText("#analytics-loading-status", "Waiting...")
      verifyAnalyticsHaveLoaded
    }

    tag("keen")
    "Add both HyprMX and Vungle to the filter list" in new WithAppBrowser(distributorID) {
      logInUser()
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id), None).url)

      verifyAnalyticsHaveLoaded

      clickAndWaitForAngular("#ad-providers-filter .add")
      clickAndWaitForAngular("#filter-ad_providers")

      fillAndWaitForAngular("#filter-ad_providers", "Vungl")
      waitUntilContainsText("#ad-providers-filter .add", "Vungle")
      clickAndWaitForAngular("#ad-providers-filter .add .dropdown-menu .active")

      clickAndWaitForAngular("#ad-providers-filter .add")
      fillAndWaitForAngular("#filter-ad_providers", "Unity")
      waitUntilContainsText("#ad-providers-filter .add", "Unity Ads")
      clickAndWaitForAngular("#ad-providers-filter .add .dropdown-menu .active")

      // Verify analytics data has been loaded
      waitUntilContainsText("#analytics-loading-status", "Waiting...")
      verifyAnalyticsHaveLoaded
    }

    tag("keen")
    "Clear all filters list" in new WithAppBrowser(distributorID) {
      logInUser()
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id), None).url)

      verifyAnalyticsHaveLoaded

      clickAndWaitForAngular("#ad-providers-filter .add")
      clickAndWaitForAngular("#filter-ad_providers")

      fillAndWaitForAngular("#filter-ad_providers", "Vungl")
      waitUntilContainsText("#ad-providers-filter .add", "Vungle")
      clickAndWaitForAngular("#ad-providers-filter .add .dropdown-menu .active")

      clickAndWaitForAngular("#ad-providers-filter .add")
      fillAndWaitForAngular("#filter-ad_providers", "Unity")
      waitUntilContainsText("#ad-providers-filter .add", "Unity Ads")
      clickAndWaitForAngular("#ad-providers-filter .add .dropdown-menu .active")
      // Verify analytics data has been loaded
      waitUntilContainsText("#analytics-loading-status", "Waiting...")
      verifyAnalyticsHaveLoaded

      clickAndWaitForAngular(".reset-filters")

      // Verify analytics data has been loaded
      waitUntilContainsText("#analytics-loading-status", "Waiting...")
      verifyAnalyticsHaveLoaded

    }
  }
}
