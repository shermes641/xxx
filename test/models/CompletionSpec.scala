package models

import models.WaterfallAdProvider.AdProviderRewardInfo
import org.junit.runner._
import org.specs2.mock.Mockito
import org.specs2.runner._
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import resources.WaterfallSpecSetup
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

@RunWith(classOf[JUnitRunner])
class CompletionSpec extends SpecificationWithFixtures with Mockito with WaterfallSpecSetup {
  "create" should {
    "create a new record in the database if the transaction ID is valid" in new WithDB {
      val completion = new Completion
      completion.create("some app token", "some ad provider name", "some transaction ID", offerProfit=Some(0.5), rewardQuantity=1, generationNumber=Some(1), JsObject(Seq()).as[JsValue]).must(not).beNone
    }
  }

  "createWithNotification" should {
    "create a completion and alert the distributor if server to server calls are enabled" in new WithDB {
      val completionCount = tableCount("completions")
      App.update(new UpdatableApp(app1.id, true, app1.distributorID, app1.name, None, true))
      val completion = spy(new Completion)
      completion.postCallback(Some(any[String]), any[JsValue], any[CallbackVerificationInfo]) returns Future { true }
      val rewardInfo = new AdProviderRewardInfo(JsObject(Seq()), cpm=Some(20.0), exchangeRate=100, rewardMin=1, rewardMax=Some(10), roundUp=true, callbackURL=Some("http://somecallbackurl.com"), serverToServerEnabled=true, generationNumber=1)
      val callbackInfo = new CallbackVerificationInfo(true, "HyprMX", "Some transaction ID", app1.token, offerProfit=Some(1.0), rewardQuantity=1, Some(rewardInfo))
      Await.result(completion.createWithNotification(callbackInfo, JsObject(Seq())), Duration(5000, "millis")) must beEqualTo(true)
      there was one(completion).postCallback(any[Option[String]], any[JsValue], any[CallbackVerificationInfo])
      tableCount("completions") must beEqualTo(completionCount + 1)
    }

    "create a completion and not alert the distributor if server to server calls are not enabled" in new WithDB {
      val completionCount = tableCount("completions")
      App.update(new UpdatableApp(app1.id, true, app1.distributorID, app1.name, None, false))
      val completion = spy(new Completion)
      val rewardInfo = new AdProviderRewardInfo(JsObject(Seq()), cpm=Some(20.0), exchangeRate=100, rewardMin=1, rewardMax=Some(10), roundUp=true, callbackURL=None, serverToServerEnabled=false, generationNumber=1)
      val callbackInfo = new CallbackVerificationInfo(true, "HyprMX", "Some transaction ID", app1.token, offerProfit=Some(1.0), rewardQuantity=1, Some(rewardInfo))
      Await.result(completion.createWithNotification(callbackInfo, JsObject(Seq())), Duration(5000, "millis")) must beEqualTo(true)
      there was no(completion).postCallback(any[Option[String]], any[JsValue], any[CallbackVerificationInfo])
      tableCount("completions") must beEqualTo(completionCount + 1)
    }
  }

  "postCallback" should {
    val rewardInfo = new AdProviderRewardInfo(JsObject(Seq()), cpm=Some(20.0), exchangeRate=100, rewardMin=1, rewardMax=Some(10), roundUp=true, callbackURL=None, serverToServerEnabled=false, generationNumber=1)
    val verification = spy(new CallbackVerificationInfo(true, "ad provider name", "transaction ID", "app token", offerProfit=None, rewardQuantity=1, Some(rewardInfo)))
    val callbackURL = Some("http://someurl.com")
    val data = (new Completion).postbackData(JsNull, verification)
    val response = mock[WSResponse]
    response.body returns ""
    val completion = spy(new Completion)

    "not POST to a callback URL if one does not exist" in new WithDB {
      val callbackURL = None
      val completion = new Completion
      Await.result(completion.postCallback(callbackURL, JsObject(Seq()), verification), Duration(5000, "millis")) must beFalse
    }

    "return true if the Distributor's servers respond with a status code of 200" in new WithDB {
      response.status returns 200
      completion.sendPost(callbackURL.get, data) returns Future { response }
      Await.result(completion.postCallback(callbackURL, JsNull, verification), Duration(5000, "millis")) must beTrue
    }

    "return false if the Distributor's servers respond with a status code other than 200" in new WithDB {
      response.status returns 400
      completion.sendPost(callbackURL.get, data) returns Future { response }
      Await.result(completion.postCallback(callbackURL, JsNull, verification), Duration(5000, "millis")) must beFalse
    }
  }

  "postbackData" should {
    "include all necessary params from our server to server callback documentation" in new WithDB {
      val rewardInfo = Some(mock[AdProviderRewardInfo])
      val verification = spy(new CallbackVerificationInfo(true, "ad provider name", "transaction ID", "app token", offerProfit=Some(0.5), rewardQuantity=1, rewardInfo))
      val adProviderRequest = Json.obj()
      val postbackData = (new Completion).postbackData(adProviderRequest, verification)

      (postbackData \ "original_postback") must beEqualTo(adProviderRequest)
      (postbackData \ "ad_provider").as[String] must beEqualTo(verification.adProviderName)
      (postbackData \ "reward_quantity").as[Long] must beEqualTo(verification.rewardQuantity)
      (postbackData \ "estimated_offer_profit").as[Double] must beEqualTo(verification.offerProfit.get)
      (postbackData \ "transaction_id").as[String] must beEqualTo(verification.transactionID)
    }
  }
}
