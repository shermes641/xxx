import io.keen.client.scala.Client
import play.api.libs.json.{JsNull,Json,JsString,JsValue}
import com.github.nscala_time.time._
import com.github.nscala_time.time.Imports._
import play.api._

// The "correct" way to start the app
new play.core.StaticApplication(new java.io.File("."))

val client = new Client(
  projectId = Play.current.configuration.getString("keen.project").get,
  masterKey = Play.current.configuration.getString("keen.masterKey"),
  writeKey = Play.current.configuration.getString("keen.writeKey"),
  readKey = Play.current.configuration.getString("keen.readKey")
)

var date = DateTime.now

var inventory_requests = Array.fill(4620) {
  date = date - 8.minute
  Json.obj(
    "distributor_id" -> 11,
    "distributor_name" -> "Test Company",
    "app_name" -> "The Video Game",
    "platform" -> "iOS",
    "app_id" -> 28,
    "ad_provider_id" -> 3,
    "ad_provider_name" -> "Vungle",
    "device_type" -> "iphone",
    "device_unique_id" -> "UUID",
    "ip_address" -> "${keen.ip}",
    "keen" -> Json.obj(
      "timestamp" -> date.toString()
    )
  )
}
date = DateTime.now

var inventory_available = Array.fill(3303) {
  date = date - 8.minute
  Json.obj(
    "distributor_name" -> "Test Company",
    "distributor_id" -> 11,
    "app_name" -> "The Video Game",
    "platform" -> "iOS",
    "app_id" -> 28,
    "ad_provider_id" -> 3,
    "ad_provider_name" -> "AppLovin",
    "device_type" -> "iphone",
    "device_unique_id" -> "UUID",
    "ip_address" -> "${keen.ip}",
    "keen" -> Json.obj(
      "timestamp" -> date.toString()
    )
  )
}
date = DateTime.now

var mediation_inventory_requests = Array.fill(4620) {
  date = date - 8.minute
  Json.obj(
    "distributor_id" -> 11,
    "distributor_name" -> "Test Company",
    "app_name" -> "The Video Game",
    "platform" -> "iOS",
    "app_id" -> 28,
    "device_type" -> "iphone",
    "device_unique_id" -> "UUID",
    "ip_address" -> "${keen.ip}",
    "keen" -> Json.obj(
      "timestamp" -> date.toString()
    )
  )
}
date = DateTime.now

var mediation_inventory_available = Array.fill(3303) {
  date = date - 8.minute
  Json.obj(
    "distributor_id" -> 11,
    "app_name" -> "The Video Game",
    "platform" -> "iOS",
    "app_id" -> 28,
    "device_type" -> "iphone",
    "device_unique_id" -> "UUID",
    "ip_address" -> "${keen.ip}",
    "ad_provider_id" -> 3,
    "ad_provider_name" -> "AppLovin",
    "keen" -> Json.obj(
      "timestamp" -> date.toString()
    )
  )
}
date = DateTime.now

var ad_displayed = Array.fill(3120) {
  date = date - 8.minute
  Json.obj(
    "distributor_name" -> "Test Company",
    "distributor_id" -> 11,
    "app_name" -> "The Video Game",
    "platform" -> "iOS",
    "app_id" -> 28,
    "ad_provider_name" -> "Testing",
    "ad_provider_id" -> 3,
    "device_type" -> "iphone",
    "device_unique_id" -> "UUID",
    "ip_address" -> "${keen.ip}",
    "keen" -> Json.obj(
      "timestamp" -> date.toString()
    )
  )
}
date = DateTime.now

var ad_completed = Array.fill(3000) {
  date = date - 9.minute
  Json.obj(
    "distributor_id" -> 11,
    "distributor_name" -> "Test Distributor",
    "app_name" -> "The Video Game",
    "platform" -> "iOS",
    "app_id" -> 28,
    "ad_provider_id" -> 3,
    "ad_provider_name" -> "HyprMarketplace",
    "device_type" -> "iphone",
    "device_unique_id" -> "UUID",
    "ip_address" -> "${keen.ip}",
    "virtual_currency_awarded" -> 1,
    "virtual_currency_reward_max" -> 100,
    "ad_provider_eCPM" -> 5,
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

val batch_request: JsValue = Json.obj(
  "availability_requested" -> inventory_requests,
  "availability_response_true" -> inventory_available
)

val batch_request_mediation: JsValue = Json.obj(
  "mediation_availability_requested" -> mediation_inventory_requests,
  "mediation_availability_response_true" -> mediation_inventory_available
)

val batch_request_ads: JsValue = Json.obj(
  "ad_displayed" -> ad_displayed,
  "ad_completed" -> ad_completed
)

// Publish lots of events
client.addEvents(Json.stringify(Json.toJson(batch_request)))
client.addEvents(Json.stringify(Json.toJson(batch_request_mediation)))
client.addEvents(Json.stringify(Json.toJson(batch_request_ads)))

