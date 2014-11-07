package controllers

import play.api.Play
import play.api.mvc._
import models._
import play.api.libs.json._

object WaterfallAdProvidersController extends Controller with Secured with JsonToValueHelper {
  /**
   * Accepts AJAX call from Waterfall edit form.
   * @param distributorID ID of the Distributor who owns the Waterfall and WaterfallAdProviders.
   * @return Responds with 200 and the ID of the new WaterfallAdProvider if successful; otherwise, returns 400.
   */
  def create(distributorID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    request.body.asJson.map { wapData =>
      WaterfallAdProvider.create((wapData \ "waterfallID").as[String].toLong, (wapData \ "adProviderID").as[String].toLong, (wapData \ "waterfallOrder") , (wapData \ "cpm"), (wapData \ "configurable").as[String].toBoolean, (wapData \ "active").as[Boolean]) match {
        case Some(id) => {
          Ok(Json.obj("status" -> "OK", "message" -> "Ad Provider configuration updated!", "wapID" -> id))
        }
        case None => {
          BadRequest(Json.obj("status" -> "error", "message" -> "Ad Provider was not created."))
        }
      }
    }.getOrElse {
      BadRequest(Json.obj("status" -> "error", "message" -> "Invalid request."))
    }
  }

  /**
   * Renders form for editing WaterfallAdProviders.
   * @param distributorID ID of the Distributor who owns the current WaterfallAdProvider.
   * @param waterfallAdProviderID ID of the current WaterfallAdProvider.
   * @param waterfallToken String identifier for the current Waterfall.
   * @return Form for editing WaterfallAdProvider.
   */
  def edit(distributorID: Long, waterfallAdProviderID: Long, waterfallToken: Option[String]) = withAuth(Some(distributorID)) { username => implicit request =>
    WaterfallAdProvider.findConfigurationData(waterfallAdProviderID) match {
      case Some(configData) => {
        val callbackUrl: Option[String] = configData.callbackUrlFormat match {
          case Some(callback) => {
            val token = waterfallToken match { case Some(apiToken) => apiToken; case None => None}
            Some(callback.format(token))
          }
          case None => None
        }
        Ok(views.html.WaterfallAdProviders.edit(distributorID, waterfallAdProviderID, configData.mappedFields("requiredParams"), configData.mappedFields("reportingParams"),
          configData.mappedFields("callbackParams"), configData.name, configData.reportingActive, callbackUrl, Play.current.configuration.getString("app_domain").get))
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
    val badResponse = Json.obj("status" -> "error", "message" -> "Ad Provider configuration was not updated.")
    request.body.asJson.map { jsonResponse =>
      WaterfallAdProvider.find(waterfallAdProviderID) match {
        case Some(record) => {
          val configData = (jsonResponse \ "configurationData").as[JsValue]
          val reportingActive = (jsonResponse \ "reportingActive").as[String].toBoolean
          val newValues = new WaterfallAdProvider(record.id, record.waterfallID, record.adProviderID, record.waterfallOrder, record.cpm, record.active, record.fillRate, configData, reportingActive)
          WaterfallAdProvider.update(newValues) match {
            case 1 => Ok(Json.obj("status" -> "OK", "message" -> "Ad Provider configuration updated!"))
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
