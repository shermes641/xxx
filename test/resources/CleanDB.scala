package resources

import anorm._
import play.api.Play.current
import play.api.db.DB
import play.api.test.Helpers._
import play.api.test._

/**
 * Drops and recreates test database schema after each test file runs.
 */
trait CleanDB {
  val testDB = {
    Map("db.default.url" -> sys.env.getOrElse("TEST_DATABASE_URL", "jdbc:postgresql://localhost/mediation_test"),
        "db.default.user" -> sys.env.getOrElse("TEST_DATABASE_USER", "postgres"),
        "db.default.password" -> sys.env.getOrElse("TEST_DATABASE_PASSWORD", "postgres"))
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
  clean
}

