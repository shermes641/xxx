package controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import models.App

object AppsController extends Controller {
  // Form mapping used in create and edit actions
  val appForm = Form[tempDistributorApp](
    mapping(
      "name" -> nonEmptyText
    )(tempDistributorApp.apply)(tempDistributorApp.unapply)
  )

  /**
   * Renders view of all apps associated with the current Distributor.
   * @param distributorID ID associated with current DistributorUser
   * @return Apps index view
   */
  def index(distributorID: Long) = Action { implicit request =>
    val apps = App.findAll(distributorID)
    Ok(views.html.Apps.index(apps, distributorID))
  }

  /**
   * Renders the form for creating a new App.
   * @param distributorID ID associated with current DistributorUser
   * @return Form for creating a new App
   */
  def newApp(distributorID: Long) = Action { implicit request =>
    Ok(views.html.Apps.newApp(appForm, distributorID))
  }

  /**
   * Creates a new App in the database.
   * @param distributorID ID associated with current DistributorUser
   * @return Responds with 201 when App is persisted successfully.  Otherwise, redirect to Application index view.
   */
  def create(distributorID: Long) = Action { implicit request =>
    appForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.Apps.newApp(formWithErrors, distributorID)),
      app => {
        App.create(distributorID, app.name) match {
          case newID: Option[Long] => {
            val apps = App.findAll(distributorID)
            Created(views.html.Apps.index(apps, distributorID)).flashing("success" -> "App created!")
          }
          case _ => {
            Redirect(routes.Application.index).flashing("error" -> "App could not be created.")
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
  def show(distributorID: Long, appID: Long) = Action { implicit request =>
    val app = App.find(appID)
    Ok(views.html.Apps.show(app.get, distributorID, appID))
  }

  /**
   * Renders form for editing App.
   * @param distributorID ID associated with current DistributorUser
   * @param appID ID associated with current App
   * @return Form for editing Apps
   */
  def edit(distributorID: Long, appID: Long) = Action { implicit request =>
    val app = App.find(appID).get
    val form = appForm.fill(new tempDistributorApp(app.name))
    Ok(views.html.Apps.edit(form, distributorID, appID))
  }

  /**
   * Updates attributes for current App.
   * @param distributorID ID associated with current DistributorUser
   * @param appID ID associated with current App
   * @return Responds with 200 if App is successfully updated.  Otherwise, flash error and respond with 304.
   */
  def update(distributorID: Long, appID: Long) = Action { implicit request =>
    appForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.Apps.edit(formWithErrors, distributorID, appID)),
      app => {
        val newAppValues = new App(appID, distributorID, app.name)
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

  /**
   * Deletes App record from the database.
   * @param distributorID ID associated with current DistributorUser
   * @param appID ID associated with current App
   * @return Responds with 200 if App is deleted successfully.  Otherwise, flash error and respond with 304.
   */
  def destroy(distributorID: Long, appID: Long) = Action { implicit request =>
    App.destroy(appID) match {
      case 1 => {
        val apps = App.findAll(distributorID)
        Ok(views.html.Apps.index(apps, distributorID)).flashing("success" -> "App deleted.")
      }
      case _ => {
        NotModified.flashing("error" -> "App was not deleted.")
      }
    }
  }
}

/**
 * Used for mapping App attributes in forms.
 * @param name Maps to the name field in the App table
 */
case class tempDistributorApp(name: String) {}
