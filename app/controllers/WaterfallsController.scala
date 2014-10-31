package controllers

import play.api.mvc._
import models._
import play.api.libs.json._
import play.api.Play
import scala.language.implicitConversions

object WaterfallsController extends Controller with Secured with JsonToValueHelper {
  /**
   * Redirects to the waterfall assigned to App.
   * Future:  Will return list of waterfalls.
   * @param distributorID ID of Distributor who owns the current Waterfall
   * @param appID ID of the Waterfall being edited
   * @return Redirects to edit page if app with waterfall exists.
   */
  def list(distributorID: Long, appID: Long, flashMessage: Option[String] = None) = withAuth(Some(distributorID)) { username => implicit request =>
    App.findAppWithWaterfalls(appID) match {
      case Some(app) => {
        flashMessage match {
          case Some(message: String) => {
            Redirect(routes.WaterfallsController.edit(distributorID, app.waterfallID)).flashing("success" -> message)
          }
          case None => {
            Redirect(routes.WaterfallsController.edit(distributorID, app.waterfallID))
          }
        }
      }
      case None => {
        Redirect(routes.AppsController.index(distributorID)).flashing("error" -> "Waterfall could not be found.")
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
    Waterfall.find(waterfallID) match {
      case Some(waterfall) => {
        val waterfallAdProviderList = WaterfallAdProvider.findAllOrdered(waterfallID) ++ AdProvider.findNonIntegrated(waterfallID).map { adProvider =>
            new OrderedWaterfallAdProvider(adProvider.name, adProvider.id, adProvider.defaultEcpm, false, None, true, adProvider.configurable)
        }
        val appsWithWaterfalls = App.findAllAppsWithWaterfalls(distributorID)
        Ok(views.html.Waterfalls.edit(distributorID, waterfall, waterfallAdProviderList, appsWithWaterfalls))
      }
      case None => {
        Redirect(routes.AppsController.index(distributorID)).flashing("error" -> "Waterfall could not be found.")
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
      val listOrder: List[JsValue] = (json \ "adProviderOrder").as[List[JsValue]]
      val adProviderConfigList = listOrder.map { jsArray =>
        new ConfigInfo((jsArray \ "id").as[String].toLong, (jsArray \ "newRecord").as[String].toBoolean, (jsArray \ "active").as[String].toBoolean, (jsArray \ "waterfallOrder").as[String].toLong, (jsArray \ "cpm"), (jsArray \ "configurable").as[String].toBoolean)
      }
      val optimizedOrder: Boolean = (json \ "optimizedOrder").as[String].toBoolean
      val testMode: Boolean = (json \ "testMode").as[String].toBoolean
      Waterfall.update(waterfallID, optimizedOrder, testMode) match {
        case 1 => {
          Waterfall.reconfigureAdProviders(waterfallID, adProviderConfigList) match {
            case true => {
              Ok(Json.obj("status" -> "OK", "message" -> "Waterfall updated!"))
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
    }.getOrElse {
      BadRequest(Json.obj("status" -> "error", "message" -> "Invalid Request."))
    }
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
 */
case class ConfigInfo(id: Long, newRecord: Boolean, active: Boolean, waterfallOrder: Long, cpm: Option[Double], configurable: Boolean)

