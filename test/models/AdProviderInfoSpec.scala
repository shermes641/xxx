package models

import models.Waterfall.AdProviderInfo
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AdProviderInfoSpec extends SpecificationWithFixtures with WaterfallSpecSetup {
  "AdProviderInfo.meetsRewardThreshold" should {
    "return true if the roundUp option is true" in new WithDB {
      val roundUp = Some(true)
      val adProviderInfo = new AdProviderInfo(None, None, None, None, None, None, None, None, None, None, None, None, roundUp, false, false, None)
      adProviderInfo.meetsRewardThreshold must beEqualTo(true)
    }

    "return true if roundUp is false, the cpm is lower than the minimum possible calculated reward amount, and there is no rewardMin set" in new WithDB {
      val rewardMin = None
      val roundUp = Some(false)
      val exchangeRate = Some(10.toLong)
      val cpm = Some(1.0)
      val adProviderInfo = new AdProviderInfo(None, None, None, None, None, None, None, cpm, None, exchangeRate, rewardMin, None, roundUp, false, false, None)
      adProviderInfo.meetsRewardThreshold must beEqualTo(true)
    }

    "return true if roundUp is false, rewardMin is set, and cpm is greater than the calculated reward amount" in new WithDB {
      val rewardMin = Some(1.toLong)
      val roundUp = Some(false)
      val exchangeRate = Some(100.toLong)
      val cpm = Some(50.0)
      val adProviderInfo = new AdProviderInfo(None, None, None, None, None, None, None, cpm, None, exchangeRate, rewardMin, None, roundUp, false, false, None)
      cpm.get must beGreaterThan((rewardMin.get/exchangeRate.get).toDouble)
      adProviderInfo.meetsRewardThreshold must beEqualTo(true)
    }

    "return false if roundUp is false, rewardMin is set, and cpm is less than the calculated reward amount" in new WithDB {
      val rewardMin = Some(10.toLong)
      val roundUp = Some(false)
      val exchangeRate = Some(10.toLong)
      val cpm = Some(50.0)
      val adProviderInfo = new AdProviderInfo(None, None, None, None, None, None, None, cpm, None, exchangeRate, rewardMin, None, roundUp, false, false, None)
      (cpm.get / 1000) must beLessThan((rewardMin.get/exchangeRate.get).toDouble)
      adProviderInfo.meetsRewardThreshold must beEqualTo(false)
    }

    "return false if roundUp is false and cpm is not set" in new WithDB {
      val roundUp = Some(false)
      val cpm = None
      val adProviderInfo = new AdProviderInfo(None, None, None, None, None, None, None, cpm, None, None, None, None, roundUp, false, false, None)
      adProviderInfo.meetsRewardThreshold must beEqualTo(false)
    }
  }
  step(clean)
}
