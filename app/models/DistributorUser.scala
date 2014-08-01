package models

import anorm._
import anorm.SqlParser._
import com.github.t3hnar.bcrypt._
import play.api.db.DB
import play.api.Play.current

case class DistributorUser (id:Pk[Long], email:String, hashed_password:String) {

  def setPassword(password: String) {
    val salt = generateSalt
    SQL("UPDATE DistributorUser SET ('hashed_password') VALUE ('{hash}') WHERE id = {id}")
      .on("hash" -> password.bcrypt(salt), "id" -> this.id.get)
  }

}

object DistributorUser {

  def checkPassword(email: String, password: String): Boolean = {
    DistributorUser.findByEmail(email) match {
      case Some(user) =>
        password.isBcrypted(user.hashed_password)
      case None =>
        false
    }
  }

  val userParser: RowParser[DistributorUser] = {
    get[Pk[Long]]("DistributorUser.id") ~
    get[String]("DistributorUser.email") ~
    get[String]("DistributorUser.hashed_password") map {
      case id ~ email ~ hashed_password => DistributorUser(id, email, hashed_password)
    }
  }

  def findByEmail(email: String): Option[DistributorUser] = {
    DB.withConnection { implicit c =>
      val query = SQL(
        """
          SELECT DistributorUser.*
          FROM DistributorUser
          WHERE DistributorUser.email = {email}
        """
      ).on("email" -> email)
      query.as(userParser*) match {
        case List(user) => Some(user)
        case List() => None
      }
    }
  }

  def create(email: String, password: String) = {
    val salt = generateSalt
    val hashedPassword = password.bcrypt(salt)
    findByEmail(email) match {
      case Some(user) => false
      case _          => {
        DB.withConnection { implicit c =>
          SQL(
            """
              INSERT INTO DistributorUser (email, hashed_password)
              VALUES ({email}, {hashed_password});
            """
          ).on("email" -> email, "hashed_password" -> hashedPassword).executeInsert()
        }
      }
    }
  }
}
