package models

import anorm._
import anorm.SqlParser._
import javax.inject._
import play.api.db.Database
import scala.language.postfixOps

/**
 * Encapsulates information for Distributors.
 * @param id Distributor ID stored in database
 * @param name Distributor name stored in database
 */
case class Distributor(id: Option[Long], name: String) {}

/**
  * Encapsulates functions for Distributors
  * @param db A shared database
  */
@Singleton
class DistributorService @Inject() (db: Database) {
  val DistributorParser: RowParser[Distributor] = {
      get[Option[Long]]("distributors.id") ~
      get[String]("distributors.name") map {
      case id ~ name => Distributor(id, name)
    }
  }

  def find(id: Long): Option[Distributor] = {
    db.withConnection { implicit connection =>
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
    db.withConnection { implicit connection =>
      SQL(
        """
          INSERT INTO distributors (name)
          VALUES ({name});
        """
      ).on("name" -> name).executeInsert()
    }
  }
}
