package controllers

import play.api.mvc._
import models._
import play.api.libs.json._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.functional.syntax._

object WaterfallsController extends Controller with Secured {
  // Form mapping used in edit action
  val waterfallForm = Form[WaterfallMapping](
    mapping(
      "name" -> nonEmptyText
    )(WaterfallMapping.apply)(WaterfallMapping.unapply)
  )

  /**
   * Renders form for editing Waterfall.
   * @param distributorID ID of Distributor who owns the current Waterfall
   * @param waterfallID ID of the Waterfall being edited
   * @return Form for editing Waterfall
   */
  def edit(distributorID: Long, waterfallID: Long) = withAuth { username => implicit request =>
    val waterfall = Waterfall.find(waterfallID).get
    val form = waterfallForm.fill(new WaterfallMapping(waterfall.name))
    val waterfallAdProviderList = WaterfallAdProvider.currentOrder(waterfallID)
    Ok(views.html.Waterfalls.edit(form, distributorID, waterfallID, waterfallAdProviderList))
  }

  // Creates WaterfallAttributes instance from JSON.
  implicit val WaterfallOrderReads: Reads[WaterfallAttributes] = (
    (JsPath \ "adProviderOrder").read[JsArray] and
    (JsPath \ "waterfallName").read[String]
  )(WaterfallAttributes.apply _)

  /**
   * Accepts AJAX call from Waterfall edit form to update attributes.
   * @param distributorID ID of Distributor who owns the current Waterfall
   * @param waterfallID ID of the Waterfall being edited
   * @return Responds with 200 if update is successful.  Otherwise, 400 is returned.
   */
  def update(distributorID: Long, waterfallID: Long) = Action(parse.json) { implicit request =>
    val incoming = request.body.validate[WaterfallAttributes]
    incoming.fold(
      errors => {
        BadRequest(Json.obj("status" -> "error", "message" -> JsError.toFlatJson(errors)))
      },
      waterfall => {
        val listOrder: List[String] = waterfall.adProviderOrder.as[List[String]]
        Waterfall.update(new Waterfall(waterfallID, waterfall.waterfallName))
        WaterfallAdProvider.updateWaterfallOrder(listOrder) match {
          case true => {
            Ok(Json.obj("status" -> "OK", "message" -> "Waterfall updated!"))
          }
          case _ => {
            BadRequest(Json.obj("status" -> "error", "message" -> "Waterfall was not updated."))
          }
        }
      }
    )
  }
}

/**
 * Used for mapping Waterfall attributes in waterfallForm.
 * @param name Maps to the name field in the waterfalls table.
 */
case class WaterfallMapping(name: String) {}

/**
 * Used for mapping JSON request to new Waterfall adProviderOrder.
 * @param adProviderOrder contains the new adProviderOrder
 * @param waterfallName contains the updated name of the Waterfall
 */
case class WaterfallAttributes(adProviderOrder: JsArray, waterfallName: String)
