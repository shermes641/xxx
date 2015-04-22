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
 * @param sdkBlacklistRegex The regex to blacklist Adapter/SDK version combinations. This value will be used to create an NSRegularExpression in the SDK.
 */
case class AdProvider(id: Long, name: String, configurationData: JsValue, configurable: Boolean = true, defaultEcpm: Option[Double] = None, sdkBlacklistRegex: String = ".^")

/**
 * Encapsulates updatable information for Ad Providers.
 * @param name name field in the ad_providers table
 * @param configurationData contains default required params, reporting params, and callback params for an ad provider.
 * @param callbackURLFormat The format of the callback URL for all WaterfallAdProvider instances.
 * @param configurable determines if the WaterfallAdProviders which belong to this AdProvider can have their eCPM edited.
 * @param defaultEcpm the starting cpm value for a newly created WaterfallAdProvider.
 */
case class UpdatableAdProvider(name: String, configurationData: String, callbackURLFormat: Option[String], configurable: Boolean = true, defaultEcpm: Option[Double] = None)

object AdProvider extends JsonConversion with AdProviderHelper {
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

