package controllers

import java.sql.Connection
import play.api.db.DB
import play.api.mvc._
import models._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.Play.current
import scala.language.implicitConversions

object WaterfallsController extends Controller with Secured with JsonToValueHelper with ValueToJsonHelper {
  /**
   * Redirects to the waterfall assigned to App.
   * Future:  Will return list of waterfalls.
   * @param distributorID ID of Distributor who owns the current Waterfall
   * @param appID ID of the Waterfall being edited
   * @return Redirects to edit page if app with waterfall exists.
   */
  def list(distributorID: Long, appID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    App.findAppWithWaterfalls(appID, distributorID) match {
      case Some(app) => {
          Redirect(routes.WaterfallsController.edit(distributorID, app.waterfallID))
      }
      case None => {
        Redirect(routes.AnalyticsController.show(distributorID, None, Some(false)))
      }
    }
  }

  /**
   * Renders form for editing Waterfall.
   * @param distributorID ID of Distributor who owns the current Waterfall
   * @param waterfallID ID of the Waterfall being edited
   * @return Form for editing Waterfall
   */
  def edit(distributorID: Long, waterfallID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    Waterfall.find(waterfallID, distributorID) match {
      case Some(waterfall) => {
        val waterfallAdProviderList = WaterfallAdProvider.findAllOrdered(waterfallID) ++ AdProvider.findNonIntegrated(waterfallID).map { adProvider =>
            new OrderedWaterfallAdProvider(adProvider.name, adProvider.id, adProvider.defaultEcpm, false, None, true, true, Option(false), Option(0), 0, adProvider.configurable, true)
        }
        val appsWithWaterfalls = App.findAllAppsWithWaterfalls(distributorID)
        Ok(views.html.Waterfalls.edit(distributorID, waterfall, waterfallAdProviderList, appsWithWaterfalls, waterfall.generationNumber))
      }
      case None => {
        Redirect(routes.AnalyticsController.show(distributorID, None, Some(false)))
      }
    }
  }

  def waterfallInfo(distributorID: Long, waterfallID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    Waterfall.find(waterfallID, distributorID) match {
      case Some(waterfall) => {
        val waterfallAdProviderList = WaterfallAdProvider.findAllOrdered(waterfallID) ++ AdProvider.findNonIntegrated(waterfallID).map { adProvider =>
          new OrderedWaterfallAdProvider(adProvider.name, adProvider.id, adProvider.defaultEcpm, false, None, true, true, Option(false), Option(0), 0, adProvider.configurable, true)
        }
        val appsWithWaterfalls = App.findAllAppsWithWaterfalls(distributorID)
        val generationNumber: Long = waterfall.generationNumber.getOrElse(0)
        Ok(Json.obj("distributorID" -> JsNumber(distributorID), "waterfall" -> waterfall, "waterfallAdProviderList" -> adProviderListJs(waterfallAdProviderList),
          "appsWithWaterfalls" -> appsWithWaterfallListJs(appsWithWaterfalls), "generationNumber" -> JsNumber(generationNumber)))
      }
      case None => {
        BadRequest(Json.obj("status" -> "error", "message" -> "Waterfall could not be found."))
      }
    }
  }

  /**
   * Converts an optional Double value to a JsValue.
   * @param param The original optional Double value.
   * @return A JsNumber if a Long value is found; otherwise, returns JsNull.
   */
  implicit def optionalDoubleToJsValue(param: Option[Double]): JsValue = {
    param match {
      case Some(paramValue) => JsNumber(paramValue)
      case None => JsNull
    }
  }

  /**
   * Converts an instance of the OrderedWaterfallAdProvider class to a JSON object.
   * @param wap An instance of the OrderedWaterfallAdProvider class.
   * @return A JSON object.
   */
  implicit def orderedWaterfallAdProviderWrites(wap: OrderedWaterfallAdProvider): JsObject = {
    JsObject(
      Seq(
        "name" -> JsString(wap.name),
        "waterfallAdProviderID" -> JsNumber(wap.waterfallAdProviderID),
        "cpm" -> wap.cpm,
        "active" -> JsBoolean(wap.active),
        "waterfallOrder" -> wap.waterfallOrder,
        "unconfigured" -> JsBoolean(wap.unconfigured),
        "newRecord" -> JsBoolean(wap.newRecord),
        "configurable" -> JsBoolean(wap.configurable),
        "meetsRewardThreshold" -> JsBoolean(wap.meetsRewardThreshold),
        "pending" -> JsBoolean(wap.pending)
      )
    )
  }

  /**
   * Converts a list of OrderedWaterfallAdProvider instances to a JsArray.
   * @param list A list of OrderedWaterfallAdProvider instances.
   * @return A JsArray containing OrderedWaterfallAdProvider objects.
   */
  def adProviderListJs(list: List[OrderedWaterfallAdProvider]): JsArray = {
    list.foldLeft(JsArray(Seq()))((array, wap) => array ++ JsArray(Seq(wap)))
  }

  /**
   * Converts an instance of the Waterfall class to a JSON object.
   * @param waterfall An instance of the Waterfall class.
   * @return A JSON object.
   */
  implicit def waterfallWrites(waterfall: Waterfall): Json.JsValueWrapper = {
    JsObject(
      Seq(
        "id" -> JsString(waterfall.id.toString),
        "appID" -> JsString(waterfall.app_id.toString),
        "name" -> JsString(waterfall.name),
        "token" -> JsString(waterfall.token),
        "optimizedOrder" -> JsBoolean(waterfall.optimizedOrder),
        "testMode" -> JsBoolean(waterfall.testMode),
        "appName" -> JsString(waterfall.appName),
        "appToken" -> JsString(waterfall.appToken)
      )
    )
  }

  /**
   * Converts an instance of the AppWithWaterfallID class to a JSON object.
   * @param app An instance of the AppWithWaterfallID class.
   * @return A JSON object.
   */
  implicit def appWithWaterfallIDWrites(app: AppWithWaterfallID): JsObject = {
    JsObject(
      Seq(
        "id" -> JsString(app.id.toString),
        "active" -> JsBoolean(app.active),
        "distributorID" -> JsNumber(app.distributorID),
        "name" -> JsString(app.name),
        "waterfallID" -> JsNumber(app.waterfallID)
      )
    )
  }

  /**
   * Converts a list of AppWithWaterfallID instances to a JsArray.
   * @param list A list of AppWithWaterfallID instances.
   * @return A JsArray containing AppWithWaterfallID objects.
   */
  def appsWithWaterfallListJs(list: List[AppWithWaterfallID]): JsArray = {
    list.foldLeft(JsArray(Seq()))((array, app) => array ++ JsArray(Seq(app)))
  }

  /**
   * Renders form for editing Waterfall if an App/Waterfall has been previously selected.
   * @param distributorID ID of Distributor who owns the current Waterfall.
   * @param currentWaterfallID ID of the Waterfall being edited.
   * @param currentAppID ID of the App whose Waterfall is being edited.
   * @return Form for editing Waterfall if a Waterfall ID or App ID is passed as a param.  Otherwise, renders a drop down list to select a Waterfall.
   */
  def editAll(distributorID: Long, currentWaterfallID: Option[Long], currentAppID: Option[Long]) = withAuth(Some(distributorID)) { username => implicit request =>
    (currentWaterfallID, currentAppID) match {
      case (Some(waterfallID), _) => {
        Redirect(routes.WaterfallsController.edit(distributorID, waterfallID))
      }
      case (_, Some(appID)) => {
        Redirect(routes.WaterfallsController.list(distributorID, appID))
      }
      case (None, None) => {
        val apps = App.findAllAppsWithWaterfalls(distributorID)
        if(apps.size == 0) {
            Redirect(routes.AppsController.newApp(distributorID))
        } else {
            Redirect(routes.WaterfallsController.list(distributorID, apps.head.id))
        }
      }
    }
  }

  /**
   * Accepts AJAX call from Waterfall edit form to update attributes.
   * @param distributorID ID of Distributor who owns the current Waterfall
   * @param waterfallID ID of the Waterfall being edited
   * @return Responds with 200 if update is successful.  Otherwise, 400 is returned.
   */
  def update(distributorID: Long, waterfallID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    request.body.asJson.map { json =>
      DB.withTransaction { implicit connection =>
        try {
          val listOrder: List[JsValue] = (json \ "adProviderOrder").as[List[JsValue]]
          val adProviderConfigList = listOrder.map { jsArray =>
            new ConfigInfo((jsArray \ "waterfallAdProviderID").as[Long], (jsArray \ "newRecord").as[Boolean], (jsArray \ "active").as[Boolean], (jsArray \ "waterfallOrder").as[Long], (jsArray \ "cpm"), (jsArray \ "configurable").as[Boolean], (jsArray \ "pending").as[Boolean])
          }
          val optimizedOrder: Boolean = (json \ "optimizedOrder").as[Boolean]
          val testMode: Boolean = (json \ "testMode").as[Boolean]
          val generationNumber: Long = (json \ "generationNumber").as[Long]
          Waterfall.updateWithTransaction(waterfallID, optimizedOrder, testMode) match {
            case 1 => {
              Waterfall.reconfigureAdProviders(waterfallID, adProviderConfigList) match {
                case true => {
                  val appToken = (json \ "appToken").as[String]
                  val newGenerationNumber: Long = AppConfig.createWithWaterfallIDInTransaction(waterfallID, Some(generationNumber)).getOrElse(0)
                  Ok(Json.obj("status" -> "success", "message" -> "Waterfall updated!", "newGenerationNumber" -> newGenerationNumber))
                }
                case _ => {
                  BadRequest(Json.obj("status" -> "error", "message" -> "Waterfall was not updated. Please refresh page."))
                }
              }
            }
            case _ => {
              BadRequest(Json.obj("status" -> "error", "message" -> "Waterfall was not updated. Please refresh page."))
            }
          }
        } catch {
          case error: org.postgresql.util.PSQLException => rollback
          case error: IllegalArgumentException => rollback
        }
      }
    }.getOrElse {
      BadRequest(Json.obj("status" -> "error", "message" -> "Invalid Request."))
    }
  }

  /**
   * Helper function to convert eCPM JsValue.
   * @param param A JsValue that could be null.
   * @return If the value exists, convert to an optional Double; otherwise, return None.
   */
  implicit def convertCpm(param: JsValue): Option[Double] = {
    param match {
      case value: JsValue if(value == JsNumber) => Some(value.as[Double])
      case _ => None
    }
  }

  /**
   * Rolls back the transaction and sends a JSON error message to the user.
   * @param connection The shared connection for the database transaction.
   */
  def rollback(implicit connection: Connection) = {
    connection.rollback()
    BadRequest(Json.obj("status" -> "error", "message" -> "The Waterfall could not be edited. Please refresh the browser."))
  }
}

/**
 * Used for mapping JSON ad provider configuration info in the update action.
 * @param id The WaterfallAdProvider ID if a record exists.  Otherwise, this is the AdProvider ID used to create a new WaterfallAdProvider.
 * @param newRecord True if no WaterfallAdProvider record exists; otherwise, false.
 * @param active True if the WaterfallAdProvider is used in the waterfall; otherwise, false.
 * @param waterfallOrder The position of this WaterfallAdProvider in the waterfall.
 * @param cpm Maps to the cpm value in the waterfall_ad_providers table.
 * @param configurable Determines if the cpm value can be edited by a DistributorUser.
 * @param pending Determines if we are still waiting for a response from Player with the appropriate configuration info.
 */
case class ConfigInfo(id: Long, newRecord: Boolean, active: Boolean, waterfallOrder: Long, cpm: Option[Double], configurable: Boolean, pending: Boolean)

