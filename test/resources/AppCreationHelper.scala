package models

import play.api.db.DB
import play.api.Play.current

trait AppCreationHelper extends WaterfallCreationHelper {
  /**
   * Helper function to create a new App, VirtualCurrency, Waterfall, and AppConfig in tests.
   * @param distributorID The ID of the Distributor to which all models in setUpApp belong.
   * @param appName The name of the new App.
   * @param currencyName The name of the new VirtualCurrency.
   * @param exchangeRate The units of virtual currency per $1.
   * @param rewardMin The minimum reward a user can receive.
   * @param rewardMax The maximum reward a user can receive.  This is optional.
   * @param roundUp If true, we will round up the payout calculation to the rewardMin value.
   * @return A tuple consisting of an App, VirtualCurrency, Waterfall, and AppConfig.
   */
  def setUpApp(distributorID: Long, appName: String = "App 1", currencyName: String = "Coins",
               exchangeRate: Long = 100, rewardMin: Long = 1, rewardMax: Option[Long] = None, roundUp: Boolean = true): (App, Waterfall, VirtualCurrency, AppConfig) = {
    val currentApp = {
      val id = App.create(distributorID, appName).get
      App.find(id).get
    }
    val virtualCurrency = {
      val id = VirtualCurrency.create(currentApp.id, currencyName, exchangeRate, Some(rewardMin), rewardMax, Some(roundUp)).get
      VirtualCurrency.find(id).get
    }
    val currentWaterfallID = DB.withTransaction { implicit connection => createWaterfallWithConfig(currentApp.id, appName) }
    val currentWaterfall = Waterfall.find(currentWaterfallID, distributorID).get
    val appConfig = AppConfig.findLatest(currentApp.token).get
    (currentApp, currentWaterfall, virtualCurrency, appConfig)
  }
}
