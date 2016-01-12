package models

import akka.actor.Actor
import akka.actor.Props
import play.api.Application
import play.api.libs.concurrent.Akka
import play.api.libs.json._
import play.api.Play.current
import play.Logger
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.util.{Success, Failure, Try}

object RevenueDataActor {
  val RevenueDataCollectionFrequency = 1.hour
  val appLovinActor = Akka.system(current).actorOf(Props(new AppLovinReportingActor))

  /**
   * Initializes background job for collecting and updating eCPM data for WaterfallAdProviders.
   * @param app The play application.
   * @return A scheduled Akka background job.
   */
  def startRevenueDataCollection(implicit app: Application) = {
    Akka.system(app).scheduler.schedule(0 seconds, RevenueDataCollectionFrequency) {
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
      val appLovinReportingCall = new AppLovinReportingAPI(waterfallAdProviderID, configurationData)
      RevenueDataActor.appLovinActor ! appLovinReportingCall
    }
    case "Vungle" => {
      new VungleReportingAPI(waterfallAdProviderID, configurationData).updateRevenueData
    }
  }
}

/**
 * AppLovin's reporting API returns a rate limit error when a specific IP address makes calls at a high frequency.
 * To avoid the rate limit, we process calls one at a time, through a single actor.
 */
class AppLovinReportingActor extends Actor {
  def receive = {
    case apiCall: AppLovinReportingAPI => {
      Try(
        Await.result(
          apiCall.updateRevenueData,
          Duration(10000, "millis")
        )
      ) match {
        case Success(_) => None
        case Failure(exception) => {
          Logger.error(
            "AppLovin reporting API call failed:\n" +
            "WaterfallAdProvider ID: " + apiCall.waterfallAdProviderID + "\n" +
            "Error: " + exception.getMessage
          )
        }
      }
    }
  }
}
