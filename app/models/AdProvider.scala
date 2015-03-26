package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current
import play.api.libs.json._
import scala.language.postfixOps

/**
 * Encapsulates information for third-party SDKs to be mediated.
 * @param id id field in the ad_providers table
 * @param name name field in the ad_providers table
 * @param configurationData contains default required params, reporting params, and callback params for an ad provider.
 * @param configurable determines if the WaterfallAdProviders which belong to this AdProvider can have their eCPM edited.
 * @param defaultEcpm the starting cpm value for a newly created WaterfallAdProvider.
 */
case class AdProvider(id: Long, name: String, configurationData: JsValue, configurable: Boolean = true, defaultEcpm: Option[Double] = None)

/**
 * Encapsulates updatable information for Ad Providers.
 * @param name name field in the ad_providers table
 * @param configurationData contains default required params, reporting params, and callback params for an ad provider.
 * @param callbackURLFormat The format of the callback URL for all WaterfallAdProvider instances.
 * @param configurable determines if the WaterfallAdProviders which belong to this AdProvider can have their eCPM edited.
 * @param defaultEcpm the starting cpm value for a newly created WaterfallAdProvider.
 */
case class UpdatableAdProvider(name: String, configurationData: String, callbackURLFormat: Option[String], configurable: Boolean = true, defaultEcpm: Option[Double] = None)

object AdProvider extends JsonConversion {
  val AdColony = {
    val name = "AdColony"

    val callbackURLFormat = Some("/v1/reward_callbacks/%s/ad_colony?id=[ID]&uid=[USER_ID]&amount=[AMOUNT]&" +
      "currency=[CURRENCY]&open_udid=[OpenUDID]&udid=[UDID]&odin1=[ODIN1]&mac_sha1=[MAC_SHA1]&verifier=[VERIFIER]&custom_id=[CUSTOM_ID]")

    val configurable = true

    val defaultEcpm = None

    val configuration = {
      val zoneIDDetails = {
        "Add a single zone or multiple zones separated by commas. Please note, we currently only support Value Exchange Zones. " +
          "For more information on configuration, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/Configuring+AdColony' target='_blank'>documentation</a>"
      }

      val appIDDescription = {
        "Your AdColony App ID can be found on the AdColony dashboard.  For more information on configuring AdColony, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/Configuring+AdColony' target='_blank'>documentation</a>."
      }

      val reportingDescription = {
        "Your Read-Only API Key can be found on the AdColony dashboard.  For more information on configuring reporting for AdColony, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/Configuring+AdColony' target='_blank'>documentation</a>."
      }

      val callbackDescription = {
        "Your V4VC Secret Key can be found on the AdColony dashboard.  For more information on configuring server to server callbacks for AdColony, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/AdColony+Server+to+Server+Callbacks+Setup' target='_blank'>documentation</a>."
      }

      "{" +
        "\"requiredParams\":[" +
          "{\"description\": \"" + appIDDescription + "\", \"displayKey\": \"AdColony App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": true, \"minLength\": 1}, " +
          "{\"description\": \"" + zoneIDDetails + "\", \"displayKey\": \"Zone IDs\", \"key\": \"zoneIds\", \"value\":\"\", \"dataType\": \"Array\", \"refreshOnAppRestart\": true, \"minLength\": 1}" +
        "], " +
        "\"reportingParams\": [" +
          "{\"description\": \"" + reportingDescription + "\", \"displayKey\": \"Reporting API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
        "], " +
        "\"callbackParams\": [" +
          "{\"description\": \"" + callbackDescription + "\", \"displayKey\": \"V4VC Secret Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
        "]" +
      "}"
    }
    new UpdatableAdProvider(name, configuration, callbackURLFormat, configurable, defaultEcpm)
  }

  val HyprMarketplace = {
    val name = "HyprMarketplace"

    val callbackURLFormat = None

    val configurable = false

    val defaultEcpm: Option[Double] = Some(20)

    val configuration = {
      "{" +
        "\"requiredParams\":[" +
          "{\"description\": \"Your HyprMarketplace Distributor ID\", \"displayKey\": \"Distributor ID\", \"key\": \"distributorID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false, \"minLength\": 1}, " +
          "{\"description\": \"Your HyprMarketplace Property ID\", \"displayKey\": \"\", \"key\": \"propertyID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false, \"minLength\": 1}" +
        "], " +
        "\"reportingParams\": [" +
          "{\"description\": \"Your API Key for HyprMarketplace\", \"displayKey\": \"API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}, " +
          "{\"description\": \"Your Placement ID\", \"displayKey\": \"Placement ID\", \"key\": \"placementID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}, " +
          "{\"description\": \"Your App ID\", \"displayKey\": \"App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
        "], " +
        "\"callbackParams\": [" +
        "]" +
      "}"
    }
    new UpdatableAdProvider(name, configuration, callbackURLFormat, configurable, defaultEcpm)
  }

  val Vungle = {
    val name = "Vungle"

    val callbackURLFormat = Some("/v1/reward_callbacks/%s/vungle?amount=%s&uid=%%user%%&openudid=%%udid%%&mac=%%mac%%&ifa=%%ifa%%&transaction_id=%%txid%%&digest=%%digest%%")

    val configurable = true

    val defaultEcpm = None

    val configuration = {
      val appIDDescription = {
        "Your App ID can be found on the Vungle dashboard.  For more information on configuring Vungle, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/Configuring+Vungle' target='_blank'>documentation</a>."
      }

      val reportingDescription = {
        "Your Reporting API Key can be found on the Vungle dashboard.  For more information on configuring reporting for Vungle, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/Configuring+Vungle' target='_blank'>documentation</a>."
      }

      val callbackDescription = {
        "Your Secret Key for Secure Callback can be found on the Vungle dashboard.  For more information on configuring server to server callbacks for Vungle, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/Vungle+Server+to+Server+Callbacks+Setup' target='_blank'>documentation</a>."
      }

      "{" +
        "\"requiredParams\":[" +
          "{\"description\": \"" + appIDDescription + "\", \"displayKey\": \"App ID\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": true, \"minLength\": 1}" +
        "], " +
        "\"reportingParams\": [" +
          "{\"description\": \"" + reportingDescription + "\", \"displayKey\": \"Reporting API ID\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
        "], " +
        "\"callbackParams\": [" +
          "{\"description\": \"" + callbackDescription + "\", \"displayKey\": \"Secret Key for Secure Callback\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
        "]" +
      "}"
    }
    new UpdatableAdProvider(name, configuration, callbackURLFormat, configurable, defaultEcpm)
  }

  val AppLovin = {
    val name = "AppLovin"

    val callbackURLFormat = Some("/v1/reward_callbacks/%s/app_lovin?idfa={IDFA}&hadid={HADID}&amount={AMOUNT}&currency={CURRENCY}&event_id={EVENT_ID}&user_id={USER_ID}")

    val configurable = true

    val defaultEcpm = None

    val configuration = {
      val sdkKeyDescription = {
        "Your SDK Key can be found on the AppLovin dashboard.  For more information on configuring AppLovin, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/Configuring+AppLovin' target='_blank'>documentation</a>."
      }

      val reportingDescription = {
        "Your Report Key can be found on the AppLovin dashboard.  For more information on configuring reporting for AppLovin, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/Configuring+AppLovin' target='_blank'>documentation</a>."
      }

      val appLovinAppNameDescription = {
        "Your application name can be found in AppLovin's dashboard.  This is the same as the Bundle identifier in your iOS target properties.  For more information on configuring reporting for AppLovin, please see our <a href='http://documentation.hyprmx.com/display/ADMIN/Configuring+AppLovin' target='_blank'>documentation</a>."
      }

      "{" +
        "\"requiredParams\":[" +
          "{\"description\": \"" + sdkKeyDescription + "\", \"displayKey\": \"SDK Key\", \"key\": \"sdkKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": true, \"minLength\": 4}" +
        "], " +
        "\"reportingParams\": [" +
          "{\"description\": \"" + reportingDescription + "\", \"displayKey\": \"Report Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}," +
          "{\"description\": \"" + appLovinAppNameDescription + "\", \"displayKey\": \"Application Name\", \"key\": \"appName\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
        "], " +
        "\"callbackParams\": [" +
        "]" +
      "}"
    }
    new UpdatableAdProvider(name, configuration, callbackURLFormat, configurable, defaultEcpm)
  }

  // Used to convert SQL query result into instances of the AdProvider class.
  val adProviderParser: RowParser[AdProvider] = {
    get[Long]("id") ~
    get[String]("name") ~
    get[JsValue]("configuration_data") ~
    get[Boolean]("configurable") ~
    get[Option[Double]]("default_ecpm") map {
      case id ~ name ~ configuration_data ~ configurable ~ default_ecpm => AdProvider(id, name, configuration_data, configurable, default_ecpm)
    }
  }

  /**
   * Finds all AdProvider records.
   * @return A list of all AdProvider records from the database if records exist. Otherwise, returns an empty list.
   */
  def findAll: List[AdProvider] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT ad_providers.*
          FROM ad_providers;
        """
      )
      query.as(adProviderParser*).toList
    }
  }

  /**
   * Retrieves a list of AdProviders who do not currently have an existing WaterfallAdProvider record for a given Waterfall ID.
   * @param waterfallID The Waterfall ID to which all integrated WaterfallAdProviders belong.
   * @return A list of AdProvider instances if any exist.  Otherwise, returns an empty list.
   */
  def findNonIntegrated(waterfallID: Long): List[AdProvider] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT name, id, configuration_data, configurable, default_ecpm
          FROM ad_providers
          WHERE id NOT IN
          (SELECT DISTINCT ad_provider_id
           FROM waterfall_ad_providers wap
           WHERE wap.waterfall_id={waterfall_id})
        """
      ).on("waterfall_id" -> waterfallID)
      query.as(adProviderParser*).toList
    }
  }

  /**
   * Creates a new record in the AdProvider table
   * @param name Maps to name column in AdProvider table
   * @param configurationData Json configuration data for AdProvider
   * @param callbackUrlFormat General format for reward callback URL
   * @param configurable determines if the WaterfallAdProviders which belong to this AdProvider can have their eCPM edited.
   * @param defaultEcpm the starting cpm value for a newly created WaterfallAdProvider.
   * @return ID of newly created record
   */
  def create(name: String, configurationData: String, callbackUrlFormat: Option[String], configurable: Boolean = true, defaultEcpm: Option[Double] = None): Option[Long] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          INSERT INTO ad_providers (name, configuration_data, callback_url_format, configurable, default_ecpm)
          VALUES ({name}, CAST({configuration_data} AS json), {callback_url_format}, {configurable}, {default_ecpm});
        """
      ).on("name" -> name, "configuration_data" -> configurationData, "callback_url_format" -> callbackUrlFormat, "configurable" -> configurable, "default_ecpm" -> defaultEcpm).executeInsert()
    }
  }
}

