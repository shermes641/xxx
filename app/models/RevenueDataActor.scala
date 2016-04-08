package models

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import javax.inject.Inject
import play.api.Application
import play.api.db.Database
import play.api.libs.json._
import play.Logger
import play.api.libs.ws.WSClient
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/**
  * Retrieves reporting data for all ad providers
  * @param waterfallAdProviderService Encapsulates all WaterfallAdProvider functions
  * @param appService                 Encapsulates all App functions
  * @param platform                   Contains information for iOS and Android platforms
  * @param db                         The default database
  * @param actorSystem                The default Akka actor system
  * @param configVars                 Shared ENV configuration variables
  * @param keenInitialization         The initialized Keen client
  * @param appEnvironment             The environment in which the app is running
  * @param wsClient                   The client used for all web service calls
  */
class RevenueDataActorWorker @Inject() (waterfallAdProviderService: WaterfallAdProviderService,
                                        appService: AppService,
                                        platform: Platform,
                                        db: Database,
                                        actorSystem: ActorSystem,
                                        configVars: ConfigVars,
                                        keenInitialization: KeenInitialization,
                                        appEnvironment: Environment,
                                        wsClient: WSClient) {
  val RevenueDataCollectionFrequency = 30.minutes
  val appLovinActor = actorSystem.actorOf(Props(new AppLovinReportingActor))

  /**
   * Initializes background job for collecting and updating eCPM data for WaterfallAdProviders.
   * @param app The play application.
   * @return    A scheduled Akka background job.
   */
  def startRevenueDataCollection(implicit app: Application) = {
    actorSystem.scheduler.schedule(0 seconds, RevenueDataCollectionFrequency) {
      val waterfallAdProviders: List[WaterfallAdProviderRevenueData] = waterfallAdProviderService.findAllReportingEnabled
      waterfallAdProviders.foreach { provider =>
        val actor = actorSystem.actorOf(Props(
          new RevenueDataActor(
            provider.waterfallAdProviderID,
            provider.configurationData,
            waterfallAdProviderService,
            appService,
            platform,
            configVars,
            keenInitialization,
            db,
            actorSystem,
            appEnvironment,
            wsClient,
            appLovinActor
          )
        ))
        actor ! provider.name
      }
    }
  }
}

/**
 * Actor which retrieves eCPM data from third-parties, then updates WaterfallAdProviders accordingly
 * @param waterfallAdProviderID      The ID of the WaterfallAdProvider to be updated
 * @param configurationData          WaterfallAdProvider configuration data containing reporting API keys
 * @param waterfallAdProviderService Encapsulates all WaterfallAdProvider functions
 * @param appService                 Encapsulates all App functions
 * @param platform                   Contains information for iOS and Android platforms
 * @param configVars                 Shared ENV configuration variables
 * @param keenInitialization         Shared Keen client
 * @param db                         The default database
 * @param actorSystem                The default Akka actor system
 * @param appEnvironment             The environment in which the app is running
 * @param wsClient                   The client used for all web service calls
 * @param appLovinActor              A shared reference to the AppLovinActor
 */
class RevenueDataActor(waterfallAdProviderID: Long,
                       configurationData: JsValue,
                       waterfallAdProviderService: WaterfallAdProviderService,
                       appService: AppService,
                       platform: Platform,
                       configVars: ConfigVars,
                       keenInitialization: KeenInitialization,
                       db: Database,
                       actorSystem: ActorSystem,
                       appEnvironment: Environment,
                       wsClient: WSClient,
                       appLovinActor: ActorRef) extends Actor {
  /**
   * Receives messages passed from Global.startRevenueDataCollection method.
   * @return Initiation of background job for updating a particular WaterfallAdProvider.
   */
  def receive = {
    case "HyprMarketplace" =>
      new HyprMarketplaceReportingAPI(waterfallAdProviderID, configurationData, db, waterfallAdProviderService, platform, configVars, appEnvironment, keenInitialization, wsClient, appService).updateRevenueData()

    case "AdColony" =>
      new AdColonyReportingAPI(waterfallAdProviderID, configurationData, db, waterfallAdProviderService, configVars, wsClient).updateRevenueData()

    case "AppLovin" =>
      val appLovinReportingCall = new AppLovinReportingAPI(waterfallAdProviderID, configurationData, db, waterfallAdProviderService, configVars, wsClient)
      appLovinActor ! appLovinReportingCall

    case "Vungle" =>
      new VungleReportingAPI(waterfallAdProviderID, configurationData, db, waterfallAdProviderService, configVars, wsClient).updateRevenueData()

    case Constants.UnityAds.Name =>
      new UnityAdsReportingAPI(waterfallAdProviderID, configurationData, db, waterfallAdProviderService, configVars, wsClient).unityUpdateRevenueData()
  }
}

/**
 * AppLovin's reporting API returns a rate limit error when a specific IP address makes calls at a high frequency.
 * To avoid the rate limit, we process calls one at a time, through a single actor.
 */
class AppLovinReportingActor extends Actor {
  def receive = {
    case apiCall: AppLovinReportingAPI =>
      Try(
        Await.result(
          apiCall.updateRevenueData(),
          Duration(10000, "millis")
        )
      ) match {
        case Success(_) => None
        case Failure(exception) =>
          Logger.error(
            "AppLovin reporting API call failed:\n" +
            "WaterfallAdProvider ID: " + apiCall.waterfallAdProviderID + "\n" +
            "Error: " + exception.getMessage
          )
      }
  }
}
