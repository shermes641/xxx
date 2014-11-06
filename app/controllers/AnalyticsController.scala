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

object AnalyticsController extends Controller with Secured {
  def show(distributorID: Long, appID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    App.find(appID) match {
      case Some(app) => {
        Ok(views.html.Analytics.show(app, distributorID, appID, App.findAll(distributorID), AdProvider.findAll, Play.current.configuration.getString("keen.project").get, getScopedReadKey(distributorID)))
      }
      case None => {
        Redirect(routes.AppsController.index(distributorID)).flashing("error" -> "App could not be found.")
      }
    }
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
}

