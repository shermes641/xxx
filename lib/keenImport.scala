import com.github.nscala_time.time.Imports._
import io.keen.client.scala.Client
import models._
import org.joda.time.format.DateTimeFormat
import play.api._
import play.api.db.DB
import play.api.libs.json.{Json, JsValue}
import play.api.Play._
import scala.util.Random
// $COVERAGE-OFF$
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
    def createApp(appName: String, platform: Platform) = {
      val appID = App.create(distributorID, appName, platform.PlatformID).get
      val app = App.find(appID).get

      val wap = DB.withTransaction { implicit connection =>
        val waterfallID = Waterfall.create(appID, appName).get
        VirtualCurrency.createWithTransaction(appID, "Gold", 1, 10, None, Some(true))
        val adProviderID = platform.hyprMarketplaceID
        val hyprWaterfallAdProviderID = WaterfallAdProvider.createWithTransaction(
          waterfallID = waterfallID,
          adProviderID = adProviderID,
          waterfallOrder = Option(0),
          cpm = Option(20),
          configurable = false,
          active = false,
          pending = true
        ).get
        AppConfig.create(appID, app.token, 0)
        val hyprWAP = WaterfallAdProvider.findWithTransaction(hyprWaterfallAdProviderID).get
        WaterfallAdProviderWithAppData(
          id = hyprWAP.id,
          waterfallID = waterfallID,
          adProviderID = adProviderID,
          waterfallOrder = hyprWAP.waterfallOrder,
          cpm = hyprWAP.cpm,
          active = hyprWAP.active,
          fillRate = hyprWAP.fillRate,
          configurationData = hyprWAP.configurationData,
          reportingActive = hyprWAP.reportingActive,
          pending = hyprWAP.pending,
          appToken = app.token,
          appName = app.name,
          companyName = companyName
        )
      }
      updateHyprMarketplaceDistributorID(wap)

      (appID, appName, platform.PlatformID)
    }
  }

  class DailyEvent(appName: String, appID: Long, adProvider: AdProvider, startDate: DateTime) {
    def platform = {
      if(adProvider.platformID == Platform.Android.PlatformID) "Linux" else "iPhone OS"
    }

    var date = startDate
    val inventoryRequestMin = 30
    lazy val inventoryRequestCount = Random.nextInt(60) + inventoryRequestMin
    lazy val inventoryRequests = Array.fill(inventoryRequestCount) {
      date = date - 1.minute
      Json.obj(
        "distributor_id" -> distributorID,
        "distributor_name" -> companyName,
        "app_name" -> appName,
        "platform" -> platform,
        "app_id" -> appID,
        "ad_provider_id" -> adProvider.id,
        "ad_provider_name" -> adProvider.name,
        "device_type" -> "iphone",
        "device_unique_id" -> "UUID",
        "ip_address" -> "${keen.ip}",
        "keen" -> Json.obj(
          "timestamp" -> date.toString
        )
      )
    }
    date = startDate

    lazy val inventoryAvailableCount = Random.nextInt(inventoryRequestCount - (inventoryRequestMin - 20))
    lazy val inventoryAvailable = Array.fill(inventoryAvailableCount) {
      date = date - 1.minute
      Json.obj(
        "distributor_name" -> companyName,
        "distributor_id" -> distributorID,
        "app_name" -> appName,
        "platform" -> platform,
        "app_id" -> appID,
        "ad_provider_id" -> adProvider.id,
        "ad_provider_name" -> adProvider.name,
        "device_type" -> "iphone",
        "device_unique_id" -> "UUID",
        "ip_address" -> "${keen.ip}",
        "keen" -> Json.obj(
          "timestamp" -> date.toString
        )
      )
    }
    date = startDate

    val mediationRequestsMin = 50
    lazy val mediationRequestsCount = Random.nextInt(100) + mediationRequestsMin
    lazy val mediationInventoryRequests = Array.fill(mediationRequestsCount) {
      date = date - 1.minute
      Json.obj(
        "distributor_id" -> distributorID,
        "distributor_name" -> companyName,
        "app_name" -> appName,
        "platform" -> platform,
        "app_id" -> appID,
        "device_type" -> "iphone",
        "device_unique_id" -> "UUID",
        "ip_address" -> "${keen.ip}",
        "keen" -> Json.obj(
          "timestamp" -> date.toString
        )
      )
    }
    date = startDate

    lazy val mediationAvailableCount = Random.nextInt(mediationRequestsCount - (mediationRequestsMin - 40))
    lazy val mediationInventoryAvailable = Array.fill(mediationAvailableCount) {
      date = date - 1.minute
      Json.obj(
        "distributor_id" -> distributorID,
        "app_name" -> appName,
        "platform" -> platform,
        "app_id" -> appID,
        "device_type" -> "iphone",
        "device_unique_id" -> "UUID",
        "ip_address" -> "${keen.ip}",
        "ad_provider_id" -> adProvider.id,
        "ad_provider_name" -> adProvider.name,
        "keen" -> Json.obj(
          "timestamp" -> date.toString
        )
      )
    }
    date = startDate

    val adDisplayedMin = 15
    lazy val adDisplayedCount = Random.nextInt(25) + adDisplayedMin
    lazy val adDisplayed = Array.fill(adDisplayedCount) {
      date = date - 1.minute
      Json.obj(
        "distributor_name" -> companyName,
        "distributor_id" -> distributorID,
        "app_name" -> appName,
        "platform" -> platform,
        "app_id" -> appID,
        "ad_provider_id" -> adProvider.id,
        "ad_provider_name" -> adProvider.name,
        "device_type" -> "iphone",
        "device_unique_id" -> "UUID",
        "ip_address" -> "${keen.ip}",
        "keen" -> Json.obj(
          "timestamp" -> date.toString
        )
      )
    }
    date = startDate

    lazy val adStartedCount = adDisplayedCount
    lazy val adStarted = Array.fill(adDisplayedCount) {
      date = date - 1.minute
      Json.obj(
        "distributor_name" -> companyName,
        "distributor_id" -> distributorID,
        "app_name" -> appName,
        "platform" -> platform,
        "app_id" -> appID,
        "ad_provider_id" -> adProvider.id,
        "ad_provider_name" -> adProvider.name,
        "device_type" -> "iphone",
        "device_unique_id" -> "UUID",
        "ip_address" -> "${keen.ip}",
        "keen" -> Json.obj(
          "timestamp" -> date.toString
        )
      )
    }
    date = startDate

    lazy val adFinishedCount = Random.nextInt(adDisplayedMin) + adDisplayedCount
    lazy val adFinished = Array.fill(adFinishedCount) {
      date = date - 1.minute
      Json.obj(
        "distributor_name" -> companyName,
        "distributor_id" -> distributorID,
        "app_name" -> appName,
        "platform" -> platform,
        "app_id" -> appID,
        "ad_provider_id" -> adProvider.id,
        "ad_provider_name" -> adProvider.name,
        "device_type" -> "iphone",
        "device_unique_id" -> "UUID",
        "ip_address" -> "${keen.ip}",
        "keen" -> Json.obj(
          "timestamp" -> date.toString
        )
      )
    }
    date = startDate

    lazy val rewardDeliveredCount = Random.nextInt(adDisplayedCount - (adDisplayedMin - 10))
    var eCPMSum = 0
    lazy val eCPMValue = Random.nextInt(10) + 10
    lazy val rewardDelivered = Array.fill(rewardDeliveredCount) {
      date = date - 1.minute
      Json.obj(
        "distributor_id" -> distributorID,
        "distributor_name" -> companyName,
        "app_name" -> appName,
        "platform" -> platform,
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
          "timestamp" -> date.toString
        )
      )
    }

    lazy val batch_request: JsValue = Json.obj(
      "availability_requested" -> inventoryRequests,
      "availability_response_true" -> inventoryAvailable
    )

    lazy val batch_request_mediation: JsValue = Json.obj(
      "mediate_availability_requested" -> mediationInventoryRequests,
      "mediate_availability_response_true" -> mediationInventoryAvailable
    )

    lazy val batch_request_ads: JsValue = Json.obj(
      "ad_displayed" -> adDisplayed,
      "ad_started" -> adStarted,
      "reward_delivered" -> rewardDelivered,
      "ad_finished" -> adFinished
    )

    def publishToKeen() = {
      client.addEvents(Json.stringify(Json.toJson(batch_request)))
      client.addEvents(Json.stringify(Json.toJson(batch_request_mediation)))
      client.addEvents(Json.stringify(Json.toJson(batch_request_ads)))

      println("==================")
      println("Date:                      " + date.toString(DateTimeFormat.forPattern("MM-dd-yyyy")))
      println("Inventory Request Count:   " + inventoryRequestCount + " (Requests)")
      println("Inventory Available Count: " + inventoryAvailableCount)
      println("Fill Rate:                 " + (inventoryAvailableCount.toFloat / inventoryRequestCount) + " (Fill Rate)")
      println("Mediate Requested Count:   " + mediationRequestsCount + " (Requests, All Ad Providers)")
      println("Mediate Available Count:   " + mediationAvailableCount)
      println("Mediate Fill Rate:         " + (mediationAvailableCount.toFloat / mediationRequestsCount))
      println("Ad Displayed Count:        " + adDisplayedCount + " (Impressions)")
      println("Reward Delivered Count:    " + rewardDeliveredCount + " (Completions)")
      println("Average eCPM Value:        " + eCPMValue + " (eCPM)")
      println("")
    }
  }

  // Repeat first app to get multiple ad providers
  val apps = List(
    AppHelper.createApp("First App", Platform.Ios),
    AppHelper.createApp("Second App", Platform.Android),
    AppHelper.createApp("Third App", Platform.Ios),
    AppHelper.createApp("Fourth App", Platform.Android)
  )

  val keenCountURLRoot = "https://api.keen.io/3.0/projects/" + Play.current.configuration.getString("keen.project").get + "/queries/count?api_key=" + Play.current.configuration.getString("keen.readKey").get + "&filters=%5B%7B%22property_name%22%3A%22distributor_id%22%2C%22operator%22%3A%22in%22%2C%22property_value%22%3A%5B" + distributorID.toString + "%5D%7D%5D"
  val keenAverageURLRoot = "https://api.keen.io/3.0/projects/" + Play.current.configuration.getString("keen.project").get + "/queries/average?api_key=" + Play.current.configuration.getString("keen.readKey").get + "&filters=%5B%7B%22property_name%22%3A%22distributor_id%22%2C%22operator%22%3A%22in%22%2C%22property_value%22%3A%5B" + distributorID.toString + "%5D%7D%5D"

  println("TESTING URLS")
  println("============")

  println("\nCount of Inventory Requests (Requests)")
  println(keenCountURLRoot + "&event_collection=availability_requested\n")

  println("\nCount of Inventory Available")
  println(keenCountURLRoot + "&event_collection=availability_response_true\n")

  println("\nCount of Mediation Requested (Requests, All Ad Providers)")
  println(keenCountURLRoot + "&event_collection=mediate_availability_requested\n")

  println("\nCount of Mediation Available")
  println(keenCountURLRoot + "&event_collection=mediate_availability_response_true\n")

  println("\nCount of Ad Displayed (Impressions)")
  println(keenCountURLRoot + "&event_collection=ad_displayed\n")

  println("\nCount of Reward Delivered (Completions)")
  println(keenCountURLRoot + "&event_collection=reward_delivered\n")

  println("\neCPM Average (eCPM)")
  println(keenAverageURLRoot + "&event_collection=reward_delivered&target_property=ad_provider_eCPM\n")

  println("\nCount of Ad Started (Not shown in analytics dashboard)")
  println(keenCountURLRoot + "&event_collection=ad_started\n")

  println("\nCount of Ad Finished (Not shown in analytics dashboard)")
  println(keenCountURLRoot + "&event_collection=ad_finished\n")

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

  apps.foreach { app =>
    val (appID, appName, platformID) = app

    AdProvider.findAllByPlatform(platformID).foreach { adProvider =>
      println("")
      println("App Name:          " + appName.toUpperCase)
      println("Ad Provider:       " + adProvider.name.toUpperCase)
      println("Platform:          " + Platform.find(platformID).PlatformName)
      println("App ID:            " + appID)
      println("Ad Provider ID:    " + adProvider.id)
      println("")

      var events = List(
        new DailyEvent(appName, appID, adProvider, DateTime.now),
        new DailyEvent(appName, appID, adProvider, DateTime.now - 1.day),
        new DailyEvent(appName, appID, adProvider, DateTime.now - 2.day)
      )

      events.map(event => event.publishToKeen())

      println("")
      println(events.length + " day totals for " + appName.toUpperCase + " using Ad Provider " + adProvider.name.toUpperCase)
      printDateRangeTotals(appName, adProvider.name, events)
    }
  }

  def printDateRangeTotals(appName: String, adProviderName: String, appEvents: List[DailyEvent]) = {
    val totalInventoryRequestCount = appEvents.foldLeft(0)((total, event) => total + event.inventoryRequestCount)
    val totalInventoryAvailableCount = appEvents.foldLeft(0)((total, event) => total + event.inventoryAvailableCount)
    val totalMediationRequestsCount = appEvents.foldLeft(0)((total, event) => total + event.mediationRequestsCount)
    val totalMediationAvailableCount = appEvents.foldLeft(0)((total, event) => total + event.mediationAvailableCount)
    val totalAdDisplayedCount = appEvents.foldLeft(0)((total, event) => total + event.adDisplayedCount)
    val totalRewardDeliveredCount = appEvents.foldLeft(0)((total, event) => total + event.rewardDeliveredCount)
    val totalEcpmAverage = {
      val totalEcpmSum: Float = appEvents.foldLeft(0)((total, event) => total + event.rewardDeliveredCount * event.eCPMValue)
      totalEcpmSum / totalRewardDeliveredCount.toDouble
    }

    println("==================")
    println("Inventory Request Count:   " + totalInventoryRequestCount + " (Requests)")
    println("Inventory Available Count: " + totalInventoryAvailableCount)
    println("Fill Rate:                 " + (totalInventoryAvailableCount.toFloat / totalInventoryRequestCount) + " (Fill Rate)")
    println("Mediate Requested Count:   " + totalMediationRequestsCount + " (Requests, All Ad Providers)")
    println("Mediate Available Count:   " + totalMediationAvailableCount)
    println("Mediate Fill Rate:         " + (totalMediationAvailableCount.toFloat / totalMediationRequestsCount))
    println("Ad Displayed Count:        " + totalAdDisplayedCount + " (Impressions)")
    println("Reward Delivered Count:    " + totalRewardDeliveredCount + " (Completions)")
    println("Average eCPM Value:        " + totalEcpmAverage + " (eCPM)")
    println("")
  }
}
// $COVERAGE-ON$