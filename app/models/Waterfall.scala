package models

import anorm._
import anorm.SqlParser._
import controllers.ConfigInfo
import java.sql.Connection
import play.api.db.DB
import play.api.libs.json._
import play.api.Play.current
import scala.language.postfixOps

/**
 * Encapsulates information for the waterfalls table.
 * @param id Maps to the id field in the waterfalls table.
 * @param app_id The ID of the App to which the Waterfall belongs.
 * @param name A non-unique identifier for the Waterfall displayed on the Waterfall edit page.
 * @param token A unique identifier used for looking up Waterfall information in the APIController.
 * @param optimizedOrder A Boolean value indicating if the WaterfallAdProviders associated with a Waterfall should be ordered by eCPM descending.
 * @param testMode A Boolean value indicating if the Waterfall will always show a test video or not.
 * @param appName The name of the App to which the Waterfall belongs.
 * @param appToken A unique identifier used to identify an App in API calls.
 * @param generationNumber A number which indicates how many times the Waterfall and associated elements have been edited.  This number is retrieved from the app_configs table.
 */
case class Waterfall(id: Long, app_id: Long, name: String, token: String, optimizedOrder: Boolean, testMode: Boolean, paused: Boolean, appName: String, generationNumber: Option[Long], appToken: String)

object Waterfall extends JsonConversion {
  // Used to convert SQL row into an instance of the Waterfall class.
  val waterfallParser: RowParser[Waterfall] = {
    get[Long]("waterfalls.id") ~
    get[Long]("app_id") ~
    get[String]("name") ~
    get[String]("token") ~
    get[Boolean]("optimized_order") ~
    get[Boolean]("test_mode") ~
    get[Boolean]("paused") ~
    get[String]("app_name") ~
    get[Option[Long]]("generation_number") ~
    get[String]("app_token") map {
      case id ~ app_id ~ name  ~ token ~ optimized_order ~ test_mode ~ paused ~ app_name ~ generation_number ~ app_token => Waterfall(id, app_id, name, token, optimized_order, test_mode, paused, app_name, generation_number, app_token)
    }
  }

  /**
   * Creates a new record in the waterfalls table within a database transaction.
   * @param appID ID of the App to which the new Waterfall belongs
   * @param name Name of the new Waterfall
   * @param connection Database transaction
   * @return ID of new record if insert is successful, otherwise None.
   */
  def create(appID: Long, name: String)(implicit connection: Connection): Option[Long] = {
    SQL(
      """
        INSERT INTO waterfalls (app_id, name, token)
        VALUES ({app_id}, {name}, uuid_generate_v4());
      """
    ).on("app_id" -> appID, "name" -> name).executeInsert()
  }

  /**
   * SQL for updating the fields for a particular record in waterfalls table.
   * @param id ID field of the waterfall to be updated.
   * @param optimizedOrder Boolean value which determines if the waterfall should always be ordered by eCPM or not.
   * @param testMode Boolean value which determines if the waterfall is live or in test mode.
   * @param paused Boolean value which determines if the waterfall is paused or not.
   * @return SQL to be executed by update and updateWithTransaction methods.
   */
  def updateSQL(id: Long, optimizedOrder: Boolean, testMode: Boolean, paused: Boolean): SimpleSql[Row] = {
    SQL(
      """
          UPDATE waterfalls
          SET optimized_order={optimized_order}, test_mode={test_mode}, paused={paused}
          WHERE id={id};
      """
    ).on("optimized_order" -> optimizedOrder, "test_mode" -> testMode, "paused" -> paused, "id" -> id)
  }

  /**
   * Updates the fields for a particular record in waterfalls table.
   * @param id ID field of the waterfall to be updated.
   * @param optimizedOrder Boolean value which determines if the waterfall should always be ordered by eCPM or not.
   * @param testMode Boolean value which determines if the waterfall is live or in test Mode.
   * @param paused Boolean value which determines if the waterfall is paused or not.
   * @return Number of rows updated
   */
  def update(id: Long, optimizedOrder: Boolean, testMode: Boolean, paused: Boolean): Int = {
    DB.withConnection { implicit connection =>
      updateSQL(id, optimizedOrder, testMode, paused).executeUpdate()
    }
  }

  /**
   * Updates the fields, within a transaction, for a particular record in waterfalls table.
   * @param id ID field of the waterfall to be updated.
   * @param optimizedOrder Boolean value which determines if the waterfall should always be ordered by eCPM or not.
   * @param testMode Boolean value which determines if the waterfall is live or in test Mode.
   * @param paused Boolean value which determines if the waterfall is paused or not.
   * @return Number of rows updated
   */
  def updateWithTransaction(id: Long, optimizedOrder: Boolean, testMode: Boolean, paused: Boolean)(implicit connection: Connection): Int = {
    updateSQL(id, optimizedOrder, testMode, paused).executeUpdate()
  }

  /**
   * Finds a record in the Waterfall table by ID.
   * @param waterfallID ID of current Waterfall
   * @param distributorID ID of Distributor to which the Waterfall belongs.
   * @return Waterfall instance
   */
  def find(waterfallID: Long, distributorID: Long): Option[Waterfall] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT waterfalls.*, apps.name as app_name, apps.token as app_token, generation_number
          FROM waterfalls
          JOIN apps ON apps.id = waterfalls.app_id
          JOIN app_configs ON app_configs.app_id = waterfalls.app_id
          WHERE waterfalls.id = {waterfall_id} AND apps.distributor_id = {distributor_id}
          ORDER BY generation_number DESC
          LIMIT 1;
        """
      ).on("waterfall_id" -> waterfallID, "distributor_id" -> distributorID)
      query.as(waterfallParser*) match {
        case List(waterfall) => Some(waterfall)
        case _ => None
      }
    }
  }

  /**
   * Finds a record in the Waterfall table by ID.
   * @param appID ID of current App
   * @return List of Waterfall instances if query is successful.  Otherwise, returns an empty list.
   */
  def findByAppID(appID: Long): List[Waterfall] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT waterfalls.*, apps.name as app_name, apps.token as app_token, generation_number
          FROM waterfalls
          JOIN apps ON apps.id = waterfalls.app_id
          JOIN app_configs ON app_configs.app_id = waterfalls.app_id
          WHERE waterfalls.app_id={app_id}
          ORDER BY generation_number DESC
          LIMIT 1;
        """
      ).on("app_id" -> appID)
      query.as(waterfallParser*).toList
    }
  }

  /**
   * Encapsulates info for App's server to server callback.
   * @param callbackURL The URL which receives the result of the completion attempt.
   * @param serverToServerEnabled Boolean value that determines if we should use the callbackURL.
   */
  case class WaterfallCallbackInfo(callbackURL: Option[String], serverToServerEnabled: Boolean)

  // Used to convert SQL row to an instance of WaterfallCallbackInfo class.
  val waterfallCallbackInfoParser: RowParser[WaterfallCallbackInfo] = {
    get[Option[String]]("callback_url") ~
    get[Boolean]("server_to_server_enabled") map {
      case callback_url ~ server_to_server_enabled => WaterfallCallbackInfo(callback_url, server_to_server_enabled)
    }
  }

  /**
   * Finds the callback information for an app based on the app token.
   * @param appToken Maps to the token field of the apps table.
   * @return An instance of the WaterfallCallbackInfo if the token is found; otherwise, returns None.
   */
  def findCallbackInfo(appToken: String): Option[WaterfallCallbackInfo] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT apps.callback_url, apps.server_to_server_enabled
          FROM waterfalls
          JOIN apps ON apps.id = waterfalls.app_id
          WHERE apps.token={app_token};
        """
      ).on("app_token" -> appToken)
      query.as(waterfallCallbackInfoParser*) match {
        case List(waterfallCallbackInfo) => Some(waterfallCallbackInfo)
        case _ => None
      }
    }
  }

  /**
   * Retrieves the order of ad providers, within a transaction, with their respective configuration data.
   * @param appToken API token used to authenticate a request and find AppConfig information.
   * @return A list containing instances of AdProviderInfo if any active WaterfallAdProviders exist.
   */
  def order(appToken: String)(implicit connection: Connection): List[AdProviderInfo] = {
    val query = SQL(
      """
        SELECT ap.name provider_name, ap.id as provider_id, ap.sdk_blacklist_regex, apps.id as app_id, apps.name as app_name,
        apps.app_config_refresh_interval, distributors.id as distributor_id, distributors.name as distributor_name, wap.configuration_data,
        wap.cpm, vc.name as vc_name, vc.exchange_rate, vc.reward_min, vc.reward_max, vc.round_up, w.test_mode, w.paused, w.optimized_order, wap.active
        FROM waterfalls w
        INNER JOIN waterfall_ad_providers wap on wap.waterfall_id = w.id
        INNER JOIN ad_providers ap on ap.id = wap.ad_provider_id
        INNER JOIN virtual_currencies vc on vc.app_id = w.app_id
        INNER JOIN apps on apps.id = vc.app_id
        INNER JOIN distributors on apps.distributor_id = distributors.id
        WHERE apps.token={app_token}
        ORDER BY wap.waterfall_order ASC
      """
    ).on("app_token" -> appToken)
    query.as(adProviderParser*).toList
  }

  // Used to convert SQL row into an instance of the AdProviderInfo class in Waterfall.order.
  val adProviderParser: RowParser[AdProviderInfo] = {
    get[Option[String]]("provider_name") ~
    get[Option[Long]]("provider_id") ~
    get[Option[String]]("sdk_blacklist_regex") ~
    get[Option[String]]("app_name") ~
    get[Option[Long]]("app_id") ~
    get[Long]("app_config_refresh_interval") ~
    get[Option[String]]("distributor_name") ~
    get[Option[Long]]("distributor_id") ~
    get[Option[JsValue]]("configuration_data") ~
    get[Option[Double]]("cpm") ~
    get[Option[String]]("vc_name") ~
    get[Option[Long]]("exchange_rate") ~
    get[Long]("reward_min") ~
    get[Option[Long]]("reward_max") ~
    get[Option[Boolean]]("round_up") ~
    get[Boolean]("test_mode") ~
    get[Boolean]("paused") ~
    get[Boolean]("optimized_order") ~
    get[Option[Boolean]]("active") map {
      case provider_name ~ provider_id ~ sdk_blacklist_regex ~ app_name ~ app_id ~ app_config_refresh_interval ~ distributor_name ~ distributor_id ~
           configuration_data ~ cpm ~ vc_name ~ exchange_rate ~ reward_min ~ reward_max ~ round_up ~ test_mode ~ paused ~ optimized_order ~ active => {
        AdProviderInfo(provider_name, provider_id, sdk_blacklist_regex, app_name, app_id, app_config_refresh_interval, distributor_name, distributor_id,
                       configuration_data, cpm, vc_name, exchange_rate, reward_min, reward_max, round_up, test_mode, paused, optimized_order, active)
      }
    }
  }

  /**
   * Encapsulates necessary information returned from SQL query in Waterfall.order.
   * @param providerName Maps to the name field in the ad_providers table.
   * @param providerID Maps to the id field in the ad_providers table.
   * @param sdkBlacklistRegex The regex to blacklist Adapter/SDK version combinations per AdProvider.
   * @param appName Maps to the name field in the apps table.
   * @param appID Maps to the id field in the apps table.
   * @param appConfigRefreshInterval Determines the TTL for AppConfigs used by the SDK.
   * @param distributorName Maps to the name field in the distributors table.
   * @param distributorID Maps to the id field in the distributors table.
   * @param configurationData Maps to the configuration_data field in the waterfall_ad_providers table.
   * @param cpm Maps to the cpm field of waterfall_ad_providers table.
   * @param virtualCurrencyName Maps to the name field in the virtual_currencies table.
   * @param exchangeRate Maps to the exchange_rate field of the virtual_currencies table.
   * @param rewardMin Maps to the reward_min field of the virtual_currencies table.
   * @param rewardMax Maps to the reward_max field of the virtual_currencies table.
   * @param roundUp Maps to the round_up field of the virtual_currencies table.
   * @param testMode Determines if a waterfall is in test mode or not.
   * @param paused Determines if a waterfall is paused or not.
   * @param optimizedOrder Determines if the waterfall_ad_providers should be sorted by cpm or not.
   * @param active Determines if a waterfall_ad_provider record should be included in the waterfall order.
   */
  case class AdProviderInfo(providerName: Option[String], providerID: Option[Long], sdkBlacklistRegex: Option[String], appName: Option[String], appID: Option[Long], appConfigRefreshInterval: Long,
                            distributorName: Option[String], distributorID: Option[Long], configurationData: Option[JsValue], cpm: Option[Double], virtualCurrencyName: Option[String],
                            exchangeRate: Option[Long], rewardMin: Long, rewardMax: Option[Long], roundUp: Option[Boolean], testMode: Boolean, paused: Boolean, optimizedOrder: Boolean, active: Option[Boolean]) {
    lazy val meetsRewardThreshold: Boolean = {
      (roundUp, cpm) match {
        case (Some(roundUpValue: Boolean), _) if(roundUpValue) => true
        case (Some(roundUpValue: Boolean), Some(cpmVal: Double)) if(!roundUpValue) => {
          cpmVal * exchangeRate.get >= rewardMin * 1000.0
        }
        // If there is no cpm value for an ad provider and the virtual currency does not roundUp, this will ensure it is excluded from the waterfall.
        case (Some(roundUpValue: Boolean), None) if(!roundUpValue) => false
        case (_, _) => true
      }
    }
  }

  /**
   * Updates WaterfallAdProvider records according to the configuration in the Waterfall edit view.
   * @param waterfallID ID of the Waterfall to which all WaterfallAdProviders belong.
   * @param adProviderConfigList List of attributes to update for each WaterfallAdProvider.
   * @return True if the update is successful; otherwise, false.
   */
  def reconfigureAdProviders(waterfallID: Long, adProviderConfigList: List[ConfigInfo])(implicit connection: Connection): Boolean = {
    var successful = true
    adProviderConfigList.map { adProviderConfig =>
      if(adProviderConfig.active && adProviderConfig.newRecord) {
        // If a Distributor wants to add a new AdProvider to the current waterfall, create a new WaterfallAdProvider record.
        WaterfallAdProvider.createWithTransaction(waterfallID, adProviderConfig.id, Some(adProviderConfig.waterfallOrder), adProviderConfig.cpm, adProviderConfig.configurable) match {
          case Some(id) => {}
          case None => successful = false
        }
      } else if(!adProviderConfig.newRecord) {
        //  Otherwise, find and update the existing WaterfallAdProvider record.
        WaterfallAdProvider.find(adProviderConfig.id) match {
          case Some(record) => {
            val newOrder = if(adProviderConfig.active) Some(adProviderConfig.waterfallOrder) else None
            // In the case that the WaterfallAdProvider's pending status has changed and the user edits the Waterfall without before refreshing the browser, use the most up to date active status from the database.
            val activeStatus: Option[Boolean] = if(adProviderConfig.pending != record.pending) record.active else Some(adProviderConfig.active)
            val updatedValues = new WaterfallAdProvider(record.id, record.waterfallID, record.adProviderID, newOrder, record.cpm, activeStatus, record.fillRate, record.configurationData, record.reportingActive, record.pending)
            WaterfallAdProvider.updateWithTransaction(updatedValues)
          }
          case _ => {
            successful = false
          }
        }
      }
    }
    successful
  }
}
