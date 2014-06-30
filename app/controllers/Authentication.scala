package controllers

import anorm.NotAssigned
import models.DistributorUser
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import views.html

/**
 * Created by jeremy on 6/30/14.
 */
object Authentication extends Controller with Secured {

  val loginForm = Form(
    tuple(
      "email" -> text,
      "password" -> text
    ) verifying ("Invalid email or password", result => result match {
      case (email, password) => check(email, password)
    })
  )

  def check(email: String, password: String) = {
    DistributorUser.checkPassword(email, password)
  }

  def login = Action { implicit request =>
    Ok(html.login(loginForm))
  }

  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.login(formWithErrors)),
      user => Redirect(routes.Application.index).withSession(Security.username -> user._1)
    )
  }

  def logout = Action {
    Redirect(routes.Application.index).withNewSession.flashing(
      "success" -> "You are now logged out."
    )
  }
}


trait Secured {

  def username(request: RequestHeader) = request.session.get(Security.username)

  def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Authentication.login)

  def withAuth(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }

  def withUser(f: DistributorUser => Request[AnyContent] => Result ) = withAuth { username => implicit request =>
    f(DistributorUser(NotAssigned, username, "hashed_password", "Salt"))(request)
  }
}