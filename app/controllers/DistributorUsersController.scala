package controllers

import models.DistributorUser
import models.Mailer
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._

object DistributorUsersController extends Controller with Secured {
  val signupForm = Form[Signup](
      mapping(
      "email" -> nonEmptyText,
      "password" -> text.verifying("Password must be at least 8 characters",
      result => result match {
        case (password: String) => password.length() >= 8
      }),
      "confirmation" -> nonEmptyText,
      "terms" -> checked("").verifying("Please agree to our terms and conditions",
      result => result match {
        case (check: Boolean) => check
      })
    )
    (Signup.apply)(Signup.unapply)
    verifying ("Passwords do not match", result => result match {
      case (signup: Signup) => signup.password == signup.confirmation
    })
  )

  def create = Action { implicit request =>
    signupForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.DistributorUsers.signup(formWithErrors)),
      signup => {
        DistributorUser.create(signup.email, signup.password) match {
          case Some(id) => {
            // Email credentials need to be configured
            // signup.sendWelcomeEmail()
            Redirect(routes.Application.index).flashing("success" -> "Your confirmation email will arrive shortly.")
          }
          case _        => {
            Redirect(routes.DistributorUsersController.signup).flashing("error" -> "This email has been registered already. Try logging in.")
          }
        }
      }
    )
  }

  def signup = Action { implicit request =>
    Ok(views.html.DistributorUsers.signup(signupForm))
  }

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
    Ok(views.html.DistributorUsers.login(loginForm))
  }

  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.DistributorUsers.login(formWithErrors)),
      user => Redirect(routes.Application.index).withSession(Security.username -> user._1)
    )
  }

  def logout = Action {
    Redirect(routes.Application.index).withNewSession.flashing(
      "success" -> "You are now logged out."
    )
  }
}

case class Signup(email: String, password: String, confirmation: String, agreeToTerms: Boolean) extends Mailer {
  def sendWelcomeEmail(): Unit = {
    val subject = "Welcome to HyprMediation"
    val body = "Welcome to HyprMediation!"
    sendEmail(email, subject, body)
  }
}

trait Secured {
  def username(request: RequestHeader) = request.session.get(Security.username)

  def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.DistributorUsersController.login)

  def withAuth(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }

  def withUser(f: DistributorUser => Request[AnyContent] => Result ) = withAuth { email => implicit request =>
    val optionalUser = DistributorUser.findByEmail(email)
    optionalUser match {
      case Some(user) => f(user)(request)
      case None => onUnauthorized(request)
    }
  }
}
