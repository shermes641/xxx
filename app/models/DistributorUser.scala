package models

import anorm._
import anorm.SqlParser._
import javax.inject._
import com.github.t3hnar.bcrypt._
import controllers._
import play.api.db.Database
import scala.language.postfixOps

/**
 * Encapsulates information for DistributorUsers.
 * @param id DistributorUser ID stored in database
 * @param email Email for DistributorUser
 * @param hashedPassword Hashed password for DistributorUser
 * @param distributorID Foreign key to maintain relationship with Distributor (Distributor has many DistributorUsers).
 */
case class DistributorUser (id: Option[Long], email: String, hashedPassword: String, distributorID: Option[Long])

/**
  * Encapsulates functions for DistributorUsers
  * @param distributorService A shared instance of the DistributorService class
  * @param db                 A shared database
  */
@Singleton
class DistributorUserService @Inject() (distributorService: DistributorService, db: Database) {

  /**
   * Checks DistributorUser log in credentials against what is stored in the database.
   * @param email Email of DistributorUser attempting to log in
   * @param password Password of DistributorUser attempting to log in
   * @return If log in credentials are valid, returns a DistributorUser instance.  Otherwise, returns false.
   */
  def checkPassword(email: String, password: String): Boolean = {
    findByEmail(email) match {
      case Some(user) =>
        password.isBcrypted(user.hashedPassword)
      case None =>
        false
    }
  }

  // Used to convert SQL row into an instance of DistributorUser class.
  val userParser: RowParser[DistributorUser] = {
    get[Option[Long]]("distributor_users.id") ~
    get[String]("distributor_users.email") ~
    get[String]("distributor_users.hashed_password") ~
    get[Option[Long]]("distributor_users.distributor_id") map {
      case id ~ email ~ hashed_password ~ distributor_id => DistributorUser(id, email, hashed_password, distributor_id)
    }
  }

  /**
   * Finds DistributorUser in database using email.
   * @param email Email to be used in SQL query
   * @return List of DistributorUsers if query is successful.  Otherwise, returns an empty list.
   */
  def findByEmail(email: String): Option[DistributorUser] = {
    db.withConnection { implicit connection =>
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
    db.withConnection { implicit connection =>
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

  def findByDistributorID(distributorID: Long): Option[DistributorUser] = {
    db.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT distributor_users.*
          FROM distributor_users
          WHERE distributor_users.distributor_id = {distributor_id}
          LIMIT 1;
        """
      ).on("distributor_id" -> distributorID)
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
   * @return ID of the new record if the insert is successful.  Otherwise, returns None.
   */
  def create(email: String, password: String, company: String): Option[Long] = {
    val salt = generateSalt
    val hashedPassword = password.bcrypt(salt)
    findByEmail(email) match {
      case Some(user) => None
      case _ => {
        distributorService.create(company) match {
          case Some(distributorID) => {
            db.withConnection { implicit connection =>
              SQL(
                """
                  INSERT INTO distributor_users (email, hashed_password, distributor_id)
                  VALUES ({email}, {hashed_password}, {distributor_id});
                """
              ).on("email" -> email, "hashed_password" -> hashedPassword, "distributor_id" -> distributorID).executeInsert()
            }
          }
          case _ => None
        }
      }
    }
  }

  /**
   * Updates fields for a particular DistributorUser instance.
   * @param user Instance of DistributorUser class.
   * @return Number of rows successfully updated.
   */
  def update(user: DistributorUser) = {
    db.withConnection { implicit connection =>
      SQL(
        """
          UPDATE distributor_users
          SET email={email}, hashed_password={hashed_password}, distributor_id={distributor_id}
          WHERE id = {id};
        """
      ).on("email" -> user.email, "hashed_password" -> user.hashedPassword, "distributor_id" -> user.distributorID, "id" -> user.id).executeUpdate()
    }
  }

  /**
   * Updates a DistributorUser's password if possible
   * @param updateInfo Encapsulates the required information for resetting a user's password
   * @return The number of records updated
   */
  def updatePassword(updateInfo: PasswordUpdate): Int = {
    findByEmail(updateInfo.email) match {
      case Some(user) if(user.id.get == updateInfo.distributorUserID) => {
        val salt = generateSalt
        val hashedPassword = updateInfo.password.bcrypt(salt)
        val updatedUser = new DistributorUser(user.id, user.email, hashedPassword, user.distributorID)
        update(updatedUser)
      }
      case _ => 0
    }
  }
}
