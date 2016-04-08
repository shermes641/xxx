package models

import org.specs2.runner._
import org.junit.runner._
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.libs.ws.WSAuthScheme
import play.api.test.Helpers._
import resources.{SpecificationWithFixtures, WaterfallSpecSetup}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class AdProviderSpec extends SpecificationWithFixtures with WaterfallSpecSetup with AdProviderManagement {
  override lazy val platform = testPlatform
  override lazy val db = database
  override lazy val adProvider = adProviderService
  val iosAdProviderIDs = List(adProviderID1, adProviderID2)

  val androidAdProviderIDs = running(testApplication) {
    List(
      adProviderService.create(
        testPlatform.Android.HyprMarketplace.name,
        testPlatform.Android.HyprMarketplace.displayName,
        testPlatform.Android.HyprMarketplace.configurationData,
        testPlatform.Android.PlatformID,
        testPlatform.Android.HyprMarketplace.callbackURLFormat,
        testPlatform.Android.HyprMarketplace.callbackURLDescription.format(testPlatform.Android.HyprMarketplace.displayName),
        testPlatform.Android.HyprMarketplace.configurable,
        testPlatform.Android.HyprMarketplace.defaultEcpm
      ),
      adProviderService.create(
        testPlatform.Android.Vungle.name,
        testPlatform.Android.Vungle.displayName,
        testPlatform.Android.Vungle.configurationData,
        testPlatform.Android.PlatformID,
        testPlatform.Android.Vungle.callbackURLFormat,
        testPlatform.Android.Vungle.callbackURLDescription.format(testPlatform.Android.Vungle.displayName),
        testPlatform.Android.Vungle.configurable,
        testPlatform.Android.Vungle.defaultEcpm
      ),
      adProviderService.create(
        testPlatform.Android.AppLovin.name,
        testPlatform.Android.AppLovin.displayName,
        testPlatform.Android.AppLovin.configurationData,
        testPlatform.Android.PlatformID,
        testPlatform.Android.AppLovin.callbackURLFormat,
        testPlatform.Android.AppLovin.callbackURLDescription.format(testPlatform.Android.AppLovin.displayName),
        testPlatform.Android.AppLovin.configurable,
        testPlatform.Android.AppLovin.defaultEcpm
      )
    )
  }

  "AdProvider.findAll" should {
    "return a list of AdProviders" in new WithDB {
      val adProviderList = adProviderService.findAll
      adProviderList.map { element => element must haveClass[AdProvider] }
      adProviderList.size must beEqualTo(iosAdProviderIDs.length + androidAdProviderIDs.length)
    }
  }

  "AdProvider.findNonIntegrated" should {
    "return a list of all AdProviders if there are no corresponding WaterfallAdProviders for a given Waterfall ID" in new WithAppDB(distributor.id.get) {
      val nonIntegratedAdProviders = adProviderService.findNonIntegrated(currentWaterfall.id, testPlatform.Ios.PlatformID)
      nonIntegratedAdProviders.size must beEqualTo(adProviderService.findAllByPlatform(testPlatform.Ios.PlatformID).size)
    }

    "return only the AdProviders that do not have a corresponding WaterfallAdProvider for a given Waterfall ID" in new WithAppDB(distributor.id.get) {
      waterfallAdProviderService.create(currentWaterfall.id, adProviderID1.get, None, None, true, true)
      val nonIntegratedAdProviders = adProviderService.findNonIntegrated(currentWaterfall.id, testPlatform.Ios.PlatformID)
      val allAdProvidersCount = adProviderService.findAllByPlatform(testPlatform.Ios.PlatformID).size
      val waterfallAdProvidersCount = waterfallAdProviderService.findAllByWaterfallID(currentWaterfall.id).size
      nonIntegratedAdProviders.size must beEqualTo(allAdProvidersCount - waterfallAdProvidersCount)
      nonIntegratedAdProviders.map(provider => provider.id must not equalTo adProviderID1.get)
    }

    "return only the AdProviders of a particular platform" in new WithAppDB(distributor.id.get) {
      val androidProvderID = androidAdProviderIDs.head.get
      waterfallAdProviderService.create(currentWaterfall.id, androidProvderID, None, None, true, true)
      val nonIntegratedAdProviders = adProviderService.findNonIntegrated(currentWaterfall.id, testPlatform.Android.PlatformID)
      val allAdProvidersCount = adProviderService.findAllByPlatform(testPlatform.Android.PlatformID).size
      val waterfallAdProvidersCount = waterfallAdProviderService.findAllByWaterfallID(currentWaterfall.id).size
      nonIntegratedAdProviders.size must beEqualTo(allAdProvidersCount - waterfallAdProvidersCount)
      nonIntegratedAdProviders.map(provider => provider.id must not equalTo androidProvderID)
    }
  }

  "AdProvider.create" should {
    "add a new record to the ad_providers table" in new WithDB {
      val originalCount = tableCount("ad_providers")
      val name = "NewAdProvider"
      val displayName = "New AdProvider"
      val newProviderID = adProviderService.create(
        name = "New AdProvider",
        displayName = displayName,
        configurationData = adProviderConfigData,
        platformID = testPlatform.Ios.PlatformID,
        callbackUrlFormat = None,
        callbackUrlDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(displayName)
      )
      newProviderID must haveClass[Some[Long]]
      tableCount("ad_providers") must beEqualTo(originalCount + 1)
    }

    "set the configuration field to the same JSON passed as an argument" in new WithAppDB(distributor.id.get) {
      val name = "NewAdProvider1"
      val displayName = "New AdProvider 1"
      val newProviderID = adProviderService.create(
        name = "New AdProvider 1",
        displayName = displayName,
        configurationData = adProviderConfigData,
        platformID = testPlatform.Ios.PlatformID,
        callbackUrlFormat = None,
        configurable = false,
        callbackUrlDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(displayName)
      ).get
      val adProvider = adProviderService.findAll.filter { provider => provider.id == newProviderID }.head
      adProvider.configurationData must beEqualTo(Json.parse(adProviderConfigData))
    }

    "set a callback URL format if one is passed as an argument" in new WithAppDB(distributor.id.get) {
      val callbackUrl = Some("/v1/reward_callbacks/%s/new_ad_provider?amount=1&uid=%%user%%&openudid=%%udid%%&mac=%%mac%%&ifa=%%ifa%%&transaction_id=%%txid%%&digest=%%digest%%")
      val name = "NewAdProvider2"
      val displayName = "New AdProvider 2"
      val newProviderID = adProviderService.create(
        name = "New AdProvider 2",
        displayName = displayName,
        configurationData = adProviderConfigData,
        platformID = testPlatform.Ios.PlatformID,
        callbackUrlFormat = callbackUrl,
        callbackUrlDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(displayName)
      ).get
      val wapID = waterfallAdProviderService.create(currentWaterfall.id, newProviderID, None, None, true, true).get
      val config = database.withConnection { implicit connection => waterfallAdProviderService.findConfigurationData(wapID).get }
      config.callbackUrlFormat must beEqualTo(callbackUrl)
    }

    "set an AdProvider to not be configurable if the configurable argument is false" in new WithAppDB(distributor.id.get) {
      val name = "NewAdProvider3"
      val displayName = "New AdProvider 3"
      val newProviderID = adProviderService.create(
        name = "New AdProvider 3",
        displayName = displayName,
        configurationData = adProviderConfigData,
        platformID = testPlatform.Ios.PlatformID,
        callbackUrlFormat = None,
        callbackUrlDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(displayName),
        configurable = false
      ).get
      val adProvider = adProviderService.findAll.filter { provider => provider.id == newProviderID }.head
      adProvider.configurable must beEqualTo(false)
    }

    "set a default eCPM if one is passed as an argument" in new WithDB {
      val defaultEcpm = Some(5.0)
      val name = "NewAdProvider4"
      val displayName = "New AdProvider 4"
      val newProviderID = adProviderService.create(
        name = "New AdProvider 4",
        displayName = displayName,
        configurationData = adProviderConfigData,
        platformID = testPlatform.Ios.PlatformID,
        callbackUrlFormat = None,
        callbackUrlDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(displayName),
        configurable = false,
        defaultEcpm = defaultEcpm).get
      val adProvider = adProviderService.findAll.filter { provider => provider.id == newProviderID }.head
      adProvider.defaultEcpm must beEqualTo(defaultEcpm)
    }

    "set the platform ID of the AdProvider" in new WithDB {
      val name = "NewAdProvider5"
      val displayName = "New AdProvider 5"
      val newProviderID = adProviderService.create(
        name = "New AdProvider 5",
        displayName = displayName,
        configurationData = adProviderConfigData,
        platformID = testPlatform.Ios.PlatformID,
        callbackUrlFormat = None,
        callbackUrlDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(displayName),
        configurable = false,
        defaultEcpm = Some(10)
      ).get
      val adProvider = adProviderService.findAll.filter { provider => provider.id == newProviderID }.head
      adProvider.platformID must beEqualTo(testPlatform.Ios.PlatformID)
    }
  }

  "AdColony documentation tooltips" should {
    val adColonyConfigurationData = List(testPlatform.Ios.AdColony.configurationData, testPlatform.Android.AdColony.configurationData)

    "include working links to documentation for general configuration" in new WithDB {
      adColonyConfigurationData.foreach { adProviderConfig =>
        val configurationData: JsValue = Json.parse(adProviderConfig)
        val requiredParams = (configurationData \ "requiredParams").get
        checkDocumentationLinks(requiredParams, "Configuring AdColony")
      }
    }

    "include working links to documentation for reporting configuration" in new WithDB {
      adColonyConfigurationData.foreach { adProviderConfig =>
        val configurationData: JsValue = Json.parse(adProviderConfig)
        val reportingParams = (configurationData \ "reportingParams").get
        checkDocumentationLinks(reportingParams, "Configuring AdColony")
      }
    }

    "include working links to documentation for server to server callback configuration" in new WithDB {
      adColonyConfigurationData.foreach { adProviderConfig =>
        val configurationData: JsValue = Json.parse(adProviderConfig)
        val callbackParams = (configurationData \ "callbackParams").get
        checkDocumentationLinks(callbackParams, "AdColony Server to Server Callbacks Setup")
      }
    }
  }

  "Vungle documentation tooltips" should {
    val vungleConfigurationData = List(testPlatform.Ios.Vungle.configurationData, testPlatform.Android.Vungle.configurationData)

    "include working links to documentation for general configuration" in new WithDB {
      vungleConfigurationData.foreach { adProviderConfig =>
        val configurationData: JsValue = Json.parse(adProviderConfig)
        val requiredParams = (configurationData \ "requiredParams").get
        checkDocumentationLinks(requiredParams, "Configuring Vungle")
      }
    }

    "include working links to documentation for reporting configuration" in new WithDB {
      vungleConfigurationData.foreach { adProviderConfig =>
        val configurationData: JsValue = Json.parse(adProviderConfig)
        val reportingParams = (configurationData \ "reportingParams").get
        checkDocumentationLinks(reportingParams, "Configuring Vungle")
      }
    }

    "include working links to documentation for server to server callback configuration" in new WithDB {
      vungleConfigurationData.foreach { adProviderConfig =>
        val configurationData: JsValue = Json.parse(adProviderConfig)
        val callbackParams = (configurationData \ "callbackParams").get
        checkDocumentationLinks(callbackParams, "Vungle Server to Server Callbacks Setup")
      }
    }
  }

  "AppLovin documentation tooltips" should {
    val appLovinConfigurationData = List(testPlatform.Ios.AppLovin.configurationData, testPlatform.Android.AppLovin.configurationData)

    "include working links to documentation for general configuration" in new WithDB {
      appLovinConfigurationData.foreach { adProviderConfig =>
        val configurationData: JsValue = Json.parse(adProviderConfig)
        val requiredParams = (configurationData \ "requiredParams").get
        checkDocumentationLinks(requiredParams, "Configuring AppLovin")
      }
    }

    "include working links to documentation for reporting configuration" in new WithDB {
      appLovinConfigurationData.foreach { adProviderConfig =>
        val configurationData: JsValue = Json.parse(adProviderConfig)
        val reportingParams = (configurationData \ "reportingParams").get
        checkDocumentationLinks(reportingParams, "Configuring AppLovin")
      }
    }

    "include working links to documentation for server to server callback configuration" in new WithDB {
      appLovinConfigurationData.foreach { adProviderConfig =>
        val configurationData: JsValue = Json.parse(adProviderConfig)
        val callbackParams = (configurationData \ "callbackParams").get
        checkDocumentationLinks(callbackParams, "AppLovin Server to Server Callbacks Setup")
      }
    }
  }

  "AdProvider.findAllByPlatform" should {
    "return all ad providers of a specific platform ID" in new WithDB {
      val adProviders = adProviderService.findAllByPlatform(testPlatform.Ios.PlatformID)
      adProviders.map(provider => provider.platformID must beEqualTo(testPlatform.Ios.PlatformID))
    }

    "exclude ad providers of any other platform ID" in new WithDB {
      val adProviders = adProviderService.findAllByPlatform(testPlatform.Android.PlatformID)
      adProviders.map(provider => provider.platformID must not equalTo testPlatform.Ios.PlatformID)
    }
  }

  // TODO: Uncomment these tests once the Unity Ads documentation has been published.
  /*
  "Unity Ads documentation tooltips" should {
    val unityAdsConfigurationData = List(platform.Ios.UnityAds.configurationData, testPlatform.Android.UnityAds.configurationData)

    "include working links to documentation for general configuration" in new WithDB {
      unityAdsConfigurationData.foreach { adProviderConfig =>
        val configurationData: JsValue = Json.parse(adProviderConfig)
        val requiredParams = configurationData \ "requiredParams"
        checkDocumentationLinks(requiredParams, "Configuring Unity Ads")
      }
    }

    "include working links to documentation for reporting configuration" in new WithDB {
      unityAdsConfigurationData.foreach { adProviderConfig =>
        val configurationData: JsValue = Json.parse(adProviderConfig)
        val reportingParams = configurationData \ "reportingParams"
        checkDocumentationLinks(reportingParams, "Configuring Unity Ads")
      }
    }

    "include working links to documentation for server to server callback configuration" in new WithDB {
      unityAdsConfigurationData.foreach { adProviderConfig =>
        val configurationData: JsValue = Json.parse(adProviderConfig)
        val callbackParams = configurationData \ "callbackParams"
        checkDocumentationLinks(callbackParams, "Unity Ads Server to Server Callbacks Setup")
      }
    }
  }
  */

  "AdProvider.findAllByPlatform" should {
    "return all ad providers of a specific platform ID" in new WithDB {
      val adProviders = adProviderService.findAllByPlatform(testPlatform.Ios.PlatformID)
      adProviders.map(provider => provider.platformID must beEqualTo(testPlatform.Ios.PlatformID))
    }

    "exclude ad providers of any other platform ID" in new WithDB {
      val adProviders = adProviderService.findAllByPlatform(testPlatform.Android.PlatformID)
      adProviders.map(provider => provider.platformID must not equalTo testPlatform.Ios.PlatformID)
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
    val tooltipDescriptions: Seq[JsValue] = configurationParams.as[JsArray].value.map(param => (param \ "description").get)
    tooltipDescriptions.foreach { description =>
      val urlPattern = new scala.util.matching.Regex("""https:\/\/documentation.hyprmx.com(\/|\w|\+)+""")
      val documentationLink = urlPattern findFirstIn description.as[String] match {
        case Some(url) => url
        case None => ""
      }
      val request = ws.url(documentationLink).withAuth(DocumentationUsername, DocumentationPassword, WSAuthScheme.BASIC)
      Await.result(request.get().map { response =>
        response.status must beEqualTo(200)
        response.body must contain(expectedPageHeader)
      }, Duration(10000, "millis"))
    }
  }
}

