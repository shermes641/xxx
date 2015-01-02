package models

import org.junit.runner._
import org.specs2.mock.Mockito
import org.specs2.runner._
import play.api.libs.json.{JsNumber, JsValue}
import resources.WaterfallSpecSetup
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

@RunWith(classOf[JUnitRunner])
class CompletionSpec extends SpecificationWithFixtures with Mockito with WaterfallSpecSetup {
  "Completion.create" should {
    "create a new record in the database if the transaction ID is valid" in new WithDB {
      Completion.create("some app token", "some ad provider name", "some transaction ID", None) must not beNone
    }
  }

  "Completion.createWithNotification" should {
    "create a completion and alert the distributor if server to server calls are enabled" in new WithDB {
      val completionCount = tableCount("completions")
      App.update(new UpdatableApp(app1.id, true, app1.distributorID, app1.name, None, true))
      val completion = spy(new Completion)
      completion.postCallback(Some(any[String]), any[String], any[String], any[CallbackVerificationInfo]) returns Future { true }
      val callbackInfo = new CallbackVerificationInfo(true, "HyprMX", "Some transaction ID", app1.token, Some(1.0), 1)
      Await.result(completion.createWithNotification(callbackInfo), Duration(5000, "millis")) must beEqualTo(true)
      there was one(completion).postCallback(any[Option[String]], any[String], any[String], any[CallbackVerificationInfo])
      tableCount("completions") must beEqualTo(completionCount + 1)
    }

    "create a completion and not alert the distributor if server to server calls are not enabled" in new WithDB {
      val completionCount = tableCount("completions")
      App.update(new UpdatableApp(app1.id, true, app1.distributorID, app1.name, None, false))
      val completion = spy(new Completion)
      val callbackInfo = new CallbackVerificationInfo(true, "HyprMX", "Some transaction ID", app1.token, Some(1.0), 1)
      Await.result(completion.createWithNotification(callbackInfo), Duration(5000, "millis")) must beEqualTo(true)
      there was no(completion).postCallback(any[Option[String]], any[String], any[String], any[CallbackVerificationInfo])
      tableCount("completions") must beEqualTo(completionCount + 1)
    }
  }
}
