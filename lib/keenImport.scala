import com.github.nscala_time.time.Imports._
import io.keen.client.scala.Client
import models._
import org.joda.time.format.DateTimeFormat
import play.api._
import play.api.db.DB
import play.api.libs.json.{Json, JsValue}
import play.api.Play._
import scala.util.Random

// The "correct" way to start the app
new play.core.StaticApplication(new java.io.File("."))

if(Environment.isProd) {
  Logger.warn("YOU ARE CURRENTLY IN A PRODUCTION ENVIRONMENT - DO NOT RUN THIS SCRIPT")
} else {
  val client = new Client(
    projectId = Play.current.configuration.getString("keen.project").get,
    masterKey = Play.current.configuration.getString("keen.masterKey"),
    writeKey = Play.current.configuration.getString("keen.writeKey"),
    readKey = Play.current.configuration.getString("keen.readKey")
  )

  val randomCharacters = Random.alphanumeric.take(5).mkString
  val companyName = "Test Company-" + randomCharacters
  val email = "mediation-testing-" + randomCharacters + "@jungroup.com"
  val password = "testtest"

  val adProviders = AdProvider.findAll

  val distributorID = DistributorUser.create(email, password, companyName).get

  object AppHelper extends UpdateHyprMarketplace {
    def createApp(appName: String) = {
      val appID = App.create(distributorID, appName).get
      val app = App.find(appID).get

      val wap = DB.withTransaction { implicit connection =>
        val waterfallID = Waterfall.create(appID, appName).get
        VirtualCurrency.createWithTransaction(appID, "Gold", 1, 10, None, Some(true))
        val adProviderID = Play.current.configuration.getLong("hyprmarketplace.ad_provider_id").get
        val hyprWaterfallAdProviderID = WaterfallAdProvider.createWithTransaction(waterfallID, adProviderID, Option(0), Option(20), configurable = false, active = false, pending = true).get
        AppConfig.create(appID, app.token, 0)
        val hyprWAP = WaterfallAdProvider.findWithTransaction(hyprWaterfallAdProviderID).get
        WaterfallAdProviderWithAppData(hyprWAP.id, waterfallID, adProviderID, hyprWAP.waterfallOrder, hyprWAP.cpm, hyprWAP.active, hyprWAP.fillRate, hyprWAP.configurationData, hyprWAP.reportingActive, hyprWAP.pending, app.token, app.name, companyName)
      }
      updateHyprMarketplaceDistributorID(wap)

      (appID, appName)
    }
  }

  val firstApp = AppHelper.createApp("First App")

  // Repeat first app to get multiple ad providers
  val apps = List(firstApp, firstApp, firstApp, AppHelper.createApp("Second App"), AppHelper.createApp("Third App"), AppHelper.createApp("Fourth App"))

  val keenCountURLRoot = "https://api.keen.io/3.0/projects/" + Play.current.configuration.getString("keen.project").get + "/queries/count?api_key=" + Play.current.configuration.getString("keen.readKey").get + "&filters=%5B%7B%22property_name%22%3A%22distributor_id%22%2C%22operator%22%3A%22in%22%2C%22property_value%22%3A%5B" + distributorID.toString + "%5D%7D%5D"
  val keenAverageURLRoot = "https://api.keen.io/3.0/projects/" + Play.current.configuration.getString("keen.project").get + "/queries/average?api_key=" + Play.current.configuration.getString("keen.readKey").get + "&filters=%5B%7B%22property_name%22%3A%22distributor_id%22%2C%22operator%22%3A%22in%22%2C%22property_value%22%3A%5B" + distributorID.toString + "%5D%7D%5D"

  println("TESTING URLS")
  println("============")
  println("")
  println("Count of Inventory Requests")
  println(keenCountURLRoot + "&event_collection=availability_requested")
  println("")
  println("Count of Inventory Available")
  println(keenCountURLRoot + "&event_collection=availability_response_true")
  println("")
  println("Count of Mediation Requested")
  println(keenCountURLRoot + "&event_collection=mediate_availability_requested")
  println("")
  println("Count of Mediation Available")
  println(keenCountURLRoot + "&event_collection=mediate_availability_response_true")
  println("")
  println("Count of Ad Displayed")
  println(keenCountURLRoot + "&event_collection=ad_displayed")
  println("")
  println("Count of Ad Completed")
  println(keenCountURLRoot + "&event_collection=ad_completed")
  println("")
  println("eCPM Average")
  println(keenAverageURLRoot + "&event_collection=ad_completed&target_property=ad_provider_eCPM")

  println("")
  println("")
  println("")
  println("KEEN IMPORT SCRIPT")
  println("==================")
  println("")
  println("DATA IMPORT MAY TAKE SOME TIME TO COMPLETE")
  println("")
  println("Username:      " + email)
  println("Password:      " + password)
  println("Company Name:  " + companyName)
  println("")
  println("Distributor ID: " + distributorID)

  apps.foreach {
    app =>
      val (appID, appName) = app
      val randomIndex = Random.nextInt(adProviders.length)
      val adProvider = adProviders(randomIndex)

      println("")
      println(appName.toUpperCase + " using Ad Provider " + adProvider.name.toUpperCase)
      println("App ID:            " + appID)
      println("Ad Provider ID:    " + adProvider.id)

      createEvents(appName, appID, adProvider, DateTime.now)
      createEvents(appName, appID, adProvider, DateTime.now - 1.day)
      createEvents(appName, appID, adProvider, DateTime.now - 2.day)
  }

  def createEvents(appName: String, appID: Long, adProvider: AdProvider, startDate: DateTime) = {
    var date = startDate
    val inventoryRequestCount = Random.nextInt(60) + 30
    val inventoryRequests = Array.fill(inventoryRequestCount) {
      date = date - 1.minute
      Json.obj(
        "distributor_id" -> distributorID,
        "distributor_name" -> companyName,
        "app_name" -> appName,
        "platform" -> "iOS",
        "app_id" -> appID,
        "ad_provider_id" -> adProvider.id,
        "ad_provider_name" -> adProvider.name,
        "device_type" -> "iphone",
        "device_unique_id" -> "UUID",
        "ip_address" -> "${keen.ip}",
        "keen" -> Json.obj(
          "timestamp" -> date.toString()
        )
      )
    }
    date = startDate

    val inventoryAvailableCount = Random.nextInt(30) + 1
    val inventoryAvailable = Array.fill(inventoryAvailableCount) {
      date = date - 1.minute
      Json.obj(
        "distributor_name" -> companyName,
        "distributor_id" -> distributorID,
        "app_name" -> appName,
        "platform" -> "iOS",
        "app_id" -> appID,
        "ad_provider_id" -> adProvider.id,
        "ad_provider_name" -> adProvider.name,
        "device_type" -> "iphone",
        "device_unique_id" -> "UUID",
        "ip_address" -> "${keen.ip}",
        "keen" -> Json.obj(
          "timestamp" -> date.toString()
        )
      )
    }
    date = startDate

    val mediationRequestsCount = Random.nextInt(100) + 50
    val mediationInventoryRequests = Array.fill(mediationRequestsCount) {
      date = date - 1.minute
      Json.obj(
        "distributor_id" -> distributorID,
        "distributor_name" -> companyName,
        "app_name" -> appName,
        "platform" -> "iOS",
        "app_id" -> appID,
        "device_type" -> "iphone",
        "device_unique_id" -> "UUID",
        "ip_address" -> "${keen.ip}",
        "keen" -> Json.obj(
          "timestamp" -> date.toString()
        )
      )
    }
    date = startDate

    val mediationAvailableCount = Random.nextInt(50) + 1
    val mediationInventoryAvailable = Array.fill(mediationAvailableCount) {
      date = date - 1.minute
      Json.obj(
        "distributor_id" -> distributorID,
        "app_name" -> appName,
        "platform" -> "iOS",
        "app_id" -> appID,
        "device_type" -> "iphone",
        "device_unique_id" -> "UUID",
        "ip_address" -> "${keen.ip}",
        "ad_provider_id" -> adProvider.id,
        "ad_provider_name" -> adProvider.name,
        "keen" -> Json.obj(
          "timestamp" -> date.toString()
        )
      )
    }
    date = startDate

    val adDisplayedCount = Random.nextInt(25) + 15
    val adDisplayed = Array.fill(adDisplayedCount) {
      date = date - 1.minute
      Json.obj(
        "distributor_name" -> companyName,
        "distributor_id" -> distributorID,
        "app_name" -> appName,
        "platform" -> "iOS",
        "app_id" -> appID,
        "ad_provider_id" -> adProvider.id,
        "ad_provider_name" -> adProvider.name,
        "device_type" -> "iphone",
        "device_unique_id" -> "UUID",
        "ip_address" -> "${keen.ip}",
        "keen" -> Json.obj(
          "timestamp" -> date.toString()
        )
      )
    }
    date = startDate

    val adCompletedCount = Random.nextInt(15) + 1
    var eCPMSum = 0
    val adCompleted = Array.fill(adCompletedCount) {
      val eCPMValue = Random.nextInt(10) + 1
      eCPMSum += eCPMValue
      date = date - 1.minute
      Json.obj(
        "distributor_id" -> distributorID,
        "distributor_name" -> companyName,
        "app_name" -> appName,
        "platform" -> "iOS",
        "app_id" -> appID,
        "ad_provider_id" -> adProvider.id,
        "ad_provider_name" -> adProvider.name,
        "device_type" -> "iphone",
        "device_unique_id" -> "UUID",
        "ip_address" -> "${keen.ip}",
        "virtual_currency_awarded" -> 1,
        "virtual_currency_reward_max" -> 100,
        "ad_provider_eCPM" -> eCPMValue,
        "virtual_currency_round_up" -> "yes",
        "offer_reward_id" -> 0,
        "offer_reward_text" -> "1 reward",
        "offer_reward_quantity" -> 1,
        "offer_title" -> "another test",
        "offer_description" -> "one more test video",
        "virtual_currency_exchange_rate" -> 100,
        "virtual_currency_name" -> "Coins",
        "offer_type" -> "video",
        "offer_is_default" -> "false",
        "virtual_currency_reward_min" -> 1,
        "keen" -> Json.obj(
          "timestamp" -> date.toString()
        )
      )
    }
    val eCPMAverage = eCPMSum.toFloat / adCompletedCount

    val batch_request: JsValue = Json.obj(
      "availability_requested" -> inventoryRequests,
      "availability_response_true" -> inventoryAvailable
    )

    val batch_request_mediation: JsValue = Json.obj(
      "mediate_availability_requested" -> mediationInventoryRequests,
      "mediate_availability_response_true" -> mediationInventoryAvailable
    )

    val batch_request_ads: JsValue = Json.obj(
      "ad_displayed" -> adDisplayed,
      "ad_completed" -> adCompleted
    )

    // Publish lots of events
    client.addEvents(Json.stringify(Json.toJson(batch_request)))
    client.addEvents(Json.stringify(Json.toJson(batch_request_mediation)))
    client.addEvents(Json.stringify(Json.toJson(batch_request_ads)))

    println("==================")
    println("Date:                      " + date.toString(DateTimeFormat.forPattern("MM-dd-yyyy")))
    println("Inventory Request Count:   " + inventoryRequestCount)
    println("Inventory Available Count: " + inventoryAvailableCount)
    println("Fill Rate:                 " + (inventoryAvailableCount.toFloat / inventoryRequestCount))
    println("Mediate Requested Count: " + mediationRequestsCount)
    println("Mediate Available Count: " + mediationAvailableCount)
    println("Mediate Fill Rate:       " + (mediationAvailableCount.toFloat / mediationRequestsCount))
    println("Ad Displayed Count:        " + adDisplayedCount)
    println("Ad Completed Count:        " + adCompletedCount)
    println("Average eCPM Value:        " + eCPMAverage)
    println("")
  }
}
