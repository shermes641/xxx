package controllers

import akka.actor.Props
import java.sql.Connection
import models._
import play.api.db.DB
import play.api.libs.concurrent.Akka
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._
import play.api.Play
import play.api.Play.current

/** Controller for models.App instances. */
object AppsController extends Controller with Secured with CustomFormValidation {
  implicit val newAppReads: Reads[NewAppMapping] = (
    (JsPath \ "appName").read[String] and
    (JsPath \ "currencyName").read[String] and
    (JsPath \ "exchangeRate").read[Long] and
    (JsPath \ "rewardMin").read[Long] and
    (JsPath \ "rewardMax").readNullable[Long] and
    (JsPath \ "roundUp").readNullable[Boolean]
  )(NewAppMapping.apply _)

  implicit val editAppReads: Reads[EditAppMapping] = (
    (JsPath \ "apiToken").read[String] and
    (JsPath \ "currencyID").read[Long] and
    (JsPath \ "active").readNullable[Boolean] and
    (JsPath \ "appName").read[String] and
    (JsPath \ "currencyName").read[String] and
    (JsPath \ "exchangeRate").read[Long] and
    (JsPath \ "rewardMin").read[Long] and
    (JsPath \ "rewardMax").readNullable[Long] and
    (JsPath \ "roundUp").readNullable[Boolean] and
    (JsPath \ "callbackURL").readNullable[String] and
    (JsPath \ "serverToServerEnabled").readNullable[Boolean] and
    (JsPath \ "generationNumber").readNullable[Long]
  )(EditAppMapping.apply _)

  implicit val editAppWrites = (
    (__ \ "apiToken").write[String] and
    (__ \ "currencyID").write[Long] and
    (__ \ "active").writeNullable[Boolean] and
    (__ \ "appName").write[String] and
    (__ \ "currencyName").write[String] and
    (__ \ "exchangeRate").write[Long] and
    (__ \ "rewardMin").write[Long] and
    (__ \ "rewardMax").writeNullable[Long] and
    (__ \ "roundUp").writeNullable[Boolean] and
    (__ \ "callbackURL").writeNullable[String] and
    (__ \ "serverToServerEnabled").writeNullable[Boolean] and
    (__ \ "generationNumber").writeNullable[Long]
    )(unlift(EditAppMapping.unapply))

  /**
   * Renders the form for creating a new App.
   * @param distributorID ID associated with current DistributorUser
   * @return Form for creating a new App
   */
  def newApp(distributorID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    Ok(views.html.Apps.newApp(distributorID))
  }

  val takenAppNameError = {
    "You already have an App with the same name.  Please choose a unique name for your new App."
  }

  val duplicateAppNameException = {
    "duplicate key value violates unique constraint \"active_distributor_app_name\""
  }

  /**
   * Creates a new App in the database along with an associated Waterfall, VirtualCurrency, and AppConfig.
   * @param distributorID ID associated with current Distributor.
   * @return Responds with 201 when App is persisted successfully.  Otherwise, returns 400.
   */
  def create(distributorID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    request.body.asJson.map { json =>
      json.validate[NewAppMapping].map { newApp =>
        DB.withTransaction { implicit connection =>
          val createErrorMessage = "App could not be created."
          try {
            App.createWithTransaction(distributorID, newApp.appName) match {
              case Some(appID) => {
                val waterfallID = Waterfall.create(appID, newApp.appName)
                val virtualCurrencyID = VirtualCurrency.createWithTransaction(appID, newApp.currencyName, newApp.exchangeRate, newApp.rewardMin, newApp.rewardMax, newApp.roundUp)
                val persistedApp = App.findWithTransaction(appID)
                (waterfallID, virtualCurrencyID, persistedApp) match {
                  case (Some(waterfallIDVal), Some(virtualCurrencyIDVal), Some(app)) => {
                    // Set up HyprMarketplace ad provider
                    val hyprMarketplace = AdProvider.HyprMarketplace
                    val hyprWaterfallAdProviderID = WaterfallAdProvider.createWithTransaction(waterfallIDVal, Play.current.configuration.getLong("hyprmarketplace.ad_provider_id").get, Option(0), hyprMarketplace.defaultEcpm, hyprMarketplace.configurable, active = false, pending = true)
                    val hyprWaterfallAdProvider = WaterfallAdProvider.findWithTransaction(hyprWaterfallAdProviderID.getOrElse(0))
                    AppConfig.create(appID, app.token, 0)
                    (hyprWaterfallAdProviderID, hyprWaterfallAdProvider) match {
                      case (Some(hyprID), Some(hyprWaterfallAdProviderInstance)) => {
                        val actor = Akka.system(current).actorOf(Props(new JunGroupAPIActor(waterfallIDVal, hyprWaterfallAdProviderInstance, app.token, app.name, distributorID, new JunGroupAPI)))
                        actor ! CreateAdNetwork(DistributorUser.find(distributorID).get)
                        Ok(Json.obj("status" -> "success", "message" -> "App Created!", "waterfallID" -> waterfallIDVal))
                      }
                      case (_, _) => rollbackWithError(createErrorMessage)
                    }
                  }
                  case (_, _, _) => rollbackWithError(createErrorMessage)
                }
              }
              case _ => rollbackWithError(createErrorMessage)
            }
          } catch {
            case error: org.postgresql.util.PSQLException if(error.toString contains duplicateAppNameException) => {
              rollbackWithError(takenAppNameError, Some("appName"))
            }
            case error: org.postgresql.util.PSQLException => {
              rollbackWithError(createErrorMessage)
            }
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
   * Renders form for editing App.
   * @param distributorID ID associated with current Distributor
   * @param appID ID associated with current App
   * @return Form for editing Apps
   */
  def edit(distributorID: Long, appID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    App.findAppWithVirtualCurrency(appID, distributorID) match {
      case Some(appInfo) => {
        val editAppInfo = new EditAppMapping(appInfo.apiToken, appInfo.currencyID, Some(appInfo.active), appInfo.appName, appInfo.currencyName,
          appInfo.exchangeRate, appInfo.rewardMin, appInfo.rewardMax, Some(appInfo.roundUp), appInfo.callbackURL, Some(appInfo.serverToServerEnabled), appInfo.generationNumber)
        Ok(Json.toJson(editAppInfo))
      }
      case None => {
        BadRequest(Json.obj("status" -> "error", "message" -> "App could not be found."))
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
    request.body.asJson.map { json =>
      json.validate[EditAppMapping].map { appInfo =>
        val updateErrorMessage = "App could not be updated."
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
                        val newGeneration: Long = AppConfig.create(appID, waterfall.appToken, appInfo.generationNumber.getOrElse(0)).getOrElse(0)
                        Ok(Json.obj("status" -> "success", "message" -> "App updated successfully.", "generationNumber" -> newGeneration))
                      }
                    }
                  }
                  case _ => {
                    BadRequest(Json.obj("status" -> "error", "message" -> updateErrorMessage))
                  }
                }
              }
              case _ => rollbackWithError(updateErrorMessage)
            }
          } catch {
            case error: org.postgresql.util.PSQLException if(error.toString contains duplicateAppNameException) => {
              rollbackWithError(takenAppNameError, Some("appName"))
            }
            case error: org.postgresql.util.PSQLException => {
              rollbackWithError(updateErrorMessage)
            }
            case error: IllegalArgumentException => rollbackWithError(updateErrorMessage + " Please refresh your browser.")
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
   * Rolls back the transaction and flashes an error message to the user.
   * @param message The error message to be displayed to the user.
   * @param fieldName The specific input field where the message should be displayed (if necessary).
   * @param connection The shared connection for the database transaction.
   */
  def rollbackWithError(message: String, fieldName: Option[String] = None)(implicit connection: Connection) = {
    connection.rollback()
    BadRequest(Json.obj("status" -> "error", "message" -> message, "fieldName" -> JsString(fieldName.getOrElse(""))))
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
case class NewAppMapping(appName: String, currencyName: String, exchangeRate: Long, rewardMin: Long, rewardMax: Option[Long], roundUp: Option[Boolean])

/**
 * Used for mapping App and VirtualCurrency attributes in editAppForm.
 * @param apiToken The unique string identifier for an app.
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
case class EditAppMapping(apiToken: String, currencyID: Long, active: Option[Boolean], appName: String, currencyName: String, exchangeRate: Long, rewardMin: Long, rewardMax: Option[Long], roundUp: Option[Boolean], callbackURL: Option[String], serverToServerEnabled: Option[Boolean], generationNumber: Option[Long])
