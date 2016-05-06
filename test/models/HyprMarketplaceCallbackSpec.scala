package models

import play.api.libs.json.JsObject
import play.api.test.Helpers._
import play.api.test._
import resources._

class HyprMarketplaceCallbackSpec extends SpecificationWithFixtures with AdProviderSpecSetup with WaterfallSpecSetup {
  running(FakeApplication(additionalConfiguration = testDB)) {
    val id = WaterfallAdProvider.create(waterfall.id, hyprMarketplaceID, waterfallOrder = None, cpm = None, configurable = true, active = true).get
    val currentWap = WaterfallAdProvider.find(id).get
    val configuration = JsObject(Seq("callbackParams" -> JsObject(Seq()),
      "requiredParams" -> JsObject(Seq()), "reportingParams" -> JsObject(Seq())))
    WaterfallAdProvider.update(
      new WaterfallAdProvider(currentWap.id, currentWap.waterfallID, currentWap.adProviderID, waterfallOrder = None,
        cpm = Some(20), active = Some(true), fillRate = None, configurationData = configuration, reportingActive = false)
    )
  }
  val userID = "abc"
  val signature = "bb5a5b5a1b0865a355ffa3ae96475753dfc55ba7d1266f24d5f8881e3427060d"
  val time = "1419972045"
  val partnerCode = Some("")
  val quantity = 1
  val payout = Some(0.5)
  val callback = running(FakeApplication(additionalConfiguration = testDB)) {
    new HyprMarketplaceCallback(app1.token, userID, signature, time, payout, quantity, partnerCode)
  }

  "adProviderName" should {
    "be set when creating a new instance of the HyprMarketplaceCallback class" in new WithDB {
      callback.adProviderName must beEqualTo("HyprMarketplace")
    }
  }

  "adProviderUserID" should {
    "be set when creating a new instance of the HyprMarketplaceCallback class" in new WithDB {
      callback.adProviderUserID must beEqualTo(userID)
    }
  }

  "token" should {
    "be set when creating a new instance of the HyprMarketplaceCallback class" in new WithDB {
      callback.token must beEqualTo(app1.token)
    }
  }

  "currencyAmount" should {
    "ignore the reward amount passed in the server to server callback" in new WithDB {
      val callback = {
        VirtualCurrency.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate=100, rewardMin=1, rewardMax=None, roundUp=true))
        new HyprMarketplaceCallback(app1.token, userID, signature, time, payout, quantity, partnerCode)
      }
      callback.currencyAmount must beEqualTo(2) // ($20 eCPM/1000) * (100 currency/dollar) = 2 currency
      callback.currencyAmount must not(beEqualTo(quantity))
    }

    "be set to the rewardMinimum value when roundUp is true and the calculated amount is less than rewardMinimum" in new WithDB {
      val callback = {
        VirtualCurrency.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate=1, rewardMin=5, rewardMax=None, roundUp=true))
        new HyprMarketplaceCallback(app1.token, userID, signature, time, payout, quantity, partnerCode)
      }
      callback.currencyAmount must beEqualTo(5) // ($20 eCPM/1000) * (1 currency/dollar) = 0.02 currency
    }

    "be set to 0 when roundUp is false and the calculated amount is less than the rewardMinimum" in new WithDB {
      val callback = {
        VirtualCurrency.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate=1, rewardMin=5, rewardMax=None, roundUp=false))
        new HyprMarketplaceCallback(app1.token, userID, signature, time, payout, quantity, partnerCode)
      }
      callback.currencyAmount must beEqualTo(0) // ($20 eCPM/1000) * (1 currency/dollar) = 0.02 currency
    }

    "be set to the rewardMaximum value if rewardMaximum is not empty and the calculated amount is greater than the rewardMaximum" in new WithDB {
      val callbackWithoutRewardMax = {
        VirtualCurrency.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate=500, rewardMin=1, rewardMax=None, roundUp=true))
        new HyprMarketplaceCallback(app1.token, userID, signature, time, payout, quantity, partnerCode)
      }
      callbackWithoutRewardMax.currencyAmount must beEqualTo(10) // ($20 eCPM/1000) * (500 currency/dollar) = 10 currency

      val callbackWithRewardMax = {
        VirtualCurrency.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate=500, rewardMin=1, rewardMax=Some(2), roundUp=true))
        new HyprMarketplaceCallback(app1.token, userID, signature, time, payout, quantity, partnerCode)
      }
      callbackWithRewardMax.currencyAmount must beEqualTo(2) // ($20 eCPM/1000) * (500 currency/dollar) = 10 currency
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

    "be valid when the generated verification matches the received verification string and transaction_id is not blank" in new WithDB {
      val partnerCodeSig = "43c038d8f6edda911ef3813fe0c3e86a10437f0fbd78fccf47cca62f61212fdc"
      val nonBlankPartnerCode = Some("partner_code")
      val callbackWithTransactionID = new HyprMarketplaceCallback(app1.token, userID, partnerCodeSig, time, payout, quantity, nonBlankPartnerCode)
      callbackWithTransactionID.verificationInfo.isValid must beEqualTo(true)
    }

    "not be valid when the generated verification does not match the received signature" in new WithDB {
      val invalidSignature = "Some fake verifier"
      val newCallback = new HyprMarketplaceCallback(app1.token, userID, invalidSignature, time, payout, quantity, partnerCode)
      val verification = newCallback.verificationInfo
      verification.isValid must beEqualTo(false)
    }
    
    "set the ad provider name correctly" in new WithDB {
      callback.verificationInfo.adProviderName must beEqualTo(callback.adProviderName)
    }

    "set the app token correctly" in new WithDB {
      callback.verificationInfo.appToken must beEqualTo(app1.token)
    }

    "set the transaction ID correctly" in new WithDB {
      callback.verificationInfo.transactionID must beEqualTo(partnerCode.get)
    }

    "set the offer profit correctly" in new WithDB {
      callback.verificationInfo.offerProfit must beEqualTo(callback.payout)
    }

    "set the reward quantity correctly" in new WithDB {
      callback.verificationInfo.rewardQuantity must beEqualTo(callback.currencyAmount)
    }

    "set the reward info correctly" in new WithDB {
      callback.verificationInfo.adProviderRewardInfo must beEqualTo(callback.adProviderRewardInfo)
    }
  }
}
