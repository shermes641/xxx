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

class KeenAnalyticsSpec extends SpecificationWithFixtures with DistributorUserSetup with AppCreationHelper {

  val distributorUser = running(FakeApplication(additionalConfiguration = testDB)) {
    DistributorUser.create(email, password, "Company Name")
    DistributorUser.findByEmail(email).get
  }

  val distributorID = distributorUser.distributorID.get

  val client = new Client(
    projectId = "54f9f83759949a4de94dd324",
    masterKey = Some("78F9F5CFB4A4D265FF60F410F0C16E9D"),
    writeKey = Some("bff9732008b801b855d83451b4c234e797c64988febbb26e18d9ee35a89a5c48e578c5da90ea7a0fac707a70fd4c65ccc61fb64cf445718009e216aad5b16803afd800780f6be10ed1431e8fb87a86e225f9258cac86766817e816c72acbd09cffe3a5c6af05ac682892af447ef1507d"),
    readKey = Some("7d85cb70ab77463227313c503dafedbe44dbf5c3e1df730574c824864d53adee0a11e83dd7a92090baea023487b1bddb826b66a6fc58e45f54e75051950027132fbbdfb146efbeb3a527845954e830ab30f13703c36fb7ec252a9bf45e52240983d0be2bda2f1dee5b38d82e913078f8")
  )

  def createInventoryRequests(distributorID: Long, appID: Long, adProviderID: Long, count: Int) = {
    val date = DateTime.now
    val inventoryRequests = Array.fill(count) {
      Json.obj(
        "distributor_id" -> distributorID,
        "distributor_name" -> "Test Company",
        "app_name" -> "The Video Game",
        "platform" -> "iOS",
        "app_id" -> appID,
        "ad_provider_id" -> adProviderID,
        "ad_provider_name" -> "HyprMarketplace",
        "device_type" -> "iphone",
        "device_unique_id" -> "UUID",
        "ip_address" -> "${keen.ip}",
        "keen" -> Json.obj(
          "timestamp" -> date.toString()
        )
      )
    }

    val batch_request: JsValue = Json.obj(
      "availability_requested" -> inventoryRequests
    )
    client.addEvents(Json.stringify(Json.toJson(batch_request)))
  }

  def createInventoryAvailable(distributorID: Long, appID: Long, adProviderID: Long, count: Int) = {
    val date = DateTime.now
    val inventoryAvailable = Array.fill(count) {
      Json.obj(
        "distributor_name" -> "Test Company",
        "distributor_id" -> distributorID,
        "app_name" -> "The Video Game",
        "platform" -> "iOS",
        "app_id" -> appID,
        "ad_provider_id" -> adProviderID,
        "ad_provider_name" -> "AppLovin",
        "device_type" -> "iphone",
        "device_unique_id" -> "UUID",
        "ip_address" -> "${keen.ip}",
        "keen" -> Json.obj(
          "timestamp" -> date.toString()
        )
      )
    }

    val batch_request: JsValue = Json.obj(
      "availability_response_true" -> inventoryAvailable
    )
    client.addEvents(Json.stringify(Json.toJson(batch_request)))
  }

  "Analytics keen tests" should {
//
//    tag("keen")
//    "Verify Keen analytics are loading" in new WithAppBrowser(distributorID) {
//      logInUser()
//      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)
//
//      // Verify analytics data has been loaded
//      waitUntilContainsText("#analytics_loading_status", "Waiting...")
//
//      // eCPM metric
//      verifyAnalyticsHaveLoaded
//    }
//
//    tag("keen")
//    "Update Countries drop down and verify content begins updating" in new WithAppBrowser(distributorID) {
//      logInUser()
//      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)
//
//      verifyAnalyticsHaveLoaded
//
//      clickAndWaitForAngular("#countries_filter .add")
//      clickAndWaitForAngular("#filter_countries")
//
//      fillAndWaitForAngular("#filter_countries", "Ire")
//      waitUntilContainsText("#countries_filter .add", "Ireland")
//      clickAndWaitForAngular("#countries_filter .add .dropdown-menu .active")
//
//      waitUntilContainsText("#analytics_loading_status", "Waiting...")
//      verifyAnalyticsHaveLoaded
//    }
//
//    tag("keen")
//    "Update Apps drop down and verify content begins updating" in new WithAppBrowser(distributorID) {
//      logInUser()
//      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)
//
//      verifyAnalyticsHaveLoaded
//
//      clickAndWaitForAngular("#apps_filter .add")
//      clickAndWaitForAngular("#filter_apps")
//
//      // hyprMX must be part of dropdown
//      waitUntilContainsText("#apps_filter .add", "New App")
//      clickAndWaitForAngular("#apps_filter .add .dropdown-menu .active")
//
//      // Verify analytics data has been loaded
//      waitUntilContainsText("#analytics_loading_status", "Waiting...")
//      verifyAnalyticsHaveLoaded
//    }
//
//    tag("keen")
//    "Update Ad Provider drop down and verify content begins updating" in new WithAppBrowser(distributorID) {
//      logInUser()
//      val adProviderID = AdProvider.create("hyprMX", "{\"required_params\":[{\"description\": \"Your Vungle App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}]}", None)
//      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id)).url)
//
//      verifyAnalyticsHaveLoaded
//
//      clickAndWaitForAngular("#ad_providers_filter .add")
//      clickAndWaitForAngular("#filter_ad_providers")
//
//      // hyprMX must be part of dropdown
//      waitUntilContainsText("#ad_providers_filter .add", "hyprMX")
//      clickAndWaitForAngular("#ad_providers_filter .add .dropdown-menu .active")
//
//      // Verify analytics data has been loaded
//      waitUntilContainsText("#analytics_loading_status", "Waiting...")
//      verifyAnalyticsHaveLoaded
//    }

    tag("keen")
    "Verify inserted Keen data shows up" in new WithAppBrowser(distributorID) {
      val appName = "Analytics Test App"
      logInUser()
      var (analyticsApp, _, _, _) = setUpApp(distributorID, appName)
      val adProviderID = AdProvider.create("hyprMX", "{\"required_params\":[{\"description\": \"Your Vungle App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}]}", None)

      createInventoryRequests(distributorID, analyticsApp.id, adProviderID.get, 20)
      createInventoryAvailable(distributorID, analyticsApp.id, adProviderID.get, 10)

      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(analyticsApp.id)).url)

      verifyAnalyticsHaveLoaded

      clickAndWaitForAngular("#apps_filter .add")
      clickAndWaitForAngular("#filter_apps")

      // hyprMX must be part of dropdown
      waitUntilContainsText("#apps_filter .add", appName)
      fillAndWaitForAngular("#filter_apps", appName)
      clickAndWaitForAngular("#apps_filter .add .dropdown-menu .active")

      // Verify analytics data has been loaded
      waitUntilContainsText("#analytics_loading_status", "Waiting...")
      verifyAnalyticsHaveLoaded
    }
  }
}
