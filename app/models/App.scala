package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current
import java.sql.Connection
import scala.language.postfixOps

/**
 * Maps to App table in the database.
 * @param id Maps to id column in App table
 * @param active Maps to the active column in App table
 * @param distributorID Maps to distributor_id column in App table
 * @param name Maps to name column in App table
 */
case class App(id: Long, active: Boolean, distributorID: Long, name: String) {}

/**
 * Maps to App table in the database.
 * @param id Maps to id column in App table
 * @param active Maps to the active column in App table
 * @param distributorID Maps to distributor_id column in App table
 * @param waterfallID ID of the Waterfall to which the app belongs
 * @param name Maps to name column in App table
 */
case class AppWithWaterfallID(id: Long, active: Boolean, distributorID: Long, name: String, waterfallID: Long)

/**
 * Encapsulates app and virtual currency information for a particular app.
 * @param currencyID Maps to the id field in the virtual_currencies table.
 * @param active Maps to the active field in the apps table.
 * @param appName Maps to the name field in the apps table
 * @param currencyName Maps to the name field in the virtual_currencies table.
 * @param exchangeRate Maps the the exchange_rate field in the virtual_currencies table.
 * @param rewardMin Maps to the reward_min field in the virtual_currencies table.
 * @param rewardMax Maps to the reward_max field in the virtual_currencies table.
 * @param roundUp Maps to the round_up field in the virtual_currencies table.
 */
case class AppWithVirtualCurrency(currencyID: Long, active: Boolean, appName: String, currencyName: String, exchangeRate: Long, rewardMin: Option[Long], rewardMax: Option[Long], roundUp: Boolean)

object App {
  // Used to convert SQL row into an instance of the App class.
  val AppParser: RowParser[App] = {
      get[Long]("apps.id") ~
      get[Boolean]("apps.active") ~
      get[Long]("apps.distributor_id") ~
      get[String]("apps.name") map {
      case id ~ active ~ distributor_id ~ name => App(id, active, distributor_id, name)
    }
  }

  // Used to convert SQL row into an instance of the App class.
  val AppsWithWaterfallsParser: RowParser[AppWithWaterfallID] = {
    get[Long]("apps.id") ~
    get[Boolean]("apps.active") ~
    get[Long]("apps.distributor_id") ~
    get[String]("apps.name") ~
    get[Long]("waterfall_id") map {
      case id ~ active ~ distributor_id ~ name ~ waterfall_id => AppWithWaterfallID(id, active, distributor_id, name, waterfall_id)
    }
  }

  // Used to convert SQL row into an instance of the AppWithVirtualCurrency class.
  val AppsWithVirtualCurrencyParser: RowParser[AppWithVirtualCurrency] = {
    get[Long]("virtual_currencies.id") ~
    get[Boolean]("apps.active") ~
    get[String]("apps.name") ~
    get[String]("virtual_currencies.name") ~
    get[Long]("virtual_currencies.exchange_rate") ~
    get[Option[Long]]("virtual_currencies.reward_min") ~
    get[Option[Long]]("virtual_currencies.reward_max") ~
    get[Boolean]("virtual_currencies.round_up") map {
      case currencyID ~ active ~ appName ~ currencyName ~ exchangeRate ~ rewardMin ~ rewardMax ~ roundUp => AppWithVirtualCurrency(currencyID, active, appName, currencyName, exchangeRate, rewardMin, rewardMax, roundUp)
    }
  }

  /**
   * Finds all editable app and virtual currency information for a particular app's ID.
   * @param appID The ID of the app to be edited.
   * @return An instance of the AppWithVirtualCurrency class if the app ID is found; otherwise, None.
   */
  def findAppWithVirtualCurrency(appID: Long): Option[AppWithVirtualCurrency] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT apps.active, apps.name, vc.id, vc.name, vc.exchange_rate, vc.reward_min, vc.reward_max, vc.round_up
          FROM apps
          JOIN virtual_currencies vc ON vc.app_id = apps.id
          WHERE apps.id = {app_id};
        """
      ).on("app_id" -> appID)
      query.as(AppsWithVirtualCurrencyParser*) match {
        case List(appInfo) => Some(appInfo)
        case _ => None
      }
    }
  }

  /**
   * Retrieves all records from the App table for a particular distributor_id
   * @param distributorID ID of the current Distributor
   * @return List of App instances
   */
  def findAllAppsWithWaterfalls(distributorID: Long): List[AppWithWaterfallID] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT apps.*, waterfalls.id as waterfall_id
          FROM apps
          JOIN waterfalls ON waterfalls.app_id = apps.id
          WHERE distributor_id = {distributor_id};
        """
      ).on("distributor_id" -> distributorID)
      query.as(AppsWithWaterfallsParser*).toList
    }
  }

  /**
   * Retrieves all records from the App table for a particular distributor_id
   * @param distributorID ID of the current Distributor
   * @return List of App instances
   */
  def findAppWithWaterfalls(appID: Long): Option[AppWithWaterfallID] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT apps.*, waterfalls.id as waterfall_id
          FROM apps
          JOIN waterfalls ON waterfalls.app_id = apps.id
          WHERE apps.id = {app_id};
        """
      ).on("app_id" -> appID)
      query.as(AppsWithWaterfallsParser*) match {
        case List(app) => Some(app)
        case _ => None
      }
    }
  }

  /**
   * Retrieves all records from the App table for a particular distributor_id
   * @param distributorID ID of the current Distributor
   * @return List of App instances
   */
  def findAll(distributorID: Long): List[App] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT apps.*
          FROM apps
          WHERE distributor_id = {distributor_id};
        """
      ).on("distributor_id" -> distributorID)
      query.as(AppParser*).toList
    }
  }

  /**
   * Finds a record in the App table by ID
   * @param appID ID of current App
   * @return App instance
   */
  def find(appID: Long): Option[App] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT apps.*
          FROM apps
          WHERE id = {id};
        """
      ).on("id" -> appID)
      query.as(AppParser*) match {
        case List(app) => Some(app)
        case _ => None
      }
    }
  }

  /**
   * Updates the fields for a particular record in App table.
   * @param app App instance with updated attributes
   * @return Number of rows updated
   */
  def update(app: App): Int = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          UPDATE apps
          SET name={name}, active={active}
          WHERE id={id};
        """
      ).on("name" -> app.name, "active" -> app.active, "id" -> app.id).executeUpdate()
    }
  }

  /**
   * SQL statement for inserting a new record into the apps table.
   * @param distributorID ID of current Distributor
   * @param name Maps to name column in the apps table
   * @return A SQL statement to be executed by create or createWithTransaction methods.
   */
  def insert(distributorID: Long, name: String): SimpleSql[Row] = {
    SQL(
      """
      INSERT INTO apps (name, distributor_id)
      VALUES ({name}, {distributor_id});
      """
    ).on("name" -> name, "distributor_id" -> distributorID)
  }

  /**
   * Creates a new record in the App table
   * @param distributorID ID of current Distributor
   * @param name Maps to name column in the apps table
   * @return ID of newly created record
   */
  def create(distributorID: Long, name: String): Option[Long] = {
    DB.withConnection{ implicit connection =>
      insert(distributorID, name).executeInsert()
    }
  }

  /**
   * Executes SQL from insert method within a database transaction.
   * @param distributorID ID of current Distributor
   * @param name Maps to name column in the apps table
   * @param connection Database transaction
   * @return ID of newly created record
   */
  def createWithTransaction(distributorID: Long, name: String)(implicit connection: Connection): Option[Long] = {
    insert(distributorID, name).executeInsert()
  }
}
