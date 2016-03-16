package resources

import anorm._
import models.App
import play.api.db.DB
import play.api.Play.current

trait GenerationNumberHelper {
  /**
   * Retrieve the latest generation_number for a particular waterfall ID.
   * @param appID The ID of the App to look up in the app_configs table.
   * @return The latest generation number if a record exists; otherwise, returns none.
   */
  def generationNumber(appID: Long): Long = {
    DB.withConnection { implicit connection =>
      SQL("""SELECT COALESCE(MAX(generation_number), 0) AS generation FROM app_configs where app_id={app_id}""").on("app_id" -> appID).apply()
    }.head[Long]("generation")
  }

  /**
   * Helper function to clear out previous generation configuration data.
   * @param appID The ID of the App to which the AppConfig belongs.
   * @return 1 if the insert is successful; otherwise, None.
   */
  def clearGeneration(appID: Long) = {
    App.find(appID) match {
      case Some(app) => {
        DB.withConnection { implicit connection =>
          SQL(
            """
              INSERT INTO app_configs (generation_number, app_id, app_token, configuration)
              VALUES ((SELECT COALESCE(MAX(generation_number), 0) + 1 AS generation FROM app_configs where app_id={app_id}),
              {app_id}, {app_token}, '{}');
            """
          ).on("app_id" -> appID, "app_token" -> app.token).executeInsert()
        }
      }
      case None => None
    }
  }
}
