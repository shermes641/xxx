package controllers

import play.api.mvc._
import models._
import play.api.libs.json._

object WaterfallsController extends Controller with Secured {
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
          OrderedWaterfallAdProvider(adProvider.name, adProvider.id, None, false, None, true)
        }
        Ok(views.html.Waterfalls.edit(distributorID, waterfall, waterfallAdProviderList))
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
        new ConfigInfo((jsArray \ "id").as[String].toLong, (jsArray \ "newRecord").as[String].toBoolean, (jsArray \ "active").as[String].toBoolean, (jsArray \ "waterfallOrder").as[String].toLong)
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
 */
case class ConfigInfo(id: Long, newRecord: Boolean, active: Boolean, waterfallOrder: Long)

