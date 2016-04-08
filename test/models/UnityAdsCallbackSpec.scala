package models

import play.api.libs.json.{JsObject, JsString}
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test._
import resources._
import scala.concurrent.Future

class UnityAdsCallbackSpec extends SpecificationWithFixtures with AdProviderSpecSetup {
  override lazy val adProvider = adProviderService
  val eCPM = 25.0
  val amount = 1
  val sharedSecret = "ccfbde79b23f0fde7867cb9177b1a15"
  val sid = "jcaintic@jungroup.com"
  val oid = "546553466"
  val hmac = "ec61dfb3f7355aea49a1a81540073f48"
  val productID = "1061310"
  val invalidHmac = "xxxxxec61dfb3f7355aea49a1a81540073f48xxxxx"

  running(testApplication) {
    val id = waterfallAdProviderService.create(waterfall.id, unityAdsID, None, None, configurable = true, active = true).get
    val currentWap = waterfallAdProviderService.find(id).get
    val configuration = JsObject(Seq("callbackParams" -> JsObject(Seq("APIKey" -> JsString(sharedSecret))),
      "requiredParams" -> JsObject(Seq(Constants.UnityAds.GameID -> JsString(productID))), "reportingParams" -> JsObject(Seq())))
    waterfallAdProviderService.update(new WaterfallAdProvider(
      currentWap.id, currentWap.waterfallID, currentWap.adProviderID, None, Some(eCPM), Some(true), None, configuration, false))
  }

  /**
   * Helper function to generate fake Unity Ads callback request
   */
  def generateRequest(token: String, sid: String, oid: String, hmac: String, productID: String) = {
    FakeRequest(GET, s"/v1/reward_callbacks/$token/unity_ads?productid=$productID&sid=$sid&oid=$oid&hmac=$hmac")
  }

  /**
   * Example of a successful Unity Ads callback
   */
  def goodCallback = running(FakeApplication(additionalConfiguration = testDB)) {
    val request = generateRequest(app1.token, sid, oid, hmac, productID)
    new UnityAdsCallback(app1.token, request.queryString, waterfallAdProviderService)
  }

  // Helper class for testing successful/unsuccessful callback responses
  case class Response(code: Int, message: String)
  val successfulResponse = Response(OK, Constants.UnityAds.Success)
  val failedResponse = Response(BAD_REQUEST, Constants.UnityAds.VerifyFailure)

  /**
   * Helper function to verify several characteristics of a successful/unsuccessful request
    *
    * @param request The incoming request from Unity Ads
   * @param response Our successful or unsuccessful response to the callback
   */
  def verifyRequest(request: Future[Result], response: Response) = {
    status(request) must equalTo(response.code)
    contentType(request) must beSome("text/plain")
    charset(request) must beSome("utf-8")
    contentAsString(request) must contain(response.message)
  }

  "unityAdsCompletionV1" should {
    "respond status 200 to valid request" in new WithApplication(FakeApplication(additionalConfiguration = testDB)) {
      val token = appService.findAll(waterfall.id).head.token
      val request = generateRequest(token, sid, oid, hmac, productID)
      val Some(result) = route(request)
      verifyRequest(result, successfulResponse)
    }

    "respond status 200 to a valid request with a duplicate productid param" in new WithApplication(FakeApplication(additionalConfiguration = testDB)) {
      val token = appService.findAll(waterfall.id).head.token
      val Some(result) = route(FakeRequest(GET, s"/v1/reward_callbacks/$token/unity_ads?productid=$productID&sid=$sid&oid=$oid&hmac=$hmac&productid=$productID"))
      verifyRequest(result, successfulResponse)
    }

    "respond status 200 to a valid request that does not contain a param that is not used in the hmac signature" in new WithApplication(FakeApplication(additionalConfiguration = testDB)) {
      val sid = "testuser1"
      val oid = "562832418"
      val hmac = "7338989ca66614440f5a92788e15ae55"
      val token = appService.findAll(waterfall.id).head.token
      val Some(result) = route(FakeRequest(GET, s"/v1/reward_callbacks/$token/unity_ads?&sid=$sid&oid=$oid&hmac=$hmac"))
      verifyRequest(result, successfulResponse)
    }

    "respond status 400 to request with bad hmac signature" in new WithApplication(FakeApplication(additionalConfiguration = testDB)) {
      val token = appService.findAll(waterfall.id).head.token
      val request = generateRequest(token, sid, oid, invalidHmac, productID)
      val Some(result) = route(request)
      verifyRequest(result, failedResponse)
    }

    "respond status 400 to request with extra query param that is not used in the hmac signature" in new WithApplication(FakeApplication(additionalConfiguration = testDB)) {
      val token = appService.findAll(waterfall.id).head.token
      val Some(result) = route(FakeRequest(GET, s"/v1/reward_callbacks/$token/unity_ads?productid=$productID&sid=$sid&oid=$oid&hmac=$hmac&dummy=123456"))
      verifyRequest(result, failedResponse)
    }

    "respond status 400 to request with bad url and good parameters" in new WithApplication(FakeApplication(additionalConfiguration = testDB)) {
      val Some(result) = route(FakeRequest(GET, s"/v1/reward_callbacks/123456/unity_ads?productid=$productID&sid=$sid&oid=$oid&hmac=$hmac"))
      verifyRequest(result, failedResponse)
    }

    "respond correctly (status 400) to request missing query params that are used in the hmac signature" in new WithApplication(FakeApplication(additionalConfiguration = testDB)) {
      val token = appService.findAll(waterfall.id).head.token
      val Some(resultOid) = route(FakeRequest(GET, s"/v1/reward_callbacks/$token/unity_ads?productid=$productID&sid=123456789&hmac=12120989fs8dfwoej"))
      verifyRequest(resultOid, failedResponse)

      val Some(resultHmac) = route(FakeRequest(GET, s"/v1/reward_callbacks/$token/unity_ads?productid=$productID&sid=123456789&oid=aksdhksdjh345345"))
      verifyRequest(resultHmac, failedResponse)

      val Some(resultSid) = route(FakeRequest(GET, s"/v1/reward_callbacks/$token/unity_ads?productid=$productID&oid=123456789&hmac=12120989fs8dfwoej"))
      verifyRequest(resultSid, failedResponse)

      val Some(resultNoParams) = route(FakeRequest(GET, s"/v1/reward_callbacks/$token/unity_ads"))
      verifyRequest(resultNoParams, failedResponse)
    }
  }

  "adProviderName" should {
    "be set when creating a new instance of the UnityAdsCallback class" in new WithDB {
      goodCallback.adProviderName must beEqualTo(Constants.UnityAds.Name)
    }
  }

  "adProviderUserID" should {
    "be set when creating a new instance of the UnityAdsCallback class" in new WithDB {
      goodCallback.adProviderUserID must beEqualTo(sid)
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
        virtualCurrencyService.update(new VirtualCurrency(
          virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate = 100, rewardMin = 1, rewardMax = None, roundUp = true))
        val request = generateRequest(app1.token, sid, oid, hmac, productID)
        new UnityAdsCallback(app1.token, request.queryString, waterfallAdProviderService)
      }
      callback.currencyAmount must beEqualTo(2)
      callback.currencyAmount must not(beEqualTo(amount))
    }

    "be set to the rewardMinimum value when roundUp is true and the calculated amount is less than rewardMinimum" in new WithDB {
      val callback = {
        virtualCurrencyService.update(new VirtualCurrency(
          virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate = 1, rewardMin = 5, rewardMax = None, roundUp = true))
        val request = generateRequest(app1.token, sid, oid, hmac, productID)
        new UnityAdsCallback(app1.token, request.queryString, waterfallAdProviderService)
      }
      callback.currencyAmount must beEqualTo(5)
    }

    "be set to 0 when roundUp is false and the calculated amount is less than the rewardMinimum" in new WithDB {
      val callback = {
        virtualCurrencyService.update(new VirtualCurrency(
          virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate = 100, rewardMin = 5, rewardMax = None, roundUp = false))
        val request = generateRequest(app1.token, sid, oid, hmac, productID)
        new UnityAdsCallback(app1.token, request.queryString, waterfallAdProviderService)
      }
      callback.currencyAmount must beEqualTo(0)
    }

    "be set to the rewardMaximum value if rewardMaximum is not empty and the calculated amount is greater than the rewardMaximum" in new WithDB {
      val callbackWithoutRewardMax = {
        virtualCurrencyService.update(new VirtualCurrency(
          virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate = 500, rewardMin = 1, rewardMax = None, roundUp = true))
        val request = generateRequest(app1.token, sid, oid, hmac, productID)
        new UnityAdsCallback(app1.token, request.queryString, waterfallAdProviderService)
      }
      callbackWithoutRewardMax.currencyAmount must beEqualTo(12)

      val callbackWithRewardMax = {
        virtualCurrencyService.update(new VirtualCurrency(
          virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate = 500, rewardMin = 1, rewardMax = Some(2), roundUp = true))
        val request = generateRequest(app1.token, sid, oid, hmac, productID)
        new UnityAdsCallback(app1.token, request.queryString, waterfallAdProviderService)
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
      val request = generateRequest(app1.token, sid, oid, invalidHmac, productID)
      val newCallback = new UnityAdsCallback(app1.token, request.queryString, waterfallAdProviderService)
      newCallback.verificationInfo.isValid mustEqual false
    }

    "set the ad provider name correctly" in new WithDB {
      goodCallback.verificationInfo.adProviderName must beEqualTo(Constants.UnityAds.Name)
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
