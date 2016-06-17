package hmac

import java.net.URLEncoder

import hmac.HmacConstants._
import models.CallbackVerificationInfo
import models.WaterfallAdProvider.AdProviderRewardInfo
import org.specs2.mock.Mockito
import play.api.Application
import play.api.libs.ws.WS
import play.api.mvc._
import play.api.test._
import resources.AdProviderRequests

import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Promise}

/**
  * Created by shermes on 1/28/16.
  *
  * Currently we do not fail on bad signatures, therefore no failing case tests
  */
class HmacRequestVerifierFunSpec extends PlaySpecification with AdProviderRequests with Mockito {
  sequential

  val testHmacSecret: String = "test hmac secret"
  val testTransID = "78319ddc-5a67-73g0-nj9b-9hs6e0bf7d3"
  val testUri = "https://somewhere.com"

  val normalUri = "/fooManChoo/"
  val funkyUri = "/ü foo Man Choo"

  val adProviderUserID = "user-id"

  val HmacHashDataTestData = Some(HmacHashData(
    adProviderRequest = hyprRequest,
    verificationInfo = CallbackVerificationInfo(isValid = true,
      adProviderName = DefaultAdProviderName,
      transactionID = DefaultTransactionId,
      appToken = "1234",
      offerProfit = Some(1.3),
      rewardQuantity = DefaultRewardQuantity,
      adProviderRewardInfo = None),
    adProviderUserID = adProviderUserID))

  /**
    * Validate that a signed request passes using original params, and fails on changed params
    *
    * @param body   The body of the request
    * @param uri    The uri of the request
    * @param secret HMAC secret for the request
    * @return true for success
    */
  def validateSignedRequest(requestHeader: RequestHeader, body: Array[Byte], uri: String, secret: String) = {
    running(FakeApplication()) {
      // change a few bytes
      val badBody = body.clone()
      badBody(50) = badBody(51)
      badBody(51) = badBody(52)
      HmacRequestVerifier.verifyRequest(requestHeader, body, secret + "123") mustEqual false
      HmacRequestVerifier.verifyRequest(requestHeader, badBody, secret) mustEqual false
      HmacRequestVerifier.verifyRequest(requestHeader, body, secret)
    }
  }

  /**
    * Validate that a unsigned request passes using original and changed params
    *
    * @param request The original signed request
    * @param body    The body of the request
    * @param uri     The uri of the request
    * @param secret  HMAC secret for the request
    * @return true for success
    */
  def validateUnsignedRequest(request: RequestHeader, body: Array[Byte], uri: String, secret: String) = {
    running(FakeApplication()) {
      HmacRequestVerifier.verifyRequest(request, body, secret + "123") mustEqual true
      HmacRequestVerifier.verifyRequest(request, Array[Byte](1, 2, 3), secret) mustEqual true
      HmacRequestVerifier.verifyRequest(request, body, secret)
    }
  }

  "HMAC" should {
    "Get values from HmacHashData" in {
      val hhd = HmacHashDataTestData.get
      hhd.postBackData.value.get(AdProviderName).get.toString.contains(DefaultAdProviderName) mustEqual true
      hhd.postBackData.value.get(AdProviderUser).get.toString.contains(adProviderUserID) mustEqual true
      hhd.postBackData.value.get(RewardQuantity).get.toString.equals(DefaultRewardQuantity.toString) mustEqual true
      hhd.postBackData.value.get(OfferProfit).get.toString.toDouble.equals(1.3) mustEqual true
      hhd.postBackData.value.get(TransactionID).get.toString.contains(DefaultTransactionId) mustEqual true
    }

    "Accept a unsigned GET normal url" in {
      val (request, body, hostUrl) = receiveRequest { implicit app => hostUrl =>
        WS.url(hostUrl + normalUri).get()
      }
      validateUnsignedRequest(request, body, hostUrl, testHmacSecret) mustEqual true
    }

    "Accept a unsigned encoded GET funky url" in {
      val (request, body, hostUrl) = receiveRequest { implicit app => hostUrl =>
        WS.url(s"$hostUrl/${URLEncoder.encode(funkyUri, "UTF8")}/").get()
      }
      validateUnsignedRequest(request, body, hostUrl, testHmacSecret) mustEqual true
    }

    "Verify a signed GET with query parameters normal url" in {
      var testUri = ""
      val postbackData = fakePostbackData
      val (request, body, _) = receiveRequest { implicit app => hostUrl =>
        testUri = hostUrl + normalUri
        val signature = HmacHashData(
          adProviderRequest = hyprRequest,
          verificationInfo = CallbackVerificationInfo(isValid = true,
            adProviderName = (postbackData \ AdProviderName).as[String],
            transactionID = (postbackData \ TransactionID).as[String],
            appToken = "1234",
            offerProfit = Some((postbackData \ OfferProfit).as[Double]),
            rewardQuantity = (postbackData \ RewardQuantity).as[Long],
            adProviderRewardInfo = None),
          adProviderUserID = (postbackData \ AdProviderUser).as[String]).toHash(testHmacSecret)

        WS.url(testUri)
          .withQueryString(Seq(("hmac", signature.get), ("version", "1.0")): _*)
          .withBody(fakePostbackData)
          .get()
      }
      validateSignedRequest(request, body, testUri, testHmacSecret) mustEqual true
    }

    "Verify a signed GET with query parameters funky url" in {
      var testUri = ""
      val postbackData = fakePostbackData
      val (request, body, _) = receiveRequest { implicit app => hostUrl =>
        testUri = hostUrl + funkyUri
        val signature = HmacHashData(
          adProviderRequest = hyprRequest,
          verificationInfo = CallbackVerificationInfo(isValid = true,
            adProviderName = (postbackData \ AdProviderName).as[String],
            transactionID = (postbackData \ TransactionID).as[String],
            appToken = "1234",
            offerProfit = Some((postbackData \ OfferProfit).as[Double]),
            rewardQuantity = (postbackData \ RewardQuantity).as[Long],
            adProviderRewardInfo = None),
          adProviderUserID = (postbackData \ AdProviderUser).as[String]).toHash(testHmacSecret)

        WS.url(s"$hostUrl/${URLEncoder.encode(funkyUri, "UTF8")}/")
          .withQueryString(Seq((QsHmac, signature.get), (QsVersionKey, QsVersionValue1_0)): _*)
          .withBody(fakePostbackData)
          .get()
      }

      validateSignedRequest(request, body, testUri, testHmacSecret) mustEqual true
    }

    "Accept a unsigned POST normal url" in {
      var testUri = ""
      val (request, body, _) = receiveRequest { implicit app => hostUrl =>
        testUri = hostUrl + normalUri
        WS.url(testUri).post(fakePostbackData)
      }

      validateUnsignedRequest(request, body, testUri, testHmacSecret) mustEqual true
    }

    "Accept a unsigned POST funky url" in {
      val (request, body, _) = receiveRequest { implicit app => hostUrl =>
        WS.url(s"$hostUrl/${URLEncoder.encode(funkyUri, UTF8)}/").post(fakePostbackData)
      }

      validateUnsignedRequest(request, body, testUri, testHmacSecret) mustEqual true
    }

    "Verify a signed POST request normal url" in {
      var testUri = ""
      val postbackData = fakePostbackData

      val (request, body, _) = receiveRequest { implicit app => hostUrl =>
        testUri = hostUrl + normalUri
        val signature = HmacHashData(
          adProviderRequest = hyprRequest,
          verificationInfo = CallbackVerificationInfo(isValid = true,
            adProviderName = (postbackData \ AdProviderName).as[String],
            transactionID = (postbackData \ TransactionID).as[String],
            appToken = "1234",
            offerProfit = Some((postbackData \ OfferProfit).as[Double]),
            rewardQuantity = (postbackData \ RewardQuantity).as[Long],
            adProviderRewardInfo = None),
          adProviderUserID = (postbackData \ AdProviderUser).as[String]).toHash(testHmacSecret)

        WS.url(testUri)
          .withQueryString(Seq(("hmac", signature.get), ("version", "1.0")): _*)
          .post(fakePostbackData)
      }

      validateSignedRequest(request, body, testUri, testHmacSecret) mustEqual true
    }

    "Verify a signed POST request funky url" in {
      var testUri = ""
      val postbackData = fakePostbackData

      val (request, body, _) = receiveRequest { implicit app => hostUrl =>
        testUri = hostUrl + funkyUri
        val signature = HmacHashData(
          adProviderRequest = hyprRequest,
          verificationInfo = CallbackVerificationInfo(isValid = true,
            adProviderName = (postbackData \ AdProviderName).as[String],
            transactionID = (postbackData \ TransactionID).as[String],
            appToken = "1234",
            offerProfit = Some((postbackData \ OfferProfit).as[Double]),
            rewardQuantity = (postbackData \ RewardQuantity).as[Long],
            adProviderRewardInfo = None),
          adProviderUserID = (postbackData \ AdProviderUser).as[String]).toHash(testHmacSecret)

        WS.url(s"$hostUrl/${URLEncoder.encode(funkyUri, "UTF8")}/")
          .withQueryString(Seq(("hmac", signature.get), ("version", "1.0")): _*)
          .post(fakePostbackData)
      }

      validateSignedRequest(request, body, testUri, testHmacSecret) mustEqual true
    }
  }

  def receiveRequest(makeRequest: Application => String => Future[_]): (RequestHeader, Array[Byte], String) = {
    val hostUrl = "http://localhost:" + testServerPort
    val promise = Promise[(RequestHeader, Array[Byte])]()
    val app = FakeApplication(withRoutes = {
      case _ => Action(BodyParsers.parse.raw) { request =>
        promise.success((request, request.body.asBytes().getOrElse(Array.empty[Byte])))
        Results.Ok
      }
    })
    running(TestServer(testServerPort, app)) {
      await(makeRequest(app)(hostUrl))
    }
    val (request, body) = await(promise.future)(Duration(5000, "millis"))
    (request, body, hostUrl)
  }

  def fakePostbackData = {
    val rewardInfo = Some(mock[AdProviderRewardInfo])
    val verification = spy(new CallbackVerificationInfo(
      true,
      DefaultAdProviderName,
      DefaultTransactionId,
      DefaultAppToken,
      offerProfit = Some(DefaultOfferProfit),
      rewardQuantity = DefaultRewardQuantity,
      rewardInfo)
    )
    val adProviderRequest = hyprRequest
    HmacHashData(adProviderRequest, verification, adProviderUserID).postBackData
  }
}
