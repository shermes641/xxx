package models

import akka.actor.Actor
import akka.actor.Props
import play.api.Application
import play.api.libs.concurrent.Akka
import play.api.libs.json._
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

object RevenueDataActor {
  val REVENUE_DATA_COLLECTION_FREQUENCY = 1.hour

  /**
   * Initializes background job for collecting and updating eCPM data for WaterfallAdProviders.
   * @param app The play application.
   * @return A scheduled Akka background job.
   */
  def startRevenueDataCollection(implicit app: Application) = {
    Akka.system(app).scheduler.schedule(0 seconds, REVENUE_DATA_COLLECTION_FREQUENCY) {
      val waterfallAdProviders: List[WaterfallAdProviderRevenueData] = WaterfallAdProvider.findAllReportingEnabled
      waterfallAdProviders.foreach { provider =>
        val actor = Akka.system(app).actorOf(Props(new RevenueDataActor(provider.waterfallAdProviderID, provider.configurationData)))
        actor ! provider.name
      }
    }
  }
}

/**
 * Actor which retrieves eCPM data from third-parties, then updates WaterfallAdProviders accordingly.
 * @param waterfallAdProviderID The ID of the WaterfallAdProvider to be updated.
 */
class RevenueDataActor(waterfallAdProviderID: Long, configurationData: JsValue) extends Actor {
  /**
   * Receives messages passed from Global.startRevenueDataCollection method.
   * @return Initiation of background job for updating a particular WaterfallAdProvider.
   */
  def receive = {
    case "HyprMarketplace" => {
      new HyprMarketplaceReportingAPI(waterfallAdProviderID, configurationData).updateRevenueData
    }
    case "AdColony" => {
      new AdColonyReportingAPI(waterfallAdProviderID, configurationData).updateRevenueData
    }
    case "AppLovin" => {
      new AppLovinReportingAPI(waterfallAdProviderID, configurationData).updateRevenueData
    }
  }
}
