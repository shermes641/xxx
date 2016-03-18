package models

import models.Waterfall.AdProviderInfo
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import resources.{SpecificationWithFixtures, WaterfallSpecSetup}

@RunWith(classOf[JUnitRunner])
class AdProviderInfoSpec extends SpecificationWithFixtures with WaterfallSpecSetup {
  "AdProviderInfo.meetsRewardThreshold" should {
    "return true if the roundUp option is true" in new WithDB {
      val roundUp = Some(true)
      val adProviderInfo = new AdProviderInfo(providerName=None, providerID=None, platformID=Some(1), sdkBlacklistRegex=None, appName=None, appID=None, appConfigRefreshInterval=0,
        distributorName=None, distributorID=None, configurationData=None, cpm=None, virtualCurrencyName=None, exchangeRate=None,
        rewardMin=1, rewardMax=None, roundUp, testMode=false, paused=false, optimizedOrder=false, active=None)
      adProviderInfo.meetsRewardThreshold must beEqualTo(true)
    }

    "return true if roundUp is false, rewardMin is set, and cpm is greater than the calculated reward amount" in new WithDB {
      val rewardMin = 1
      val roundUp = Some(false)
      val exchangeRate = Some(100L)
      val cpm = Some(50.0)
      val adProviderInfo = new AdProviderInfo(providerName=None, providerID=None, platformID=Some(1), sdkBlacklistRegex=None, appName=None, appID=None, appConfigRefreshInterval=0,
        distributorName=None, distributorID=None, configurationData=None, cpm, virtualCurrencyName=None, exchangeRate,
        rewardMin, rewardMax=None, roundUp, testMode=false, paused=false, optimizedOrder=false, active=None)
      adProviderInfo.meetsRewardThreshold must beEqualTo(true)
    }

    "return false if roundUp is false, rewardMin is set, and cpm is less than the calculated reward amount" in new WithDB {
      val rewardMin = 1
      val roundUp = Some(false)
      val exchangeRate = Some(25L)
      val cpm = Some(25.0)
      val adProviderInfo = new AdProviderInfo(providerName=None, providerID=None, platformID=Some(1), sdkBlacklistRegex=None, appName=None, appID=None, appConfigRefreshInterval=0,
        distributorName=None, distributorID=None, configurationData=None, cpm, virtualCurrencyName=None, exchangeRate,
        rewardMin, rewardMax=None, roundUp, testMode=false, paused=false, optimizedOrder=false, active=None)
      adProviderInfo.meetsRewardThreshold must beEqualTo(false)
    }

    "return false if roundUp is false and cpm is not set" in new WithDB {
      val roundUp = Some(false)
      val cpm = None
      val adProviderInfo = new AdProviderInfo(providerName=None, providerID=None, platformID=Some(1), sdkBlacklistRegex=None, appName=None, appID=None, appConfigRefreshInterval=0,
        distributorName=None, distributorID=None, configurationData=None, cpm, virtualCurrencyName=None, exchangeRate=None,
        rewardMin=1, rewardMax=None, roundUp, testMode=false, paused=false, optimizedOrder=false, active=None)
      adProviderInfo.meetsRewardThreshold must beEqualTo(false)
    }
  }
}
