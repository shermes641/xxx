import models._
import models.Environment
import play.api._
import play.api.ApplicationLoader.Context
import play.api.db.evolutions.Evolutions
import play.api.db.{DBComponents, Database, HikariCPComponents}
import play.api.libs.mailer._
import play.api.libs.ws.ning.NingWSClient
import play.api.mvc._
import play.api.mvc.Results._
import router.Routes
import scala.concurrent.Future

/**
  * Loads all dependencies for the HyprMediate app
  */
// $COVERAGE-OFF$
class MainApplicationLoader extends ApplicationLoader {
  def load(context: Context) = {
    lazy val mainComponents = new MainComponents(context)
    lazy val mainApp = mainComponents.application
    Startup.beforeStart(mainComponents.configVars, mainComponents.appEnvironment)
    Startup.onStart(mainComponents.modelService, mainComponents.appEnvironment)
    mainApp
  }
}

/**
  * Encapsulates all dependencies that will be injected when the app is loaded
 *
  * @param context The context in which the app is running
  */
class MainComponents(context: Context) extends BuiltInComponentsFromContext(context) with MailerComponents with DBComponents with HikariCPComponents {
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

  // Filters
  val httpAuthFilter = new HTTPAuthFilter(appEnvironment, environmentConfig)
  val httpsFilter = new HTTPSFilter(appEnvironment)
  // Override filters so they are used in the app
  override lazy val httpFilters = Seq(httpAuthFilter, httpsFilter)
}

/**
  * Filter for adding basic auth on staging environments
  * @param appEnvironment Class encapsulating environment information
  * @param config         The shared Play environment configuration
  */
class HTTPAuthFilter(appEnvironment: Environment, config: Configuration) extends Filter {
  def apply(next: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {
    request.tags.get("ROUTE_CONTROLLER") match {
      case Some(controller: String) if controller != "controllers.APIController" && controller != "controllers.Assets" && appEnvironment.isStaging =>
        val httpAuthUser = config.getString(Constants.AppConfig.HttpAuthUser).get
        val httpAuthPassword = config.getString(Constants.AppConfig.HttpAuthPw).get

        request.headers.get("Authorization").flatMap { authorization =>
          authorization.split(" ").drop(1).headOption.filter { encoded =>
            new String(org.apache.commons.codec.binary.Base64.decodeBase64(encoded.getBytes)).split(":").toList match {
              case user :: password :: Nil if user == httpAuthUser && password == httpAuthPassword => true
              case _ => false
            }
          }.map(_ => next(request))
        }.getOrElse {
          Future.successful(Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="Secured""""))
        }
      case _ =>
        next(request)
    }
  }
}

/**
  * Filter to redirect to HTTPS in production
  * @param appEnvironment Class encapsulating environment information
  */
class HTTPSFilter(appEnvironment: Environment) extends Filter {
  def apply(next: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {
    if(appEnvironment.isProdOrStaging && !request.headers.get("x-forwarded-proto").getOrElse("").contains("https")) {
      Future.successful(MovedPermanently("https://" + request.host + request.uri))
    } else {
      next(request)
    }
  }
}
// $COVERAGE-ON$
