package models

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.libs.json.{JsObject, JsString}
import play.api.test.Helpers._
import play.api.test._
import resources._

@RunWith(classOf[JUnitRunner])
class UnityAdsCallbackSpec extends SpecificationWithFixtures with AdProviderSpecSetup with WaterfallSpecSetup {
  val eCPM = 25.0
  val amount = 1
  val sharedSecret = "ccfbde79b23f0fde7867cb9177b1a15"
  val sid = "jcaintic@jungroup.com"
  val oid = "546553466"
  val hmac = "ec61dfb3f7355aea49a1a81540073f48"
  val productID = "1061310"
  val invalidHmac = "xxxxxec61dfb3f7355aea49a1a81540073f48xxxxx"

  running(FakeApplication(additionalConfiguration = testDB)) {
    val id = WaterfallAdProvider.create(waterfall.id, unityAdsID, None, None, configurable = true, active = true).get
    val currentWap = WaterfallAdProvider.find(id).get
    val configuration = JsObject(Seq("callbackParams" -> JsObject(Seq("APIKey" -> JsString(sharedSecret))),
      "requiredParams" -> JsObject(Seq("APIKey" -> JsString(productID))), "reportingParams" -> JsObject(Seq())))
    WaterfallAdProvider.update(new WaterfallAdProvider(
      currentWap.id, currentWap.waterfallID, currentWap.adProviderID, None, Some(eCPM), Some(true), None, configuration, false))
  }

  def goodCallback = running(FakeApplication(additionalConfiguration = testDB)) {
    new UnityAdsCallback(app1.token, sid, oid, hmac, productID)
  }

  "unityAdsCompletionV1" should {
    "respond status 200 to good request" in new WithApplication(FakeApplication(additionalConfiguration = testDB)) {
      val token = App.findAll(waterfall.id).head.token
      val Some(result) = route(FakeRequest(GET, s"/v1/reward_callbacks/$token/unity_ads?productid=$productID&sid=$sid&oid=$oid&hmac=$hmac"))

      status(result) must equalTo(OK)
      contentType(result) must beSome("text/plain")
      charset(result) must beSome("utf-8")
      contentAsString(result) must contain("1")
    }

    "respond status 400 to request with bad hmac signature" in new WithApplication(FakeApplication(additionalConfiguration = testDB)) {
      val token = App.findAll(waterfall.id).head.token
      val Some(result) = route(FakeRequest(GET, s"/v1/reward_callbacks/$token/unity_ads?productid=$productID&sid=$sid&oid=$oid&hmac=$invalidHmac"))

      status(result) must equalTo(BAD_REQUEST)
      contentType(result) must beSome("text/plain")
      charset(result) must beSome("utf-8")
      contentAsString(result) must contain("1")
    }

    "respond status 200 to request with extra query params" in new WithApplication(FakeApplication(additionalConfiguration = testDB)) {
      val token = App.findAll(waterfall.id).head.token
      val Some(result) = route(FakeRequest(GET, s"/v1/reward_callbacks/$token/unity_ads?productid=$productID&sid=$sid&oid=$oid&hmac=$hmac&dummy=123456"))

      status(result) must equalTo(OK)
      contentType(result) must beSome("text/plain")
      charset(result) must beSome("utf-8")
      contentAsString(result) must contain("1")
    }

    "respond status 400 to request with bad url and good parameters" in new WithApplication(FakeApplication(additionalConfiguration = testDB)) {
      val Some(result) = route(FakeRequest(GET, s"/v1/reward_callbacks/123456/unity_ads?productid=$productID&sid=$sid&oid=$oid&hmac=$hmac"))

      status(result) must equalTo(BAD_REQUEST)
      contentType(result) must beSome("text/plain")
      charset(result) must beSome("utf-8")
      contentAsString(result) must contain("1")
    }

    "respond correctly (status 400) to request missing query params" in new WithApplication(FakeApplication(additionalConfiguration = testDB)) {
      val token = App.findAll(waterfall.id).head.token
      val Some(resultOid) = route(FakeRequest(GET, s"/v1/reward_callbacks/$token/unity_ads?productid=$productID&sid=123456789&hmac=12120989fs8dfwoej"))
      status(resultOid) must equalTo(BAD_REQUEST)
      contentType(resultOid) must beSome("text/plain")
      charset(resultOid) must beSome("utf-8")
      contentAsString(resultOid) must contain(Constants.UnityAdsVerifyFailure)

      val Some(resultHmac) = route(FakeRequest(GET, s"/v1/reward_callbacks/$token/unity_ads?productid=$productID&sid=123456789&oid=aksdhksdjh345345"))
      status(resultHmac) must equalTo(BAD_REQUEST)
      contentType(resultHmac) must beSome("text/plain")
      charset(resultHmac) must beSome("utf-8")
      contentAsString(resultHmac) must contain(Constants.UnityAdsVerifyFailure)

      val Some(resultSid) = route(FakeRequest(GET, s"/v1/reward_callbacks/$token/unity_ads?productid=$productID&oid=123456789&hmac=12120989fs8dfwoej"))
      status(resultSid) must equalTo(BAD_REQUEST)
      contentType(resultSid) must beSome("text/plain")
      charset(resultSid) must beSome("utf-8")
      contentAsString(resultSid) must contain(Constants.UnityAdsVerifyFailure)

      val Some(resultNoParams) = route(FakeRequest(GET, s"/v1/reward_callbacks/$token/unity_ads"))
      status(resultNoParams) must equalTo(BAD_REQUEST)
      contentType(resultNoParams) must beSome("text/plain")
      charset(resultNoParams) must beSome("utf-8")
      contentAsString(resultNoParams) must contain(Constants.UnityAdsVerifyFailure)
    }
  }

  "adProviderName" should {
    "be set when creating a new instance of the UnityAdsCallback class" in new WithDB {
      goodCallback.adProviderName must beEqualTo(Constants.UnityAdsName)
    }
  }

  "token" should {
    "be set when creating a new instance of the UnityAdsCallback class" in new WithDB {
      goodCallback.token must beEqualTo(app1.token)
    }
  }

  "currencyAmount" should {
    "ignore the reward amount passed in the server to server callback" in new WithDB {
      val callback = {
        VirtualCurrency.update(new VirtualCurrency(
          virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate = 100, rewardMin = 1, rewardMax = None, roundUp = true))
        new UnityAdsCallback(app1.token, sid, oid, hmac, productID)
      }
      callback.currencyAmount must beEqualTo(2)
      callback.currencyAmount must not(beEqualTo(amount))
    }

    "be set to the rewardMinimum value when roundUp is true and the calculated amount is less than rewardMinimum" in new WithDB {
      val callback = {
        VirtualCurrency.update(new VirtualCurrency(
          virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate = 1, rewardMin = 5, rewardMax = None, roundUp = true))
        new UnityAdsCallback(app1.token, sid, oid, hmac, productID)
      }
      callback.currencyAmount must beEqualTo(5)
    }

    "be set to 0 when roundUp is false and the calculated amount is less than the rewardMinimum" in new WithDB {
      val callback = {
        VirtualCurrency.update(new VirtualCurrency(
          virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate = 100, rewardMin = 5, rewardMax = None, roundUp = false))
        new UnityAdsCallback(app1.token, sid, oid, hmac, productID)
      }
      callback.currencyAmount must beEqualTo(0)
    }

    "be set to the rewardMaximum value if rewardMaximum is not empty and the calculated amount is greater than the rewardMaximum" in new WithDB {
      val callbackWithoutRewardMax = {
        VirtualCurrency.update(new VirtualCurrency(
          virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate = 500, rewardMin = 1, rewardMax = None, roundUp = true))
        new UnityAdsCallback(app1.token, sid, oid, hmac, productID)
      }
      callbackWithoutRewardMax.currencyAmount must beEqualTo(12)

      val callbackWithRewardMax = {
        VirtualCurrency.update(new VirtualCurrency(
          virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate = 500, rewardMin = 1, rewardMax = Some(2), roundUp = true))
        new UnityAdsCallback(app1.token, sid, oid, hmac, productID)
      }
      callbackWithRewardMax.currencyAmount must beEqualTo(2)
    }
  }

  "receivedVerification" should {
    "be set when creating a new instance of the UnityAdsCallback class" in new WithDB {
      goodCallback.receivedVerification must beEqualTo(hmac)
    }
  }

  "verificationInfo" should {
    "return an instance of the CallbackVerificationInfo class" in new WithDB {
      goodCallback.verificationInfo must haveClass[CallbackVerificationInfo]
    }

    "be valid when the generated verification matches the received verification string" in new WithDB {
      goodCallback.verificationInfo.isValid mustEqual true
    }

    "not be valid when the generated verification does not match the received hmac query param" in new WithDB {
      val newCallback = new UnityAdsCallback(app1.token, sid, oid, invalidHmac, productID)
      newCallback.verificationInfo.isValid mustEqual false
    }

    "set the ad provider name correctly" in new WithDB {
      goodCallback.verificationInfo.adProviderName must beEqualTo(Constants.UnityAdsName)
    }

    "set the app token correctly" in new WithDB {
      goodCallback.verificationInfo.appToken must beEqualTo(app1.token)
    }

    "set the transaction ID correctly" in new WithDB {
      goodCallback.verificationInfo.transactionID must beEqualTo(oid)
    }

    "set the offer profit correctly" in new WithDB {
      goodCallback.verificationInfo.offerProfit must beEqualTo(goodCallback.payout)
    }

    "set the reward quantity correctly" in new WithDB {
      goodCallback.verificationInfo.rewardQuantity must beEqualTo(goodCallback.currencyAmount)
    }

    "set the reward info correctly" in new WithDB {
      goodCallback.verificationInfo.adProviderRewardInfo must beEqualTo(goodCallback.adProviderRewardInfo)
    }
  }

  "receivedVerification" should {
    "be set when creating a new instance of the UnityAdsCallback class" in new WithDB {
      goodCallback.receivedVerification must beEqualTo(hmac)
    }
  }
}
