package models

import anorm._
import org.specs2.mutable._
import play.api.Play.current
import play.api.db.DB
import play.api.test.Helpers._
import play.api.test._

abstract class SpecificationWithFixtures extends Specification with cleanDB {
  val email = "tdepplito@jungroup.com"
  val password = "password"
  val companyName = "Some Company"
  val testDB = Map("db.default.url" -> "jdbc:postgresql://localhost/mediation_test")

  abstract class WithDB extends WithApplication(FakeApplication(additionalConfiguration = testDB)) {
  }

  abstract class WithFakeBrowser extends WithBrowser(app = FakeApplication(additionalConfiguration = testDB)) { //, webDriver = WebDriverFactory(Helpers.FIREFOX)
  }
}

trait cleanDB {
  def clean = {
    running(FakeApplication(additionalConfiguration = Map("db.default.url" -> "jdbc:postgresql://localhost/mediation_test"))) {
      DB.withConnection { implicit connection =>
        SQL("DROP SCHEMA PUBLIC CASCADE;").execute()
        SQL("CREATE SCHEMA PUBLIC;").execute()
      }
    }
  }
}
