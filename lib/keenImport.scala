import io.keen.client.scala.Client
import play.api.libs.json.{JsNull,Json,JsString,JsValue}

val client = new Client(
  projectId = "53f75d42709a3952e3000002",
  masterKey = Option("69FD3FC4F9C6366602DE0E230331D3B8"),
  writeKey = Option("5594a568ccf77b8c4d389a60daf4e99b9a65b2e39b9a5bc9faeda098bc410121cc3dafe5b957bdd9f58dcfbb41d0d27908507c7fa6dc705399fec29e4c867b52df2666c34c0558e3d52806d0e14e81d7bee9e8d43f571f0c1452ca05ccf66f54a78d06764d3cba08e6cbfd2e897ec20c"),
  readKey = Option("38e91b786e4c8150f22eac2368b038bc50d7e2a6904e97578a32e11d08a89b1ec1192272df9d9b7ca2586d5852e059f5604c702ded6d914ba68f14e8049d6023b076555e23500a8baf660c503b038a0a3fc9050872441938525c888a65cb49b85186e1b060fa5ceb8256351ef22c0902")
)


var inventory_requests = Array.fill(100) {
  Json.obj(
    "distributor_id" -> 1,
    "app_name" -> "The Video Game",
    "platform" -> "iOS",
    "app_id" -> 105,
    "device_type" -> "iphone",
    "device_unique_id" -> "UUID",
    "ip_address" -> "${keen.ip}",
    "keen" -> Json.obj(
      "timestamp" -> "2014-02-21T15:29:30.438Z"
    )
  )
}


var inventory_available = Array.fill(100) {
  Json.obj(
    "distributor_id" -> 1,
    "app_name" -> "The Video Game",
    "platform" -> "iOS",
    "app_id" -> 105,
    "device_type" -> "iphone",
    "device_unique_id" -> "UUID",
    "ip_address" -> "${keen.ip}",
    "keen" -> Json.obj(
      "timestamp" -> "2014-02-21T15:29:30.438Z"
    )
  )
}

val batch_request: JsValue = Json.obj(
  "inventory_request" -> inventory_requests,
  "inventory_available" -> inventory_available,
  "ad_displayed" -> inventory_available,
  "ad_completed" -> inventory_availablesc
)

println(batch_request)

// Publish lots of events
client.addEvents(Json.stringify(Json.toJson(batch_request)))

// Add an onComplete callback for failures!
//resp onComplete {
//  case Success(r) => println(resp.statusCode)
//  case Failure(t) => println(t.getMessage) // A Throwable
//}
//
//// Or use a map
//resp map {
//  println("I succeeded!")
//} getOrElse {
//  println("I failed :(")
//}

