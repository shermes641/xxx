import io.keen.client.scala.Client

//val client = new Client(
//  projectId = sys.env("KEEN_PROJECT_ID"),
//  masterKey = sys.env.get("KEEN_MASTER_KEY"),
//  writeKey = sys.env.get("KEEN_WRITE_KEY"),
//  readKey = sys.env.get("KEEN_READ_KEY")
//)
//
//
//// Publish an event!
//client.addEvent(
//  collection = "collectionNameHere",
//  event = """{"foo": "bar"}"""
//)
//
//// Publish an event and care about the result!
//val resp = client.addEvent(
//  collection = "collectionNameHere",
//  event = """{"foo": "bar"}"""
//)
//
//// Publish lots of events
//client.addEvents(someEvents)
//
//// Add an onComplete callback for failures!
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