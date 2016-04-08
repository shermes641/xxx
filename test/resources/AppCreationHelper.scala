package models

//import models._
import play.api.db.{Database, DB}
import play.api.Play.current
import play.api.db.DB
import scala.util.Random

trait AppCreationHelper extends WaterfallCreationHelper {
  val appModel: AppService
  val virtualCurrencyModel: VirtualCurrencyService
  val waterfallModel: WaterfallService
  val appConfigModel: AppConfigService
  val db: Database
  val thisPlatform: Platform

  /**
   * Generates an App name using random characters.
   * @return A randomly generated String
   */
  def randomAppName: String = Random.alphanumeric.take(10).mkString

  /**
   * Helper function to create a new App, VirtualCurrency, Waterfall, and AppConfig in tests.
   * @param distributorID The ID of the Distributor to which all models in setUpApp belong.
   * @param appName The name of the new App.
   * @param currencyName The name of the new VirtualCurrency.
   * @param exchangeRate The units of virtual currency per $1.
   * @param rewardMin The minimum reward a user can receive.
   * @param rewardMax The maximum reward a user can receive.  This is optional.
   * @param roundUp If true, we will round up the payout calculation to the rewardMin value.
   * @param callbackUrl  Some(url) if callback is required.
   * @return A tuple consisting of an App, VirtualCurrency, Waterfall, and AppConfig.
   */
  def setUpApp(distributorID: Long,
  appName: Option[String] = None, 
  currencyName: String = "Coins",
  exchangeRate: Long = 100, 
  rewardMin: Long = 1, 
  rewardMax: Option[Long] = None, 
  roundUp: Boolean = true, 
  platformID: Long = thisPlatform.Ios.PlatformID,
  callbackUrl: Option[String] = None
  ): (App, Waterfall, VirtualCurrency, AppConfig) = {
    val currentApp = {
      val id = appModel.create(distributorID, appName.getOrElse(randomAppName), platformID, cbUrl = callbackUrl).get
      appModel.find(id).get
    }
    val virtualCurrency = {
      val id = virtualCurrencyModel.create(currentApp.id, currencyName, exchangeRate, rewardMin, rewardMax, Some(roundUp)).get
      virtualCurrencyModel.find(id).get
    }
    val currentWaterfallID = db.withTransaction { implicit connection => createWaterfallWithConfig(currentApp.id, currentApp.name) }
    val currentWaterfall = waterfallModel.find(currentWaterfallID, distributorID).get
    val appConfig = appConfigModel.findLatest(currentApp.token).get
    (currentApp, currentWaterfall, virtualCurrency, appConfig)
  }
}
