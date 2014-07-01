package models

import anorm.SqlParser._
import anorm._
import play.api.Play.current

import com.github.t3hnar.bcrypt._
import play.api.db.DB

/**
 * Created by jeremy on 6/30/14.
 */
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

}


