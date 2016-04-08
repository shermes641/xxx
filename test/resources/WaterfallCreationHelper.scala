package models

import anorm._
import java.sql.Connection
import play.api.db.DB
//import models._

trait WaterfallCreationHelper {
  val appConfigModel: AppConfigService
  val waterfallModel: WaterfallService
  val appModel: AppService
  /**
   * Helper function to create a Waterfall along with an AppConfig record.
   * @param appID The ID of the App to which the Waterfall belongs.
   * @param waterfallName The name of the Waterfall to be created.
   * @param connection A Shared database connection.
   * @return The ID of the newly created Waterfall if the insert is successful; otherwise, None.
   */
  def createWaterfallWithConfig(appID: Long, waterfallName: String)(implicit connection: Connection): Long = {
    val genNum = SQL(
      """SELECT COALESCE(MAX(generation_number), 0) AS generation FROM app_configs where app_id={app_id}"""
    ).on("app_id" -> appID).as(SqlParser.long("generation").single)
    val id = waterfallModel.create(appID, waterfallName).get
    val app = appModel.find(appID).get
    appConfigModel.create(appID, app.token, genNum)
    id
  }
}

