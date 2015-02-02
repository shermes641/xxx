package controllers

import akka.actor.Props
import models.{sendUserCreationEmail, WelcomeEmailActor, DistributorUser}
import play.api.libs.concurrent.Akka
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, JsError, JsPath, Reads}
import play.api.mvc._
import play.api.Play.current

/** Controller for models.DistributorUser instances. */
object DistributorUsersController extends Controller with Secured with CustomFormValidation {
  implicit val signupReads: Reads[Signup] = (
    (JsPath \ "company").read[String] and
    (JsPath \ "email").read[String] and
    (JsPath \ "password").read[String] and
    (JsPath \ "confirmation").read[String] and
    (JsPath \ "terms").read[Boolean]
  )(Signup.apply _)

  /**
   * Creates a new DistributorUser in the database.
   * @return Responds with 201 when DistributorUser is properly persisted and redirects to the login page if the email is already taken.
   */
  def create = Action { implicit request =>
    request.body.asJson.map { json =>
      json.validate[Signup].map { signup =>
        DistributorUser.create(signup.email, signup.password, signup.company) match {
          case Some(id: Long) => {
            val emailActor = Akka.system.actorOf(Props(new WelcomeEmailActor))
            emailActor ! sendUserCreationEmail(signup.email, signup.company)
            DistributorUser.find(id) match {
              case Some(user: DistributorUser) => {
                Redirect(routes.AppsController.newApp(user.distributorID.get)).withSession(Security.username -> user.email, "distributorID" -> user.distributorID.get.toString).flashing("success" -> "Your confirmation email will arrive shortly.")
              }
              case None => {
                BadRequest(Json.obj("status" -> "error", "message" -> "Something went wrong. Please try logging in again."))
              }
            }
          }
          case _ => {
            delayedResponse("This email has been registered already. Try logging in.")
          }
        }
      }.recoverTotal {
        error => BadRequest(Json.obj("status" -> "error", "message" -> JsError.toFlatJson(error)))
      }
    }.getOrElse {
      BadRequest(Json.obj("status" -> "error", "message" -> "Invalid Request."))
    }
  }

  /**
   * Renders form to create a new DistributorUser.
   * @return Form for sign up
   */
  def signup = Action { implicit request =>
    request.session.get("username").map { user =>
      val currentUser = DistributorUser.findByEmail(user).get
      Redirect(routes.AnalyticsController.show(currentUser.distributorID.get, None))
    }.getOrElse {
      Ok(views.html.DistributorUsers.signup())
    }
  }

  /**
   * Renders form for DistributorUser log in.
   * @return Log in form
   */
  def login = Action { implicit request =>
    request.session.get("username").map { user =>
      val currentUser = DistributorUser.findByEmail(user).get
      Redirect(routes.AnalyticsController.show(currentUser.distributorID.get, None))
    }.getOrElse {
      Ok(views.html.DistributorUsers.login())
    }
  }

  implicit val loginReads: Reads[Login] = (
    (JsPath \ "email").read[String] and
      (JsPath \ "password").read[String]
  )(Login.apply _)

  /**
   * Authenticates DistributorUser and creates a new session.
   * @return Redirect to Application index if successful and stay on log in page otherwise.
   */
  def authenticate = Action { implicit request =>
    request.body.asJson.map { json =>
      json.validate[Login].map { user =>
        DistributorUser.findByEmail(user.email) match {
          case Some(currentUser) => {
            DistributorUser.checkPassword(user.email, user.password) match {
              case true => {
                Redirect(routes.AnalyticsController.show(currentUser.distributorID.get, None)).flashing("success" -> "App updated successfully.")
              }
              case false => {
                delayedResponse("Invalid Password.")
              }
            }
          }
          case None => {
            delayedResponse("Email not found.  Please Sign up.")
          }
        }
      }.recoverTotal {
        error => BadRequest(Json.obj("status" -> "error", "message" -> JsError.toFlatJson(error)))
      }
    }.getOrElse {
      BadRequest(Json.obj("status" -> "error", "message" -> "Invalid Request."))
    }
  }

  /**
   * Logs out DistributorUser and ends session.
   * @return Redirects to Application index.
   */
  def logout = Action {
    Redirect(routes.DistributorUsersController.login).withNewSession.flashing(
      "success" -> "You are now logged out."
    )
  }

  /**
   * Delays response before rendering a view to discourage brute force attacks.
   * This is used for failures on the sign up or sign in actions.
   * @param errorMessage The error message to be displayed in the login form.
   * @return a 400 response and render a form with errors.
   */
  def delayedResponse(errorMessage: String): Result = {
    Thread.sleep(1000)
    BadRequest(Json.obj("status" -> "error", "message" -> errorMessage))
  }
}

/**
 * Used for mapping sign up form fields to DistributorUser attributes.
 * @param company Company name to be used for creating a Distributor.
 * @param email Email for new DistributorUser.
 * @param password Password for new DistributorUser.
 * @param confirmation Password confirmation for new DistributorUser.
 * @param agreeToTerms Boolean checkbox for terms of service.
 */
case class Signup(company: String, email: String, password: String, confirmation: String, agreeToTerms: Boolean)

/**
 * Used for mapping log in form fields to DistributorUser attributes.
 * @param email Email for current DistributorUser.
 * @param password Password for current DistributorUser.
 */
case class Login(email: String, password: String)

/** Handles authentication for DistributorUsers. */
trait Secured {
  /**
   * Retrieves username for DistributorUser.
   * @param request Current request
   * @return username for DistributorUser if available.
   */
  def username(request: RequestHeader): Option[String] = request.session.get(Security.username)

  /**
   * Redirects unauthorized requests to log in page.
   * @param request Current request.
   * @return Renders log in view.
   */
  def onUnauthorized(request: RequestHeader): Result = Results.Redirect(routes.DistributorUsersController.login)

  /**
   * Authenticates DistributorUser for controller actions.
   * @param controllerDistributorID Distributor ID passed from a controller action.
   * @param controllerAction Function corresponding to a controller action which returns DistributorUser username.
   * @return Continue request if DistributorUser is logged in.  Otherwise, redirect to log in page.
   */
  def withAuth(controllerDistributorID: Option[Long])(controllerAction: => String => Request[AnyContent] => Result): EssentialAction = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => {
        controllerDistributorID match {
          case Some(ctrlDistID) => {
            request.session.get("distributorID") match {
              // Check if Distributor ID from session matches the ID passed from the controller
              case Some(sessionDistID) if (sessionDistID == ctrlDistID.toString()) => {
                controllerAction(user)(request)
              }
              case _ => Results.Redirect(routes.DistributorUsersController.login)
            }
          }
          case _ => Results.Redirect(routes.DistributorUsersController.login)
        }
      })
    }
  }

  /**
   * Finds DistributorUser by email during controller actions.
   * @param controllerAction Function corresponding to a controller action which returns the current DistributorUser.
   * @return If DistributorUser is found, continue request.  Otherwise, redirect to log in page.
   */
  def withUser(controllerAction: DistributorUser => Request[AnyContent] => Result ): EssentialAction = withAuth(None) { email => implicit request =>
    val optionalUser = DistributorUser.findByEmail(email)
    optionalUser match {
      case Some(user) => controllerAction(user)(request)
      case None => onUnauthorized(request)
    }
  }
}
