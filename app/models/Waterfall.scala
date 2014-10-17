package models

import java.sql.Connection

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current
import controllers.ConfigInfo
import play.api.libs.json.{JsValue}
import scala.language.postfixOps

case class Waterfall(id: Long, app_id: Long, name: String, token: String, optimizedOrder: Boolean, testMode: Boolean)

object Waterfall extends JsonConversion {
  // Used to convert SQL row into an instance of the Waterfall class.
  val waterfallParser: RowParser[Waterfall] = {
    get[Long]("waterfalls.id") ~
    get[Long]("app_id") ~
    get[String]("name") ~
    get[String]("token") ~
    get[Boolean]("optimized_order") ~
    get[Boolean]("test_mode") map {
      case id ~ app_id ~ name  ~ token ~ optimized_order ~ test_mode => Waterfall(id, app_id, name, token, optimized_order, test_mode)
    }
  }

  /**
   * SQL statement for inserting a new record into the waterfalls table.
   * @param appID ID of the App to which the new Waterfall belongs
   * @param name Name of the new Waterfall
   * @return A SQL statement to be executed by create or createWithTransaction methods.
   */
  def insert(appID: Long, name: String): SimpleSql[Row] = {
    SQL(
      """
        INSERT INTO waterfalls (app_id, name, token)
        VALUES ({app_id}, {name}, {token});
      """
    ).on("app_id" -> appID, "name" -> name, "token" -> generateToken)
  }

  /**
   * Creates a new Waterfall record in the database.
   * @param appID ID of the App to which the new Waterfall belongs
   * @param name Name of the new Waterfall
   * @return ID of new record if insert is successful, otherwise None.
   */
  def create(appID: Long, name: String): Option[Long] = {
    DB.withConnection { implicit connection =>
      insert(appID, name).executeInsert()
    }
  }

  /**
   * Executes SQL from insert method within a database transaction.
   * @param appID ID of the App to which the new Waterfall belongs
   * @param name Name of the new Waterfall
   * @param connection Database transaction
   * @return ID of new record if insert is successful, otherwise None.
   */
  def createWithTransaction(appID: Long, name: String)(implicit connection: Connection): Option[Long] = {
    insert(appID, name).executeInsert()
  }

  /**
   * Updates the fields for a particular record in waterfalls table.
   * @param id ID field of the waterfall to be updated.
   * @param optimizedOrder Boolean value which determines if the waterfall should always be ordered by eCPM or not.
   * @param testMode Boolean value which determines if the waterfall is live or not.
   * @return Number of rows updated
   */
  def update(id: Long, optimizedOrder: Boolean, testMode: Boolean): Int = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          UPDATE waterfalls
          SET optimized_order={optimized_order}, test_mode={test_mode}
          WHERE id={id};
        """
      ).on("optimized_order" -> optimizedOrder, "test_mode" -> testMode, "id" -> id).executeUpdate()
    }
  }

  /**
   * Finds a record in the Waterfall table by ID.
   * @param waterfallID ID of current Waterfall
   * @return Waterfall instance
   */
  def find(waterfallID: Long): Option[Waterfall] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT waterfalls.*
          FROM waterfalls
          WHERE id={id};
        """
      ).on("id" -> waterfallID)
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
          SELECT waterfalls.*
          FROM waterfalls
          WHERE app_id={app_id};
        """
      ).on("app_id" -> appID)
      query.as(waterfallParser*).toList
    }
  }

  /**
   * Retrieves the order of ad providers with their respective configuration data.
   * @param token API token used to authenticate a request and find a particular waterfall.
   * @return A list containing instances of AdProviderInfo if any active WaterfallAdProviders exist.
   */
  def order(token: String): List[AdProviderInfo] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT ap.name, wap.configuration_data, wap.cpm, vc.name as vc_name, vc.exchange_rate, vc.reward_min, vc.reward_max, vc.round_up, w.test_mode, w.optimized_order, wap.active
          FROM waterfalls w
          FULL OUTER JOIN waterfall_ad_providers wap on wap.waterfall_id = w.id
          FULL OUTER JOIN ad_providers ap on ap.id = wap.ad_provider_id
          FULL OUTER JOIN virtual_currencies vc on vc.app_id = w.app_id
          WHERE w.token={token}
          ORDER BY wap.waterfall_order ASC
        """
      ).on("token" -> token)
      query.as(adProviderParser*).toList
    }
  }

  // Used to convert SQL row into an instance of the AdProviderInfo class in Waterfall.order.
  val adProviderParser: RowParser[AdProviderInfo] = {
    get[Option[String]]("name") ~
    get[Option[JsValue]]("configuration_data") ~
    get[Option[Double]]("cpm") ~
    get[Option[String]]("vc_name") ~
    get[Option[Long]]("exchange_rate") ~
    get[Option[Long]]("reward_min") ~
    get[Option[Long]]("reward_max") ~
    get[Option[Boolean]]("round_up") ~
    get[Boolean]("test_mode") ~
    get[Boolean]("optimized_order") ~
    get[Option[Boolean]]("active") map {
      case name ~ configuration_data ~ cpm ~ vc_name ~ exchange_rate ~ reward_min ~ reward_max ~ round_up ~ test_mode ~ optimized_order ~ active => AdProviderInfo(name, configuration_data, cpm, vc_name, exchange_rate, reward_min, reward_max, round_up, test_mode, optimized_order, active)
    }
  }

  /**
   * Encapsulates necessary information returned from SQL query in Waterfall.order.
   * @param providerName Maps to the name field in the ad_providers table.
   * @param configurationData Maps to the configuration_data field in the waterfall_ad_providers table.
   * @param cpm Maps to the cpm field of waterfall_ad_providers table.
   * @param virtualCurrencyName Maps to the name field in the virtual_currencies table.
   * @param exchangeRate Maps to the exchange_rate field of the virtual_currencies table.
   * @param rewardMin Maps to the reward_min field of the virtual_currencies table.
   * @param rewardMax Maps to the reward_max field of the virtual_currencies table.
   * @param roundUp Maps to the round_up field of the virtual_currencies table.
   * @param testMode Determines if a waterfall is live or not.
   * @param optimizedOrder Determines if the waterfall_ad_providers should be sorted by cpm or not.
   * @param active Determines if a waterfall_ad_provider record should be included in the waterfall order.
   */
  case class AdProviderInfo(providerName: Option[String], configurationData: Option[JsValue], cpm: Option[Double], virtualCurrencyName: Option[String], exchangeRate: Option[Long], rewardMin: Option[Long], rewardMax: Option[Long], roundUp: Option[Boolean], testMode: Boolean, optimizedOrder: Boolean, active: Option[Boolean]) {
    lazy val meetsRewardThreshold: Boolean = {
      (roundUp, cpm, rewardMin) match {
        case (Some(roundUpValue: Boolean), _, _) if(roundUpValue) => true
        case (Some(roundUpValue: Boolean), Some(cpmVal: Double), Some(minReward: Long)) if(!roundUpValue) => {
          val rewardAmount = minReward / exchangeRate.get
          (cpmVal / 1000) >= rewardAmount
        }
        // If there is no cpm value for an ad provider and the virtual currency does not roundUp, this will ensure it is excluded from the waterfall.
        case (Some(roundUpValue: Boolean), None, _) if(!roundUpValue) => false
        case (_, _, _) => true
      }
    }
  }

  /**
   * Updates WaterfallAdProvider records according to the configuration in the Waterfall edit view.
   * @param waterfallID ID of the Waterfall to which all WaterfallAdProviders belong.
   * @param adProviderConfigList List of attributes to update for each WaterfallAdProvider.
   * @return True if the update is successful; otherwise, false.
   */
  def reconfigureAdProviders(waterfallID: Long, adProviderConfigList: List[ConfigInfo]): Boolean = {
    var successful = true
    adProviderConfigList.map { adProviderConfig =>
      if(adProviderConfig.active && adProviderConfig.newRecord) {
        // If a Distributor wants to add a new AdProvider to the current waterfall, create a new WaterfallAdProvider record.
        WaterfallAdProvider.create(waterfallID, adProviderConfig.id, Some(adProviderConfig.waterfallOrder)) match {
          case Some(id) => {}
          case None => successful = false
        }
      } else if(!adProviderConfig.newRecord) {
        //  Otherwise, find and update the existing WaterfallAdProvider record.
        WaterfallAdProvider.find(adProviderConfig.id) match {
          case Some(record) => {
            val newOrder = if(adProviderConfig.active) Some(adProviderConfig.waterfallOrder) else None
            val updatedValues = new WaterfallAdProvider(record.id, record.waterfallID, record.adProviderID, newOrder, record.cpm, Some(adProviderConfig.active), record.fillRate, record.configurationData, record.reportingActive)
            WaterfallAdProvider.update(updatedValues)
          }
          case _ => {
            successful = false
          }
        }
      }
    }
    successful
  }

  /**
   * Generates token field for waterfall.  This is called once on waterfall creation.
   * @return Random string to be saved as token.
   */
  def generateToken = {
    java.util.UUID.randomUUID.toString
  }
}
