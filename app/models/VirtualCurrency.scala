package models

import anorm._
import anorm.SqlParser._
import java.sql.Connection
import play.api.db.DB
import play.api.Play.current
import scala.language.postfixOps

/**
 * A currency, specific to an app, which is rewarded to users.
 * @param id Maps to the id field in the virtual_currencies table.
 * @param appID The ID of the app to which the virtual currency belongs.
 * @param name An identifier displayed in the UI.
 * @param exchangeRate The units of virtual currency per $1.
 * @param rewardMin The minimum reward a user can receive.  This is optional.
 * @param rewardMax The maximum reward a user can receive.  This is optional.
 * @param roundUp If true, we will round up the payout calculation to the rewardMin value.
 */
case class VirtualCurrency(id: Long, appID: Long, name: String, exchangeRate: Long, rewardMin: Option[Long], rewardMax: Option[Long], roundUp: Boolean)

object VirtualCurrency {
  // Used to convert SQL row into an instance of the VirtualCurrency class.
  val VirtualCurrencyParser: RowParser[VirtualCurrency] = {
    get[Long]("virtual_currencies.id") ~
    get[Long]("virtual_currencies.app_id") ~
    get[String]("virtual_currencies.name") ~
    get[Long]("virtual_currencies.exchange_rate") ~
    get[Option[Long]]("virtual_currencies.reward_min") ~
    get[Option[Long]]("virtual_currencies.reward_max") ~
    get[Boolean]("virtual_currencies.round_up") map {
      case id ~ app_id ~ name ~ exchange_rate ~ reward_min ~ reward_max ~ round_up => VirtualCurrency(id, app_id, name, exchange_rate, reward_min, reward_max, round_up)
    }
  }

  /**
   * SQL statement for inserting a new record into the virtual_currencies table.
   * @return A SQL statement to be executed by create or createWithTransaction methods.
   */
  def insert(appID: Long, name: String, exchangeRate: Long, rewardMin: Option[Long], rewardMax: Option[Long], roundUp: Option[Boolean]): SimpleSql[Row] = {
    val roundUpVal = roundUp match {
      case Some(boolVal: Boolean) => boolVal
      case None => false
    }
    SQL(
      """
        INSERT INTO virtual_currencies (app_id, name, exchange_rate, reward_min, reward_max, round_up)
        VALUES ({app_id}, {name}, {exchange_rate}, {reward_min}, {reward_max}, {round_up});
      """
    ).on("app_id" -> appID, "name" -> name, "exchange_rate" -> exchangeRate, "reward_min" -> rewardMin, "reward_max" -> rewardMax, "round_up" -> roundUpVal)
  }

  /**
   * Executes SQL from insert method within a database transaction.
   * @param appID ID of the app to which the virtual currency belongs.
   * @param name An identifier displayed in the UI.
   * @param exchangeRate The units of virtual currency per $1.
   * @param rewardMin The minimum reward a user can receive.  This is optional.
   * @param rewardMax The maximum reward a user can receive.  This is optional.
   * @param roundUp If true, we will round up the payout calculation to the rewardMin value.
   * @return
   */
  def createWithTransaction(appID: Long, name: String, exchangeRate: Long, rewardMin: Option[Long], rewardMax: Option[Long], roundUp: Option[Boolean])(implicit connection: Connection): Option[Long] = {
    insert(appID, name, exchangeRate, rewardMin, rewardMax, roundUp).executeInsert()
  }

  /**
   * Creates a new record in the virtual_currencies table.
   * @param appID ID of the app to which the virtual currency belongs.
   * @param name An identifier displayed in the UI.
   * @param exchangeRate The units of virtual currency per $1.
   * @param rewardMin The minimum reward a user can receive.  This is optional.
   * @param rewardMax The maximum reward a user can receive.  This is optional.
   * @param roundUp If true, we will round up the payout calculation to the rewardMin value.
   * @return ID of the new record if insert is successful; otherwise, None.
   */
  def create(appID: Long, name: String, exchangeRate: Long, rewardMin: Option[Long], rewardMax: Option[Long], roundUp: Option[Boolean]): Option[Long] = {
    DB.withConnection { implicit connection =>
      insert(appID, name, exchangeRate, rewardMin, rewardMax, roundUp).executeInsert()
    }
  }

  /**
   * Retrieves virtual currency instance from the database.
   * @param virtualCurrencyID ID of the virtual currency record.
   * @return An instance of VirtualCurrency if one exists; otherwise, None.
   */
  def find(virtualCurrencyID: Long): Option[VirtualCurrency] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT virtual_currencies.*
          FROM virtual_currencies
          WHERE id = {id};
        """
      ).on("id" -> virtualCurrencyID)
      query.as(VirtualCurrencyParser*) match {
        case List(virtualCurrency) => Some(virtualCurrency)
        case _ => None
      }
    }
  }

  /**
   * Retrieves virtual currency instance by App ID.
   * @param appID ID of the app to which the virtual currency belongs.
   * @return An instance of VirtualCurrency if one exists; otherwise, None.
   */
  def findByAppID(appID: Long): Option[VirtualCurrency] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT virtual_currencies.*
          FROM virtual_currencies
          WHERE app_id = {app_id};
        """
      ).on("app_id" -> appID)
      query.as(VirtualCurrencyParser*) match {
        case List(virtualCurrency) => Some(virtualCurrency)
        case _ => None
      }
    }
  }

  /**
   * Updates a record in the virtual_currencies table.
   * @param virtualCurrency An instance of the VirtualCurrency class with update values.
   * @return The number of rows updated.
   */
  def update(virtualCurrency: VirtualCurrency): Int = {
    DB.withConnection { implicit connection =>
      updateSQL(virtualCurrency).executeUpdate()
    }
  }

  def updateSQL(virtualCurrency: VirtualCurrency): SimpleSql[Row] = {
    SQL(
      """
          UPDATE virtual_currencies
          SET app_id={app_id}, name={name}, exchange_rate={exchange_rate}, reward_min={reward_min}, reward_max={reward_max}, round_up={round_up}
          WHERE id={id};
      """
    ).on("app_id" -> virtualCurrency.appID, "name" -> virtualCurrency.name, "exchange_rate" -> virtualCurrency.exchangeRate,
        "reward_min" -> virtualCurrency.rewardMin, "reward_max" -> virtualCurrency.rewardMax, "round_up" -> virtualCurrency.roundUp, "id" -> virtualCurrency.id)
  }

  def updateWithTransaction(virtualCurrency: VirtualCurrency)(implicit connection: Connection): Int = {
    updateSQL(virtualCurrency).executeUpdate()
  }
}
