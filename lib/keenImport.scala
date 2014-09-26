import io.keen.client.scala.Client
import play.api.libs.json.{JsNull,Json,JsString,JsValue}
import com.github.nscala_time.time._
import com.github.nscala_time.time.Imports._
import play.api._

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
    "app_name" -> "The Video Game",
    "platform" -> "iOS",
    "app_id" -> 28,
    "ad_provider" -> 3,
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
    "distributor_id" -> 11,
    "app_name" -> "The Video Game",
    "platform" -> "iOS",
    "app_id" -> 28,
    "ad_provider" -> 3,
    "device_type" -> "iphone",
    "device_unique_id" -> "UUID",
    "ip_address" -> "${keen.ip}",
    "keen" -> Json.obj(
      "timestamp" -> date.toString()
    )
  )
}
date = DateTime.now

var ad_displayed = Array.fill(3120) {
  date = date - 8.minute
  Json.obj(
    "distributor_id" -> 11,
    "app_name" -> "The Video Game",
    "platform" -> "iOS",
    "app_id" -> 28,
    "ad_provider" -> 3,
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
    "app_name" -> "The Video Game",
    "platform" -> "iOS",
    "app_id" -> 28,
    "ad_provider" -> 3,
    "device_type" -> "iphone",
    "device_unique_id" -> "UUID",
    "ip_address" -> "${keen.ip}",
    "keen" -> Json.obj(
      "timestamp" -> date.toString()
    )
  )
}

val batch_request: JsValue = Json.obj(
  "inventory_request" -> inventory_requests,
  "inventory_available" -> inventory_available,
  "ad_displayed" -> ad_displayed,
  "ad_completed" -> ad_completed
)

println(batch_request)

// Publish lots of events
client.addEvents(Json.stringify(Json.toJson(batch_request)))

