package models

import org.specs2.runner._
import org.junit.runner._
import play.api.db.DB
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.libs.ws.{WSAuthScheme, WS}
import play.api.Play.current
import play.api.test.FakeApplication
import play.api.test.Helpers._
import resources.WaterfallSpecSetup
import scala.concurrent.Await
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class AdProviderSpec extends SpecificationWithFixtures with WaterfallSpecSetup with AdProviderManagement {
  val iosAdProviderIDs = List(adProviderID1, adProviderID2)

  val androidAdProviderIDs = running(FakeApplication(additionalConfiguration = testDB)) {
    List(
      AdProvider.create(Platform.Android.HyprMarketplace.name, Platform.Android.HyprMarketplace.configurationData, Platform.Android.PlatformID, Platform.Android.HyprMarketplace.callbackURLFormat, Platform.Android.HyprMarketplace.configurable, Platform.Android.HyprMarketplace.defaultEcpm),
      AdProvider.create(Platform.Android.Vungle.name, Platform.Android.Vungle.configurationData, Platform.Android.PlatformID, Platform.Android.Vungle.callbackURLFormat, Platform.Android.Vungle.configurable, Platform.Android.Vungle.defaultEcpm),
      AdProvider.create(Platform.Android.AppLovin.name, Platform.Android.AppLovin.configurationData, Platform.Android.PlatformID, Platform.Android.AppLovin.callbackURLFormat, Platform.Android.AppLovin.configurable, Platform.Android.AppLovin.defaultEcpm)
    )
  }

  "AdProvider.findAll" should {
    "return a list of AdProviders" in new WithDB {
      val adProviderList = AdProvider.findAll
      adProviderList.map { element => element must haveClass[AdProvider] }
      adProviderList.size must beEqualTo(iosAdProviderIDs.length + androidAdProviderIDs.length)
    }
  }

  "AdProvider.findNonIntegrated" should {
    "return a list of all AdProviders if there are no corresponding WaterfallAdProviders for a given Waterfall ID" in new WithAppDB(distributor.id.get) {
      val nonIntegratedAdProviders = AdProvider.findNonIntegrated(currentWaterfall.id, Platform.Ios.PlatformID)
      nonIntegratedAdProviders.size must beEqualTo(AdProvider.findAllByPlatform(Platform.Ios.PlatformID).size)
    }

    "return only the AdProviders that do not have a corresponding WaterfallAdProvider for a given Waterfall ID" in new WithAppDB(distributor.id.get) {
      WaterfallAdProvider.create(currentWaterfall.id, adProviderID1.get, None, None, configurable = true, active = true)
      val nonIntegratedAdProviders = AdProvider.findNonIntegrated(currentWaterfall.id, Platform.Ios.PlatformID)
      val allAdProvidersCount = AdProvider.findAllByPlatform(Platform.Ios.PlatformID).size
      val waterfallAdProvidersCount = WaterfallAdProvider.findAllByWaterfallID(currentWaterfall.id).size
      nonIntegratedAdProviders.size must beEqualTo(allAdProvidersCount - waterfallAdProvidersCount)
      nonIntegratedAdProviders.map(provider => provider.id must not equalTo adProviderID1.get)
    }

    "return only the AdProviders of a particular platform" in new WithAppDB(distributor.id.get) {
      val androidProvderID = androidAdProviderIDs.head.get
      WaterfallAdProvider.create(currentWaterfall.id, androidProvderID, None, None, configurable = true, active = true)
      val nonIntegratedAdProviders = AdProvider.findNonIntegrated(currentWaterfall.id, Platform.Android.PlatformID)
      val allAdProvidersCount = AdProvider.findAllByPlatform(Platform.Android.PlatformID).size
      val waterfallAdProvidersCount = WaterfallAdProvider.findAllByWaterfallID(currentWaterfall.id).size
      nonIntegratedAdProviders.size must beEqualTo(allAdProvidersCount - waterfallAdProvidersCount)
      nonIntegratedAdProviders.map(provider => provider.id must not equalTo androidProvderID)
    }
  }

  "AdProvider.create" should {
    "add a new record to the ad_providers table" in new WithDB {
      val originalCount = tableCount("ad_providers")
      val newProviderID = AdProvider.create(
        name = "New AdProvider",
        configurationData = adProviderConfigData,
        platformID = Platform.Ios.PlatformID,
        callbackUrlFormat = None
      )
      newProviderID must haveClass[Some[Long]]
      tableCount("ad_providers") must beEqualTo(originalCount + 1)
    }

    "set the configuration field to the same JSON passed as an argument" in new WithAppDB(distributor.id.get) {
      val newProviderID = AdProvider.create(
        name = "New AdProvider 1",
        configurationData = adProviderConfigData,
        platformID = Platform.Ios.PlatformID,
        callbackUrlFormat = None,
        configurable = false
      ).get
      val adProvider = AdProvider.findAll.filter { provider => provider.id == newProviderID }.head
      adProvider.configurationData must beEqualTo(Json.parse(adProviderConfigData))
    }

    "set a callback URL format if one is passed as an argument" in new WithAppDB(distributor.id.get) {
      val callbackUrl = Some("/v1/reward_callbacks/%s/new_ad_provider?amount=1&uid=%%user%%&openudid=%%udid%%&mac=%%mac%%&ifa=%%ifa%%&transaction_id=%%txid%%&digest=%%digest%%")
      val newProviderID = AdProvider.create(
        name = "New AdProvider 2",
        configurationData = adProviderConfigData,
        platformID = Platform.Ios.PlatformID,
        callbackUrlFormat = callbackUrl
      ).get
      val wapID = WaterfallAdProvider.create(currentWaterfall.id, newProviderID, None, None, configurable = true, active = true).get
      val config = DB.withConnection { implicit connection => WaterfallAdProvider.findConfigurationData(wapID).get }
      config.callbackUrlFormat must beEqualTo(callbackUrl)
    }

    "set an AdProvider to not be configurable if the configurable argument is false" in new WithAppDB(distributor.id.get) {
      val newProviderID = AdProvider.create(
        name = "New AdProvider 3",
        configurationData = adProviderConfigData,
        platformID = Platform.Ios.PlatformID,
        callbackUrlFormat = None,
        configurable = false
      ).get
      val adProvider = AdProvider.findAll.filter { provider => provider.id == newProviderID }.head
      adProvider.configurable must beEqualTo(false)
    }

    "set a default eCPM if one is passed as an argument" in new WithDB {
      val defaultEcpm = Some(5.0)
      val newProviderID = AdProvider.create(
        name = "New AdProvider 4",
        configurationData = adProviderConfigData,
        platformID = Platform.Ios.PlatformID,
        callbackUrlFormat = None,
        configurable = false,
        defaultEcpm = defaultEcpm).get
      val adProvider = AdProvider.findAll.filter { provider => provider.id == newProviderID }.head
      adProvider.defaultEcpm must beEqualTo(defaultEcpm)
    }

    "set the platform ID of the AdProvider" in new WithDB {
      val newProviderID = AdProvider.create(
        name = "New AdProvider 5",
        configurationData = adProviderConfigData,
        platformID = Platform.Ios.PlatformID,
        callbackUrlFormat = None,
        configurable = false,
        defaultEcpm = Some(10)
      ).get
      val adProvider = AdProvider.findAll.filter { provider => provider.id == newProviderID }.head
      adProvider.platformID must beEqualTo(Platform.Ios.PlatformID)
    }
  }

  "AdColony documentation tooltips" should {
    val adColonyConfigurationData = List(Platform.Ios.AdColony.configurationData, Platform.Android.AdColony.configurationData)

    "include working links to documentation for general configuration" in new WithDB {
      adColonyConfigurationData.foreach { adProviderConfig =>
        val configurationData: JsValue = Json.parse(adProviderConfig)
        val requiredParams = configurationData \ "requiredParams"
        checkDocumentationLinks(requiredParams, "Configuring AdColony")
      }
    }

    "include working links to documentation for reporting configuration" in new WithDB {
      adColonyConfigurationData.foreach { adProviderConfig =>
        val configurationData: JsValue = Json.parse(adProviderConfig)
        val reportingParams = configurationData \ "reportingParams"
        checkDocumentationLinks(reportingParams, "Configuring AdColony")
      }
    }

    "include working links to documentation for server to server callback configuration" in new WithDB {
      adColonyConfigurationData.foreach { adProviderConfig =>
        val configurationData: JsValue = Json.parse(adProviderConfig)
        val callbackParams = configurationData \ "callbackParams"
        checkDocumentationLinks(callbackParams, "AdColony Server to Server Callbacks Setup")
      }
    }
  }

  "Vungle documentation tooltips" should {
    val vungleConfigurationData = List(Platform.Ios.Vungle.configurationData, Platform.Android.Vungle.configurationData)

    "include working links to documentation for general configuration" in new WithDB {
      vungleConfigurationData.foreach { adProviderConfig =>
        val configurationData: JsValue = Json.parse(adProviderConfig)
        val requiredParams = configurationData \ "requiredParams"
        checkDocumentationLinks(requiredParams, "Configuring Vungle")
      }
    }

    "include working links to documentation for reporting configuration" in new WithDB {
      vungleConfigurationData.foreach { adProviderConfig =>
        val configurationData: JsValue = Json.parse(adProviderConfig)
        val reportingParams = configurationData \ "reportingParams"
        checkDocumentationLinks(reportingParams, "Configuring Vungle")
      }
    }

    "include working links to documentation for server to server callback configuration" in new WithDB {
      vungleConfigurationData.foreach { adProviderConfig =>
        val configurationData: JsValue = Json.parse(adProviderConfig)
        val callbackParams = configurationData \ "callbackParams"
        checkDocumentationLinks(callbackParams, "Vungle Server to Server Callbacks Setup")
      }
    }
  }

  "AppLovin documentation tooltips" should {
    val appLovinConfigurationData = List(Platform.Ios.AppLovin.configurationData, Platform.Android.AppLovin.configurationData)

    "include working links to documentation for general configuration" in new WithDB {
      appLovinConfigurationData.foreach { adProviderConfig =>
        val configurationData: JsValue = Json.parse(adProviderConfig)
        val requiredParams = configurationData \ "requiredParams"
        checkDocumentationLinks(requiredParams, "Configuring AppLovin")
      }
    }

    "include working links to documentation for reporting configuration" in new WithDB {
      appLovinConfigurationData.foreach { adProviderConfig =>
        val configurationData: JsValue = Json.parse(adProviderConfig)
        val reportingParams = configurationData \ "reportingParams"
        checkDocumentationLinks(reportingParams, "Configuring AppLovin")
      }
    }

    "include working links to documentation for server to server callback configuration" in new WithDB {
      appLovinConfigurationData.foreach { adProviderConfig =>
        val configurationData: JsValue = Json.parse(adProviderConfig)
        val callbackParams = configurationData \ "callbackParams"
        checkDocumentationLinks(callbackParams, "AppLovin Server to Server Callbacks Setup")
      }
    }
  }

  "AdProvider.findAllByPlatform" should {
    "return all ad providers of a specific platform ID" in new WithDB {
      val adProviders = AdProvider.findAllByPlatform(Platform.Ios.PlatformID)
      adProviders.map(provider => provider.platformID must beEqualTo(Platform.Ios.PlatformID))
    }

    "exclude ad providers of any other platform ID" in new WithDB {
      val adProviders = AdProvider.findAllByPlatform(Platform.Android.PlatformID)
      adProviders.map(provider => provider.platformID must not equalTo Platform.Ios.PlatformID)
    }
  }

  /**
    * Verifies that the documentation page exists and contains certain content.
    *
    * @param configurationParams WaterfallAdProvider JSON configuration containing documentation links
    * @param expectedPageHeader  Text we expect to be present when we visit the documentation link
    */
  def checkDocumentationLinks(configurationParams: JsValue, expectedPageHeader: String) = {
    // collect descriptions for every param in the array so we can check each link individually
    val tooltipDescriptions: Seq[JsValue] = configurationParams.as[JsArray].value.map(param => param \ "description")
    tooltipDescriptions.foreach {
      description =>
        val urlPattern = new scala.util.matching.Regex("""https:\/\/documentation.hyprmx.com(\/|\w|\+)+""")
        val documentationLink = urlPattern findFirstIn description.as[String] match {
          case Some(url) => url
          case None => ""
        }
        val request = WS.url(documentationLink).withAuth(DocumentationUsername, DocumentationPassword, WSAuthScheme.BASIC)
        Await.result(request.get().map {
          response =>
            response.status must beEqualTo(200)
            response.body must contain(expectedPageHeader)
        }, Duration(10000, "millis"))
    }
  }
}

