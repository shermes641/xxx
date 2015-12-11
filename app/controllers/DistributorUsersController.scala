package controllers

import akka.actor.Props
import models._
import play.api.libs.concurrent.Akka
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._
import play.api.Play
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
            val userIPAddress: String = request.headers.get("X-Forwarded-For") match {
              case Some(ip: String) => ip
              case None => request.remoteAddress
            }
            emailActor ! sendUserCreationEmail(signup.email, signup.company, userIPAddress)
            DistributorUser.find(id) match {
              case Some(user: DistributorUser) => {
                Ok(Json.obj("status" -> "success", "distributorID" -> user.distributorID.get.toString)).withSession(Security.username -> user.email, "distributorID" -> user.distributorID.get.toString)
              }
              case None => {
                BadRequest(Json.obj("status" -> "error", "message" -> "Something went wrong. Please try logging in again."))
              }
            }
          }
          case _ => {
            delayedResponse("This email has been registered already. Try logging in.", "email")
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
      Redirect(routes.AnalyticsController.show(currentUser.distributorID.get, None, None))
    }.getOrElse {
      Ok(views.html.DistributorUsers.signup())
    }
  }

  /**
   * Renders form for DistributorUser log in.
   * @return Log in form
   */
  def login(recentlyLoggedOut: Option[Boolean]) = Action { implicit request =>
    request.session.get("username").map { user =>
      DistributorUser.findByEmail(user) match {
        case Some(currentUser) => {
          Redirect(routes.AnalyticsController.show(currentUser.distributorID.get, None, None))
        }
        case None => {
          Ok(views.html.DistributorUsers.login()).withNewSession
        }
      }
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
                Redirect(routes.AnalyticsController.show(currentUser.distributorID.get, None, None)).withSession(Security.username -> user.email, "distributorID" -> currentUser.distributorID.get.toString())
              }
              case false => {
                delayedResponse("Invalid Password.", "password")
              }
            }
          }
          case None => {
            delayedResponse("Email not found.  Please Sign up.", "email")
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
    Redirect(routes.DistributorUsersController.login(Some(true))).withNewSession
  }

  /**
   * Renders view to initiate the password reset email
   */
  def forgotPassword() = Action { implicit request =>
    Ok(views.html.DistributorUsers.forgot_password())
  }

  lazy val resetPasswordActor = Akka.system.actorOf(Props(new PasswordResetActor))

  /**
   * Accepts request from reset password email page and sends the user an email
   */
  def sendPasswordResetEmail() = Action { implicit request =>
    request.body.asJson.map { json =>
      (json \ "email").validate[String] match {
        case email: JsSuccess[String] => {
          DistributorUser.findByEmail(email.get) match {
            case Some(currentUser: DistributorUser) => {
              resetPasswordActor ! currentUser
              Ok(Json.obj("status" -> "success", "message" -> "Password reset email sent!"))
            }
            case None => {
              delayedResponse("Email not found. Please Sign up.", "email")
            }
          }
        }
        case error: JsError => {
          BadRequest(Json.obj("status" -> "error", "message" -> JsString(error.get)))
        }
      }
    }.getOrElse {
      BadRequest(Json.obj("status" -> "error", "message" -> "Invalid Request."))
    }
  }

  /**
   * Encapsulates all necessary info for resetting a user's password
   * @param email The email of the DistributorUser
   * @param distributorUserID The ID of the DistributorUser
   * @param token The one-time use token stored in the password_resets table
   * @param password The updated password
   * @param passwordConfirmation The confirmation of the updated password
   */
  case class PasswordUpdate(email: String, distributorUserID: Long, token: String, password: String, passwordConfirmation: String)

  implicit val passwordUpdateReads: Reads[PasswordUpdate] = (
    (JsPath \ "email").read[String] and
    (JsPath \ "distributor_user_id").read[Long] and
    (JsPath \ "token").read[String] and
    (JsPath \ "password").read[String] and
    (JsPath \ "confirmation").read[String]
  )(PasswordUpdate.apply _)

  /**
   * Updates the password and completes the password reset process
   */
  def updatePassword() = Action { implicit request =>
    request.body.asJson.map { json =>
      json.validate[PasswordUpdate].map { passwordUpdate =>
        if(PasswordReset.isValid(passwordUpdate.distributorUserID, passwordUpdate.token)) {
          DistributorUser.findByEmail(passwordUpdate.email) match {
            case Some(user) => {
              DistributorUser.updatePassword(passwordUpdate) match {
                case 1 => {
                  PasswordReset.complete(user.id.get, passwordUpdate.token)
                  resetPasswordActor ! user.email // Send followup email to alert user of password change
                  Redirect(routes.AnalyticsController.show(user.distributorID.get, None, None)).withSession(Security.username -> user.email, "distributorID" -> user.distributorID.get.toString())
                }
                case _ => {
                  BadRequest(Json.obj("status" -> "error", "message" -> "Password could not be updated."))
                }
              }
            }
            case None => BadRequest(Json.obj("status" -> "error", "message" -> "Could not find user."))
          }
        } else {
          BadRequest(Json.obj("status" -> "error", "message" -> "Password reset token is no longer valid. Please generate a new forgotten password request."))
        }
      }.recoverTotal {
        error => BadRequest(Json.obj("status" -> "error", "message" -> JsError.toFlatJson(error)))
      }
    }.getOrElse {
      BadRequest(Json.obj("status" -> "error", "message" -> "Invalid Request."))
    }
  }

  /**
   * Renders the page to update a user's password
   * @param userEmail The DistributorUsers's email
   * @param resetToken The one-time use token stored in the password_resets table
   * @param distributorUserID The ID of the DistributorUser
   * @return If the resetToken is valid, return the password update page; otherwise, redirect to the login page
   */
  def resetPassword(userEmail: Option[String], resetToken: Option[String], distributorUserID: Option[Long]) = Action { implicit request =>
    (userEmail, resetToken, distributorUserID) match {
      case (Some(email), Some(token), Some(distributorUserIDVal)) => {
        DistributorUser.findByEmail(email) match {
          case Some(user) if(PasswordReset.isValid(distributorUserIDVal, token)) => {
            Ok(views.html.DistributorUsers.reset_password())
          }
          case _ => Redirect(routes.Application.notFound())
        }
      }
      case _ => Redirect(routes.Application.notFound())
    }
  }

  /**
   * Delays response before rendering a view to discourage brute force attacks.
   * This is used for failures on the sign up or sign in actions.
   * @param errorMessage The error message to be displayed in the login form.
   * @param fieldName The name of the field under which the error message will be displayed.
   * @return a 400 response and render a form with errors.
   */
  def delayedResponse(errorMessage: String, fieldName: String): Result = {
    val delayValue: Long = Play.current.configuration.getLong("authentication_failure_delay").getOrElse(1000)
    Thread.sleep(delayValue)
    BadRequest(Json.obj("status" -> "error", "message" -> errorMessage, "fieldName" -> fieldName))
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
  def onUnauthorized(request: RequestHeader): Result = Results.Redirect(routes.DistributorUsersController.login(None))

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
              case _ => Results.Redirect(routes.DistributorUsersController.login(None))
            }
          }
          case _ => Results.Redirect(routes.DistributorUsersController.login(None))
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
