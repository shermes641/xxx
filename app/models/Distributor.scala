package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current
import scala.language.postfixOps

/**
 * Encapsulates information for Distributors.
 * @param id Distributor ID stored in database
 * @param name Distributor name stored in database
 * @param hyprMarketplaceID HyprMarketplace ID
 */
case class Distributor(id: Option[Long], name: String, hyprMarketplaceID: Option[Long]) {}

object Distributor {
  val DistributorParser: RowParser[Distributor] = {
      get[Option[Long]]("distributors.id") ~
      get[String]("distributors.name") ~
      get[Option[Long]]("distributor_users.hypr_marketplace_id") map {
      case id ~ name ~ hypr_marketplace_id => Distributor(id, name, hypr_marketplace_id)
    }
  }

  def find(id: Long): Option[Distributor] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT distributors.*
          FROM distributors
          WHERE id = {id}
        """
      ).on("id" -> id)
      query.as(DistributorParser*) match {
        case List(distributor) => Some(distributor)
        case List() => None
      }
    }
  }

  def create(name: String) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          INSERT INTO distributors (name)
          VALUES ({name});
        """
      ).on("name" -> name).executeInsert()
    }
  }

  /**
   * Sets Distributor HyprMarketplace ID
   * @param distributor Instance of Distributor class.
   * @param hyprMarketplaceID ID to set
   * @return Number of rows successfully updated.
   */
  def setHyprMarketplaceID(distributor: Distributor, hyprMarketplaceID: Int) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          UPDATE distributor_users
          SET hypr_marketplace_id={hypr_marketplace_id}
          WHERE id = {id};
        """
      ).on("id" -> distributor.id, "hypr_marketplace_id" -> hyprMarketplaceID).executeUpdate()
    }
  }
}
