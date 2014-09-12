package controllers

import play.api._
import play.api.mvc._
import play.api.Play.current
import anorm._
import play.api.db.DB
import models._
import play.api.libs.json._

object AnalyticsController extends Controller with Secured {
  def show(distributorID: Long, appID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    App.find(appID) match {
      case Some(app) => {
        Ok(views.html.Analytics.show(app, distributorID, appID, AdProvider.findAll))
      }
      case None => {
        Redirect(routes.AppsController.index(distributorID)).flashing("error" -> "App could not be found.")
      }
    }
  }
}

