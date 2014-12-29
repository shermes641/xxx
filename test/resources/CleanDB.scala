package resources

import anorm._
import play.api.Play
import play.api.Play.current
import play.api.db.DB
import play.api.test.Helpers._
import play.api.test._

/**
 * Drops and recreates test database schema after each test file runs.
 */
trait CleanDB {
  val testDB = {
    running(FakeApplication()) {
      Map("db.default.url" -> Play.current.configuration.getString("db.test.url").getOrElse("jdbc:postgresql://localhost/mediation_test"),
          "db.default.user" -> Play.current.configuration.getString("db.test.user").getOrElse("postgres"),
          "db.default.password" -> Play.current.configuration.getString("db.test.password").getOrElse("postgres"))
    }
  }

  // Helper function to drop and recreate the schema for the test database.
  def clean = {
    running(FakeApplication(additionalConfiguration = testDB)) {
      DB.withConnection { implicit connection =>
        SQL("DROP SCHEMA PUBLIC CASCADE;").execute()
        SQL("CREATE SCHEMA PUBLIC;").execute()
      }
    }
  }
}

