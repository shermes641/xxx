package controllers

import models.{WelcomeEmailActor, DistributorUser}
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import play.api.libs.concurrent.Akka
import akka.actor.Props
import play.api.Play.current
import play.twirl.api.Html

/** Controller for models.DistributorUser instances. */
object DistributorUsersController extends Controller with Secured with CustomFormValidation {
  // Form mapping used in new and create actions
  val signupForm = Form[Signup](
      mapping(
      "company" -> text.verifying(nonEmptyConstraint("Company Name")),
      "email" -> text.verifying(nonEmptyConstraint("Email")),
      "password" -> text.verifying("Password must be at least 8 characters",
      result => result match {
        case (password: String) => password.length() >= 8
      }),
      "confirmation" -> text.verifying(nonEmptyConstraint("Password confirmation")),
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

  /**
   * Creates a new DistributorUser in the database.
   * @return Responds with 201 when DistributorUser is properly persisted and redirects to the login page if the email is already taken.
   */
  def create = Action { implicit request =>
    signupForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.DistributorUsers.signup(formWithErrors)),
      signup => {
        DistributorUser.create(signup.email, signup.password, signup.company) match {
          case Some(id: Long) => {
            val emailActor = Akka.system.actorOf(Props(new WelcomeEmailActor))
            emailActor ! signup.email
            DistributorUser.find(id) match {
              case Some(user: DistributorUser) => {
                Redirect(routes.AppsController.newApp(user.distributorID.get)).withSession(Security.username -> user.email, "distributorID" -> user.distributorID.get.toString).flashing("success" -> "Your confirmation email will arrive shortly.")
              }
              case None => {
                Redirect(routes.DistributorUsersController.signup).flashing("error" -> "User was not found.")
              }
            }
          }
          case _ => {
            val emailError = Seq(new play.api.data.FormError("email", "This email has been registered already. Try logging in."))
            val form = Form(signupForm.mapping, signupForm.data, emailError, signupForm.value).fill(signup)
            delayedResponse(views.html.DistributorUsers.signup(form))
          }
        }
      }
    )
  }

  /**
   * Renders form to create a new DistributorUser.
   * @return Form for sign up
   */
  def signup = Action { implicit request =>
    request.session.get("username").map { user =>
      val currentUser = DistributorUser.findByEmail(user).get
      Redirect(routes.AppsController.index(currentUser.distributorID.get))
    }.getOrElse {
      Ok(views.html.DistributorUsers.signup(signupForm))
    }
  }

  // Form mapping used in login and authenticate actions.
  val loginForm = Form[Login](
    mapping(
      "email" -> text,
      "password" -> text
    )
    (Login.apply)(Login.unapply)
      verifying ("Invalid email or password", result => result match {
      case (login: Login) => check(login.email, login.password)
    })
  )

  /**
   * Used to validate email/password combination on login.
   * @param email Email of current DistributorUser
   * @param password Password of current DistributorUser
   * @return True if email/password combination is valid and false otherwise.
   */
  def check(email: String, password: String): Boolean = {
    DistributorUser.checkPassword(email, password)
  }

  /**
   * Renders form for DistributorUser log in.
   * @return Log in form
   */
  def login = Action { implicit request =>
    request.session.get("username").map { user =>
      val currentUser = DistributorUser.findByEmail(user).get
      Redirect(routes.AppsController.index(currentUser.distributorID.get))
    }.getOrElse {
      Ok(views.html.DistributorUsers.login(loginForm))
    }
  }

  /**
   * Authenticates DistributorUser and creates a new session.
   * @return Redirect to Application index if successful and stay on log in page otherwise.
   */
  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => {
        delayedResponse(views.html.DistributorUsers.login(formWithErrors))
      },
      user => {
        val currentUser = DistributorUser.findByEmail(user.email).get
        Redirect(routes.AppsController.index(currentUser.distributorID.get)).withSession(Security.username -> user.email, "distributorID" -> currentUser.distributorID.get.toString())
      }
    )
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
   * @param view The form to be rendered after the delay.
   * @return a 400 response and render a form with errors.
   */
  def delayedResponse(view: Html): Result = {
    Thread.sleep(1000)
    BadRequest(view)
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
