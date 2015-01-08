package models

import play.api.libs.json.{JsString, JsObject}
import play.api.test._
import play.api.test.Helpers._
import resources._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AdColonyCallbackSpec extends SpecificationWithFixtures with WaterfallSpecSetup with AdProviderSpecSetup {
  val amount = 1
  val currency = "Credits"
  val macSha1, odin1, openUDID, udid, uid = ""
  val verifier = "15c16e889f382cb60d5b4550380bd5f7"
  val transactionID = "0123456789"
  val appToken = app1.token

  running(FakeApplication(additionalConfiguration = testDB)) {
    val id = WaterfallAdProvider.create(waterfall.id, adColonyID, None, None, true, true).get
    val currentWap = WaterfallAdProvider.find(id).get
    val configuration = JsObject(Seq("callbackParams" -> JsObject(Seq("APIKey" -> JsString("abcdefg"))),
      "requiredParams" -> JsObject(Seq()), "reportingParams" -> JsObject(Seq())))
    WaterfallAdProvider.update(new WaterfallAdProvider(currentWap.id, currentWap.waterfallID, currentWap.adProviderID, None, None, Some(true), None, configuration, false))
  }

  "adProviderName" should {
    "be set when creating a new instance of the AdColonyCallback class" in new WithDB {
      val callback = new AdColonyCallback(appToken, transactionID, uid, amount, currency, openUDID, udid, odin1, macSha1, verifier)
      callback.adProviderName must beEqualTo("AdColony")
    }
  }

  "appToken" should {
    "be set when creating a new instance of the AdColonyCallback class" in new WithDB {
      val callback = new AdColonyCallback(appToken, transactionID, uid, amount, currency, openUDID, udid, odin1, macSha1, verifier)
      callback.token must beEqualTo(appToken)
    }
  }

  "currencyAmount" should {
    "be set when creating a new instance of the AdColonyCallback class" in new WithDB {
      val callback = new AdColonyCallback(appToken, transactionID, uid, amount, currency, openUDID, udid, odin1, macSha1, verifier)
      callback.currencyAmount must beEqualTo(amount)
    }
  }

  "receivedVerification" should {
    "be set when creating a new instance of the AdColonyCallback class" in new WithDB {
      val callback = new AdColonyCallback(appToken, transactionID, uid, amount, currency, openUDID, udid, odin1, macSha1, verifier)
      callback.receivedVerification must beEqualTo(verifier)
    }
  }

  "verificationInfo" should {
    "return an instance of the CallbackVerificationInfo class" in new WithDB {
      val callback = new AdColonyCallback(appToken, transactionID, uid, amount, currency, openUDID, udid, odin1, macSha1, verifier)
      callback.verificationInfo must haveClass[CallbackVerificationInfo]
    }

    "be valid when the generated verification matches the received verification string" in new WithDB {
      val callback = new AdColonyCallback(appToken, transactionID, uid, amount, currency, openUDID, udid, odin1, macSha1, verifier)
      val verification = callback.verificationInfo
      verification.isValid must beEqualTo(true)
    }

    "not be valid when the generated verification does not match the received verification string" in new WithDB {
      val invalidVerifier = "Some fake verifier"
      val callback = new AdColonyCallback(appToken, transactionID, uid, amount, currency, openUDID, udid, odin1, macSha1, invalidVerifier)
      val verification = callback.verificationInfo
      verification.isValid must beEqualTo(false)
    }
  }
}
