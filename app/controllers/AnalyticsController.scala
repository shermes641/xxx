package controllers

import play.api._
import play.api.mvc._
import play.api.Play.current
import anorm._
import play.api.db.DB
import models._
import play.api.libs.json._

object AnalyticsController extends Controller {
  def show(distributorID: Long, appID: Long) = Action {
    val app = App.find(appID)

    Ok(views.html.Analytics.show(app.get, distributorID, appID, AdProvider.findAll))
  }
}

