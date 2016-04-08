package models

import anorm._
import com.github.nscala_time.time.Imports._
import org.joda.time.{DateTimeZone, DateTime}
import org.junit.runner._
import org.specs2.runner._
import play.api.test.Helpers._
import resources.{SpecificationWithFixtures, DistributorUserSetup}
import resources.DistributorUserSetup
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class PasswordResetSpec extends SpecificationWithFixtures with DistributorUserSetup {
  override val (user, _) = running(testApplication) {
    newDistributorUser("newUser@gmail.com")
  }

  "PasswordReset.isValid" should {
    "return true if the token has not expired and has never been used" in new WithDB {
      val resetToken = PasswordReset.create(user.distributorID.get, database).get
      PasswordReset.isValid(user.id.get, resetToken, database) must beEqualTo(true).eventually(3, Duration(1000, "millis"))
    }

    "return false if the token is not found" in new WithDB {
      val invalidToken = "invalid-token"
      PasswordReset.isValid(user.id.get, invalidToken, database) must beFalse
    }

    "return false if the distributor user is not found" in new WithDB {
      val resetToken = PasswordReset.create(user.distributorID.get, database).get
      val unknownUserID = 9999
      PasswordReset.isValid(unknownUserID, resetToken, database) must beFalse
    }

    "return false if the token has been used" in new WithDB {
      val resetToken = PasswordReset.create(user.distributorID.get, database).get
      PasswordReset.isValid(user.id.get, resetToken, database) must beEqualTo(true).eventually(3, Duration(1000, "millis"))
      PasswordReset.complete(user.distributorID.get, resetToken, database) must beEqualTo(1)
      PasswordReset.isValid(user.id.get, resetToken, database) must beEqualTo(false).eventually(3, Duration(1000, "millis"))
    }

    "return false if the token is too old" in new WithDB {
      val resetToken = PasswordReset.create(user.distributorID.get, database).get
      PasswordReset.isValid(user.id.get, resetToken, database) must beEqualTo(true).eventually(3, Duration(1000, "millis"))
      val newCreatedAt = (new DateTime(DateTimeZone.UTC) - PasswordReset.ResetPasswordWindow).toString(PasswordReset.dateFormatGeneration)
      database.withConnection { implicit connection =>
        SQL(
          """
            UPDATE password_resets SET created_at = to_timestamp({new_created_at}, 'YYYY-MM-DD HH24:MI:SS')
            WHERE token = {token};
          """
        ).on(
            "token" -> resetToken,
            "new_created_at" -> newCreatedAt
          ).executeUpdate()
      }
      PasswordReset.isValid(user.id.get, resetToken, database) must beEqualTo(false).eventually(3, Duration(1000, "millis"))
    }
  }

  "PasswordReset.create" should {
    "create a new record in the password_resets table" in new WithDB {
      val originalCount = tableCount("password_resets")
      PasswordReset.create(user.distributorID.get, database)
      tableCount("password_resets") must beEqualTo(originalCount + 1)
    }

    "create a new valid record" in new WithDB {
      val resetToken = PasswordReset.create(user.distributorID.get, database).get
      PasswordReset.isValid(user.id.get, resetToken, database) must beEqualTo(true).eventually(3, Duration(1000, "millis"))
    }

    "not create a new record if the distributor user ID is not valid" in new WithDB {
      val invalidUserID = 9999
      PasswordReset.create(invalidUserID, database) must beNone
    }
  }

  "PasswordReset.complete" should {
    "mark a password reset as completed" in new WithDB {
      val resetToken = PasswordReset.create(user.distributorID.get, database).get
      PasswordReset.complete(user.distributorID.get, resetToken, database) must beEqualTo(1)
      val resetComplete = database.withConnection { implicit connection =>
        SQL(
          """
            SELECT password_resets.completed FROM password_resets
            WHERE token = {token};
          """
        ).on(
            "token" -> resetToken
          ).as(SqlParser.bool("completed").single)
      }
      resetComplete must beTrue
    }
  }
}
