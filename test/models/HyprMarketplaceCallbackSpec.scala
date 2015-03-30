package models

import play.api.libs.json.JsObject
import play.api.test._
import play.api.test.Helpers._
import resources._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HyprMarketplaceCallbackSpec extends SpecificationWithFixtures with AdProviderSpecSetup with WaterfallSpecSetup {
  running(FakeApplication(additionalConfiguration = testDB)) {
    val id = WaterfallAdProvider.create(waterfall.id, hyprMarketplaceID, None, None, true, true).get
    val currentWap = WaterfallAdProvider.find(id).get
    val configuration = JsObject(Seq("callbackParams" -> JsObject(Seq()),
      "requiredParams" -> JsObject(Seq()), "reportingParams" -> JsObject(Seq())))
    WaterfallAdProvider.update(new WaterfallAdProvider(currentWap.id, currentWap.waterfallID, currentWap.adProviderID, None, None, Some(true), None, configuration, false))
  }

  val userID = "abc"
  val signature = "b6125341cbd0393e5b5ab67169964c63aba583982cb44f0bc75a48f2587ab870"
  val time = "1419972045"
  val subID = "1111"
  val quantity = 1
  val payout = Some(0.5)
  val callback = running(FakeApplication(additionalConfiguration = testDB)) {
    new HyprMarketplaceCallback(app1.token, userID, signature, time, subID, payout, quantity)
  }

  "adProviderName" should {
    "be set when creating a new instance of the HyprMarketplaceCallback class" in new WithDB {
      callback.adProviderName must beEqualTo("HyprMarketplace")
    }
  }

  "token" should {
    "be set when creating a new instance of the HyprMarketplaceCallback class" in new WithDB {
      callback.token must beEqualTo(app1.token)
    }
  }

  "currencyAmount" should {
    "be set when creating a new instance of the HyprMarketplaceCallback class" in new WithDB {
      callback.currencyAmount must beEqualTo(quantity)
    }
  }

  "payout" should {
    "be set when creating a new instance of the HyprMarketplace class" in new WithDB {
      callback.payout must beEqualTo(payout)
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

    "not be valid when the generated verification does not match the received signature" in new WithDB {
      val invalidSignature = "Some fake verifier"
      val newCallback = new HyprMarketplaceCallback(app1.token, userID, invalidSignature, time, subID, payout, quantity)
      val verification = newCallback.verificationInfo
      verification.isValid must beEqualTo(false)
    }
  }
}
