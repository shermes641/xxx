package models

import play.api.libs.json.JsObject
import play.api.test._
import play.api.test.Helpers._
import resources._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AppLovinCallbackSpec extends SpecificationWithFixtures with AdProviderSpecSetup with WaterfallSpecSetup {
  running(FakeApplication(additionalConfiguration = testDB)) {
    val id = WaterfallAdProvider.create(waterfall.id, appLovinID, None, None, true, true).get
    val currentWap = WaterfallAdProvider.find(id).get
    val configuration = JsObject(Seq("callbackParams" -> JsObject(Seq()),
      "requiredParams" -> JsObject(Seq()), "reportingParams" -> JsObject(Seq())))
    WaterfallAdProvider.update(new WaterfallAdProvider(currentWap.id, currentWap.waterfallID, currentWap.adProviderID, None, None, Some(true), None, configuration, false))
  }

  val transactionID = "some-transaction-id"
  val amount = 5.0
  val callback = running(FakeApplication(additionalConfiguration = testDB)) {
    new AppLovinCallback(transactionID, app1.token, amount)
  }

  "adProviderName" should {
    "be set when creating a new instance of the AppLovinCallback class" in new WithDB {
      callback.adProviderName must beEqualTo("AppLovin")
    }
  }

  "token" should {
    "be set when creating a new instance of the AppLovinCallback class" in new WithDB {
      callback.token must beEqualTo(app1.token)
    }
  }

  "currencyAmount" should {
    "be set when creating a new instance of the AppLovinCallback class" in new WithDB {
      callback.currencyAmount must beEqualTo(amount)
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
  }
  step(clean)
}
