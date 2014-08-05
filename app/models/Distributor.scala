package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current

case class Distributor(id: Option[Long], name: String) {}

object Distributor {
  val DistributorParser: RowParser[Distributor] = {
      get[Option[Long]]("Distributor.id") ~
      get[String]("Distributor.name") map {
      case id ~ name => Distributor(id, name)
    }
  }

  def find(id: Long): Option[Distributor] = {
    DB.withConnection { implicit c =>
      val query = SQL(
        """
          SELECT Distributor.*
          FROM Distributor
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
    DB.withConnection { implicit c =>
      SQL(
        """
          INSERT INTO Distributor (name)
          VALUES ({name});
        """
      ).on("name" -> name).executeInsert()
    }
  }
}
