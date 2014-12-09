package models

import anorm._
import anorm.SqlParser._
import com.github.t3hnar.bcrypt._
import play.api.db.DB
import play.api.Play.current
import scala.language.postfixOps

/**
 * Encapsulates information for DistributorUsers.
 * @param id DistributorUser ID stored in database
 * @param email Email for DistributorUser
 * @param hashedPassword Hashed password for DistributorUser
 * @param distributorID Foreign key to maintain relationship with Distributor (Distributor has many DistributorUsers).
 * @param hyprMarketplaceID HyprMarketplace ID
 */
case class DistributorUser (id: Option[Long], email: String, hashedPassword: String, hyprMarketplaceID: Option[Long], distributorID: Option[Long]) {
  /**
   * Stores hashed password in database for DistributorUser.
   * @param password Password for current DistributorUser
   */
  def setPassword(password: String) {
    val salt = generateSalt
    SQL("UPDATE distributor_users SET ('hashed_password') VALUE ('{hash}') WHERE id = {id}")
      .on("hash" -> password.bcrypt(salt), "id" -> this.id.get)
  }
}

/** Encapsulates methods for DistributorUser class */
object DistributorUser {

  /**
   * Checks DistributorUser log in credentials against what is stored in the database.
   * @param email Email of DistributorUser attempting to log in
   * @param password Password of DistributorUser attempting to log in
   * @return If log in credentials are valid, returns a DistributorUser instance.  Otherwise, returns false.
   */
  def checkPassword(email: String, password: String): Boolean = {
    DistributorUser.findByEmail(email) match {
      case Some(user) =>
        password.isBcrypted(user.hashedPassword)
      case None =>
        false
    }
  }

  def isNotActive(id: Long): Boolean = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT distributor_users.*
          FROM distributor_users
          WHERE distributor_users.distributor_id = {id}
          AND active = true
        """
      ).on("id" -> id)
      query.as(userParser*) match {
        case List(user) => false
        case List() => true
      }
    }
  }

  // Used to convert SQL row into an instance of DistributorUser class.
  val userParser: RowParser[DistributorUser] = {
    get[Option[Long]]("distributor_users.id") ~
    get[String]("distributor_users.email") ~
    get[String]("distributor_users.hashed_password") ~
    get[Option[Long]]("distributor_users.hypr_marketplace_id") ~
    get[Option[Long]]("distributor_users.distributor_id") map {
      case id ~ email ~ hashed_password ~ hypr_marketplace_id ~ distributor_id => DistributorUser(id, email, hashed_password, hypr_marketplace_id, distributor_id)
    }
  }

  /**
   * Finds DistributorUser in database using email.
   * @param email Email to be used in SQL query
   * @return List of DistributorUsers if query is successful.  Otherwise, returns an empty list.
   */
  def findByEmail(email: String): Option[DistributorUser] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT distributor_users.*
          FROM distributor_users
          WHERE LOWER(distributor_users.email) = LOWER({email})
        """
      ).on("email" -> email)
      query.as(userParser*) match {
        case List(user) => Some(user)
        case List() => None
      }
    }
  }

  def find(userID: Long): Option[DistributorUser] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT distributor_users.*
          FROM distributor_users
          WHERE distributor_users.id = {id}
        """
      ).on("id" -> userID)
      query.as(userParser*) match {
        case List(user) => Some(user)
        case List() => None
      }
    }
  }

  /**
   * Creates a new record in the database for a DistributorUser.
   * @param email Email to be stored for DistributorUser
   * @param password Password to be hashed and stored for DistributorUser
   * @param company Company name to be used for creation of Distributor.
   * @return ID of the new record if the insert is successful.  Otherwise, returns false.
   */
  def create(email: String, password: String, company: String) = {
    val salt = generateSalt
    val hashedPassword = password.bcrypt(salt)
    findByEmail(email) match {
      case Some(user) => false
      case _ => {
        Distributor.create(company) match {
          case Some(distributorID) => {
            DB.withConnection { implicit connection =>
              SQL(
                """
                  INSERT INTO distributor_users (email, hashed_password, distributor_id)
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

  /**
   * Sets Distributor User to Active
   * @param user Instance of DistributorUser class.
   * @return Number of rows successfully updated.
   */
  def setActive(user: DistributorUser) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          UPDATE distributor_users
          SET active=true
          WHERE id = {id};
        """
      ).on("id" -> user.id).executeUpdate()
    }
  }

  /**
   * Sets Distributor User HyprMarketplace ID
   * @param user Instance of DistributorUser class.
   * @param hyprMarketplaceID ID to set
   * @return Number of rows successfully updated.
   */
  def setHyprMarketplaceID(user: DistributorUser, hyprMarketplaceID: Int) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          UPDATE distributor_users
          SET hypr_marketplace_id={hypr_marketplace_id}
          WHERE id = {id};
        """
      ).on("id" -> user.id, "hypr_marketplace_id" -> hyprMarketplaceID).executeUpdate()
    }
  }

  /**
   * Updates fields for a particular DistributorUser instance.
   * @param user Instance of DistributorUser class.
   * @return Number of rows successfully updated.
   */
  def update(user: DistributorUser) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          UPDATE distributor_users
          SET email={email}, hashed_password={hashed_password}, distributor_id={distributor_id}
          WHERE id = {id};
        """
      ).on("email" -> user.email, "hashed_password" -> user.hashedPassword, "distributor_id" -> user.distributorID, "id" -> user.id).executeUpdate()
    }
  }
}
