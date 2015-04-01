/**
 * Updates all fields for all ad providers.
 */

import anorm._
import models.{UpdatableAdProvider, AdProvider}
import play.api.db.DB
import play.api.Play.current

new play.core.StaticApplication(new java.io.File("."))

// All ad providers to be updated
val allAdProviders = List(AdProvider.AdColony, AdProvider.HyprMarketplace, AdProvider.Vungle, AdProvider.AppLovin)

/**
 * Updates a single ad provider.
 * @param adProvider A class encapsulating all of the updatable information for an ad provider.
 * @return The number of rows updated if the ad provider is updated successfully; otherwise, 0.
 */
def update(adProvider: UpdatableAdProvider): Long = {
  DB.withConnection { implicit connection =>
    SQL(
      """
        UPDATE ad_providers
        SET configuration_data=CAST({configuration_data} AS json), callback_url_format={callback_url_format},
        configurable={configurable}, default_ecpm={default_ecpm}
        WHERE name={name};
      """
    ).on("name" -> adProvider.name, "configuration_data" -> adProvider.configurationData, "callback_url_format" -> adProvider.callbackURLFormat,
        "configurable" -> adProvider.configurable, "default_ecpm" -> adProvider.defaultEcpm).executeUpdate()
  }
}

/**
 * Updates all ad providers
 */
def updateAll = {
  allAdProviders.foreach { adProvider =>
    update(adProvider) match {
      case 1 => println(adProvider.name + " was updated successfully!")
      case _ => println(adProvider.name + " was not updated successfully.")
    }
  }
}

updateAll
