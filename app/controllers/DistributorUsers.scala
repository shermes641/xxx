package controllers

import models.DistributorUser
import models.Mailer
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._

object DistributorUsers extends Controller {
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
            Redirect(routes.DistributorUsers.signup).flashing("error" -> "This email has been registered already. Try logging in.")
          }
        }
      }
    )
  }

  def signup = Action { implicit request =>
    Ok(views.html.DistributorUsers.signup(signupForm))
  }
}

case class Signup(email: String, password: String, confirmation: String, agreeToTerms: Boolean) extends Mailer {
  def sendWelcomeEmail(): Unit = {
    val subject = "Welcome to HyprMediation"
    val body = "Welcome to HyprMediation!"
    sendEmail(email, subject, body)
  }
}
