package models

import anorm._
import org.specs2.mutable._
import org.specs2.specification._
import play.api.Play.current
import play.api.db.DB
import play.api.test._
import resources._


abstract class SpecificationWithFixtures extends Specification with CleanDB with DefaultUserValues with GenerationNumberHelper {
  /**
   * Drops and recreates database schema after tests are run.
   * @param tests The tests for a specific test class.
   * @return Unit. Run tests and then clean database.
   */
  override def map(tests: => Fragments) = tests ^ step(clean)

  /**
   * Retrieve the count of all records in a particular table.
   * @param tableName The table on which the count is performed.
   * @return The number of rows in the table.
   */
  def tableCount(tableName: String): Long = {
    DB.withConnection { implicit connection =>
      SQL("""SELECT COUNT(1) FROM """ + tableName).apply()
    }.head[Long]("count")
  }

  /**
   * Creates application for unit tests using a test database.
   */
  abstract class WithDB extends WithApplication(FakeApplication(additionalConfiguration = testDB))

  /**
   * Creates application for functional tests using a test database and a Firefox web browser.
   */
  abstract class WithFakeBrowser extends WithBrowser(app = FakeApplication(additionalConfiguration = testDB), webDriver = WebDriverFactory(Helpers.FIREFOX)) with DefaultUserValues {
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

  /**
   * Creates application for unit tests with set up code for a new App/Waterfall/VirtualCurrency/AppConfig combination.
   * @param distributorID The ID of the Distributor to which the App (and related models) belong.
   */
  abstract class WithAppDB(distributorID: Long) extends WithDB with DefaultUserValues with AppCreationHelper {
    lazy val (currentApp, currentWaterfall, currentVirtualCurrency, currentAppConfig) = setUpApp(distributorID)
  }

  /**
   * Creates application for functional tests with set up code for a new App/Waterfall/VirtualCurrency/AppConfig combination.
   * @param distributorID The ID of the Distributor to which the App (and related models) belong.
   */
  abstract class WithAppBrowser(distributorID: Long) extends WithFakeBrowser with DefaultUserValues with AppCreationHelper {
    lazy val (currentApp, currentWaterfall, currentVirtualCurrency, currentAppConfig) = setUpApp(distributorID)
  }
}
