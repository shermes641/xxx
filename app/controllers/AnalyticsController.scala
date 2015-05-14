package controllers

import play.api._
import play.api.mvc._
import play.api.Play.current
import anorm._
import play.api.db.DB
import models._
import play.api.libs.json._
import io.keen.client.java.{ScopedKeys, KeenProject, JavaKeenClientBuilder, KeenClient}
import collection.JavaConversions._
import scala.language.implicitConversions

object AnalyticsController extends Controller with Secured {
  def show(distributorID: Long, currentAppID: Option[Long], waterfallFound: Option[Boolean]) = withAuth(Some(distributorID)) { username => implicit request =>
    val apps = App.findAllAppsWithWaterfalls(distributorID)
    if(apps.size == 0) {
      Redirect(routes.AppsController.newApp(distributorID))
    } else {
      Ok(views.html.Analytics.show(distributorID = distributorID, appID = currentAppID, apps = apps, adProviders = AdProvider.findAll, keenProject = Play.current.configuration.getString("keen.project").get, scopedKey = getScopedReadKey(distributorID)))
    }
  }

  def export(distributorID: Long) = withAuth(Some(distributorID)) { username => implicit request =>

    request.body.asJson.map { json =>
      ((json \ "email").asOpt[String], (json \ "filters").asOpt[JsArray],
       (json \ "timeframe").asOpt[JsObject], (json \ "apps").asOpt[List[String]]) match {
        case (email: Option[String], filters: Option[JsArray], timeframe: Option[JsObject], selectedApps: Option[List[String]]) => {
          KeenExport().exportToCSV(distributorID, email.get, filters.get, timeframe.get, selectedApps.get)
          Ok("success")
        }
        case (email, filters, timeframe, selectedApps) => {
          BadRequest("Missing parameter(s) - Email: " + email + " Filters: " + filters +  " Timeframe: " + timeframe + " Apps: " + selectedApps)
        }
      }
    }.getOrElse {
      BadRequest("Expecting Json data")
    }
  }

  def info(distributorID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
        Ok(Json.obj(
          "distributorID" -> JsNumber(distributorID),
          "adProviders" -> adProviderListJs(AdProvider.findAll),
          "apps" -> appListJs(App.findAll(distributorID)),
          "keenProject" -> Play.current.configuration.getString("keen.project").get,
          "scopedKey" -> getScopedReadKey(distributorID)
        ))
  }

  // Uses the keen library to get a scoped read key
  def getScopedReadKey(distributorID: Long) = {
    val client = new JavaKeenClientBuilder().build()
    val project = new KeenProject(Play.current.configuration.getString("keen.project").get, Play.current.configuration.getString("keen.writeKey").get, Play.current.configuration.getString("keen.readKey").get)
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

    val scopedKey = ScopedKeys.encrypt(Play.current.configuration.getString("keen.masterKey").get,scope)
    scopedKey
  }


  /**
   * Converts an instance of the AdProvider class to a JSON object.
   * @param provider An instance of the AdProvider class.
   * @return A JSON object.
   */
  implicit def adProviderWrites(provider: AdProvider): JsObject = {
    JsObject(
      Seq(
        "name" -> JsString(provider.name),
        "id" -> JsNumber(provider.id)
      )
    )
  }

  /**
   * Converts a list of AdProviders instances to a JsArray.
   * @param list A list of AdProvider instances.
   * @return A JsArray containing AdProvider objects.
   */
  def adProviderListJs(list: List[AdProvider]): JsArray = {
    list.foldLeft(JsArray(Seq()))((array, adProvider) => array ++ JsArray(Seq(adProvider)))
  }
  /**
   * Converts an instance of the App class to a JSON object.
   * @param app An instance of the App class.
   * @return A JSON object.
   */
  implicit def appWrites(app: App): JsObject = {
    JsObject(
      Seq(
        "id" -> JsString(app.id.toString),
        "distributorID" -> JsNumber(app.distributorID),
        "name" -> JsString(app.name)
      )
    )
  }

  /**
   * Converts a list of AppWithWaterfallID instances to a JsArray.
   * @param list A list of AppWithWaterfallID instances.
   * @return A JsArray containing AppWithWaterfallID objects.
   */
  def appListJs(list: List[App]): JsArray = {
    list.foldLeft(JsArray(Seq()))((array, app) => array ++ JsArray(Seq(app)))
  }
}

