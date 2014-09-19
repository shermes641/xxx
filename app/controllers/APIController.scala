package controllers

import models.Waterfall.AdProviderInfo
import models.JsonBuilder
import play.api.mvc._
import models.Waterfall
import play.api.libs.json._

object APIController extends Controller {
  val TEST_MODE_DISTRIBUTOR_ID = "111"
  val TEST_MODE_PROVIDER_NAME = "HyprMX"
  val TEST_MODE_APP_ID = " "
  /**
   * Responds with waterfall order in JSON form.
   * @param token Random string which both authenticates the request and identifies the waterfall.
   * @return If a waterfall is found and ad providers are configured, return a JSON with the ordered ad providers and configuration info.  Otherwise, return a JSON with an error message.
   */
  def waterfallV1(token: String) = Action { implicit request =>
    Waterfall.order(token) match {
      // Token was not found in waterfalls table.
      case adProviders: List[AdProviderInfo] if(adProviders.size == 0) => {
        BadRequest(Json.obj("status" -> "error", "message" -> "Waterfall not found."))
      }
      // Waterfall is in test mode.
      case adProviders: List[AdProviderInfo] if(adProviders(0).testMode) => {
        val testConfigData: JsValue = JsObject(Seq("distributorID" -> JsString(TEST_MODE_DISTRIBUTOR_ID), "appID" -> JsString(TEST_MODE_APP_ID)))
        val testAdProviderConfig: AdProviderInfo = new AdProviderInfo(Some(TEST_MODE_PROVIDER_NAME), Some(testConfigData), None, true, false, Some(false))
        Ok(JsonBuilder.waterfallResponse(List(testAdProviderConfig)))
      }
      // Waterfall is in "Optimized" mode.
      case adProviders: List[AdProviderInfo] if(adProviders(0).optimizedOrder) => {
        val providerList = adProviders.map( adProvider =>
          adProvider.cpm match {
            case Some(cpm: Double) => new AdProviderInfo(adProvider.providerName, adProvider.configurationData, adProvider.cpm, adProvider.optimizedOrder, adProvider.testMode, adProvider.active)
            case None => new AdProviderInfo(adProvider.providerName, adProvider.configurationData, Some(0), adProvider.optimizedOrder, adProvider.testMode, adProvider.active)
          }
        ).filter(adProvider => adProvider.active.get).sortWith(_.cpm.get > _.cpm.get)
        Ok(JsonBuilder.waterfallResponse(providerList))
      }
      // All other cases.
      case adProviders: List[AdProviderInfo] => {
        Ok(JsonBuilder.waterfallResponse(adProviders.filter(adProvider => adProvider.active.get)))
      }
    }
  }
}
