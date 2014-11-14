package models

import anorm._
import org.specs2.mutable._
import play.api.Play.current
import play.api.db.DB
import play.api.libs.json.{JsString, JsObject}
import play.api.test.Helpers._
import play.api.test._

abstract class SpecificationWithFixtures extends Specification with cleanDB {
  val email = "tdepplito@jungroup.com"
  val password = "password"
  val companyName = "Some Company"

  /**
   * Retrieve the count of all records in a particular table.
   * @param tableName The table on which the count is performed.
   * @return The number of rows in the table.
   */
  def tableCount(tableName: String): Long = {
    (DB.withConnection { implicit connection =>
      SQL("""SELECT COUNT(1) FROM """ + tableName).apply()
    }.head)[Long]("count")
  }

  /**
   * Retrieve the latest generation_number for a particular waterfall ID.
   * @param waterfallID The ID of the Waterfall to look up in the waterfall_generations table.
   * @return The latest generation number if a record exists; otherwise, returns none.
   */
  def generationNumber(waterfallID: Long): Long = {
    (DB.withConnection { implicit connection =>
      SQL("""SELECT COALESCE(MAX(generation_number), 0) AS generation FROM waterfall_generations where waterfall_id={waterfall_id}""").on("waterfall_id" -> waterfallID).apply()
    }.head)[Long]("generation")
  }


  abstract class WithDB extends WithApplication(FakeApplication(additionalConfiguration = testDB)) {
  }

  abstract class WithFakeBrowser extends WithBrowser(app = FakeApplication(additionalConfiguration = testDB), webDriver = WebDriverFactory(Helpers.FIREFOX)) {
    /**
     * Logs in a distributor user for automated browser tests
     * @param email A distributor user's email; defaults to the email value within the SpecificationWithFixtures class.
     * @param password A distributor user's password; defaults to the password value within the SpecificationWithFixtures class.
     * @return A browser with a session for the distributor user's email that was passed in.
     */
    def logInUser(email: String = email, password: String = password): Unit = {
      browser.goTo("http://localhost:" + port + "/login")
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.click("button")
    }
  }
}

trait cleanDB {
  val testDB = Map("db.default.url" -> "jdbc:postgresql://localhost/mediation_test", "db.default.user" -> "postgres", "db.default.password" -> "postgres")

  def clean = {
    running(FakeApplication(additionalConfiguration = testDB)) {
      DB.withConnection { implicit connection =>
        SQL("DROP SCHEMA PUBLIC CASCADE;").execute()
        SQL("CREATE SCHEMA PUBLIC;").execute()
      }
    }
  }
}

trait JsonTesting {
  val configurationParams = List("key1", "key2")
  val configurationValues = List("value1", "value2")
  def paramJson(paramKey: Int) = "{\"key\":\"" + configurationParams(paramKey) + "\", \"value\":\"\", \"dataType\": \"String\", \"description\": \"some description\"}"
  val configurationData = "{\"requiredParams\": [" + paramJson(0) + ", " + paramJson(1) + "], \"reportingParams\": [], \"callbackParams\": []}"
  val configurationJson = JsObject(Seq("requiredParams" -> JsObject(Seq(configurationParams(0) -> JsString(configurationValues(0)), configurationParams(1) -> JsString(configurationValues(1))))))
}
