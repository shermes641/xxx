package models

import models.Waterfall.AdProviderInfo
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import resources.WaterfallSpecSetup

@RunWith(classOf[JUnitRunner])
class AdProviderInfoSpec extends SpecificationWithFixtures with WaterfallSpecSetup {
  "AdProviderInfo.meetsRewardThreshold" should {
    "return true if the roundUp option is true" in new WithDB {
      val roundUp = Some(true)
      val adProviderInfo = new AdProviderInfo(None, None, None, None, 0, None, None, None, None, None, None, 1, None, roundUp, false, false, false, None)
      adProviderInfo.meetsRewardThreshold must beEqualTo(true)
    }

    "return true if roundUp is false, rewardMin is set, and cpm is greater than the calculated reward amount" in new WithDB {
      val rewardMin = 1
      val roundUp = Some(false)
      val exchangeRate = Some(100L)
      val cpm = Some(50.0)
      val adProviderInfo = new AdProviderInfo(None, None, None, None, 0, None, None, None, cpm, None, exchangeRate, rewardMin, None, roundUp, false, false, false, None)
      adProviderInfo.meetsRewardThreshold must beEqualTo(true)
    }

    "return false if roundUp is false, rewardMin is set, and cpm is less than the calculated reward amount" in new WithDB {
      val rewardMin = 1
      val roundUp = Some(false)
      val exchangeRate = Some(25L)
      val cpm = Some(25.0)
      val adProviderInfo = new AdProviderInfo(None, None, None, None, 0, None, None, None, cpm, None, exchangeRate, rewardMin, None, roundUp, false, false, false, None)
      adProviderInfo.meetsRewardThreshold must beEqualTo(false)
    }

    "return false if roundUp is false and cpm is not set" in new WithDB {
      val roundUp = Some(false)
      val cpm = None
      val adProviderInfo = new AdProviderInfo(None, None, None, None, 0, None, None, None, cpm, None, None, 1, None, roundUp, false, false, false, None)
      adProviderInfo.meetsRewardThreshold must beEqualTo(false)
    }
  }
}
