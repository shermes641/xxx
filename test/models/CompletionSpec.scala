package models

import hmac.{HmacConstants, HmacHashData}
import models.WaterfallAdProvider.AdProviderRewardInfo
import org.specs2.mock.Mockito
import play.api.libs.json._
import resources.{AdProviderRequests, SpecificationWithFixtures, WaterfallSpecSetup}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class CompletionSpec extends SpecificationWithFixtures with Mockito with WaterfallSpecSetup with AdProviderRequests {
  val adProviderUserID = "user-id"
  "createWithNotification" should {
    "create a completion and alert the distributor if server to server calls are enabled" in new WithDB {
      val completionCount = tableCount("completions")
      App.update(new UpdatableApp(app1.id, true, app1.distributorID, app1.name, None, true))
      val completion = spy(new Completion)
      completion.postCallback(Some(any[String]), any[JsValue], any[CallbackVerificationInfo], any[String], any[String]) returns Future {
        true
      }
      val rewardInfo = new AdProviderRewardInfo(JsObject(Seq()), cpm = Some(20.0), exchangeRate = 100, rewardMin = 1, rewardMax = Some(10), roundUp = true, callbackURL = Some("http://somecallbackurl.com"), serverToServerEnabled = true, generationNumber = 1)
      val callbackInfo = new CallbackVerificationInfo(true, "HyprMX", "Some transaction ID", app1.token, offerProfit = Some(1.0), rewardQuantity = 1, Some(rewardInfo))
      Await.result(completion.createWithNotification(callbackInfo, JsObject(Seq()), adProviderUserID = adProviderUserID), Duration(5000, "millis")) must beEqualTo(true)
      there was one(completion).postCallback(any[Option[String]], any[JsValue], any[CallbackVerificationInfo], any[String], any[String])
      tableCount("completions") must beEqualTo(completionCount + 1)

      Await.result(
        completion.createWithNotification(callbackInfo,
          JsObject(Seq()),
          adProviderUserID = adProviderUserID,
          sharedSecretKey = Some(HmacConstants.DefaultSecret)),
        Duration(5000, "millis")) must beEqualTo(true)
      there was two(completion).postCallback(any[Option[String]], any[JsValue], any[CallbackVerificationInfo], any[String], any[String])
      tableCount("completions") must beEqualTo(completionCount + 2)
    }

    "create a completion and not alert the distributor if server to server calls are not enabled" in new WithDB {
      val completionCount = tableCount("completions")
      App.update(new UpdatableApp(app1.id, true, app1.distributorID, app1.name, None, false))
      val completion = spy(new Completion)
      val rewardInfo = new AdProviderRewardInfo(JsObject(Seq()), cpm = Some(20.0), exchangeRate = 100, rewardMin = 1, rewardMax = Some(10), roundUp = true, callbackURL = None, serverToServerEnabled = false, generationNumber = 1)
      val callbackInfo = new CallbackVerificationInfo(true, "HyprMX", "Some transaction ID", app1.token, offerProfit = Some(1.0), rewardQuantity = 1, Some(rewardInfo))
      Await.result(completion.createWithNotification(callbackInfo, JsObject(Seq()), adProviderUserID = adProviderUserID), Duration(5000, "millis")) must beEqualTo(true)
      there was no(completion).postCallback(any[Option[String]], any[JsValue], any[CallbackVerificationInfo], any[String], any[String])
      tableCount("completions") must beEqualTo(completionCount + 1)

      Await.result(completion.createWithNotification(callbackInfo,
        JsObject(Seq()),
        adProviderUserID = adProviderUserID,
        sharedSecretKey = Some(HmacConstants.DefaultSecret)),
        Duration(5000, "millis")) must beEqualTo(true)
      there was no(completion).postCallback(any[Option[String]], any[JsValue], any[CallbackVerificationInfo], any[String], any[String])
      tableCount("completions") must beEqualTo(completionCount + 2)
    }

    "create a completion and not alert the distributor if server to server calls are enabled and url is bad" in new WithDB {
      val completionCount = tableCount("completions")
      App.update(new UpdatableApp(app1.id, true, app1.distributorID, app1.name, None, false))
      val completion = spy(new Completion)
      val rewardInfo = new AdProviderRewardInfo(JsObject(Seq()), cpm = Some(20.0), exchangeRate = 100, rewardMin = 1, rewardMax = Some(10), roundUp = true, callbackURL = None, serverToServerEnabled = true, generationNumber = 1)
      val callbackInfo = new CallbackVerificationInfo(true, "HyprMX", "Some transaction ID", app1.token, offerProfit = Some(1.0), rewardQuantity = 1, Some(rewardInfo))
      Await.result(completion.createWithNotification(callbackInfo, JsObject(Seq()), adProviderUserID = adProviderUserID), Duration(5000, "millis")) must beEqualTo(false)
      there was no(completion).postCallback(any[Option[String]], any[JsValue], any[CallbackVerificationInfo], any[String], any[String])
      tableCount("completions") must beEqualTo(completionCount + 1)
    }
  }

  "postCallback" should {
    val rewardInfo = new AdProviderRewardInfo(JsObject(Seq()), cpm = Some(20.0), exchangeRate = 100, rewardMin = 1, rewardMax = Some(10), roundUp = true, callbackURL = None, serverToServerEnabled = false, generationNumber = 1)
    val verification = new CallbackVerificationInfo(true, "ad provider name", "transaction ID", "app token", offerProfit = None, rewardQuantity = 1, Some(rewardInfo))
    //These urls return the status code indicated --- if this service goes away the tests using the urls will fail
    val callbackURL400 = Some("http://httpstat.us/400")
    val callbackURL500 = Some("http://httpstat.us/500")
    val callbackURL200 = Some("http://httpstat.us/200")
    val callbackURLNone = None
    val nonExistentCallbackURL = Some("http://your-reward-callback-goes-here.com")
    val callbackURLNoVerb = Some("httpstat.us")

    val comp = new Completion
    val data = HmacHashData(Json.obj(
      "method" -> "GET",
      "path" -> "/v1/reward_callbacks/1e15566a-4859-4c89-9f1d-7a9576a2e3d3/hyprmarketplace",
      "query" -> Json.obj(
        "quantity" -> "1",
        "offer_profit" -> "0.01",
        "sig" -> "0b89f364ee22f8cee55e5fdc4b9951397fbc1571b20c8606d6fe6f58415f670d",
        "reward_id" -> "0",
        "sub_id" -> "",
        "uid" -> "TestUser",
        "time" -> "1427143627")), verification, adProviderUserID).postBackData

    "not POST to a callback URL if one does not exist" in new WithDB {
      Await.result(comp.postCallback(callbackURLNone, JsObject(Seq()), verification, adProviderUserID, HmacConstants.DefaultSecret), Duration(5000, "millis")) must beFalse
      Await.result(comp.postCallback(callbackURLNone, JsObject(Seq()), verification, adProviderUserID, ""), Duration(5000, "millis")) must beFalse
    }

    "not POST to a callback URL that is invalid" in new WithDB {
      Await.result(comp.postCallback(callbackURLNoVerb, JsObject(Seq()), verification, adProviderUserID, HmacConstants.DefaultSecret), Duration(5000, "millis")) must beFalse
      Await.result(comp.postCallback(callbackURLNoVerb, JsObject(Seq()), verification, adProviderUserID, ""), Duration(5000, "millis")) must beFalse
    }

    "return false if the hostname of the callback URL is nonexistent" in new WithDB {
      Await.result(comp.postCallback(nonExistentCallbackURL, JsNull, verification, adProviderUserID, HmacConstants.DefaultSecret), Duration(5000, "millis")) must beFalse
      Await.result(comp.postCallback(nonExistentCallbackURL, JsObject(Seq()), verification, adProviderUserID, ""), Duration(5000, "millis")) must beFalse
    }

    "return true if the Distributor's servers respond with a status code of 200" in new WithDB {
      Await.result(comp.postCallback(callbackURL200, data, verification, adProviderUserID, HmacConstants.DefaultSecret), Duration(5000, "millis")) must beTrue
      Await.result(comp.postCallback(callbackURL200, JsObject(Seq()), verification, adProviderUserID, ""), Duration(5000, "millis")) must beTrue
    }

    "return false if the Distributor's servers respond with a status code other than 200" in new WithDB {
      Await.result(comp.postCallback(callbackURL400, data, verification, adProviderUserID, HmacConstants.DefaultSecret), Duration(5000, "millis")) must beFalse
      Await.result(comp.postCallback(callbackURL500, data, verification, adProviderUserID, HmacConstants.DefaultSecret), Duration(5000, "millis")) must beFalse
      Await.result(comp.postCallback(callbackURL400, data, verification, adProviderUserID, ""), Duration(5000, "millis")) must beFalse
      Await.result(comp.postCallback(callbackURL500, data, verification, adProviderUserID, ""), Duration(5000, "millis")) must beFalse
    }
  }

  "postbackData" should {
    "include all necessary params from our server to server callback documentation" in new WithDB {
      val rewardInfo = Some(mock[AdProviderRewardInfo])
      val verification = spy(new CallbackVerificationInfo(true, "ad provider name", "transaction ID", "app token", offerProfit = Some(0.5), rewardQuantity = 1, rewardInfo))
      val postbackData = HmacHashData(hyprRequest, verification, adProviderUserID).postBackData

      (postbackData \ hmac.HmacConstants.AdProviderRequest) must beEqualTo(hyprRequest)
      (postbackData \ HmacConstants.AdProviderName).as[String] must beEqualTo(verification.adProviderName)
      (postbackData \ HmacConstants.RewardQuantity).as[Long] must beEqualTo(verification.rewardQuantity)
      (postbackData \ HmacConstants.OfferProfit).as[Double] must beEqualTo(verification.offerProfit.get)
      (postbackData \ HmacConstants.TransactionID).as[String] must beEqualTo(verification.transactionID)
      (postbackData \ HmacConstants.AdProviderUser).as[String] must beEqualTo(adProviderUserID)
    }
  }
}
