package models

import play.api.libs.json.JsObject
import play.api.test.Helpers._
import play.api.test._
import resources._

class AppLovinCallbackSpec extends SpecificationWithFixtures with AdProviderSpecSetup with WaterfallSpecSetup {
  override lazy val adProvider = adProviderService
  val eCPM = 25.0
  val adProviderUserID= Some("user-id")

  running(testApplication) {
    val id = waterfallAdProviderService.create(waterfall.id, appLovinID, None, None, true, true).get
    val currentWap = waterfallAdProviderService.find(id).get
    val configuration = JsObject(Seq("callbackParams" -> JsObject(Seq()),
      "requiredParams" -> JsObject(Seq()), "reportingParams" -> JsObject(Seq())))
    waterfallAdProviderService.update(new WaterfallAdProvider(currentWap.id, currentWap.waterfallID, currentWap.adProviderID, None, Some(eCPM), Some(true), None, configuration, false))
  }

  val transactionID = "some-transaction-id"
  val amount = 5.0

  val callbackNoUserID = running(FakeApplication(additionalConfiguration = testDB)) {
    new AppLovinCallback(transactionID, app1.token, amount, None, waterfallAdProviderService)
  }

  val callback = running(testApplication) {
    new AppLovinCallback(transactionID, app1.token, amount, adProviderUserID, waterfallAdProviderService)
  }

  "adProviderName" should {
    "be set when creating a new instance of the AppLovinCallback class" in new WithDB {
      callback.adProviderName must beEqualTo("AppLovin")
      callbackNoUserID.adProviderName must beEqualTo("AppLovin")
    }
  }

  "adProviderUserID" should {
    "be set when creating a new instance of the AppLovinCallback class" in new WithDB {
      callback.adProviderUserID must beEqualTo(adProviderUserID.get)
      callbackNoUserID.adProviderUserID must beEqualTo("")
    }
  }

  "token" should {
    "be set when creating a new instance of the AppLovinCallback class" in new WithDB {
      callback.token must beEqualTo(app1.token)
      callbackNoUserID.token must beEqualTo(app1.token)
    }
  }

  "currencyAmount" should {
    "ignore the reward amount passed in the server to server callback" in new WithDB {
      val callback = {
        virtualCurrencyService.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate=100, rewardMin=1, rewardMax=None, roundUp=true))
        new AppLovinCallback(transactionID, app1.token, amount, adProviderUserID, waterfallAdProviderService)
      }
      callback.currencyAmount must beEqualTo(2)
      callback.currencyAmount must not(beEqualTo(amount, waterfallAdProviderService))
    }

    "be set to the rewardMinimum value when roundUp is true and the calculated amount is less than rewardMinimum" in new WithDB {
      val callback = {
        virtualCurrencyService.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate=1, rewardMin=5, rewardMax=None, roundUp=true))
        new AppLovinCallback(transactionID, app1.token, amount, adProviderUserID, waterfallAdProviderService)
      }
      callback.currencyAmount must beEqualTo(5)
    }

    "be set to 0 when roundUp is false and the calculated amount is less than the rewardMinimum" in new WithDB {
      val callback = {
        virtualCurrencyService.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate=100, rewardMin=5, rewardMax=None, roundUp=false))
        new AppLovinCallback(transactionID, app1.token, amount, adProviderUserID, waterfallAdProviderService)
      }
      callback.currencyAmount must beEqualTo(0)
    }

    "be set to the rewardMaximum value if rewardMaximum is not empty and the calculated amount is greater than the rewardMaximum" in new WithDB {
      val callbackWithoutRewardMax = {
        virtualCurrencyService.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate=500, rewardMin=1, rewardMax=None, roundUp=true))
        new AppLovinCallback(transactionID, app1.token, amount, adProviderUserID, waterfallAdProviderService)
      }
      callbackWithoutRewardMax.currencyAmount must beEqualTo(12)

      val callbackWithRewardMax = {
        virtualCurrencyService.update(new VirtualCurrency(virtualCurrency1.id, virtualCurrency1.appID, virtualCurrency1.name, exchangeRate=500, rewardMin=1, rewardMax=Some(2), roundUp=true))
        new AppLovinCallback(transactionID, app1.token, amount, adProviderUserID, waterfallAdProviderService)
      }
      callbackWithRewardMax.currencyAmount must beEqualTo(2)
    }
  }

  "verificationInfo" should {
    "return an instance of the CallbackVerificationInfo class" in new WithDB {
      callback.verificationInfo must haveClass[CallbackVerificationInfo]
    }

    "always be valid" in new WithDB {
      val verification = callback.verificationInfo
      verification.isValid must beEqualTo(true)
    }
    
    "set the ad provider name correctly" in new WithDB {
      callback.verificationInfo.adProviderName must beEqualTo(callback.adProviderName)
    }

    "set the app token correctly" in new WithDB {
      callback.verificationInfo.appToken must beEqualTo(app1.token)
    }

    "set the transaction ID correctly" in new WithDB {
      callback.verificationInfo.transactionID must beEqualTo(transactionID)
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
