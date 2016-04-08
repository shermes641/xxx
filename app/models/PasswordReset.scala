package models

import anorm._
import com.github.nscala_time.time.Imports._
import org.joda.time.format.DateTimeFormatter
import play.api.db.Database
import scala.util.{Failure, Success, Try}

object PasswordReset {
  val ResetPasswordWindow = 1.hour // The amount of time a password reset token is valid
  val dateFormatGeneration: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SS")

  /**
   * Checks if a password reset request is valid
   * @param distributorUserID The ID of the DistributorUser
   * @param token             The one-time use reset token stored in the password_resets table
   * @param db                A shared database
   * @return                  True if the token has not been used before and the request occurs within the reset window (1 hour from creation time); otherwise, False.
   */
  def isValid(distributorUserID: Long, token: String, db: Database): Boolean = {
    val resetWindowMax = new DateTime(DateTimeZone.UTC)
    val resetWindowMin = resetWindowMax - ResetPasswordWindow
    val resetList = db.withConnection { implicit connection =>
      SQL(
        """
          SELECT password_resets.id FROM password_resets
          WHERE distributor_user_id = {distributor_user_id} AND token = {token} AND completed = false
          AND created_at <= to_timestamp({reset_window_max}, 'YYYY-MM-DD HH24:MI:SS')
          AND created_at >= to_timestamp({reset_window_min}, 'YYYY-MM-DD HH24:MI:SS');
        """
      ).on(
          "distributor_user_id" -> distributorUserID,
          "token" -> token,
          "reset_window_max" -> resetWindowMax.toString(dateFormatGeneration),
          "reset_window_min" -> resetWindowMin.toString(dateFormatGeneration)
        ).as(SqlParser.long("id").*)
    }
    resetList.length == 1
  }

  /**
   * Creates a new record in the password_resets table
   * @param distributorUserID The ID of the DistributorUser who is generating the password reset request
   * @param db                A shared database
   * @return                  The one-time use reset token
   */
  def create(distributorUserID: Long, db: Database): Option[String] = {
    Try(
      db.withTransaction { implicit connection =>
        val resetID: Option[Long] = SQL(
          """
             INSERT INTO password_resets (distributor_user_id) VALUES ({distributor_user_id})
          """
        ).on("distributor_user_id" -> distributorUserID).executeInsert()

        SQL(
          """
             SELECT token from password_resets where id = {id}
          """
        ).on("id" -> resetID).as(SqlParser.str("token").singleOpt)
      }
    ) match {
      case Success(token) => token
      case Failure(error) => None
    }
  }

  /**
   * Completes a password reset and ensures that a token cannot be reused
   * @param distributorUserID The ID of the DistributorUser who initiated the password reset process
   * @param token             The one-time use reset token
   * @param db                A shared database
   * @return                  The number of rows updated in the password_resets table
   */
  def complete(distributorUserID: Long, token: String, db: Database): Long = {
    db.withConnection { implicit connection =>
      SQL(
        """
          UPDATE password_resets SET completed = true
          WHERE distributor_user_id = {distributor_user_id} AND token = {token} AND completed = false;
        """
      ).on(
          "distributor_user_id" -> distributorUserID,
          "token" -> token
        ).executeUpdate()
    }
  }
}
