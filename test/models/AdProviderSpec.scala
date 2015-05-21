package models

import org.specs2.runner._
import org.junit.runner._
import play.api.db.DB
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.libs.ws.{WSAuthScheme, WS}
import play.api.Play.current
import resources.WaterfallSpecSetup
import scala.concurrent.Await
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class AdProviderSpec extends SpecificationWithFixtures with WaterfallSpecSetup with AdProviderHelper {
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

  "AdColony documentation tooltips" should {
    "include working links to documentation for general configuration" in new WithDB {
      val configurationData: JsValue = Json.parse(AdColony.configurationData)
      val requiredParams = configurationData \ "requiredParams"
      checkDocumentationLinks(requiredParams, "Configuring AdColony")
    }

    "include working links to documentation for reporting configuration" in new WithDB {
      val configurationData: JsValue = Json.parse(AdColony.configurationData)
      val reportingParams = configurationData \ "reportingParams"
      checkDocumentationLinks(reportingParams, "Configuring AdColony")
    }

    "include working links to documentation for server to server callback configuration" in new WithDB {
      val configurationData: JsValue = Json.parse(AdColony.configurationData)
      val callbackParams = configurationData \ "callbackParams"
      checkDocumentationLinks(callbackParams, "AdColony Server to Server Callbacks Setup")
    }
  }

  "Vungle documentation tooltips" should {
    "include working links to documentation for general configuration" in new WithDB {
      val configurationData: JsValue = Json.parse(Vungle.configurationData)
      val requiredParams = configurationData \ "requiredParams"
      checkDocumentationLinks(requiredParams, "Configuring Vungle")
    }

    "include working links to documentation for reporting configuration" in new WithDB {
      val configurationData: JsValue = Json.parse(Vungle.configurationData)
      val reportingParams = configurationData \ "reportingParams"
      checkDocumentationLinks(reportingParams, "Configuring Vungle")
    }

    "include working links to documentation for server to server callback configuration" in new WithDB {
      val configurationData: JsValue = Json.parse(Vungle.configurationData)
      val callbackParams = configurationData \ "callbackParams"
      checkDocumentationLinks(callbackParams, "Vungle Server to Server Callbacks Setup")
    }
  }

  "AppLovin documentation tooltips" should {
    "include working links to documentation for general configuration" in new WithDB {
      val configurationData: JsValue = Json.parse(AppLovin.configurationData)
      val requiredParams = configurationData \ "requiredParams"
      checkDocumentationLinks(requiredParams, "Configuring AppLovin")
    }

    "include working links to documentation for reporting configuration" in new WithDB {
      val configurationData: JsValue = Json.parse(AppLovin.configurationData)
      val reportingParams = configurationData \ "reportingParams"
      checkDocumentationLinks(reportingParams, "Configuring AppLovin")
    }

    "include working links to documentation for server to server callback configuration" in new WithDB {
      val configurationData: JsValue = Json.parse(AppLovin.configurationData)
      val callbackParams = configurationData \ "callbackParams"
      checkDocumentationLinks(callbackParams, "AppLovin Server to Server Callbacks Setup")
    }
  }

  /**
   * Verifies that the documentation page exists and contains certain content.
   * @param configurationParams WaterfallAdProvider JSON configuration containing documentation links
   * @param expectedPageHeader Text we expect to be present when we visit the documentation link
   */
  def checkDocumentationLinks(configurationParams: JsValue, expectedPageHeader: String) = {
    // collect descriptions for every param in the array so we can check each link individually
    val tooltipDescriptions: Seq[JsValue] = configurationParams.as[JsArray].value.map(param => param \ "description")
    tooltipDescriptions.foreach { description =>
      val urlPattern = new scala.util.matching.Regex("""http:\/\/documentation.hyprmx.com(\/|\w|\+)+""")
      val documentationLink = urlPattern findFirstIn description.as[String] match {
        case Some(url) => url
        case None => ""
      }
      val request = WS.url(documentationLink).withAuth(DocumentationUsername, DocumentationPassword, WSAuthScheme.BASIC)
      Await.result(request.get().map { response =>
        response.status must beEqualTo(200)
        response.body must contain(expectedPageHeader)
      }, Duration(5000, "millis"))
    }
  }
}
