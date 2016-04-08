package models

import anorm._
import anorm.SqlParser._
import javax.inject._
import play.api.db.Database
import play.api.libs.json._
import scala.language.postfixOps

/**
 * Encapsulates information for third-party SDKs to be mediated.
 * @param id id field in the ad_providers table
 * @param name The name of the ad provider sent to the SDK (WARNING: This name must not contain spaces or punctuation).
 * @param displayName The name of the ad provider as it appears on our dashboard.
 * @param configurationData contains default required params, reporting params, and callback params for an ad provider.
 * @param callbackUrlDescription The tooltip description for the Callback URL field.
 * @param platformID Indicates the platform to which this AdProvider belongs (e.g. iOS or Android).
 * @param configurable determines if the WaterfallAdProviders which belong to this AdProvider can have their eCPM edited.
 * @param defaultEcpm the starting cpm value for a newly created WaterfallAdProvider.
 * @param sdkBlacklistRegex The regex to blacklist Adapter/SDK version combinations. This value will be used to create an NSRegularExpression in the SDK.
 */
case class AdProvider(id: Long,
                      name: String,
                      displayName: String,
                      configurationData: JsValue,
                      callbackUrlDescription: String,
                      platformID: Long,
                      configurable: Boolean = true,
                      defaultEcpm: Option[Double] = None,
                      sdkBlacklistRegex: String = ".^")

/**
 * Encapsulates updatable information for Ad Providers.
 * @param name The name of the ad provider sent to the SDK (WARNING: This name must not contain spaces or punctuation).
 * @param displayName The name of the ad provider as it appears on our dashboard.
 * @param configurationData contains default required params, reporting params, and callback params for an ad provider.
 * @param platformID Indicates the platform to which this AdProvider belongs (e.g. iOS or Android).
 * @param callbackURLFormat The format of the callback URL for all WaterfallAdProvider instances.
 * @param callbackURLDescription The tooltip description for the Callback URL field.
 * @param configurable determines if the WaterfallAdProviders which belong to this AdProvider can have their eCPM edited.
 * @param defaultEcpm the starting cpm value for a newly created WaterfallAdProvider.
 */
case class UpdatableAdProvider(name: String,
                               displayName: String,
                               configurationData: String,
                               platformID: Long,
                               callbackURLFormat: Option[String],
                               callbackURLDescription: String,
                               configurable: Boolean = true,
                               defaultEcpm: Option[Double] = None) {
  require(
    Constants.AdProvider.namePattern.findFirstIn(name).isDefined,
    s"Ad Provider name: $name should not contain any spaces or punctuation"
  )
}

/**
  * Encapsulates functions for AdProviders
  * @param database         A shared database
  * @param platformInstance A shared instance of the Platform class
  */
@Singleton
class AdProviderService @Inject() (database: Database, platformInstance: Platform) extends JsonConversion with AdProviderManagement {
  override val db = database
  override val adProvider = this
  override val platform = platformInstance

  // Used to convert SQL query result into instances of the AdProvider class.
  val adProviderParser: RowParser[AdProvider] = {
    get[Long]("id") ~
    get[String]("name") ~
    get[String]("display_name") ~
    get[JsValue]("configuration_data") ~
    get[String]("callback_url_description") ~
    get[Long]("platform_id") ~
    get[Boolean]("configurable") ~
    get[Option[Double]]("default_ecpm") map {
      case id ~ name ~ display_name ~ configuration_data ~ callback_url_description ~ platform_id ~ configurable ~ default_ecpm =>
        AdProvider(id, name, display_name, configuration_data, callback_url_description, platform_id, configurable, default_ecpm)
    }
  }

  /**
    * Finds all AdProvider records.
    * @return A list of all AdProvider records from the database if records exist. Otherwise, returns an empty list.
    */
  def findAll: List[AdProvider] = {
    db.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT ad_providers.*
          FROM ad_providers;
        """
      )
      query.as(adProviderParser*)
    }
  }

  /**
    * Finds all AdProvider records of a certain platform type.
    * @param platformID The ID of the platform to which the app belongs (e.g. Android or iOS)
    * @return A list of all AdProvider records from the database if records exist. Otherwise, returns an empty list.
    */
  def findAllByPlatform(platformID: Long): List[AdProvider] = {
    db.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT ad_providers.*
          FROM ad_providers
          WHERE platform_id = {platform_id};
        """
      ).on("platform_id" -> platformID)
      query.as(adProviderParser*)
    }
  }

  /**
    * Find a single ad provider
    *
    * @param platformID either Android or Ios
    * @param name       Ad provider name
    * @return           Some(AdProvider) or None
    */
  def findByPlatformAndName(platformID: Long, name: String): Option[AdProvider] = {
    val res = findAllByPlatform(platformID)
    res.find {e => e.name == name}
  }

  /**
    * Retrieves a list of AdProviders who do not currently have an existing WaterfallAdProvider record for a given Waterfall ID.
    * @param waterfallID The Waterfall ID to which all integrated WaterfallAdProviders belong.
    * @param platformID Indicates the platform to which this AdProvider belongs (e.g. iOS or Android).
    * @return A list of AdProvider instances if any exist.  Otherwise, returns an empty list.
    */
  def findNonIntegrated(waterfallID: Long, platformID: Long): List[AdProvider] = {
    db.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT name, display_name, id, configuration_data, callback_url_description, platform_id, configurable, default_ecpm
          FROM ad_providers
          WHERE platform_id = {platform_id} AND id NOT IN
          (SELECT DISTINCT ad_provider_id
           FROM waterfall_ad_providers wap
           WHERE wap.waterfall_id={waterfall_id})
        """
      ).on("waterfall_id" -> waterfallID, "platform_id" -> platformID)
      query.as(adProviderParser*)
    }
  }

  /**
   * Creates a new record in the AdProvider table
   * @param name The name of the ad provider sent to the SDK (WARNING: This name must not contain spaces or punctuation).
   * @param displayName The name of the ad provider as it appears on our dashboard.
   * @param configurationData Json configuration data for AdProvider
   * @param platformID Indicates the platform to which this AdProvider belongs (e.g. iOS or Android).
   * @param callbackUrlFormat General format for reward callback URL
   * @param callbackUrlDescription The tooltip description for the Callback URL field.
   * @param configurable determines if the WaterfallAdProviders which belong to this AdProvider can have their eCPM edited.
   * @param defaultEcpm the starting cpm value for a newly created WaterfallAdProvider.
   * @return ID of newly created record
   */
  def create(name: String,
             displayName: String,
             configurationData: String,
             platformID: Long,
             callbackUrlFormat: Option[String],
             callbackUrlDescription: String,
             configurable: Boolean = true,
             defaultEcpm: Option[Double] = None): Option[Long] = {
    db.withConnection { implicit connection =>
      SQL(
        """
          INSERT INTO ad_providers (name, display_name, configuration_data, platform_id, callback_url_format, callback_url_description, configurable, default_ecpm)
          VALUES ({name}, {display_name}, CAST({configuration_data} AS json), {platform_id}, {callback_url_format}, {callback_url_description}, {configurable}, {default_ecpm});
        """
      ).on(
          "name" -> name,
          "display_name" -> displayName,
          "configuration_data" -> configurationData,
          "platform_id" -> platformID,
          "callback_url_format" -> callbackUrlFormat,
          "callback_url_description" -> callbackUrlDescription,
          "configurable" -> configurable,
          "default_ecpm" -> defaultEcpm
        ).executeInsert()
    }
  }
}