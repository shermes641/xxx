package hmac

import models.WaterfallAdProvider.AdProviderRewardInfo
import models.{CallbackVerificationInfo, Completion}
import oauth.signpost.OAuth.percentEncode
import org.specs2.mock.Mockito
import play.api.Application
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.mvc._
import play.api.test._

import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Promise}

/**
  * Created by shermes on 1/28/16.
  *
  * Currently we do not fail on bad signatures, therefore no failing case tests
  */
class HmacRequestVerifierFunSpec extends PlaySpecification with Mockito {

  val testHmacSecret: String = "test hmac secret"
  val testNonce = "78319ddc-5a67-73g0-nj9b-9hs6e0bf7d3"
  val testUri = "https://somewhere.com"

  val normalUri = "/fooManChoo/"
  val funkyUri = "/ü foo Man Choo"

  /**
    * Validate that a signed request passes using original params, and fails on changed params
    *
    * @param request      The original signed request
    * @param body         The body of the request
    * @param uri          The uri of the request
    * @param nonce        Unique identifier for request
    * @param secret       HMAC secret for the request
    * @return true for success
    */
  def validateSignedRequest(request: RequestHeader, body: Array[Byte], uri: String, nonce: String, secret: String) = {
    HmacRequestVerifier.verifyRequest(request, body, uri, nonce, secret+"123") mustEqual false
    HmacRequestVerifier.verifyRequest(request, body, uri, nonce+"123", secret) mustEqual false
    HmacRequestVerifier.verifyRequest(request, body, uri+"/123", nonce, secret) mustEqual false

    HmacRequestVerifier.verifyRequest(request, body, uri, nonce, secret)
  }

  /**
    * Validate that a unsigned request passes using original and changed params
    *
    * @param request      The original signed request
    * @param body         The body of the request
    * @param uri          The uri of the request
    * @param nonce        Unique identifier for request
    * @param secret       HMAC secret for the request
    * @return true for success
    */
  def validateUnsignedRequest(request: RequestHeader, body: Array[Byte], uri: String, nonce: String, secret: String) = {
    HmacRequestVerifier.verifyRequest(request, body, uri, nonce, secret+"123") mustEqual true
    HmacRequestVerifier.verifyRequest(request, body, uri, nonce+"123", secret) mustEqual true
    HmacRequestVerifier.verifyRequest(request, body, uri+"/123", nonce, secret) mustEqual true
    //verify body is not checked
    HmacRequestVerifier.verifyRequest(request, Array[Byte](1,2,3), uri, nonce, secret) mustEqual true

    HmacRequestVerifier.verifyRequest(request, body, uri, nonce, secret)
  }
  "HMAC" should {
    "Accept a unsigned GET normal url" in {
      val (request, body, hostUrl) = receiveRequest { implicit app => hostUrl =>
        WS.url(hostUrl + normalUri).get()
      }
      validateUnsignedRequest(request, body, hostUrl, testNonce, testHmacSecret) mustEqual true
    }

    "Accept a unsigned encoded GET funky url" in {
      val (request, body, hostUrl) = receiveRequest { implicit app => hostUrl =>
        WS.url(s"$hostUrl/${percentEncode(funkyUri)}/").get()
      }
      validateUnsignedRequest(request, body, hostUrl, testNonce, testHmacSecret) mustEqual true
    }

    "Verify a signed GET with query parameters normal url" in {
      var testUri = ""
      val postbackData = fakePostbackData
      val (request, body, _) = receiveRequest { implicit app => hostUrl =>
        testUri = hostUrl + normalUri
        val queryParamSeq = HmacHashData(
          uri = testUri,
          adProviderName = (postbackData \ Constants.adProviderName).as[String],
          rewardQuantity = (postbackData \ Constants.rewardQuantity).as[Long],
          estimatedOfferProfit = Some((postbackData \ Constants.offerProfit).as[Double]),
          transactionId = (postbackData \ Constants.transactionID).as[String]
        ).toQueryParamMap(Signer.getNewTimestamp, testNonce, Some(testHmacSecret))

        WS.url(testUri)
          .withQueryString(queryParamSeq: _*)
          .withBody(fakePostbackData)
          .get()
      }

      validateSignedRequest(request, body, testUri, testNonce, testHmacSecret) mustEqual true
    }

    "Verify a signed GET with query parameters funky url" in {
      var testUri = ""
      val postbackData = fakePostbackData
      val (request, body, _) = receiveRequest { implicit app => hostUrl =>
        testUri = hostUrl + funkyUri
        val queryParamSeq = HmacHashData(
          uri = testUri,
          adProviderName = (postbackData \ Constants.adProviderName).as[String],
          rewardQuantity = (postbackData \ Constants.rewardQuantity).as[Long],
          estimatedOfferProfit = Some((postbackData \ Constants.offerProfit).as[Double]),
          transactionId = (postbackData \ Constants.transactionID).as[String]
        ).toQueryParamMap(Signer.getNewTimestamp, testNonce, Some(testHmacSecret))

        WS.url(s"$hostUrl/${percentEncode(funkyUri)}/")
          .withQueryString(queryParamSeq: _*)
          .withBody(fakePostbackData)
          .get()
      }

      validateSignedRequest(request, body, testUri, testNonce, testHmacSecret) mustEqual true
    }

    "Accept a unsigned POST normal url" in {
      var testUri = ""
      val (request, body, _) = receiveRequest { implicit app => hostUrl =>
        testUri = hostUrl + normalUri
        WS.url(testUri).post(fakePostbackData)
      }

      validateUnsignedRequest(request, body, testUri, testNonce, testHmacSecret) mustEqual true
    }

    "Accept a unsigned POST funky url" in {
      val (request, body, _) = receiveRequest { implicit app => hostUrl =>
        WS.url(s"$hostUrl/${percentEncode(funkyUri)}/").post(fakePostbackData)
      }

      validateUnsignedRequest(request, body, testUri, testNonce, testHmacSecret) mustEqual true
    }

    "Verify a signed POST request normal url" in {
      var testUri = ""
      val postbackData = fakePostbackData

      val (request, body, _) = receiveRequest { implicit app => hostUrl =>
        testUri = hostUrl + normalUri
        val queryParamSeq = HmacHashData(
          uri = testUri,
          adProviderName = (postbackData \ Constants.adProviderName).as[String],
          rewardQuantity = (postbackData \ Constants.rewardQuantity).as[Long],
          estimatedOfferProfit = Some((postbackData \ Constants.offerProfit).as[Double]),
          transactionId = (postbackData \ Constants.transactionID).as[String]
        ).toQueryParamMap(Signer.getNewTimestamp, testNonce, Some(testHmacSecret))

        WS.url(testUri)
          .withQueryString(queryParamSeq: _*)
          .post(fakePostbackData)
      }

      validateSignedRequest(request, body, testUri, testNonce, testHmacSecret) mustEqual true
    }

    "Verify a signed POST request funky url" in {
      var testUri = ""
      val postbackData = fakePostbackData

      val (request, body, _) = receiveRequest { implicit app => hostUrl =>
        testUri = hostUrl + funkyUri
        val queryParamSeq = HmacHashData(
          uri = testUri,
          adProviderName = (postbackData \ Constants.adProviderName).as[String],
          rewardQuantity = (postbackData \ Constants.rewardQuantity).as[Long],
          estimatedOfferProfit = Some((postbackData \ Constants.offerProfit).as[Double]),
          transactionId = (postbackData \ Constants.transactionID).as[String]
        ).toQueryParamMap(Signer.getNewTimestamp, testNonce, Some(testHmacSecret))

        WS.url(s"$hostUrl/${percentEncode(funkyUri)}/")
          .withQueryString(queryParamSeq: _*)
          .post(fakePostbackData)
      }

      validateSignedRequest(request, body, testUri, testNonce, testHmacSecret) mustEqual true
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
      Constants.DefaultAdProviderName,
      Constants.DefaultTransactionId,
      Constants.DefaultAppToken,
      offerProfit = Some(Constants.DefaultOfferProfit),
      rewardQuantity = Constants.DefaultRewardQuantity,
      rewardInfo)
    )
    val adProviderRequest = Json.obj()
    (new Completion).postbackData(adProviderRequest, verification)
  }
}
