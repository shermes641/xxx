/**
 * Creates seed data to bootstrap the database.
 */

import models.AdProvider
import play.api.Logger

// The "correct" way to start the app
new play.core.StaticApplication(new java.io.File("."))

if(AdProvider.findAll.size == 0) {
  val adColonyResult = AdProvider.create(AdProvider.AdColony.name, AdProvider.AdColony.configurationData, AdProvider.AdColony.callbackURLFormat, AdProvider.AdColony.configurable, AdProvider.AdColony.defaultEcpm)
  val hyprResult = AdProvider.create(AdProvider.HyprMarketplace.name, AdProvider.HyprMarketplace.configurationData, AdProvider.HyprMarketplace.callbackURLFormat, AdProvider.HyprMarketplace.configurable, AdProvider.HyprMarketplace.defaultEcpm)
  val vungleResult = AdProvider.create(AdProvider.Vungle.name, AdProvider.Vungle.configurationData, AdProvider.Vungle.callbackURLFormat, AdProvider.Vungle.configurable, AdProvider.Vungle.defaultEcpm)
  val appLovinResult = AdProvider.create(AdProvider.AppLovin.name, AdProvider.AppLovin.configurationData, AdProvider.AppLovin.callbackURLFormat, AdProvider.AppLovin.configurable, AdProvider.AppLovin.defaultEcpm)
  (adColonyResult, hyprResult, vungleResult, appLovinResult) match {
    case (Some(adColonyID), Some(hyprID), Some(vungleID), Some(appLovinID)) => Logger.debug("All ad providers created successfully!")
    case (_, _, _, _) => Logger.error("Something went wrong and one or more of the ad providers were not created properly.")
  }
} else {
  Logger.warn("Running this script without an empty ad_providers table may result in duplicate ad providers.  If you need to create one or more of these ad providers, you should do it manually.")
}
