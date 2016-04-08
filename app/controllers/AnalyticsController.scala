package controllers

import collection.JavaConversions._
import io.keen.client.java.{JavaKeenClientBuilder, KeenClient, KeenProject, ScopedKeys}
import javax.inject._
import models._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc._
import scala.language.implicitConversions

/**
  * Controller for all analytics actions
  * @param modelService Helper class containing dependency-injected instances of service classes
  * @param keenExport   Shared instance of KeenExport class
  * @param configVars   Shared ENV configuration variables
  * @param wsClient     Shared instance of web service client
  * @param mailer       Shared instance of Mailer class
  */
@Singleton
class AnalyticsController @Inject() (modelService: ModelService,
                                     keenExport: KeenExport,
                                     configVars: ConfigVars,
                                     wsClient: WSClient,
                                     mailer: Mailer) extends Controller with Secured {
  val authUser = modelService.distributorUserService // Used in Secured trait
  val distributorUserService = modelService.distributorUserService
  val appService = modelService.appService
  val adProviderService = modelService.adProviderService
  val platform = modelService.platform
  val environmentConfig = modelService.environmentConfig

  implicit val exportReads: Reads[exportMapping] = (
    (JsPath \ "displayFillRate").read[Boolean] and
      (JsPath \ "email").read[String] and
      (JsPath \ "filters").read[JsArray] and
      (JsPath \ "timeframe").read[JsObject] and
      (JsPath \ "apps").read[List[String]] and
      (JsPath \ "ad_providers_selected").read[Boolean]
    ) (exportMapping.apply _)

  def show(distributorID: Long, currentAppID: Option[Long], waterfallFound: Option[Boolean]) = withAuth(Some(distributorID)) { username => implicit request =>
    val apps = appService.findAllAppsWithWaterfalls(distributorID)
    if(apps.isEmpty) {
      Redirect(routes.AppsController.newApp(distributorID))
    } else {
      Ok(views.html.Analytics.show(
        distributorID = distributorID,
        appID = currentAppID,
        apps = apps,
        adProviders = adProviderService.findAll,
        keenProject = configVars.ConfigVarsKeen.projectID,
        scopedKey = getScopedReadKey(distributorID)
      ))
    }
  }

  def export(distributorID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    request.body.asJson.map { json =>
      json.validate[exportMapping].map { exportParameters =>
        keenExport.exportToCSV(distributorID, exportParameters.displayFillRate, exportParameters.email, exportParameters.filters,
          exportParameters.timeframe, exportParameters.apps, exportParameters.ad_providers_selected, getScopedReadKey(distributorID), wsClient, mailer)
        Ok("success")
      }.recoverTotal {
        error => BadRequest(Json.obj("status" -> "error", "message" -> "Missing parameters"))
      }
    }.getOrElse {
      BadRequest("Expecting Json data")
    }
  }

  def info(distributorID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    // There are duplicate records in the ad_providers table for iOS and Android-based AdProviders.
    // To prevent showing duplicate ad provider names in the analytics dashboard, we only select unique ad provider names.
    val adProviders = adProviderService.findAll
      .foldLeft(Set[AdProvider]())((providers, provider) =>
        if (providers.count(e => e.name == provider.name) > 0) providers else providers + provider
      ).toList

    Ok(Json.obj(
      "distributorID" -> JsNumber(distributorID),
      "adProviders" -> adProviderListJs(adProviders),
      "apps" -> appListJs(appService.findAll(distributorID)),
      "keenProject" -> configVars.ConfigVarsKeen.projectID,
      "scopedKey" -> getScopedReadKey(distributorID)
    ))
  }

  // Uses the keen library to get a scoped read key
  def getScopedReadKey(distributorID: Long) = {
    val client = new JavaKeenClientBuilder().build()
    val project = new KeenProject(
      configVars.ConfigVarsKeen.projectID,
      configVars.ConfigVarsKeen.writeKey,
      configVars.ConfigVarsKeen.readKey
    )
    client.setDefaultProject(project)
    KeenClient.initialize(client)

    // Manual convert filter to Java map due to JSON library not supporting embedded maps
    // http://stackoverflow.com/questions/12113010/scala-responsebody-and-map
    def filter(): java.util.Map[String, Any] = {
      Map(
        "property_name" -> "distributor_id",
        "operator" -> "eq",
        "property_value" -> distributorID
      )
    }

    val scope = Map(
      "filters" -> Array(
        filter()
      ),
      "allowed_operations" -> Array("read")
    )

    val scopedKey = ScopedKeys.encrypt(configVars.ConfigVarsKeen.masterKey, scope)
    scopedKey
  }


  /**
    * Converts an instance of the AdProvider class to a JSON object.
    *
    * @param provider An instance of the AdProvider class.
    * @return A JSON object.
    */
  implicit def adProviderWrites(provider: AdProvider): JsObject = {
    JsObject(
      Seq(
        "name" -> JsString(provider.displayName),
        "id" -> JsString(provider.name)
      )
    )
  }

  /**
    * Converts a list of AdProviders instances to a JsArray.
    *
    * @param list A list of AdProvider instances.
    * @return A JsArray containing AdProvider objects.
    */
  def adProviderListJs(list: List[AdProvider]): JsArray = {
    list.foldLeft(JsArray(Seq()))((array, adProvider) => array ++ JsArray(Seq(adProvider)))
  }

  /**
    * Converts an instance of the App class to a JSON object.
    *
    * @param app An instance of the App class.
    * @return A JSON object.
    */
  implicit def appWrites(app: App): JsObject = {
    val platformName = platform.find(app.platformID).PlatformName
    JsObject(
      Seq(
        "id" -> JsString(app.id.toString),
        "distributorID" -> JsNumber(app.distributorID),
        "name" -> JsString(app.name),
        "platformName" -> JsString(platformName)
      )
    )
  }

  /**
    * Converts a list of AppWithWaterfallID instances to a JsArray.
    *
    * @param list A list of AppWithWaterfallID instances.
    * @return A JsArray containing AppWithWaterfallID objects.
    */
  def appListJs(list: List[App]): JsArray = {
    list.foldLeft(JsArray(Seq()))((array, app) => array ++ JsArray(Seq(app)))
  }

  /**
    * Used for mapping Export parameters
    *
    * @param displayFillRate       Indicates if fill rate should be shown in the CSV.
    *                              When True (one ad provider or "all" ad providers are selected), we display the fill rate.
    *                              When False (more than one ad provider selected), we display "N/A" for fill rate.
    * @param email                 Maps to the email field
    * @param filters               Maps to the filters JsArray
    * @param timeframe             Maps to the timeframe as a JsObject
    * @param apps                  ad_providers_selected to the apps list in the Json Array
    * @param ad_providers_selected Maps to the ad_providers_selected Boolean
    */
  case class exportMapping(displayFillRate: Boolean, email: String, filters: JsArray, timeframe: JsObject, apps: List[String], ad_providers_selected: Boolean)

}
