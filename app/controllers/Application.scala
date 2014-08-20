package controllers

import play.api.mvc._

object Application extends Controller with Secured {

  def index = withUser { user => implicit request =>
    Redirect(routes.AppsController.index(user.distributorID.get))
  }

}
