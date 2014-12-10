package models

import play.api.Play.current
import play.api.db.DB

trait AppSpecSetup extends SpecificationWithFixtures {
  /**
   * Helper function to create a new App, VirtualCurrency, Waterfall, and AppConfig in tests.
   * @param distributorID The ID of the Distributor to which all models in setUpApp belong.
   * @param appName The name of the new App.
   * @param currencyName The name of the new VirtualCurrency.
   * @return A tuple consisting of an App, VirtualCurrency, Waterfall, and AppConfig.
   */
  def setUpApp(distributorID: Long, appName: String = "App 1", currencyName: String = "Coins"): (App, Waterfall, VirtualCurrency, AppConfig) = {
    val currentApp = {
      val id = App.create(distributorID, "App 1").get
      App.find(id).get
    }
    val virtualCurrency = {
      val id = VirtualCurrency.create(currentApp.id, "Gold", 100, None, None, Some(true)).get
      VirtualCurrency.find(id).get
    }
    val currentWaterfallID = DB.withTransaction { implicit connection => createWaterfallWithConfig(currentApp.id, appName) }
    val currentWaterfall = Waterfall.find(currentWaterfallID).get
    val appConfig = AppConfig.findLatest(currentApp.token).get
    (currentApp, currentWaterfall, virtualCurrency, appConfig)
  }
}
