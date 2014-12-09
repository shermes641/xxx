package controllers

import java.sql.Connection
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.db.DB
import play.api.mvc._
import play.api.Play.current
import play.api.Play

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
      "active" -> optional(checked("")),
      "appName" -> text.verifying(nonEmptyConstraint(appNameError)),
      "currencyName" -> text.verifying(nonEmptyConstraint(currencyNameError)),
      "exchangeRate" -> longNumber,
      "rewardMin" -> optional(longNumber),
      "rewardMax" -> optional(longNumber),
      "roundUp" -> optional(checked("")),
      "callbackURL" -> optional(text),
      "serverToServerEnabled" -> optional(checked("")),
      "generationNumber" -> optional(longNumber)
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
   * Creates a new App in the database along with an associated Waterfall, VirtualCurrency, and AppConfig.
   * @param distributorID ID associated with current Distributor.
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
                val waterfallID = Waterfall.create(appID, newApp.appName)
                val virtualCurrencyID = VirtualCurrency.createWithTransaction(appID, newApp.currencyName, newApp.exchangeRate, newApp.rewardMin, newApp.rewardMax, newApp.roundUp)
                val persistedApp = App.findWithTransaction(appID)
                (waterfallID, virtualCurrencyID, persistedApp) match {
                  case (Some(waterfallIDVal), Some(virtualCurrencyIDVal), Some(app)) => {
                    AppConfig.create(appID, app.token, 0)
                    // Set up HyprMarketplace ad provider
                    val hyprWaterfallAdProviderID = WaterfallAdProvider.createWithTransaction(waterfallIDVal, Play.current.configuration.getLong("hyprmarketplace.ad_provider_id").get, Option(0), Option(20), false, false, true)
                    val hyprWaterfallAdProvider = WaterfallAdProvider.findWithTransaction(hyprWaterfallAdProviderID.getOrElse(0))
                    (hyprWaterfallAdProviderID, hyprWaterfallAdProvider) match {
                      case (Some(hyprID), Some(hyprWaterfallAdProviderInstance)) => {
                        new JunGroupAPI().createJunGroupAdNetwork(DistributorUser.find(distributorID).get, waterfallIDVal, hyprWaterfallAdProviderInstance, app.token)
                        Redirect(routes.WaterfallsController.list(distributorID, appID, Some("App created!")))
                      }
                      case (_, _) => onCreateRollback(distributorID)
                    }
                  }
                  case (_, _, _) => onCreateRollback(distributorID)
                }
              }
              case _ => onCreateRollback(distributorID)
            }
          }
          catch {
            case error: org.postgresql.util.PSQLException => {
              onCreateRollback(distributorID)
            }
          }
        }
      }
    )
  }

  /**
   * Rolls back database and renders error message for App creation action.
   * @param distributorID The ID of the Distributor to which the new App belongs.
   * @param connection A shared database connection
   * @return Redirect back to the Apps index page.
   */
  def onCreateRollback(distributorID: Long)(implicit connection: Connection) = {
    connection.rollback()
    Redirect(routes.AppsController.index(distributorID)).flashing("error" -> "App could not be created.")
  }

  /**
   * Renders form for editing App.
   * @param distributorID ID associated with current Distributor
   * @param appID ID associated with current App
   * @return Form for editing Apps
   */
  def edit(distributorID: Long, appID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    App.findAppWithVirtualCurrency(appID, distributorID) match {
      case Some(appInfo) => {
        val form = editAppForm.fill(new EditAppMapping(appInfo.currencyID, Some(appInfo.active), appInfo.appName, appInfo.currencyName,
          appInfo.exchangeRate, appInfo.rewardMin, appInfo.rewardMax, Some(appInfo.roundUp), appInfo.callbackURL, Some(appInfo.serverToServerEnabled), appInfo.generationNumber))
        Ok(views.html.Apps.edit(form, distributorID, appID))
      }
      case None => {
        Redirect(routes.AppsController.index(distributorID)).flashing("error" -> "App could not be found.")
      }
    }
  }

  /**
   * Updates attributes for current App/VirtualCurrency and generates a new AppConfig.
   * @param distributorID ID associated with current Distributor
   * @param appID ID associated with current App
   * @return Responds with 200 if App is successfully updated.  Otherwise, flash error and respond with 304.
   */
  def update(distributorID: Long, appID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    editAppForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.Apps.edit(formWithErrors, distributorID, appID))
      },
      appInfo => {
        val newAppValues = new UpdatableApp(appID, appInfo.active.getOrElse(false), distributorID, appInfo.appName, appInfo.callbackURL, appInfo.serverToServerEnabled.getOrElse(false))
        DB.withTransaction { implicit connection =>
          try {
            App.updateWithTransaction(newAppValues) match {
              case 1 => {
                VirtualCurrency.updateWithTransaction(new VirtualCurrency(appInfo.currencyID, appID, appInfo.currencyName, appInfo.exchangeRate,
                  appInfo.rewardMin, appInfo.rewardMax, appInfo.roundUp.getOrElse(false))) match {
                  case 1 => {
                    Waterfall.findByAppID(appID) match {
                      case waterfalls: List[Waterfall] if(waterfalls.size > 0) => {
                        val waterfall = waterfalls(0)
                        AppConfig.create(appID, waterfall.appToken, appInfo.generationNumber.getOrElse(0))
                      }
                    }
                    Redirect(routes.AppsController.index(distributorID)).flashing("success" -> "Configurations updated successfully.")
                  }
                  case _ => onUpdateRollback
                }
                Redirect(routes.AppsController.index(distributorID)).flashing("success" -> "App updated successfully.")
              }
              case _ => onUpdateRollback
            }
          } catch {
            case error: org.postgresql.util.PSQLException => onUpdateRollback
            case error: IllegalArgumentException => onUpdateRollback
          }
        }
      }
    )
  }

  /**
   * Rolls back the transaction and flashes an error message to the user.
   * @param connection The shared connection for the database transaction.
   */
  def onUpdateRollback(implicit connection: Connection) = {
    connection.rollback()
    NotModified.flashing("error" -> "App could not be updated.")
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
 * @param generationNumber The revision number which tracks the state of the corresponding AppConfig model at the time the page renders.
 */
case class EditAppMapping(currencyID: Long, active: Option[Boolean], appName: String, currencyName: String, exchangeRate: Long, rewardMin: Option[Long], rewardMax: Option[Long], roundUp: Option[Boolean], callbackURL: Option[String], serverToServerEnabled: Option[Boolean], generationNumber: Option[Long])
