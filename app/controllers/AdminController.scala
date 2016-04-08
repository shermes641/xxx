package controllers

import admin._
import be.objectify.deadbolt.scala.DeadboltActions
import javax.inject.Inject
import models._
import play.api.db.DB
import play.api.libs.json._
import play.api.mvc._
import play.api.Play.current
import security.AdminDeadboltHandler
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

/**
  * Controller for all Admin-related actions
  * @param modelService        Encapsulates all common models
  * @param adminService        Encapsulates Admin functions
  * @param securityRoleService Encapsulates SecurityRole functions
  * @param deadbolt            Deadbolt API actions
  */
class AdminController @Inject() (modelService: ModelService,
                                 adminService: AdminService,
                                 securityRoleService: SecurityRoleService,
                                 deadbolt: DeadboltActions) extends Controller {
  val distributorService = modelService.distributorService
  val distributorUserService = modelService.distributorUserService
  val appService = modelService.appService

  // Default error JSON that is returned for bad requests
  val errorJson = Json.obj(
    "status" -> "error"
  )

  /**
   * Converts the Distributor class into JSON
    *
    * @param distributor An instance of the Distributor class
   * @return            A JSON blob containing Distributor info
   */
  implicit def DistributorWrites(distributor: Distributor): JsObject = {
    Json.obj(
      "name" -> JsString(distributor.name),
      "id" -> JsNumber(distributor.id.get)
    )
  }

  /**
   * Converts the App class into JSON
    *
    * @param app An instance of the App class
   * @return    A JSON blob containing App info
   */
  implicit def AppWrites(app: App): JsObject = {
    Json.obj(
      "name" -> JsString(app.name),
      "token" -> JsString(app.token),
      "distributor_id" -> JsNumber(app.distributorID),
      "id" -> JsNumber(app.id),
      "app_config_refresh_interval" -> JsNumber(app.appConfigRefreshInterval)
    )
  }

  /**
   * Converts the SecurityRole class into JSON
    *
    * @param role An instance of the SecurityRole class
   * @return     A JSON blob containing SecurityRole info
   */
  implicit def SecurityRoleWrites(role: SecurityRole): JsObject = {
    Json.obj(
      "name" -> JsString(role.getName),
      "id" -> JsNumber(role.getRoleID)
    )
  }

  /**
   * Converts the UserWithRoles class into JSON
    *
    * @param userRole An instance of the UserWithRole class
   * @return         A JSON blob containing UserWithRole info
   */
  implicit def UserWithRolesWrites(userRole: UserWithRole): JsObject = {
    Json.obj(
      "distributor_user_id" -> userRole.distributorUserID,
      "email" -> userRole.email,
      "name" -> userRole.name,
      "role_id" -> userRole.roleID,
      "distributor_users_role_id" -> userRole.distributorUserRoleID
    )
  }

  /**
   * Finds all Distributors and converts them into a JSON array
    *
    * @return A JsArray containing all Distributor info
   */
  def distributors: JsArray = {
    distributorService.findAll().foldLeft(JsArray(Seq()))((array, distributor) => array ++ JsArray(Seq(distributor)))
  }

  /**
   * Renders the main admin page if the DistributorUser has admin access
   */
  def edit = deadbolt.SubjectPresent(new AdminDeadboltHandler(adminService))() { authRequest =>
    Future { Ok(views.html.Admin.edit(0)(authRequest.flash, authRequest.session)) }
  }

  /**
   * Renders the role management page if the DistributorUser has admin access
   */
  def manage = deadbolt.SubjectPresent(new AdminDeadboltHandler(adminService))() { authRequest =>
    Future { Ok(views.html.Admin.manage(0)(authRequest.flash, authRequest.session)) }
  }

  /**
   * Renders the completion search page if the DistributorUser has admin access
   */
  def completions = deadbolt.SubjectPresent(new AdminDeadboltHandler(adminService))() { authRequest =>
    Future { Ok(views.html.Admin.completions(0)(authRequest.flash, authRequest.session)) }
  }

  /**
   * Updates the appConfigRefreshInterval for an app
    *
    * @return If the app is not in test mode, return a message stating the update was successful.
   *         Otherwise, return a bad request.
   */
  def updateApp() = deadbolt.SubjectPresent(new AdminDeadboltHandler(adminService))() { authRequest =>
    Future {
      authRequest.body.asInstanceOf[AnyContentAsJson].asJson.map { json =>
        val appID = (json \ "id").as[Long]
        val appConfigRefreshInterval = (json \ "app_config_refresh_interval").as[Long]
        appService.updateAppConfigRefreshInterval(appID, appConfigRefreshInterval) match {
          case true => {
            Ok(Json.obj("status" -> "success", "message" -> "App updated!"))
          }
          case false => {
            BadRequest(
              errorJson ++ Json.obj("message" -> "Could not update app. Check if it is in test mode.")
            )
          }
        }
      }.getOrElse {
        BadRequest(
          errorJson ++ Json.obj("message" -> "Invalid Request.")
        )
      }
    }
  }

  /**
    * Finds all Distributors and Apps for the main admin page
    *
    * @return A JSON list of Distributors and Apps
   */
  def distributorInfo = deadbolt.SubjectPresent(new AdminDeadboltHandler(adminService))() { authRequest =>
    Future {
      val apps = appService.findAll().foldLeft(JsArray(Seq()))((array, app) => array ++ JsArray(Seq(app)))
      Ok(Json.obj("distributors" -> distributors, "apps" -> apps))
    }
  }

  /**
   * Finds all DistributorUsers and Roles
    *
    * @return A JSON list of DistributorUsers and existing or potential roles for each user
   */
  def roleInfo = deadbolt.SubjectPresent(new AdminDeadboltHandler(adminService))() { authRequest =>
    Future {
      val roles = securityRoleService.allRoles.foldLeft(JsArray(Seq()))((array, role) => array ++ JsArray(Seq(role)))
      val distributorUsers = securityRoleService.findAllUsersWithRoles.foldLeft(JsArray(Seq()))((array, user) => array ++ JsArray(Seq(user)))
      Ok(Json.obj("distributor_users" -> distributorUsers, "roles" -> roles))
    }
  }

  /**
   * Assigns a particular role to a DistributorUser
    *
    * @return A JSON blob containing role info if the assignment is successful.
   *         Otherwise, roll back the transaction and return a bad request.
   */
  def addRole() = deadbolt.SubjectPresent(new AdminDeadboltHandler(adminService))() { authRequest =>
    Future {
      authRequest.body.asInstanceOf[AnyContentAsJson].asJson.map { json =>
        DB.withTransaction { implicit connection =>
          try {
            val distributorUserID = (json \ "distributor_user_id").as[Long]
            val roleID = (json \ "role_id").as[Int]
            securityRoleService.addUserRole(distributorUserID, roleID) match {
              case Some(userRole: UserWithRole) => {
                Created(Json.obj("status" -> "success", "message" -> "Role added!", "user_role" -> UserWithRolesWrites(userRole)))
              }
              case None => {
                BadRequest(
                  errorJson ++ Json.obj("message" -> "Could not add role.")
                )
              }
            }
          } catch {
            case error: org.postgresql.util.PSQLException => {
              connection.rollback()
              BadRequest(
                errorJson ++ Json.obj("message" -> "Role already exists.")
              )
            }
          }
        }
      }.getOrElse {
        BadRequest(
          errorJson ++ Json.obj("message" -> "Invalid Request.")
        )
      }
    }
  }

  /**
   * Removes the role assignment for a DistributorUser
    *
    * @return A JSON success message if the role was removed successfully.
   *         Otherwise, roll back the transaction and return a bad request.
   */
  def removeRole() = deadbolt.SubjectPresent(new AdminDeadboltHandler(adminService))() { authRequest =>
    Future {
      authRequest.body.asInstanceOf[AnyContentAsJson].asJson.map { json =>
        DB.withTransaction { implicit connection =>
          try {
            val userRoleID = (json \ "distributor_users_role_id").as[Int]
            securityRoleService.deleteUserRole(userRoleID) match {
              case 1 => {
                Ok(Json.obj("status" -> "success", "message" -> "Role removed!"))
              }
              case _ => {
                BadRequest(
                  errorJson ++ Json.obj("message" -> "Could not remove role.")
                )
              }
            }
          } catch {
            case error: org.postgresql.util.PSQLException => {
              connection.rollback()
              BadRequest(
                errorJson ++ Json.obj("message" -> "Could not remove role.")
              )
            }
          }
        }
      }.getOrElse {
        BadRequest(
          errorJson ++ Json.obj("message" -> "Invalid Request.")
        )
      }
    }
  }

  def username(request: RequestHeader): Option[String] = {
    request.session.get(Security.username)
  }

  def onUnauthorized(request: RequestHeader): Result = {
    Results.Redirect(routes.DistributorUsersController.login(None))
  }

  def withAdmin(controllerDistributorID: Option[Long])(controllerAction: => String => Request[AnyContent] => Result): EssentialAction = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => {
        (controllerDistributorID, username(request)) match {
          case (Some(ctrlDistID), Some(email)) =>
            adminService.find(email, ctrlDistID) match {
              case Some(admin) =>
                request.session.get("distributorID") match {
                  // Check if Distributor ID from session matches the ID passed from the controller
                  case Some(sessionDistID) if sessionDistID == admin.distributorID.toString =>
                    controllerAction(user)(request)
                  case _ => Results.Redirect(routes.DistributorUsersController.login(None))
                }
              case _ => Results.Redirect(routes.DistributorUsersController.login(None))
            }

          case _ => Results.Redirect(routes.DistributorUsersController.login(None))
        }
      })
    }
  }

  /**
   * Logs in an admin DistributorUser as the Distributor whose ID is passed as an argument
    *
    * @param distributorID The ID of the Distributor that will be logged in
   * @return If the user was an admin, redirect and start a new session as the specified Distributor.
   *         Otherwise, return the appropriate error JSON.
   */
  def morph(distributorID: Option[Long]) = deadbolt.SubjectPresent(new AdminDeadboltHandler(adminService))() { authRequest =>
    Future {
      distributorID match {
        case Some(id) => {
          distributorUserService.findByDistributorID(id) match {
            case Some(user) => {
              Redirect(routes.AnalyticsController.show(user.distributorID.get, None, None))
                .withSession(Security.username -> user.email, "distributorID" -> user.distributorID.get.toString)
            }
            case None => BadRequest(
              errorJson ++ Json.obj("message" -> "Distributor User could not be found")
            )
          }
        }
        case None => BadRequest(
          errorJson ++ Json.obj("message" -> "Distributor ID is required")
        )
      }
    }
  }
}
