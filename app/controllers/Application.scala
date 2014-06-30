package controllers

import play.api.mvc._
import views.html


object Application extends Controller with Secured {

  def index = withUser { user => implicit request =>
    val s = "Hi"
    Ok(html.index(s, user))
  }

}
