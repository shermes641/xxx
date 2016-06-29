package models

import anorm._
import anorm.SqlParser._
import javax.inject._
import play.api.db.Database
import play.api.Logger
import java.sql.Connection
import scala.language.postfixOps

/**
  * Maps to the apps table in the database.
  *
  * @param id                       Maps to the id column in the apps table
  * @param active                   Maps to the active column in apps table
  * @param distributorID            Maps to the distributor_id column in the apps table
  * @param name                     Maps to the name column in the apps table
  * @param callbackURL              Maps to the callback_url column in the apps table
  * @param serverToServerEnabled    Maps to the server_to_server_enabled column in the apps table
  * @param appConfigRefreshInterval The amount of time (in seconds) the SDK waits before checking for a new app config.
  * @param token                    The unique identifier for an App.  This is used for API calls.
  * @param platformID               Indicates the platform to which this App belongs (e.g. iOS or Android).
  * @param hmacSecret               The App's shared secret used by the distributor to decode the hmac signature
  *
  */
case class App(id: Long,
               active: Boolean,
               distributorID: Long,
               name: String,
               callbackURL: Option[String],
               serverToServerEnabled: Boolean,
               appConfigRefreshInterval: Long,
               token: String,
               platformID: Long,
               hmacSecret: String)

/**
  * Maps to the apps table in the database.
  *
  * @param id                    Maps to the id column in the apps table
  * @param active                Maps to the active column in apps table
  * @param distributorID         Maps to the distributor_id column in the apps table
  * @param name                  Maps to the name column in the apps table
  * @param callbackURL           Maps to the callback_url column in the apps table
  * @param serverToServerEnabled Maps to the server_to_server_enabled column in the apps table
  */
case class UpdatableApp(id: Long,
                        active: Boolean,
                        distributorID: Long,
                        name: String,
                        callbackURL: Option[String],
                        serverToServerEnabled: Boolean)

/**
  * Maps to App table in the database.
  *
  * @param id            Maps to id column in App table
  * @param active        Maps to the active column in App table
  * @param distributorID Maps to distributor_id column in App table
  * @param name          Maps to name column in App table
  * @param platformID    Indicates the platform to which this App/Waterfall belongs
  * @param platformName  The name of the Platform to which the App/Waterfall belongs (e.g. iOS or Android)
  * @param waterfallID   ID of the Waterfall to which the app belongs
  */
case class AppWithWaterfallID(id: Long,
                              active: Boolean,
                              distributorID: Long,
                              name: String,
                              platformID: Long,
                              platformName: String,
                              waterfallID: Long)

/**
  * Encapsulates app and virtual currency information for a particular app.
  *
  * @param apiToken              The unique string identifier for an app.
  * @param currencyID            Maps to the id field in the virtual_currencies table.
  * @param active                Maps to the active field in the apps table.
  * @param appName               Maps to the name field in the apps table
  * @param callbackURL           Maps to the callback_url field in the apps table
  * @param serverToServerEnabled Maps to the server_to_server_enabled column in the apps table
  * @param platformID            The ID of the Platform to which the App/VirtualCurrency belongs.
  * @param platformName          Indicates the platform to which this App/VirtualCurrency belongs (e.g. iOS or Android).
  * @param currencyName          Maps to the name field in the virtual_currencies table.
  * @param exchangeRate          Maps the the exchange_rate field in the virtual_currencies table.
  * @param rewardMin             Maps to the reward_min field in the virtual_currencies table.
  * @param rewardMax             Maps to the reward_max field in the virtual_currencies table.
  * @param roundUp               Maps to the round_up field in the virtual_currencies table.
  * @param generationNumber      A number identifying the current AppConfig state.
  * @param hmacSecret            The App's shared secret used by the distributor to decode the hmac signature
  */
case class AppWithVirtualCurrency(apiToken: String,
                                  currencyID: Long,
                                  active: Boolean,
                                  appName: String,
                                  callbackURL: Option[String],
                                  serverToServerEnabled: Boolean,
                                  platformID: Long,
                                  platformName: String,
                                  currencyName: String,
                                  exchangeRate: Long,
                                  rewardMin: Long,
                                  rewardMax: Option[Long],
                                  roundUp: Boolean,
                                  generationNumber: Option[Long],
                                  hmacSecret: String)

/**
  * Encapsulates functions for Apps
  * @param appConfigService A shared instance of the AppConfigService class
  * @param db               A shared database
  */
@Singleton
class AppService @Inject() (appConfigService: AppConfigService, db: Database) extends WaterfallFind {
  val database = db

  // Used to convert SQL row into an instance of the App class.
  val AppsWithWaterfallsParser: RowParser[AppWithWaterfallID] = {
    get[Long]("apps.id") ~
      get[Boolean]("apps.active") ~
      get[Long]("apps.distributor_id") ~
      get[String]("apps.name") ~
      get[Long]("apps.platform_id") ~
      get[String]("platforms.name") ~
      get[Long]("waterfall_id") map {
      case id ~ active ~ distributor_id ~ name ~ platform_id ~ platform_name ~ waterfall_id =>
        AppWithWaterfallID(id, active, distributor_id, name, platform_id, platform_name, waterfall_id)
    }
  }

  // Used to convert SQL row into an instance of the AppWithVirtualCurrency class.
  val AppsWithVirtualCurrencyParser: RowParser[AppWithVirtualCurrency] = {
    get[String]("apps.token") ~
      get[Long]("virtual_currencies.id") ~
      get[Boolean]("apps.active") ~
      get[String]("apps.name") ~
      get[Option[String]]("apps.callback_url") ~
      get[Boolean]("apps.server_to_server_enabled") ~
      get[Long]("apps.platform_id") ~
      get[String]("platforms.name") ~
      get[String]("virtual_currencies.name") ~
      get[Long]("virtual_currencies.exchange_rate") ~
      get[Long]("virtual_currencies.reward_min") ~
      get[Option[Long]]("virtual_currencies.reward_max") ~
      get[Boolean]("virtual_currencies.round_up") ~
      get[Option[Long]]("generation_number") ~
      get[String]("apps.hmac_secret") map {
      case apiToken ~ currencyID ~ active ~ appName ~ callbackURL ~ serverToServerEnabled ~ platform_id ~ platform_name ~ currencyName ~ exchangeRate ~ rewardMin ~ rewardMax ~ roundUp ~ generationNumber ~ hmacSecret =>
        AppWithVirtualCurrency(
          apiToken,
          currencyID,
          active,
          appName,
          callbackURL,
          serverToServerEnabled,
          platform_id,
          platform_name,
          currencyName,
          exchangeRate,
          rewardMin,
          rewardMax,
          roundUp,
          generationNumber,
          hmacSecret
        )
    }
  }

  /**
    * Finds all editable app and virtual currency information for a particular app's ID.
    *
    * @param appID         The ID of the app to be edited.
    * @param distributorID ID of the Distributor to which the App belongs.
    * @return An instance of the AppWithVirtualCurrency class if the app ID is found; otherwise, None.
    */
  def findAppWithVirtualCurrency(appID: Long, distributorID: Long): Option[AppWithVirtualCurrency] = {
    db.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT apps.token, apps.active, apps.name, apps.callback_url, apps.server_to_server_enabled, platforms.name,
          apps.platform_id, vc.id, vc.name, vc.exchange_rate, vc.reward_min, vc.reward_max, vc.round_up, generation_number, hmac_secret
          FROM apps
          JOIN virtual_currencies vc ON vc.app_id = apps.id
          JOIN app_configs ON app_configs.app_id = apps.id
          JOIN platforms ON platforms.id = apps.platform_id
          WHERE apps.id = {app_id} AND apps.distributor_id = {distributor_id}
          ORDER BY generation_number DESC
          LIMIT 1;
        """
      ).on("app_id" -> appID, "distributor_id" -> distributorID)
      query.as(AppsWithVirtualCurrencyParser *) match {
        case List(appInfo) => Some(appInfo)
        case _ => None
      }
    }
  }

  /**
    * Retrieves all records from the App table for a particular distributor_id
    *
    * @param distributorID ID of the current Distributor
    * @return List of App instances
    */
  def findAllAppsWithWaterfalls(distributorID: Long): List[AppWithWaterfallID] = {
    db.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT apps.*, platforms.name, waterfalls.id as waterfall_id
          FROM apps
          JOIN platforms ON platforms.id = apps.platform_id
          JOIN waterfalls ON waterfalls.app_id = apps.id
          WHERE distributor_id = {distributor_id};
        """
      ).on("distributor_id" -> distributorID)
      query.as(AppsWithWaterfallsParser *)
    }
  }

  /**
    * Retrieves all records from the apps table along with Waterfall ID for a particular App ID.
    *
    * @param appID         The ID of the App to which the Waterfall belongs.
    * @param distributorID The ID of the Distributor to which the Waterfall belongs.
    * @return List of App instances
    */
  def findAppWithWaterfalls(appID: Long, distributorID: Long): Option[AppWithWaterfallID] = {
    db.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT apps.*, platforms.name, waterfalls.id as waterfall_id
          FROM apps
          JOIN platforms ON platforms.id = apps.platform_id
          JOIN waterfalls ON waterfalls.app_id = apps.id
          WHERE apps.id = {app_id} AND apps.distributor_id = {distributor_id};
        """
      ).on("app_id" -> appID, "distributor_id" -> distributorID)
      query.as(AppsWithWaterfallsParser *) match {
        case List(app) => Some(app)
        case _ => None
      }
    }
  }

  /**
    * Retrieves all records from the apps table for a particular Distributor ID.
    *
    * @param distributorID ID of the current Distributor
    * @return List of App instances
    */
  def findAll(distributorID: Long): List[App] = {
    db.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT apps.*
          FROM apps
          WHERE distributor_id = {distributor_id};
        """
      ).on("distributor_id" -> distributorID)
      query.as(AppParser *)
    }
  }

  /**
   * Finds a list of all apps in the database. This is only used by the Admin UI.
   * @return A list of all apps
   */
  def findAll(): List[App] = {
    db.withConnection { implicit connection =>
      SQL(
        """
        SELECT apps.*
        FROM apps
        ORDER BY created_at DESC;
        """
      ).as(AppParser*)
    }
  }

  /**
    * SQL to retrieve an App from the database by ID.
    *
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
    * SQL to retrieve an hmac secret from the apps table, by App token.
    *
    * @param token The token of the App to be selected.
    * @return None if app not found, otherwise the hmac secret for the App.
    */
  def findHmacSecretByToken(token: String): Option[String] = {
    db.withConnection { implicit connection =>
      val result: List[String] = SQL(""" SELECT hmac_secret as hmacSecret FROM apps WHERE token = {token}; """)
        .on("token" -> token)
        .as(SqlParser.str("hmacSecret").*)
      result.length match {
        case 0 => None
        case _ => Some(result.head)
      }
    }
  }

  /**
    * Finds a record in the apps table by ID
    *
    * @param appID ID of current App
    * @return App instance if one exists; otherwise, None.
    */
  def find(appID: Long): Option[App] = {
    db.withConnection { implicit connection =>
      findSQL(appID).as(AppParser *) match {
        case List(app) => Some(app)
        case _ => None
      }
    }
  }

  /**
    * Within a transaction, finds a record in the apps table by ID
    *
    * @param appID ID of current App
    * @return App instance if one exists; otherwise, None.
    */
  def findWithTransaction(appID: Long)(implicit connection: Connection): Option[App] = {
    findSQL(appID).as(AppParser *) match {
      case List(app) => Some(app)
      case _ => None
    }
  }

  /**
   * SQL statement that updates the fields for a particular record in apps table.
   * @param app UpdatableApp instance with updated attributes
   * @return    SQL to be executed by update and updateWithTransaction methods.
   */
  def updateSQL(app: UpdatableApp): SimpleSql[Row] = {
    SQL(
      """
          UPDATE apps
          SET name={name}, active={active}, callback_url={callback_url}, server_to_server_enabled={server_to_server_enabled}
          WHERE id={id} AND distributor_id={distributor_id};
      """
    ).on(
      "name" -> app.name,
      "active" -> app.active,
      "callback_url" -> app.callbackURL,
      "server_to_server_enabled" -> app.serverToServerEnabled,
      "id" -> app.id,
      "distributor_id" -> app.distributorID)
  }

  /**
    * Updates the fields for a particular record in apps table.
    *
    * @param app UpdatableApp instance with updated attributes
    * @return Number of rows updated
    */
  def update(app: UpdatableApp): Int = {
    db.withConnection { implicit connection =>
      updateSQL(app).executeUpdate()
    }
  }

  /**
    * Updates the fields, within a transaction, for a particular record in apps table.
    *
    * @param app UpdatableApp instance with updated attributes
    * @return Number of rows updated
    */
  def updateWithTransaction(app: UpdatableApp)(implicit connection: Connection): Int = {
    updateSQL(app).executeUpdate()
  }

  /**
    * SQL statement for inserting a new record into the apps table.
    *
    * @param distributorID  ID of current Distributor
    * @param name           Maps to name column in the apps table
    * @param platformID     Indicated the platform to which the App belongs (e.g. iOS or Android).
    * @param cbUrl          The callback URL
    * @return A SQL statement to be executed by create or createWithTransaction methods.
    */
  def insert(distributorID: Long, name: String, platformID: Long, cbUrl: String): SimpleSql[Row] = {
    SQL(
      """
      INSERT INTO apps (name, distributor_id, platform_id, token, callback_url)
      VALUES ({name}, {distributor_id}, {platform_id}, uuid_generate_v4(),
      """ + s"$cbUrl);"
    ).on("name" -> name, "distributor_id" -> distributorID, "platform_id" -> platformID)
  }

  /**
    * Convert string to SQL format
    *
    * @param value value to convert
    * @return NULL or quoted value
    */
  def getNullOrQuotedString(value: Option[String]): String = if (value.isEmpty) "NULL" else s"'${value.get}'"

  /**
    * Creates a new record in the App table
    *
    * @param distributorID  ID of current Distributor
    * @param name           Maps to name column in the apps table
    * @param platformID     Indicated the platform to which the App belongs (e.g. iOS or Android).
    * @param cbUrl          The callback URL
    * @return               ID of newly created record
    */
  def create(distributorID: Long, name: String, platformID: Long, cbUrl: Option[String] = None): Option[Long] = {
    db.withConnection { implicit connection =>
      insert(distributorID, name, platformID, getNullOrQuotedString(cbUrl)).executeInsert()
    }
  }

  /**
    * Executes SQL from insert method within a database transaction.
    *
    * @param distributorID ID of current Distributor
    * @param name          Maps to name column in the apps table
    * @param platformID    Indicated the platform to which the App belongs (e.g. iOS or Android).
    * @param connection    Database transaction
    * @return ID of newly created record
    */
  def createWithTransaction(distributorID: Long, name: String, platformID: Long, cb: Option[String] = None)(implicit connection: Connection): Option[Long] = {
    insert(distributorID, name, platformID, getNullOrQuotedString(cb)).executeInsert()
  }

  /**
    * Helper method to update the appConfigRefreshInterval for an App and create a new generation in the app_configs table.
    *
    * @param appID                    The ID of the App to be updated.
    * @param appConfigRefreshInterval The new TTL value in seconds.
    * @return Prints a success or error message to the console depending on the success or error of the update.
    */
  def updateAppConfigRefreshInterval(appID: Long, appConfigRefreshInterval: Long): Boolean = {
    def logFailure(failureMessage: String = "App was not updated.")(implicit connection: Connection): Boolean = {
      connection.rollback()
      Logger.error(failureMessage)
      false
    }

    find(appID) match {
      case Some(app) =>
        db.withTransaction { implicit connection =>
          try {
            val update = SQL(
              """
                UPDATE apps
                SET app_config_refresh_interval={app_config_refresh_interval}
                WHERE id={id} AND distributor_id={distributor_id};
              """
            ).on("app_config_refresh_interval" -> appConfigRefreshInterval, "id" -> appID, "distributor_id" -> app.distributorID).executeUpdate()
            val appConfig = appConfigService.findLatestWithTransaction(app.token)
            (update, appConfig) match {
              case (1, Some(config)) =>
                appConfigService.create(app.id, app.token, config.generationNumber) match {
                  case Some(newGenerationNumber) if newGenerationNumber == (config.generationNumber + 1) =>
                    Logger.debug("App was updated successfully!")
                    true

                  case _ => logFailure("App was not updated because the app config has not changed.  Check if the waterfall for this app is in Test mode.")
                }

              case (_, _) => logFailure()
            }
          } catch {
            case error: org.postgresql.util.PSQLException => logFailure()
            case error: IllegalArgumentException => logFailure()
          }
        }

      case None =>
        Logger.error("App could not be found.")
        false
    }
  }
}

trait WaterfallFind {
  val database: Database

  // Used to convert SQL row into an instance of the App class.
  val AppParser: RowParser[App] = {
    get[Long]("apps.id") ~
      get[Boolean]("apps.active") ~
      get[Long]("apps.distributor_id") ~
      get[String]("apps.name") ~
      get[Option[String]]("apps.callback_url") ~
      get[Boolean]("apps.server_to_server_enabled") ~
      get[Long]("apps.app_config_refresh_interval") ~
      get[Long]("apps.platform_id") ~
      get[String]("apps.token") ~
      get[String]("apps.hmac_secret") map {
      case id ~ active ~ distributor_id ~ name ~ callback_url ~ server_to_server_enabled ~ app_config_refresh ~ platform_id ~ token ~ hmacSecret =>
        App(id, active, distributor_id, name, callback_url, server_to_server_enabled, app_config_refresh, token, platform_id, hmacSecret)
    }
  }

  /**
   * Finds the App which owns the Waterfall ID.
   * @param waterfallID The ID of the Waterfall owned by the App.
   * @return An instance of the App class, if one exists; otherwise, None.
   */
  def findAppByWaterfallID(waterfallID: Long): Option[App] = {
    database.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT apps.*
          FROM waterfalls
          JOIN apps ON apps.id = waterfalls.app_id
          WHERE waterfalls.id = {id};
        """
      ).on("id" -> waterfallID)
      query.as(AppParser *) match {
        case List(app) => Some(app)
        case _ => None
      }
    }
  }
}
