package controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.db.DB
import play.api.mvc._
import play.api.Play.current
import anorm._
import models.{VirtualCurrency, App, Waterfall}

/** Controller for models.App instances. */
object AppsController extends Controller with Secured with CustomFormValidation {
  // Partial error messages used in newAppForm and editAppForm
  val appNameError: String = "App name"
  val currencyNameError: String = "Currency name"

  // Form mapping used in create action
  val newAppForm = Form[NewAppMapping](
    mapping(
      "appName" -> text.verifying(nonEmptyConstraint(appNameError)),
      "currencyName" -> text.verifying(nonEmptyConstraint(currencyNameError)),
      "exchangeRate" -> longNumber,
      "rewardMin" -> optional(longNumber),
      "rewardMax" -> optional(longNumber),
      "roundUp" -> optional(checked(""))
    )(NewAppMapping.apply)(NewAppMapping.unapply)
  )

  // Form mapping used in edit action.
  val editAppForm = Form[EditAppMapping](
    mapping(
      "currencyID" -> longNumber,
      "active" -> text,
      "appName" -> text.verifying(nonEmptyConstraint(appNameError)),
      "currencyName" -> text.verifying(nonEmptyConstraint(currencyNameError)),
      "exchangeRate" -> longNumber,
      "rewardMin" -> optional(longNumber),
      "rewardMax" -> optional(longNumber),
      "roundUp" -> text,
      "callbackURL" -> optional(text),
      "serverToServerEnabled" -> text
    )(EditAppMapping.apply)(EditAppMapping.unapply)
  )

  /**
   * Renders view of all apps associated with the current Distributor.
   * @param distributorID ID associated with current DistributorUser
   * @return Apps index view
   */
  def index(distributorID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    val apps = App.findAllAppsWithWaterfalls(distributorID)
    Ok(views.html.Apps.index(apps, distributorID))
  }

  /**
   * Renders the form for creating a new App.
   * @param distributorID ID associated with current DistributorUser
   * @return Form for creating a new App
   */
  def newApp(distributorID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    Ok(views.html.Apps.newApp(newAppForm, distributorID))
  }

  /**
   * Creates a new App in the database.
   * @param distributorID ID associated with current DistributorUser
   * @return Responds with 201 when App is persisted successfully.  Otherwise, redirect to Application index view.
   */
  def create(distributorID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    newAppForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.Apps.newApp(formWithErrors, distributorID)),
      newApp => {
        DB.withTransaction { implicit connection =>
          try {
            App.createWithTransaction(distributorID, newApp.appName) match {
              case Some(appID) => {
                Waterfall.createWithTransaction(appID, newApp.appName)
                VirtualCurrency.createWithTransaction(appID, newApp.currencyName, newApp.exchangeRate, newApp.rewardMin, newApp.rewardMax, newApp.roundUp)
                Redirect(routes.WaterfallsController.list(distributorID, appID)).flashing("success" -> "App created!")
              }
              case _ => {
                Redirect(routes.AppsController.index(distributorID)).flashing("error" -> "App could not be created.")
              }
            }
          }
          catch {
            case error: Throwable => {
              connection.rollback()
              Redirect(routes.AppsController.index(distributorID)).flashing("error" -> "App could not be created.")
            }
          }
        }
      }
    )
  }

  /**
   * Renders form for editing App.
   * @param distributorID ID associated with current DistributorUser
   * @param appID ID associated with current App
   * @return Form for editing Apps
   */
  def edit(distributorID: Long, appID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    App.findAppWithVirtualCurrency(appID) match {
      case Some(appInfo) => {
        val form = editAppForm.fill(new EditAppMapping(appInfo.currencyID, if(appInfo.active) { "1" } else { "0" }, appInfo.appName, appInfo.currencyName,
          appInfo.exchangeRate, appInfo.rewardMin, appInfo.rewardMax, if(appInfo.roundUp) { "1" } else { "0" }, appInfo.callbackURL, if(appInfo.serverToServerEnabled) { "1" } else { "0" }))
        Ok(views.html.Apps.edit(form, distributorID, appID))
      }
      case None => {
        Redirect(routes.AppsController.index(distributorID)).flashing("error" -> "Could not find App.")
      }
    }
  }

  /**
   * Updates attributes for current App.
   * @param distributorID ID associated with current DistributorUser
   * @param appID ID associated with current App
   * @return Responds with 200 if App is successfully updated.  Otherwise, flash error and respond with 304.
   */
  def update(distributorID: Long, appID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    editAppForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.Apps.edit(formWithErrors, distributorID, appID))
      },
      appInfo => {
        val newAppValues = new App(appID, if(appInfo.active == "1") { true } else { false }, distributorID, appInfo.appName, appInfo.callbackURL, if(appInfo.serverToServerEnabled == "1") { true } else { false })
        App.update(newAppValues) match {
          case 1 => {
            VirtualCurrency.update(new VirtualCurrency(appInfo.currencyID, appID, appInfo.currencyName, appInfo.exchangeRate,
              appInfo.rewardMin, appInfo.rewardMax, if(appInfo.roundUp == "1") { true } else { false })) match {
              case 1 => {
                Redirect(routes.AppsController.index(distributorID)).flashing("success" -> "Configurations updated successfully.")
              }
              case _ => {
                NotModified.flashing("error" -> "Virtual Currency could not be updated.")
              }
            }
            Redirect(routes.AppsController.index(distributorID)).flashing("success" -> "App updated successfully.")
          }
          case _ => {
            NotModified.flashing("error" -> "App could not be updated.")
          }
        }
      }
    )
  }
}

/**
 * Used for mapping App attributes in newAppForm.
 * @param appName Maps to the name field in the apps table
 * @param currencyName Maps to the name field in the virtual_currencies table.
 * @param exchangeRate Maps the the exchange_rate field in the virtual_currencies table.
 * @param rewardMin Maps to the reward_min field in the virtual_currencies table.
 * @param rewardMax Maps to the reward_max field in the virtual_currencies table.
 * @param roundUp Maps to the round_up field in the virtual_currencies table.
 */
case class NewAppMapping(appName: String, currencyName: String, exchangeRate: Long, rewardMin: Option[Long], rewardMax: Option[Long], roundUp: Option[Boolean])

/**
 * Used for mapping App and VirtualCurrency attributes in editAppForm.
 * @param currencyID Maps to the id field in the virtual_currencies table.
 * @param active Maps to the active field in the apps table.
 * @param appName Maps to the name field in the apps table
 * @param currencyName Maps to the name field in the virtual_currencies table.
 * @param exchangeRate Maps the the exchange_rate field in the virtual_currencies table.
 * @param rewardMin Maps to the reward_min field in the virtual_currencies table.
 * @param rewardMax Maps to the reward_max field in the virtual_currencies table.
 * @param roundUp Maps to the round_up field in the virtual_currencies table.
 * @param callbackURL Maps to the callback_url field in the apps table.
 * @param serverToServerEnabled Maps to the server_to_server_enabled field in the apps table.
 */
case class EditAppMapping(currencyID: Long, active: String, appName: String, currencyName: String, exchangeRate: Long, rewardMin: Option[Long], rewardMax: Option[Long], roundUp: String, callbackURL: Option[String], serverToServerEnabled: String)
