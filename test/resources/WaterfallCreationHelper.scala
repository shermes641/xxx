package resources

import java.sql.Connection

import models.{AppConfig, App, Waterfall}

trait WaterfallCreationHelper extends GenerationNumberHelper {
  /**
   * Helper function to create a Waterfall along with an AppConfig record.
   * @param appID The ID of the App to which the Waterfall belongs.
   * @param waterfallName The name of the Waterfall to be created.
   * @param connection A Shared database connection.
   * @return The ID of the newly created Waterfall if the insert is successful; otherwise, None.
   */
  def createWaterfallWithConfig(appID: Long, waterfallName: String)(implicit connection: Connection): Long = {
    val id = Waterfall.create(appID, waterfallName).get
    val app = App.find(appID).get
    AppConfig.create(appID, app.token, generationNumber(appID))
    id
  }
}

