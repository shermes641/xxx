package controllers

import models.Waterfall.AdProviderInfo
import models.JsonBuilder
import play.api.mvc._
import models.Waterfall
import play.api.libs.json._

object APIController extends Controller {
  /**
   * Responds with waterfall order in JSON form.
   * @param token Random string which both authenticates the request and identifies the waterfall.
   * @return If a waterfall is found and ad providers are configured, return a JSON with the ordered ad providers and configuration info.  Otherwise, return a JSON with an error message.
   */
  def waterfallV1(token: String) = Action { implicit request =>
    Waterfall.order(token) match {
      case adProviders: List[AdProviderInfo] if(adProviders.size == 0) => {
        BadRequest(Json.obj("status" -> "error", "message" -> "No ad providers are active."))
      }
      case adProviders: List[AdProviderInfo] => {
        Ok(JsonBuilder.waterfallResponse(adProviders))
      }
    }
  }
}
