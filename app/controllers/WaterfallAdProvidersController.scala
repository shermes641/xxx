package controllers

import play.api.mvc._
import models._
import play.api.libs.json._

object WaterfallAdProvidersController extends Controller with Secured {
  /**
   * Renders form for editing WaterfallAdProviders.
   * @param distributorID ID of the Distributor who owns the current WaterfallAdProvider.
   * @param waterfallAdProviderID ID of the current WaterfallAdProvider.
   * @return Form for editing WaterfallAdProvider.
   */
  def edit(distributorID: Long, waterfallAdProviderID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    WaterfallAdProvider.findConfigurationData(waterfallAdProviderID) match {
      case Some(configData) => {
        Ok(views.html.WaterfallAdProviders.edit(distributorID, waterfallAdProviderID, configData.mappedFields))
      }
      case _ => {
        Redirect(routes.AppsController.index(distributorID)).flashing("error" -> "Could not find ad provider.")
      }
    }
  }

  /**
   * Accepts AJAX call from WaterfallAdProvider edit form.
   * @param distributorID ID of the Distributor who owns the current WaterfallAdProvider.
   * @param waterfallAdProviderID ID of the current WaterfallAdProvider.
   * @return Responds with 200 if update is successful.  Otherwise, 400 is returned.
   */
  def update(distributorID: Long, waterfallAdProviderID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    val badResponse = Json.obj("status" -> "error", "message" -> "WaterfallAdProvider was not updated.")
    request.body.asJson.map { configData =>
      WaterfallAdProvider.find(waterfallAdProviderID) match {
        case Some(record) => {
          val newValues = new WaterfallAdProvider(record.id, record.waterfallID, record.adProviderID, record.waterfallOrder, record.cpm, record.active, record.fillRate, configData)
          WaterfallAdProvider.update(newValues) match {
            case 1 => Ok(Json.obj("status" -> "OK", "message" -> "WaterfallAdProvider updated!"))
            case _ => BadRequest(badResponse)
          }
        }
        case _ => BadRequest(badResponse)
      }
    }.getOrElse {
      BadRequest(Json.obj("status" -> "error", "message" -> "Invalid request."))
    }
  }
}
