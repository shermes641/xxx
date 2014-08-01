package models

import anorm._
import anorm.SqlParser._
import com.github.t3hnar.bcrypt._
import play.api.db.DB
import play.api.Play.current

case class DistributorUser (id: Option[Long], email: String, hashedPassword: String, distributorID: Option[Long]) {
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
        password.isBcrypted(user.hashedPassword)
      case None =>
        false
    }
  }

  val userParser: RowParser[DistributorUser] = {
    get[Option[Long]]("DistributorUser.id") ~
    get[String]("DistributorUser.email") ~
    get[String]("DistributorUser.hashed_password") ~
    get[Option[Long]]("DistributorUser.distributor_id") map {
      case id ~ email ~ hashed_password ~ distributor_id => DistributorUser(id, email, hashed_password, distributor_id)
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

  def create(email: String, password: String, company: String) = {
    val salt = generateSalt
    val hashedPassword = password.bcrypt(salt)
    findByEmail(email) match {
      case Some(user) => false
      case _ => {
        Distributor.create(company) match {
          case Some(distributorID) => {
            DB.withConnection { implicit c =>
              SQL(
                """
                  INSERT INTO DistributorUser (email, hashed_password, distributor_id)
                  VALUES ({email}, {hashed_password}, {distributor_id});
                """
              ).on("email" -> email, "hashed_password" -> hashedPassword, "distributor_id" -> distributorID).executeInsert()
            }
          }
          case _ => false
        }
      }
    }
  }

  def update(user: DistributorUser) = {
    DB.withConnection { implicit c =>
      SQL(
        """
          UPDATE DistributorUser
          SET email={email}, hashed_password={hashed_password}, distributor_id={distributor_id}
          WHERE id = {id};
        """
      ).on("email" -> user.email, "hashed_password" -> user.hashedPassword, "distributor_id" -> user.distributorID, "id" -> user.id).executeUpdate()
    }
  }
}
