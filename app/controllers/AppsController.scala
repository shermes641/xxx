package controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import models.App

/** Controller for models.App instances. */
object AppsController extends Controller with Secured {
  // Form mapping used in edit action
  val appForm = Form[AppMapping](
    mapping(
      "name" -> text.verifying("App name is required", {!_.isEmpty}),
      "active" -> text
    )(AppMapping.apply)(AppMapping.unapply)
  )

  // Form mapping used in create action
  val newAppForm = Form[NewAppMapping](
    mapping(
      "name" -> text.verifying("App name is required", {!_.isEmpty})
    )(NewAppMapping.apply)(NewAppMapping.unapply)
  )

  /**
   * Renders view of all apps associated with the current Distributor.
   * @param distributorID ID associated with current DistributorUser
   * @return Apps index view
   */
  def index(distributorID: Long) = withAuth { username => implicit request =>
    val apps = App.findAll(distributorID)
    Ok(views.html.Apps.index(apps, distributorID))
  }

  /**
   * Renders the form for creating a new App.
   * @param distributorID ID associated with current DistributorUser
   * @return Form for creating a new App
   */
  def newApp(distributorID: Long) = withAuth { username => implicit request =>
    Ok(views.html.Apps.newApp(newAppForm, distributorID))
  }

  /**
   * Creates a new App in the database.
   * @param distributorID ID associated with current DistributorUser
   * @return Responds with 201 when App is persisted successfully.  Otherwise, redirect to Application index view.
   */
  def create(distributorID: Long) = withAuth { username => implicit request =>
    newAppForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.Apps.newApp(formWithErrors, distributorID)),
      app => {
        App.create(distributorID, app.name) match {
          case newID: Option[Long] => {
            val apps = App.findAll(distributorID)
            Created(views.html.Apps.index(apps, distributorID)).flashing("success" -> "App created!")
          }
          case _ => {
            Redirect(routes.AppsController.index(distributorID)).flashing("error" -> "App could not be created.")
          }
        }
      }
    )
  }

  /**
   * Displays attributes for current App.
   * @param distributorID ID associated with current DistributorUser
   * @param appID ID associated with current App
   * @return Apps show view
   */
  def show(distributorID: Long, appID: Long) = withAuth { username => implicit request =>
    val app = App.find(appID)
    Ok(views.html.Apps.show(app.get, distributorID, appID))
  }

  /**
   * Renders form for editing App.
   * @param distributorID ID associated with current DistributorUser
   * @param appID ID associated with current App
   * @return Form for editing Apps
   */
  def edit(distributorID: Long, appID: Long) = withAuth { username => implicit request =>
    val app = App.find(appID).get
    val active = if(app.active) "1" else "0"
    val form = appForm.fill(new AppMapping(app.name, active))
    Ok(views.html.Apps.edit(form, distributorID, appID))
  }

  /**
   * Updates attributes for current App.
   * @param distributorID ID associated with current DistributorUser
   * @param appID ID associated with current App
   * @return Responds with 200 if App is successfully updated.  Otherwise, flash error and respond with 304.
   */
  def update(distributorID: Long, appID: Long) = withAuth { username => implicit request =>
    appForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.Apps.edit(formWithErrors, distributorID, appID))
      },
      app => {
        val active = if(app.active == "1") true else false
        val newAppValues = new App(appID, active, distributorID, app.name)
        App.update(newAppValues) match {
          case 1 => {
            Ok(views.html.Apps.show(newAppValues, distributorID, appID))
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
 * Used for mapping App attributes in appForm.
 * @param name Maps to the name field in the App table
 * @param active Maps to the active field in the App table
 */
case class AppMapping(name: String, active: String) {}

/**
 * Used for mapping App attributes in newAppForm.
 * @param name Maps to the name field in the App table
 */
case class NewAppMapping(name: String) {}
