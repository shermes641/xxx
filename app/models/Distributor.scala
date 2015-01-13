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
 */
case class Distributor(id: Option[Long], name: String) {}

object Distributor {
  val DistributorParser: RowParser[Distributor] = {
      get[Option[Long]]("distributors.id") ~
      get[String]("distributors.name") map {
      case id ~ name => Distributor(id, name)
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

  def create(name: String): Option[Long] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          INSERT INTO distributors (name)
          VALUES ({name});
        """
      ).on("name" -> name).executeInsert()
    }
  }
}
