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
  val sharedSecret = "4d05fec7ba136b5f8c829e579ef12e"
  val sid = "1234567890"
  val oid = "0987654321"
  val hmac = "c2a195ab225144ad31a4dc3b36391da2"
  val invalidHmac = "xxxxx14e4b8d35f59bc43a7fdcf9ec0e3d54dxxxxx"

  running(FakeApplication(additionalConfiguration = testDB)) {
    val id = WaterfallAdProvider.create(waterfall.id, unityAdsID, None, None, configurable = true, active = true).get
    val currentWap = WaterfallAdProvider.find(id).get
    val configuration = JsObject(Seq("callbackParams" -> JsObject(Seq("APIKey" -> JsString(sharedSecret))),
      "requiredParams" -> JsObject(Seq("APIKey" -> JsString(sharedSecret))), "reportingParams" -> JsObject(Seq())))
    WaterfallAdProvider.update(new WaterfallAdProvider(
      currentWap.id, currentWap.waterfallID, currentWap.adProviderID, None, Some(eCPM), Some(true), None, configuration, false))
  }

  def goodCallback = running(FakeApplication(additionalConfiguration = testDB)) {
    new UnityAdsCallback(app1.token, sid, oid, hmac)
  }

  "unityAdsCompletionV1" should {
    "respond status 200 to good request" in new WithApplication(FakeApplication(additionalConfiguration = testDB)) {
      val token = App.findAll(waterfall.id).head.token
      val Some(result) = route(FakeRequest(GET, s"/v1/reward_callbacks/$token/unity_ads?sid=$sid&oid=$oid&hmac=$hmac"))

      status(result) must equalTo(OK)
      contentType(result) must beSome("text/plain")
      charset(result) must beSome("utf-8")
      contentAsString(result) must contain("1")
    }

    "respond status 400 to request with bad hmac signature" in new WithApplication(FakeApplication(additionalConfiguration = testDB)) {
      val token = App.findAll(waterfall.id).head.token
      val Some(result) = route(FakeRequest(GET, s"/v1/reward_callbacks/$token/unity_ads?sid=$sid&oid=$oid&hmac=$invalidHmac"))

      status(result) must equalTo(BAD_REQUEST)
      contentType(result) must beSome("text/plain")
      charset(result) must beSome("utf-8")
      contentAsString(result) must contain("1")
    }

    "respond status 400 to request with extra query params" in new WithApplication(FakeApplication(additionalConfiguration = testDB)) {
      val token = App.findAll(waterfall.id).head.token
      val Some(result) = route(FakeRequest(GET, s"/v1/reward_callbacks/$token/unity_ads?sid=$sid&oid=$oid&hmac=$hmac&dummy=123456"))

      status(result) must equalTo(BAD_REQUEST)
      contentType(result) must beSome("text/plain")
      charset(result) must beSome("utf-8")
      contentAsString(result) must contain("1")
    }

    "respond status 400 to request with bad url and good parameters" in new WithApplication(FakeApplication(additionalConfiguration = testDB)) {
      val Some(result) = route(FakeRequest(GET, s"/v1/reward_callbacks/123456/unity_ads?sid=$sid&oid=$oid&hmac=$hmac"))

      status(result) must equalTo(BAD_REQUEST)
      contentType(result) must beSome("text/plain")
      charset(result) must beSome("utf-8")
      contentAsString(result) must contain("1")
    }

    "respond correctly (status 400) to request missing query params" in new WithApplication(FakeApplication(additionalConfiguration = testDB)) {
      val token = App.findAll(waterfall.id).head.token
      val Some(resultOid) = route(FakeRequest(GET, s"/v1/reward_callbacks/$token/unity_ads?sid=123456789&hmac=12120989fs8dfwoej"))
      status(resultOid) must equalTo(BAD_REQUEST)
      contentType(resultOid) must beSome("text/plain")
      charset(resultOid) must beSome("utf-8")
      contentAsString(resultOid) must contain(Constants.UnityAdsVerifyFailure)

      val Some(resultHmac) = route(FakeRequest(GET, s"/v1/reward_callbacks/$token/unity_ads?sid=123456789&oid=aksdhksdjh345345"))
      status(resultHmac) must equalTo(BAD_REQUEST)
      contentType(resultHmac) must beSome("text/plain")
      charset(resultHmac) must beSome("utf-8")
      contentAsString(resultHmac) must contain(Constants.UnityAdsVerifyFailure)

      val Some(resultSid) = route(FakeRequest(GET, s"/v1/reward_callbacks/$token/unity_ads?oid=123456789&hmac=12120989fs8dfwoej"))
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
        new UnityAdsCallback(app1.token, sid, oid, hmac)
      }
      callback.currencyAmount must beEqualTo(2)
      callback.currencyAmount must not(beEqualTo(amount))
    }

    "be set to the rewardMinimum value when roundUp is true and the calculated amount is less than rewardMinimum" in new WithDB {
      val callback = {
        VirtualCurrency.update(new VirtualCurrency(
          virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate = 1, rewardMin = 5, rewardMax = None, roundUp = true))
        new UnityAdsCallback(app1.token, sid, oid, hmac)
      }
      callback.currencyAmount must beEqualTo(5)
    }

    "be set to 0 when roundUp is false and the calculated amount is less than the rewardMinimum" in new WithDB {
      val callback = {
        VirtualCurrency.update(new VirtualCurrency(
          virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate = 100, rewardMin = 5, rewardMax = None, roundUp = false))
        new UnityAdsCallback(app1.token, sid, oid, hmac)
      }
      callback.currencyAmount must beEqualTo(0)
    }

    "be set to the rewardMaximum value if rewardMaximum is not empty and the calculated amount is greater than the rewardMaximum" in new WithDB {
      val callbackWithoutRewardMax = {
        VirtualCurrency.update(new VirtualCurrency(
          virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate = 500, rewardMin = 1, rewardMax = None, roundUp = true))
        new UnityAdsCallback(app1.token, sid, oid, hmac)
      }
      callbackWithoutRewardMax.currencyAmount must beEqualTo(12)

      val callbackWithRewardMax = {
        VirtualCurrency.update(new VirtualCurrency(
          virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate = 500, rewardMin = 1, rewardMax = Some(2), roundUp = true))
        new UnityAdsCallback(app1.token, sid, oid, hmac)
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
      val newCallback = new UnityAdsCallback(app1.token, sid, oid, invalidHmac)
      newCallback.verificationInfo.isValid mustEqual false
    }
  }

  "receivedVerification" should {
    "be set when creating a new instance of the UnityAdsCallback class" in new WithDB {
      goodCallback.receivedVerification must beEqualTo(hmac)
    }
  }
}

