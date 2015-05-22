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
  val verifier = "6c9186f082be09baef312da505114fa2"
  val transactionID = "0123456789"
  val appToken = app1.token
  val customID = "testuser"
  val eCPM = 25.0

  running(FakeApplication(additionalConfiguration = testDB)) {
    val id = WaterfallAdProvider.create(waterfall.id, adColonyID, None, None, true, true).get
    val currentWap = WaterfallAdProvider.find(id).get
    val configuration = JsObject(Seq("callbackParams" -> JsObject(Seq("APIKey" -> JsString("abcdefg"))),
      "requiredParams" -> JsObject(Seq()), "reportingParams" -> JsObject(Seq())))
    WaterfallAdProvider.update(new WaterfallAdProvider(currentWap.id, currentWap.waterfallID, currentWap.adProviderID, None, Some(eCPM), Some(true), None, configuration, false))
  }

  "adProviderName" should {
    "be set when creating a new instance of the AdColonyCallback class" in new WithDB {
      val callback = new AdColonyCallback(appToken, transactionID, uid, amount, currency, openUDID, udid, odin1, macSha1, verifier, customID)
      callback.adProviderName must beEqualTo("AdColony")
    }
  }

  "appToken" should {
    "be set when creating a new instance of the AdColonyCallback class" in new WithDB {
      val callback = new AdColonyCallback(appToken, transactionID, uid, amount, currency, openUDID, udid, odin1, macSha1, verifier, customID)
      callback.token must beEqualTo(appToken)
    }
  }

  "currencyAmount" should {
    "ignore the reward amount passed in the server to server callback" in new WithDB {
      val callback = {
        VirtualCurrency.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate=100, rewardMin=1, rewardMax=None, roundUp=true))
        new AdColonyCallback(appToken, transactionID, uid, amount, currency, openUDID, udid, odin1, macSha1, verifier, customID)
      }
      callback.currencyAmount must beEqualTo(2)
      callback.currencyAmount must not(beEqualTo(amount))
    }

    "be set to the rewardMinimum value when roundUp is true and the calculated amount is less than rewardMinimum" in new WithDB {
      val callback = {
        VirtualCurrency.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate=1, rewardMin=5, rewardMax=None, roundUp=true))
        new AdColonyCallback(appToken, transactionID, uid, amount, currency, openUDID, udid, odin1, macSha1, verifier, customID)
      }
      callback.currencyAmount must beEqualTo(5)
    }

    "be set to 0 when roundUp is false and the calculated amount is less than the rewardMinimum" in new WithDB {
      val callback = {
        VirtualCurrency.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate=100, rewardMin=5, rewardMax=None, roundUp=false))
        new AdColonyCallback(appToken, transactionID, uid, amount, currency, openUDID, udid, odin1, macSha1, verifier, customID)
      }
      callback.currencyAmount must beEqualTo(0)
    }

    "be set to the rewardMaximum value if rewardMaximum is not empty and the calculated amount is greater than the rewardMaximum" in new WithDB {
      val callbackWithoutRewardMax = {
        VirtualCurrency.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate=500, rewardMin=1, rewardMax=None, roundUp=true))
        new AdColonyCallback(appToken, transactionID, uid, amount, currency, openUDID, udid, odin1, macSha1, verifier, customID)
      }
      callbackWithoutRewardMax.currencyAmount must beEqualTo(12)

      val callbackWithRewardMax = {
        VirtualCurrency.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate=500, rewardMin=1, rewardMax=Some(2), roundUp=true))
        new AdColonyCallback(appToken, transactionID, uid, amount, currency, openUDID, udid, odin1, macSha1, verifier, customID)
      }
      callbackWithRewardMax.currencyAmount must beEqualTo(2)
    }
  }

  "receivedVerification" should {
    "be set when creating a new instance of the AdColonyCallback class" in new WithDB {
      val callback = new AdColonyCallback(appToken, transactionID, uid, amount, currency, openUDID, udid, odin1, macSha1, verifier, customID)
      callback.receivedVerification must beEqualTo(verifier)
    }
  }

  "verificationInfo" should {
    "return an instance of the CallbackVerificationInfo class" in new WithDB {
      val callback = new AdColonyCallback(appToken, transactionID, uid, amount, currency, openUDID, udid, odin1, macSha1, verifier, customID)
      callback.verificationInfo must haveClass[CallbackVerificationInfo]
    }

    "be valid when the generated verification matches the received verification string" in new WithDB {
      val callback = new AdColonyCallback(appToken, transactionID, uid, amount, currency, openUDID, udid, odin1, macSha1, verifier, customID)
      val verification = callback.verificationInfo
      verification.isValid must beEqualTo(true)
    }

    "not be valid when the generated verification does not match the received verification string" in new WithDB {
      val invalidVerifier = "Some fake verifier"
      val callback = new AdColonyCallback(appToken, transactionID, uid, amount, currency, openUDID, udid, odin1, macSha1, invalidVerifier, customID)
      val verification = callback.verificationInfo
      verification.isValid must beEqualTo(false)
    }
  }
}
