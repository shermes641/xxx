import models._
import play.api._
import play.api.ApplicationLoader.Context
import play.api.db.{DBComponents, Database, HikariCPComponents}
import play.api.db.evolutions.Evolutions
import play.api.libs.mailer._
import play.api.libs.ws.ning.NingWSClient
import router.Routes
import scala.language.postfixOps

/**
  * Loads and starts worker for reporting
  */
// $COVERAGE-OFF$
class ReportingApplicationLoader extends ApplicationLoader {
  def load(context: Context) = {
    val reportingComponents = new ReportingComponents(context)
    val application = reportingComponents.application
    Startup.beforeStart(reportingComponents.configVars, reportingComponents.appEnvironment)
    Startup.onStart(reportingComponents.modelService, reportingComponents.appEnvironment)
    reportingComponents.revenueDataWorker.startRevenueDataCollection(application)
    application
  }
}

/**
  * Encapsulates all dependencies that will be injected when the app is loaded
  * @param context The context in which the app is running
  */
class ReportingComponents(context: Context) extends BuiltInComponentsFromContext(context) with MailerComponents with DBComponents with HikariCPComponents {
  lazy val database: Database = dbApi.database("default")
  Evolutions.applyEvolutions(database) // Apply evolutions by default
  lazy val wsClient = NingWSClient() // Reuse this web service client throughout app
  lazy val environmentConfig = context.initialConfiguration
  lazy val appEnvironment = {
    lazy val appMode = new AppMode(context.environment)
    new models.Environment(environmentConfig, appMode)
  }
  lazy val configVars = new ConfigVars(environmentConfig, appEnvironment) // Access ENV vars from here
  lazy val keenInitialization = new KeenInitialization(configVars)
  lazy val mailerComponent = new Mailer(mailerClient, appEnvironment)

  lazy val signer = new hmac.Signer(configVars)
  lazy val platform = new Platform(configVars)

  // Model Services
  lazy val jsonBuilder = new JsonBuilder(configVars)
  lazy val appConfigService = new AppConfigService(database, jsonBuilder)
  lazy val distributorService = new DistributorService(database)
  lazy val distributorUserService = new DistributorUserService(distributorService, database)
  lazy val appService = new AppService(appConfigService, database)
  lazy val keenExportService = new KeenExportService(configVars)
  lazy val keenExport = new KeenExport(appService, keenExportService, configVars, actorSystem)
  lazy val adProviderService = new AdProviderService(database, platform)
  lazy val waterfallAdProviderService = new WaterfallAdProviderService(appConfigService, database)
  lazy val waterfallService = new WaterfallService(waterfallAdProviderService, database)
  lazy val virtualCurrencyService = new VirtualCurrencyService(database)

  // Instance of class that will kick off reporting
  lazy val revenueDataWorker = new RevenueDataActorWorker(
    waterfallAdProviderService,
    appService,
    platform,
    database,
    actorSystem,
    configVars,
    keenInitialization,
    appEnvironment,
    wsClient
  )

  // Helper class to encapsulate many service classes
  lazy val modelService = new ModelService(
    distributorUserService,
    distributorService,
    appService,
    adProviderService,
    waterfallService,
    waterfallAdProviderService,
    appConfigService,
    virtualCurrencyService,
    jsonBuilder,
    platform,
    environmentConfig
  )

  // Controllers
  lazy val distributorUsersController = new controllers.DistributorUsersController(actorSystem, modelService, configVars, mailerComponent, database)
  lazy val appsController = new controllers.AppsController(modelService, database, actorSystem, wsClient, mailerComponent, configVars, appEnvironment)
  lazy val analyticsController = new controllers.AnalyticsController(modelService, keenExport, configVars, wsClient, mailerComponent)
  lazy val waterfallsController = new controllers.WaterfallsController(modelService, database)
  lazy val waterfallAdProvidersController = new controllers.WaterfallAdProvidersController(modelService, database)
  lazy val apiController = new controllers.APIController(modelService, signer, configVars, database, wsClient)
  lazy val applicationController = new controllers.Application(distributorUserService)
  lazy val configVarsController = new controllers.ConfigVarsController(configVars, appEnvironment)

  // Assets
  lazy val assets = new controllers.Assets(httpErrorHandler)

  // Routes
  lazy val router = new Routes(
    httpErrorHandler,
    applicationController,
    distributorUsersController,
    appsController,
    analyticsController,
    waterfallsController,
    waterfallAdProvidersController,
    apiController,
    configVarsController,
    assets
  )
}
// $COVERAGE-ON$
