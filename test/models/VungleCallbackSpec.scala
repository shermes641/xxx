package models

import play.api.libs.json.{JsString, JsObject}
import play.api.test._
import play.api.test.Helpers._
import resources._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class VungleCallbackSpec extends SpecificationWithFixtures with AdProviderSpecSetup with WaterfallSpecSetup {
  val eCPM = 25.0
  running(FakeApplication(additionalConfiguration = testDB)) {
    val id = WaterfallAdProvider.create(waterfall.id, vungleID, None, None, configurable = true, active = true).get
    val currentWap = WaterfallAdProvider.find(id).get
    val configuration = JsObject(Seq("callbackParams" -> JsObject(Seq("APIKey" -> JsString("abcdefg"))),
      "requiredParams" -> JsObject(Seq()), "reportingParams" -> JsObject(Seq())))
    WaterfallAdProvider.update(new WaterfallAdProvider(currentWap.id, currentWap.waterfallID, currentWap.adProviderID, None, Some(eCPM), Some(true), None, configuration, false))
  }

  val transactionID = "0123456789"
  val digest = "bf80d53f84df22bb91b48acc7606bc0909876f6fe981b1610a0352433ae16a63"
  val amount = 1
  val callback = running(FakeApplication(additionalConfiguration = testDB)) {
    new VungleCallback(app1.token, transactionID, digest, amount)
  }

  "adProviderName" should {
    "be set when creating a new instance of the VungleCallback class" in new WithDB {
      callback.adProviderName must beEqualTo("Vungle")
    }
  }

  "token" should {
    "be set when creating a new instance of the VungleCallback class" in new WithDB {
      callback.token must beEqualTo(app1.token)
    }
  }

  "currencyAmount" should {
    "ignore the reward amount passed in the server to server callback" in new WithDB {
      val callback = {
        VirtualCurrency.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate=100, rewardMin=1, rewardMax=None, roundUp=true))
        new VungleCallback(app1.token, transactionID, digest, amount)
      }
      callback.currencyAmount must beEqualTo(2)
      callback.currencyAmount must not(beEqualTo(amount))
    }

    "be set to the rewardMinimum value when roundUp is true and the calculated amount is less than rewardMinimum" in new WithDB {
      val callback = {
        VirtualCurrency.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate=1, rewardMin=5, rewardMax=None, roundUp=true))
        new VungleCallback(app1.token, transactionID, digest, amount)
      }
      callback.currencyAmount must beEqualTo(5)
    }

    "be set to 0 when roundUp is false and the calculated amount is less than the rewardMinimum" in new WithDB {
      val callback = {
        VirtualCurrency.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate=100, rewardMin=5, rewardMax=None, roundUp=false))
        new VungleCallback(app1.token, transactionID, digest, amount)
      }
      callback.currencyAmount must beEqualTo(0)
    }

    "be set to the rewardMaximum value if rewardMaximum is not empty and the calculated amount is greater than the rewardMaximum" in new WithDB {
      val callbackWithoutRewardMax = {
        VirtualCurrency.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate=500, rewardMin=1, rewardMax=None, roundUp=true))
        new VungleCallback(app1.token, transactionID, digest, amount)
      }
      callbackWithoutRewardMax.currencyAmount must beEqualTo(12)

      val callbackWithRewardMax = {
        VirtualCurrency.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate=500, rewardMin=1, rewardMax=Some(2), roundUp=true))
        new VungleCallback(app1.token, transactionID, digest, amount)
      }
      callbackWithRewardMax.currencyAmount must beEqualTo(2)
    }
  }

  "receivedVerification" should {
    "be set when creating a new instance of the VungleCallback class" in new WithDB {
      callback.receivedVerification must beEqualTo(digest)
    }
  }

  "verificationInfo" should {
    "return an instance of the CallbackVerificationInfo class" in new WithDB {
      callback.verificationInfo must haveClass[CallbackVerificationInfo]
    }

    "be valid when the generated verification matches the received verification string" in new WithDB {
      val verification = callback.verificationInfo
      verification.isValid must beEqualTo(true)
    }

    "not be valid when the generated verification does not match the received digest" in new WithDB {
      val invalidDigest = "Some fake verifier"
      val newCallback = new VungleCallback(app1.token, transactionID, invalidDigest, amount)
      val verification = newCallback.verificationInfo
      verification.isValid must beEqualTo(false)
    }
  }
}
