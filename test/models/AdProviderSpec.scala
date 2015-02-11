package models

import org.specs2.runner._
import org.junit.runner._
import play.api.db.DB
import play.api.libs.json.Json
import resources.WaterfallSpecSetup

@RunWith(classOf[JUnitRunner])
class AdProviderSpec extends SpecificationWithFixtures with WaterfallSpecSetup {
  "AdProvider.findAll" should {
    "return a list of AdProviders" in new WithDB {
      val adProviderList = AdProvider.findAll
      adProviderList.map { element => element must haveClass[AdProvider] }
      adProviderList.size must beEqualTo(2)
    }
  }

  "AdProvider.findNonIntegrated" should {
    "return a list of all AdProviders if there are no corresponding WaterfallAdProviders for a given Waterfall ID" in new WithAppDB(distributor.id.get) {
      val nonIntegratedAdProviders = AdProvider.findNonIntegrated(currentWaterfall.id)
      nonIntegratedAdProviders.size must beEqualTo(AdProvider.findAll.size)
    }

    "return only the AdProviders that do not have a corresponding WaterfallAdProvider for a given Waterfall ID" in new WithAppDB(distributor.id.get) {
      WaterfallAdProvider.create(currentWaterfall.id, adProviderID1.get, None, None, true, true)
      val nonIntegratedAdProviders = AdProvider.findNonIntegrated(currentWaterfall.id)
      val allAdProvidersCount = AdProvider.findAll.size
      val waterfallAdProvidersCount = WaterfallAdProvider.findAllByWaterfallID(currentWaterfall.id).size
      nonIntegratedAdProviders.size must beEqualTo(allAdProvidersCount - waterfallAdProvidersCount)
    }
  }

  "AdProvider.create" should {
    "add a new record to the ad_providers table" in new WithDB {
      val originalCount = tableCount("ad_providers")
      val newProviderID = AdProvider.create("New AdProvider", adProviderConfigData, None)
      newProviderID must haveClass[Some[Long]]
      tableCount("ad_providers") must beEqualTo(originalCount + 1)
    }

    "set the configuration field to the same JSON passed as an argument" in new  WithAppDB(distributor.id.get) {
      val newProviderID = AdProvider.create("New AdProvider", adProviderConfigData, None, false).get
      val adProvider = AdProvider.findAll.filter { provider => provider.id == newProviderID }(0)
      adProvider.configurationData must beEqualTo(Json.parse(adProviderConfigData))
    }

    "set a callback URL format if one is passed as an argument" in new  WithAppDB(distributor.id.get) {
      val callbackUrl = Some("/v1/reward_callbacks/%s/new_ad_provider?amount=1&uid=%%user%%&openudid=%%udid%%&mac=%%mac%%&ifa=%%ifa%%&transaction_id=%%txid%%&digest=%%digest%%")
      val newProviderID = AdProvider.create("New AdProvider", adProviderConfigData, callbackUrl).get
      val wapID = WaterfallAdProvider.create(currentWaterfall.id, newProviderID, None, None, true, true).get
      val config = DB.withConnection { implicit connection => WaterfallAdProvider.findConfigurationData(wapID).get }
      config.callbackUrlFormat must beEqualTo(callbackUrl)
    }

    "set an AdProvider to not be configurable if the configurable argument is false" in new  WithAppDB(distributor.id.get) {
      val newProviderID = AdProvider.create("New AdProvider", adProviderConfigData, None, false).get
      val adProvider = AdProvider.findAll.filter { provider => provider.id == newProviderID }(0)
      adProvider.configurable must beEqualTo(false)
    }

    "set a default eCPM if one is passed as an argument" in new  WithDB {
      val defaultEcpm = Some(5.0)
      val newProviderID = AdProvider.create("New AdProvider", adProviderConfigData, None, false, defaultEcpm).get
      val adProvider = AdProvider.findAll.filter { provider => provider.id == newProviderID }(0)
      adProvider.defaultEcpm must beEqualTo(defaultEcpm)
    }
  }
}
