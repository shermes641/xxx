package resources

import anorm._
import play.api.Play.current
import play.api.db.{Database, DB}
import play.api.test.Helpers._
import play.api.test._

/**
 * Drops and recreates test database schema after each test file runs.
 */
trait CleanDB {
  val testDatabase: Database
  val testDB = {
    Map("db.default.url" -> sys.env.getOrElse("TEST_DATABASE_URL", "jdbc:postgresql://localhost/mediation_test?user=postgres&password=postgres"),
        "default.hikaricp.connectionTestQuery" -> "SELECT TRUE",
        "play.mailer.mock" -> "true",
        "play.evolutions.enabled" -> "true")
  }

  // Helper function to drop and recreate the schema for the test database.
  def clean() = {
    running(FakeApplication(additionalConfiguration = testDB)) {
      testDatabase.withConnection { implicit connection =>
        SQL("DROP SCHEMA PUBLIC CASCADE;").execute()
        SQL("CREATE SCHEMA PUBLIC;").execute()
      }
      testDatabase.shutdown()
    }
  }
}
