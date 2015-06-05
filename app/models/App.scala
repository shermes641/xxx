package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Logger
import play.api.Play.current
import java.sql.Connection
import scala.language.postfixOps

/**
 * Maps to the apps table in the database.
 * @param id Maps to the id column in the apps table
 * @param active Maps to the active column in apps table
 * @param distributorID Maps to the distributor_id column in the apps table
 * @param name Maps to the name column in the apps table
 * @param callbackURL Maps to the callback_url column in the apps table
 * @param serverToServerEnabled Maps to the server_to_server_enabled column in the apps table
 * @param token The unique identifier for an App.  This is used for API calls.
 */
case class App(id: Long, active: Boolean, distributorID: Long, name: String, callbackURL: Option[String], serverToServerEnabled: Boolean, token: String)

/**
 * Maps to the apps table in the database.
 * @param id Maps to the id column in the apps table
 * @param active Maps to the active column in apps table
 * @param distributorID Maps to the distributor_id column in the apps table
 * @param name Maps to the name column in the apps table
 * @param callbackURL Maps to the callback_url column in the apps table
 * @param serverToServerEnabled Maps to the server_to_server_enabled column in the apps table
 */
case class UpdatableApp(id: Long, active: Boolean, distributorID: Long, name: String, callbackURL: Option[String], serverToServerEnabled: Boolean)

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
 * @param callbackURL Maps to the callback_url field in the apps table
 * @param serverToServerEnabled Maps to the server_to_server_enabled column in the apps table
 * @param currencyName Maps to the name field in the virtual_currencies table.
 * @param exchangeRate Maps the the exchange_rate field in the virtual_currencies table.
 * @param rewardMin Maps to the reward_min field in the virtual_currencies table.
 * @param rewardMax Maps to the reward_max field in the virtual_currencies table.
 * @param roundUp Maps to the round_up field in the virtual_currencies table.
 * @param generationNumber A number identifying the current AppConfig state.
 */
case class AppWithVirtualCurrency(currencyID: Long, active: Boolean, appName: String, callbackURL: Option[String], serverToServerEnabled: Boolean, currencyName: String, exchangeRate: Long, rewardMin: Long, rewardMax: Option[Long], roundUp: Boolean, generationNumber: Option[Long])

object App {
  // Used to convert SQL row into an instance of the App class.
  val AppParser: RowParser[App] = {
      get[Long]("apps.id") ~
      get[Boolean]("apps.active") ~
      get[Long]("apps.distributor_id") ~
      get[String]("apps.name") ~
      get[Option[String]]("apps.callback_url") ~
      get[Boolean]("apps.server_to_server_enabled") ~
      get[String]("apps.token") map {
      case id ~ active ~ distributor_id ~ name ~ callback_url ~ server_to_server_enabled ~ token => App(id, active, distributor_id, name, callback_url, server_to_server_enabled, token)
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
    get[Option[String]]("apps.callback_url") ~
    get[Boolean]("apps.server_to_server_enabled") ~
    get[String]("virtual_currencies.name") ~
    get[Long]("virtual_currencies.exchange_rate") ~
    get[Long]("virtual_currencies.reward_min") ~
    get[Option[Long]]("virtual_currencies.reward_max") ~
    get[Boolean]("virtual_currencies.round_up") ~
    get[Option[Long]]("generation_number") map {
      case currencyID ~ active ~ appName ~ callbackURL ~ serverToServerEnabled ~ currencyName ~ exchangeRate ~ rewardMin ~ rewardMax ~ roundUp  ~ generationNumber => AppWithVirtualCurrency(currencyID, active, appName, callbackURL, serverToServerEnabled, currencyName, exchangeRate, rewardMin, rewardMax, roundUp, generationNumber)
    }
  }

  /**
   * Finds all editable app and virtual currency information for a particular app's ID.
   * @param appID The ID of the app to be edited.
   * @param distributorID ID of the Distributor to which the App belongs.
   * @return An instance of the AppWithVirtualCurrency class if the app ID is found; otherwise, None.
   */
  def findAppWithVirtualCurrency(appID: Long, distributorID: Long): Option[AppWithVirtualCurrency] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT apps.active, apps.name, apps.callback_url, apps.server_to_server_enabled, vc.id, vc.name, vc.exchange_rate, vc.reward_min, vc.reward_max, vc.round_up, generation_number
          FROM apps
          JOIN virtual_currencies vc ON vc.app_id = apps.id
          JOIN app_configs ON app_configs.app_id = apps.id
          WHERE apps.id = {app_id} AND apps.distributor_id = {distributor_id}
          ORDER BY generation_number DESC
          LIMIT 1;
        """
      ).on("app_id" -> appID, "distributor_id" -> distributorID)
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
   * Retrieves all records from the apps table along with Waterfall ID for a particular App ID.
   * @param appID The ID of the App to which the Waterfall belongs.
   * @param distributorID The ID of the Distributor to which the Waterfall belongs.
   * @return List of App instances
   */
  def findAppWithWaterfalls(appID: Long, distributorID: Long): Option[AppWithWaterfallID] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT apps.*, waterfalls.id as waterfall_id
          FROM apps
          JOIN waterfalls ON waterfalls.app_id = apps.id
          WHERE apps.id = {app_id} AND apps.distributor_id = {distributor_id};
        """
      ).on("app_id" -> appID, "distributor_id" -> distributorID)
      query.as(AppsWithWaterfallsParser*) match {
        case List(app) => Some(app)
        case _ => None
      }
    }
  }

  /**
   * Retrieves all records from the apps table for a particular Distributor ID.
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
   * SQL to retrieve an App from the database by ID.
   * @param appID The ID of the App to be selected.
   * @return SQL to be executed by find and findWithTransaction.
   */
  def findSQL(appID: Long): SimpleSql[Row] = {
    SQL(
      """
          SELECT apps.*
          FROM apps
          WHERE id = {id};
      """
    ).on("id" -> appID)
  }

  /**
   * Finds a record in the apps table by ID
   * @param appID ID of current App
   * @return App instance if one exists; otherwise, None.
   */
  def find(appID: Long): Option[App] = {
    DB.withConnection { implicit connection =>
      findSQL(appID).as(AppParser*) match {
        case List(app) => Some(app)
        case _ => None
      }
    }
  }

  /**
   * Within a transaction, finds a record in the apps table by ID
   * @param appID ID of current App
   * @return App instance if one exists; otherwise, None.
   */
  def findWithTransaction(appID: Long)(implicit connection: Connection): Option[App] = {
    findSQL(appID).as(AppParser*) match {
      case List(app) => Some(app)
      case _ => None
    }
  }

  /**
   * Finds the App which owns the Waterfall ID.
   * @param waterfallID The ID of the Waterfall owned by the App.
   * @return An instance of the App class, if one exists; otherwise, None.
   */
  def findByWaterfallID(waterfallID: Long): Option[App] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT apps.*
          FROM waterfalls
          JOIN apps ON apps.id = waterfalls.app_id
          WHERE waterfalls.id = {id};
        """
      ).on("id" -> waterfallID)
      query.as(AppParser*) match {
        case List(app) => Some(app)
        case _ => None
      }
    }
  }

  /**
   * SQL statement that updates the fields for a particular record in apps table.
   * @param app UpdatableApp instance with updated attributes
   * @return SQL to be executed by update and updateWithTransaction methods.
   */
  def updateSQL(app: UpdatableApp): SimpleSql[Row] = {
    SQL(
      """
          UPDATE apps
          SET name={name}, active={active}, callback_url={callback_url}, server_to_server_enabled={server_to_server_enabled}
          WHERE id={id};
      """
    ).on("name" -> app.name, "active" -> app.active, "callback_url" -> app.callbackURL, "server_to_server_enabled" -> app.serverToServerEnabled, "id" -> app.id)
  }

  /**
   * Updates the fields for a particular record in apps table.
   * @param app UpdatableApp instance with updated attributes
   * @return Number of rows updated
   */
  def update(app: UpdatableApp): Int = {
    DB.withConnection { implicit connection =>
      updateSQL(app).executeUpdate()
    }
  }

  /**
   * Updates the fields, within a transaction, for a particular record in apps table.
   * @param app UpdatableApp instance with updated attributes
   * @return Number of rows updated
   */
  def updateWithTransaction(app: UpdatableApp)(implicit connection: Connection): Int = {
    updateSQL(app).executeUpdate()
  }

  /**
   * Checks database for an existing enabled App name for a particular Distributor.
   * @param appName The potential name of a new App.
   * @param distributorID The Distributor who will own the new App.
   */
  def nameExists(appName: String, distributorID: Long, appID: Option[Long] = None): Option[Long] = {
    DB.withConnection { implicit connection =>
      val appNameInfo: List[(Long, String)] = SQL(
        """
          SELECT id, name FROM apps
          WHERE distributor_id = {distributor_id} AND active = true;
        """
      ).on("distributor_id" -> distributorID)().map(row => (row[Long]("id"), row[String]("name"))).toList
      appNameInfo.find(info => info._2.toLowerCase == appName.toLowerCase) match {
        case None => None
        case Some(info) if(info._1 != appID.getOrElse(None)) => Some(info._1)
        case _ => None
      }
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
      INSERT INTO apps (name, distributor_id, token)
      VALUES ({name}, {distributor_id}, uuid_generate_v4());
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

  /**
   * Helper method to update the appConfigRefreshInterval for an App and create a new generation in the app_configs table.
   * @param appID The ID of the App to be updated.
   * @param appConfigRefreshInterval The new TTL value in seconds.
   * @return Prints a success or error message to the console depending on the success or error of the update.
   */
  def updateAppConfigRefreshInterval(appID: Long, appConfigRefreshInterval: Long): Boolean = {
    def logFailure(failureMessage: String = "App was not updated.")(implicit connection: Connection): Boolean = {
      connection.rollback()
      Logger.error(failureMessage)
      false
    }

    App.find(appID) match {
      case Some(app) => {
        DB.withTransaction { implicit connection =>
          try {
            val update = SQL(
              """
                UPDATE apps
                SET app_config_refresh_interval={app_config_refresh_interval}
                WHERE id={id};
              """
            ).on("app_config_refresh_interval" -> appConfigRefreshInterval, "id" -> appID).executeUpdate()
            val appConfig = AppConfig.findLatestWithTransaction(app.token)
            (update, appConfig) match {
              case (1, Some(config)) =>  {
                AppConfig.create(app.id, app.token, config.generationNumber) match {
                  case Some(newGenerationNumber) if (newGenerationNumber == config.generationNumber + 1) => {
                    Logger.debug("App was updated successfully!")
                    true
                  }
                  case _ => logFailure("App was not updated because the app config has not changed.  Check if the waterfall for this app is in Test mode.")
                }
              }
              case (_, _) => logFailure()
            }
          } catch {
            case error: org.postgresql.util.PSQLException => logFailure()
            case error: IllegalArgumentException => logFailure()
          }
        }
      }
      case None => {
        Logger.error("App could not be found.")
        false
      }
    }
  }
}
