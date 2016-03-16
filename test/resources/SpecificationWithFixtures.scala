package resources

import anorm._
import com.google.common.base.Predicate
import org.openqa.selenium.chrome.ChromeDriver
import org.specs2.mutable._
import org.specs2.specification._
import play.api.Play
import play.api.Play.current
import play.api.db.DB
import play.api.test._
import play.api.test.Helpers._


abstract class SpecificationWithFixtures extends Specification with CleanDB with DefaultUserValues with GenerationNumberHelper {
  sequential

  val DocumentationUsername = running(FakeApplication(additionalConfiguration = testDB)) {
    Play.current.configuration.getString("httpAuthUser").getOrElse("")
  }

  val DocumentationPassword = running(FakeApplication(additionalConfiguration = testDB)) {
    Play.current.configuration.getString("httpAuthPassword").getOrElse("")
  }

  val webDriverType = running(FakeApplication(additionalConfiguration = testDB)) {
    Play.current.configuration.getString("webDriverType").getOrElse("chromedriver")
  }

  System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir") + "/" + webDriverType)

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
  abstract class WithFakeBrowser extends WithBrowser(app = FakeApplication(additionalConfiguration = testDB), webDriver = new ChromeDriver()) with DefaultUserValues {

    /** makes it possible to use any f: => Boolean function with browser.await.until(f) */
    implicit def fixPredicate[E1, E2](p: => Boolean): Predicate[E2] = new Predicate[Any] {
      def apply(p1: Any) = p
    }.asInstanceOf[Predicate[E2]]

    /**
     * Logs in a distributor user for automated browser tests
     * @param email A distributor user's email; defaults to the email value within the SpecificationWithFixtures class.
     * @param password A distributor user's password; defaults to the password value within the SpecificationWithFixtures class.
     */
    def logInUser(email: String = email, password: String = password): Unit = {
      goToAndWaitForAngular("http://localhost:" + port + "/login")
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.click("button")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(!browser.url().contains("login"))
    }

    /**
     * Helper function to fill out fields for App/Virtual Currency forms.
     * @param appName The name of the App
     * @param currencyName The name of the Virtual Currency
     * @param exchangeRate The units of virtual currency per $1.
     * @param rewardMin The minimum reward a user can receive.
     * @param rewardMax The maximum reward a user can receive.  This is optional.
     */
    def fillInAppValues(appName: String = "New App", currencyName: String = "Coins", exchangeRate: String = "100", rewardMin: String = "1", rewardMax: String = "10"): Unit = {
      browser.fill("#newAppName").`with`(appName)
      browser.fill("#newAppCurrencyName").`with`(currencyName)
      browser.fill("#newAppExchangeRate").`with`(exchangeRate)
      browser.fill("#newAppRewardMin").`with`(rewardMin)
      browser.fill("#newAppRewardMax").`with`(rewardMax)
    }

    /**
     * Helper function to check if element contains text
     * @param element Element selector to check
     * @param content String to check
     * @param timeout Timeout in seconds
     */
    def waitUntilContainsText(element: String, content: String, timeout: Long = 5) = {
      browser.await().atMost(timeout, java.util.concurrent.TimeUnit.SECONDS).until(element).containsText(content)
    }

    /**
     * Helper function to Goto URL and wait for angular to finish
     * @param url Url to navigate to
     */
    def goToAndWaitForAngular(url: String) = {
      browser.goTo(url)
      waitForAngular
    }

    /**
     * Helper function to Click element and wait for angular to finish
     * @param element Element selector to click
     */
    def clickAndWaitForAngular(element: String) = {
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(element).isPresent
      browser.click(element)
      waitForAngular
    }

    /**
     * Helper function to Fill element with content provided
     * @param element Element selector to fill
     * @param content String to fill
     */
    def fillAndWaitForAngular(element: String, content: String) = {
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(element).isPresent
      browser.fill(element).`with`(content)
      waitForAngular
    }

    /**
     * Helper function to wait for Angular to finish processing its current requests
     */
    def waitForAngular = {
      val ngAppElement = "body"
      val markerClass = "angularReady"

      browser.await().atMost(10, java.util.concurrent.TimeUnit.SECONDS).until("body").isPresent
      browser.executeScript(
      "window.onload = function (){" +
        "angular.element(document).ready(function () {" +
          "angular.element(document.querySelector('body')).removeClass('" + markerClass + "');" +
            "angular.element(document.querySelector('" + ngAppElement + "'))" +
            "  .injector().get('$browser').notifyWhenNoOutstandingRequests("+
            "    function() {" +
            "      angular.element(document.querySelector('body')).addClass('" + markerClass + "');" +
            "    })" +
        "});" +
      "};" +
      "window.onload();")
      try {
        browser.await().atMost(20, java.util.concurrent.TimeUnit.SECONDS).until("body." + markerClass).isPresent
      } catch {
        // Angular has most likely finished after 20 seconds, so we catch this exception and continue with the test
        case _: org.openqa.selenium.TimeoutException => None
      }
      browser.await().atMost(1, java.util.concurrent.TimeUnit.SECONDS).until("body.javascript_error").isNotPresent
    }

    /**
     * Helper function to verify Analytics have loaded
     */
    def verifyAnalyticsHaveLoaded = {
      // Extended wait for Keen to load
      browser.await().atMost(120, java.util.concurrent.TimeUnit.SECONDS).until("#analytics-header.loaded").isPresent
      // Average Revenue metric
      waitUntilContainsText("#unique-users", "$")
      // Revenue Table
      waitUntilContainsText("#analytics-revenue-table", "$")
    }

    /**
     * Creates application for unit tests with set up code for a new App/Waterfall/VirtualCurrency/AppConfig combination.
     * @param url The URL to check.
     */
    def assertUrlEquals(url: String) = {
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(browser.url().contains(url))
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
   * @param appName The name of the new App.
   * @param distributorID The ID of the Distributor to which the App (and related models) belong.
   */
  abstract class WithAppBrowser(distributorID: Long, appName: Option[String] = None) extends WithFakeBrowser with DefaultUserValues with AppCreationHelper {
    lazy val (currentApp, currentWaterfall, currentVirtualCurrency, currentAppConfig) = setUpApp(distributorID, appName)
  }

  class ApplicationFake(additionalConfig: Map[String, _ <: Any] = Map("mode" -> play.api.Mode.Test.toString))
    extends FakeApplication(additionalConfiguration = additionalConfig) {
    override val mode = additionalConfig.getOrElse("mode", "No Mode").toString.toLowerCase() match {
      case "dev" => play.api.Mode.Dev
      case "prod" => play.api.Mode.Prod
      case _ => play.api.Mode.Test
    }
  }
  abstract class ProdApp extends WithApplication(new ApplicationFake(Map("mode" -> play.api.Mode.Prod.toString.toLowerCase())))
  abstract class TestApp extends WithApplication(new ApplicationFake(Map("mode" -> play.api.Mode.Test.toString.toLowerCase())))
  abstract class DevApp extends WithApplication(new ApplicationFake(Map("mode" -> play.api.Mode.Dev.toString.toLowerCase())))
}
