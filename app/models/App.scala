package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current

/**
 * Maps to App table in the database.
 * @param id Maps to id column in App table
 * @param active Maps to the active column in App table
 * @param distributorID Maps to distributor_id column in App table
 * @param name Maps to name column in App table
 */
case class App(id: Long, active: Boolean, distributorID: Long, name: String) {}

object App {
  // Used to convert SQL row into an instance of the App class.
  val AppParser: RowParser[App] = {
      get[Long]("App.id") ~
      get[Boolean]("App.active") ~
      get[Long]("App.distributor_id") ~
      get[String]("App.name") map {
      case id ~ active ~ distributor_id ~ name => App(id, active, distributor_id, name)
    }
  }

  /**
   * Retrieves all records from the App table for a particular distributor_id
   * @param distributorID ID of the current Distributor
   * @return List of App instances
   */
  def findAll(distributorID: Long): List[App] = {
    DB.withConnection { implicit c =>
      val query = SQL(
        """
          SELECT App.*
          FROM App
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
    DB.withConnection { implicit c =>
      val query = SQL(
        """
          SELECT App.*
          FROM App
          WHERE id = {app_id};
        """
      ).on("app_id" -> appID)
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
    DB.withConnection { implicit c =>
      SQL(
        """
          UPDATE App
          SET name={name}, active={active}
          WHERE id={id};
        """
      ).on("name" -> app.name, "active" -> app.active, "id" -> app.id).executeUpdate()
    }
  }

  /**
   * Creates a new record in the App table
   * @param distributorID ID of current Distributor
   * @param name Maps to name column in App table
   * @return ID of newly created record
   */
  def create(distributorID: Long, name: String): Option[Long] = {
    DB.withConnection { implicit c =>
      SQL(
        """
          INSERT INTO App (name, distributor_id)
          VALUES ({name}, {distributor_id});
        """
      ).on("name" -> name, "distributor_id" -> distributorID).executeInsert()
    }
  }
}
