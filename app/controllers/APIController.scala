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
    // Removes ad providers that are inactive or do not have a high enough eCPM value from the response.
    def filteredAdProviders(unfilteredAdProviders: List[AdProviderInfo]): List[AdProviderInfo] = {
      unfilteredAdProviders.filter(adProvider => adProvider.active.get && adProvider.meetsRewardThreshold)
    }
    Waterfall.order(token) match {
      // Token was not found in waterfalls table.
      case adProviders: List[AdProviderInfo] if(adProviders.size == 0) => {
        BadRequest(Json.obj("status" -> "error", "message" -> "Waterfall not found."))
      }
      // Waterfall is in test mode.
      case adProviders: List[AdProviderInfo] if(adProviders(0).testMode) => {
        val testConfigData: JsValue = JsObject(Seq("requiredParams" -> JsObject(Seq("distributorID" -> JsString(TEST_MODE_DISTRIBUTOR_ID), "appID" -> JsString(TEST_MODE_APP_ID)))))
        val testAdProviderConfig: AdProviderInfo = new AdProviderInfo(Some(TEST_MODE_PROVIDER_NAME), Some(testConfigData), Some(5.0), Some(100), Some(1), Some(100), Some(true), true, false, Some(false))
        Ok(JsonBuilder.waterfallResponse(List(testAdProviderConfig)))
      }
      // No ad providers are active.
      case adProviders: List[AdProviderInfo] if(filteredAdProviders(adProviders).size == 0) => {
        BadRequest(Json.obj("status" -> "error", "message" -> "There were no active ad providers found."))
      }
      // Waterfall is in "Optimized" mode.
      case adProviders: List[AdProviderInfo] if(adProviders(0).optimizedOrder) => {
        val providerList = filteredAdProviders(adProviders).sortWith { (provider1, provider2) =>
          (provider1.cpm, provider2.cpm) match {
            case (Some(cpm1: Double), Some(cpm2: Double)) => cpm1 > cpm2
            case (_, _) => false
          }
        }
        Ok(JsonBuilder.waterfallResponse(providerList))
      }
      // All other cases.
      case adProviders: List[AdProviderInfo] => {
        Ok(JsonBuilder.waterfallResponse(filteredAdProviders(adProviders)))
      }
    }
  }
}
