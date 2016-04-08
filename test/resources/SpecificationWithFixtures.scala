package resources

import java.io.File

import akka.actor.{ActorRef, ActorSystem, Props}
import be.objectify.deadbolt.scala.{DeadboltComponents, ExecutionContextProvider}
import be.objectify.deadbolt.scala.cache.{DefaultPatternCache, HandlerCache, PatternCache}
import play.api.cache.EhCacheComponents
import play.api.db.evolutions.Evolutions
import anorm._
import com.google.common.base.Predicate
import models.ConfigVars
import org.openqa.selenium.chrome.ChromeDriver
import org.specs2.mutable._
import org.openqa.selenium.chrome.{ChromeDriver, ChromeDriverService}
import org.specs2.specification._
import play.api.Play
import play.api.Play.current
import play.api.db.DB
import play.api.test.Helpers._
import play.api.test._
import com.typesafe.config.ConfigFactory
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.specs2.mutable._
import tasks.RegenerateAppConfigsService
import play.api._
import play.api.db._
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.ning.NingWSClient
import play.api.test._
import play.api.test.Helpers._
import com.google.common.base.Predicate
import play.api.libs.mailer._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import play.test.TestBrowser
import models._
import admin._
import scala.io.Source
import security.AdminHandlerCache

trait DBSetup extends org.specs2.specification.AfterAll with CleanDB {
  override def afterAll() = clean()
}

abstract class SpecificationWithFixtures extends Specification with DBSetup with DefaultUserValues with DistributorUserSetup with AppCreationHelper {
  sequential

  val testApplication = FakeApplication(withGlobal = Some(new GlobalSettings() {
  }), additionalConfiguration = testDB)

  lazy val database: Database = running(testApplication) {
    Databases(
      driver = "org.postgresql.Driver",
      url = "jdbc:postgresql://localhost/mediation_test?user=postgres&password=postgres",
      config = testDB
    )
  }

  override lazy val db = database

  Evolutions.applyEvolutions(database) // Apply evolutions by default

  lazy val DocumentationUsername = running(testApplication) {
    Play.current.configuration.getString("httpAuthUser").getOrElse("")
  }

  lazy val DocumentationPassword = running(testApplication) {
    Play.current.configuration.getString("httpAuthPassword").getOrElse("")
  }

  lazy val webDriverType = running(testApplication) {
    Play.current.configuration.getString("webDriverType").getOrElse("chromedriver")
  }

  System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir") + "/" + webDriverType)

  lazy val ws = NingWSClient()

  lazy val environmentConfig = running(testApplication) {
    testApplication.configuration
  }

  lazy val testPlatform = new Platform(configVars)

  lazy val keenInitialization = new KeenInitialization(configVars)

  lazy val appEnvironment = {
    lazy val appMode = new AppMode(play.api.Environment.simple())
    new models.Environment(environmentConfig, appMode)
  }

  lazy val configVars = new ConfigVars(environmentConfig, appEnvironment)

  val (
    appConfigService,
    distributorService,
    distributorUserService,
    appService,
    keenExportService,
    keenExport,
    adProviderService,
    waterfallAdProviderService,
    waterfallService,
    virtualCurrencyService,
    modelService,
    regenerateAppConfigsService,
    jsonBuilder
    ) = running(testApplication) {
    lazy val jsonBuilder = new models.JsonBuilder(configVars)
    lazy val appConfigService = new models.AppConfigService(database, jsonBuilder)
    lazy val distributorService = new models.DistributorService(database)
    lazy val distributorUserService = new models.DistributorUserService(distributorService, database)
    lazy val appService = new models.AppService(appConfigService, database)
    lazy val keenExportService = new models.KeenExportService(configVars)
    lazy val keenExport = new models.KeenExport(appService, keenExportService, configVars, testApplication.actorSystem)
    lazy val adProviderService = new models.AdProviderService(database, testPlatform)
    lazy val waterfallAdProviderService = new models.WaterfallAdProviderService(appConfigService, database)
    lazy val waterfallService = new models.WaterfallService(waterfallAdProviderService, database)
    lazy val virtualCurrencyService = new models.VirtualCurrencyService(database)
    val regenerateAppConfigsService = new tasks.RegenerateAppConfigsService(database,
      waterfallAdProviderService,
      appConfigService,
      adProviderService,
      testPlatform,
      jsonBuilder)
    val modelService = new models.ModelService(
      distributorUserService,
      distributorService,
      appService,
      adProviderService,
      waterfallService,
      waterfallAdProviderService,
      appConfigService,
      virtualCurrencyService,
      jsonBuilder,
      testPlatform,
      environmentConfig
    )
    (appConfigService,
     distributorService,
     distributorUserService,
     appService,
     keenExportService,
     keenExport,
     adProviderService,
     waterfallAdProviderService,
     waterfallService,
     virtualCurrencyService,
     modelService,
     regenerateAppConfigsService,
     jsonBuilder)
  }

  lazy val securityRoleService = new SecurityRoleService(database)
  lazy val adminService = new AdminService(securityRoleService)

  override lazy val appModel: AppService = appService
  override lazy val distributorModel: DistributorService = distributorService
  override lazy val userService: DistributorUserService = distributorUserService
  override lazy val waterfallModel: WaterfallService = waterfallService
  override lazy val appConfigModel: AppConfigService = appConfigService
  override lazy val virtualCurrencyModel: VirtualCurrencyService = virtualCurrencyService
  override lazy val testDatabase: Database = database
  override lazy val thisPlatform = testPlatform

  /**
   * Retrieve the count of all records in a particular table.
 *
   * @param tableName The table on which the count is performed.
   * @return The number of rows in the table.
   */
  def tableCount(tableName: String): Long = {
    database.withConnection { implicit connection =>
      SQL("""SELECT COUNT(1) FROM """ + tableName)
        .as(SqlParser.long("count").single)
    }
  }

  /**
   * Retrieve the latest generation_number for a particular waterfall ID.
 *
   * @param appID The ID of the App to look up in the app_configs table.
   * @return The latest generation number if a record exists; otherwise, returns none.
   */
  def generationNumber(appID: Long): Long = {
    database.withConnection { implicit connection =>
      SQL("""SELECT COALESCE(MAX(generation_number), 0) AS generation FROM app_configs where app_id={app_id}""")
        .on("app_id" -> appID).as(SqlParser.long("generation").single)
    }
  }

  /**
   * Helper function to clear out previous generation configuration data.
 *
   * @param appID The ID of the App to which the AppConfig belongs.
   * @return 1 if the insert is successful; otherwise, None.
   */
  def clearGeneration(appID: Long) = {
    appService.find(appID) match {
      case Some(app) => {
        database.withConnection { implicit connection =>
          SQL(
            """
              INSERT INTO app_configs (generation_number, app_id, app_token, configuration)
              VALUES ((SELECT COALESCE(MAX(generation_number), 0) + 1 AS generation FROM app_configs where app_id={app_id}),
              {app_id}, {app_token}, '{}');
            """
          ).on("app_id" -> appID, "app_token" -> app.token).executeInsert()
        }
      }
      case None => None
    }
  }

  val (user, distributor) = running(testApplication) {
    newDistributorUser()
  }

  val (app1, waterfall, virtualCurrency1, _) = running(testApplication) {
    setUpApp(distributor.id.get)
  }

  val requiredKeys = List("distributorID", "appID")
  val reportingKeys = List("APIKey", "placementID", "appID")

  val adProviderConfigData = {
    "{" +
      "\"requiredParams\":[" +
      "{\"description\": \"Your Distributor ID\", \"displayKey\": \"Distributor ID\", \"key\": \"" + requiredKeys(0) + "\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": true}, " +
      "{\"description\": \"Your App Id\", \"displayKey\": \"App ID\", \"key\": \"" + requiredKeys(1) + "\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": true}" +
      "], " +
      "\"reportingParams\": [" +
      "{\"description\": \"Your Mediation Reporting API Key\", \"key\": \"" + reportingKeys(0) + "\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}, " +
      "{\"description\": \"Your Mediation Reporting Placement ID\", \"key\": \"" + reportingKeys(1) + "\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}, " +
      "{\"description\": \"Your App ID\", \"key\": \"" + reportingKeys(2) + "\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
      "], " +
      "\"callbackParams\": [" +
      "{\"description\": \"Your Event API Key\", \"key\": \"APIKey\", \"value\":\"\", \"dataType\": \"String\", \"refreshOnAppRestart\": false}" +
      "]" +
      "}"
  }

  val adProviders = List("testAdProvider1", "testAdProvider2")
  val adProviderDisplayNames = List("test ad provider 1", "test ad provider 2")
  val adProvider1CallbackUrlDescription = Constants.AdProviderConfig.CallbackUrlDescription.format(adProviderDisplayNames.head)
  val adProvider2CallbackUrlDescription = "Some other callback URL description"

  val adProviderID1 = running(testApplication) {
    adProviderService.create(adProviders(0), adProviderDisplayNames(0), adProviderConfigData, testPlatform.Ios.PlatformID, None, adProvider1CallbackUrlDescription, true)
  }

  val adProviderID2 = running(testApplication) {
    adProviderService.create(adProviders(1), adProviderDisplayNames(1), adProviderConfigData, testPlatform.Ios.PlatformID, None, adProvider2CallbackUrlDescription, true)
  }

  /**
   * Helper function to create WaterfallAdProvider with configuration JSON.
 *
   * @param waterfallID ID of the Waterfall to which the new WaterfallAdProvider belongs
   * @param adProviderID ID of the Ad Provider to which the new WaterfallAdProvider belongs
   * @param cpm The estimated cost per thousand completions for an AdProvider.
   * @param configurable Determines if the cpm value can be edited for the WaterfallAdProvider.
   * @param active Boolean value determining if the WaterfallAdProvider can be included in the AppConfig list of AdProviders.
   * @param configuration The JSON configuration.
   * @return An instance of the WaterfallAdProvider class.
   */
  def createWaterfallAdProvider(waterfallID: Long, adProviderID: Long, waterfallOrder: Option[Long] = None, cpm: Option[Double] = None, configurable: Boolean = true, active: Boolean = true, configuration: JsObject = Json.obj("requiredParams" -> Json.obj())): WaterfallAdProvider = {
    val id = waterfallAdProviderService.create(waterfallID, adProviderID, waterfallOrder, cpm, configurable, active).get
    val wap = waterfallAdProviderService.find(id).get
    waterfallAdProviderService.update(new WaterfallAdProvider(id, wap.waterfallID, wap.adProviderID, None, cpm, Some(true), None, configuration, false))
    waterfallAdProviderService.find(id).get
  }

  /**
   * Creates application for unit tests using a test database.
   */
  abstract class WithDB extends WithApplication

  /**
   * Creates application for functional tests using a test database and a Firefox web browser.
   */
  class WithFakeBrowser extends WithBrowser(webDriver = new ChromeDriver()) with DefaultUserValues {
    lazy val apiController = app.injector.instanceOf[controllers.APIController]
    lazy val analyticsController = app.injector.instanceOf[controllers.AnalyticsController]
    lazy val waterfallsController = app.injector.instanceOf[controllers.WaterfallsController]
    lazy val waterfallAdProvidersController = app.injector.instanceOf[controllers.WaterfallAdProvidersController]
    lazy val adminController = app.injector.instanceOf[controllers.AdminController]


    lazy val mailerComponent = {
      val component = app.injector.instanceOf[play.api.libs.mailer.MailerComponents]
      new Mailer(component.mailerClient, appEnvironment)
    }
    lazy val welcomeEmailActor = app.actorSystem.actorOf(Props(new WelcomeEmailActor(mailerComponent, configVars)))
    lazy val junGroupAPIActor = app.actorSystem.actorOf(Props(new JunGroupAPIActor(modelService, database, mailerComponent)))
    lazy val distributorUsersController = new controllers.DistributorUsersController(app.actorSystem, modelService, configVars, mailerComponent, database)
    lazy val appsController = new controllers.AppsController(modelService, database, app.actorSystem, ws, mailerComponent, configVars, appEnvironment)
    /** makes it possible to use any f: => Boolean function with browser.await.until(f) */
    implicit def fixPredicate[E1, E2](p: => Boolean): Predicate[E2] = new Predicate[Any] {
      def apply(p1: Any) = p
    }.asInstanceOf[Predicate[E2]]

    /**
     * Logs in a distributor user for automated browser tests
 *
     * @param email A distributor user's email; defaults to the email value within the SpecificationWithFixtures class.
     * @param password A distributor user's password; defaults to the password value within the SpecificationWithFixtures class.
     */
    def logInUser(email: String = email, password: String = password): Unit = {
      goToAndWaitForAngular("http://localhost:" + port + "/login")
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.click("button[name=submit]")
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(!browser.url().contains("login"))
    }

    /**
     * Helper function to fill out fields for App/Virtual Currency forms.
 *
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
 *
     * @param element Element selector to check
     * @param content String to check
     * @param timeout Timeout in seconds
     */
    def waitUntilContainsText(element: String, content: String, timeout: Long = 5) = {
      browser.await().atMost(timeout, java.util.concurrent.TimeUnit.SECONDS).until(element).containsText(content)
    }

    /**
     * Helper function to Goto URL and wait for angular to finish
 *
     * @param url Url to navigate to
     */
    def goToAndWaitForAngular(url: String) = {
      browser.goTo(url)
      waitForAngular
    }

    /**
     * Helper function to Click element and wait for angular to finish
 *
     * @param element Element selector to click
     */
    def clickAndWaitForAngular(element: String) = {
      browser.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(element).isPresent
      browser.click(element)
      waitForAngular
    }

    /**
     * Helper function to Fill element with content provided
 *
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
 *
     * @param url The URL to check.
     */
    def assertUrlEquals(url: String) = {
      browser.await().atMost(10, java.util.concurrent.TimeUnit.SECONDS).until(browser.url().contains(url))
    }
  }

  def readFileAsString(file: String) = {
    Source.fromFile(file).getLines.mkString("", "", "")
  }

  /**
   * Creates application for unit tests with set up code for a new App/Waterfall/VirtualCurrency/AppConfig combination.
 *
   * @param distributorID The ID of the Distributor to which the App (and related models) belong.
   */
  abstract class WithAppDB(distributorID: Long) extends WithDB {
    lazy val (currentApp, currentWaterfall, currentVirtualCurrency, currentAppConfig) = setUpApp(distributorID)
  }

  /**
   * Creates application for functional tests with set up code for a new App/Waterfall/VirtualCurrency/AppConfig combination.
 *
   * @param appName The name of the new App.
   * @param distributorID The ID of the Distributor to which the App (and related models) belong.
   */
  abstract class WithAppBrowser(distributorID: Long, appName: Option[String] = None) extends WithFakeBrowser {
    lazy val (currentApp, currentWaterfall, currentVirtualCurrency, currentAppConfig) = setUpApp(distributorID, appName)
  }

  abstract class ProdApp extends WithApplication(new ApplicationFake(Map("mode" -> play.api.Mode.Prod.toString.toLowerCase())))
  abstract class TestApp extends WithApplication(new ApplicationFake(Map("mode" -> play.api.Mode.Test.toString.toLowerCase())))
  abstract class DevApp extends WithApplication(new ApplicationFake(Map("mode" -> play.api.Mode.Dev.toString.toLowerCase())))

  lazy val stagingAppFake = new ApplicationFake(Map(
    "mode" -> play.api.Mode.Prod.toString,
    "staging" -> "true",
    "play.mailer.mock" -> "false"
  ))
  abstract class StagingApp extends WithApplication(app = stagingAppFake) {
    lazy val mailerClient: MailerClient = app.injector.instanceOf[MailerClient]
    lazy val appMode = new AppMode(play.api.Environment.simple(mode = play.api.Mode.Prod))
    lazy val stagingEnv = new models.Environment(stagingAppFake.configuration, appMode)
    lazy val mailer = new Mailer(mailerClient, stagingEnv)
    lazy val db: Database = {
      Databases(
        driver = "org.postgresql.Driver",
        url = "jdbc:postgresql://localhost/mediation_test?user=postgres&password=postgres"
      )
    }
  }
}
