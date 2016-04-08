package tasks

import anorm._
import java.sql.Connection
import models._
import play.api.db.Database
import play.api.Logger
import scala.language.postfixOps

/**
  * Regenerates an app config for every app in the database
  * @param db                         A shared database
  * @param waterfallAdProviderService A shared instance of the WaterfallAdProviderService class
  * @param appConfigService           A shared instance of the AppConfigService class
  * @param adProviderService          A shared instance of the AdProviderService class
  * @param jsonBuilder                A shared instance of the JsonBuilder class
  */
class RegenerateAppConfigsService(db: Database,
                                  waterfallAdProviderService: WaterfallAdProviderService,
                                  appConfigService: AppConfigService,
                                  adProviderService: AdProviderService,
                                  platform: Platform,
                                  jsonBuilder: JsonBuilder) {
  val taskName = "RegenerateAppConfigs: "
  /**
   * Script to update AppConfigs for all Waterfalls.
   */
  def run() = {
    // make sure all ad providers exist
    adProviderService.loadAll()
    if(adProviderService.updateAll == (platform.Ios.allAdProviders ++ platform.Android.allAdProviders).size) {
      var unsuccessfulWaterfallIDs: Set[Long] = Set()
      var unsuccessfulWaterfallAdProviderIDs: Vector[Long] = Vector()

      db.withTransaction { implicit connection =>
        val waterfallIDs: Vector[Long] = SQL("""SELECT id from waterfalls""").as(SqlParser.long("id").*).toVector
        waterfallIDs.map { waterfallID =>
          val waps = waterfallAdProviderService.findAllByWaterfallID(waterfallID)
          waps.map { wap =>
            try {
              val configData = waterfallAdProviderService.findConfigurationData(wap.id).get
              if(!configData.pending) {
                val newConfig = jsonBuilder.buildWAPParams(jsonBuilder.buildWAPParamsForDB, configData)
                val newWapValues = new WaterfallAdProvider(wap.id, waterfallID, wap.adProviderID, wap.waterfallOrder, wap.cpm, wap.active, wap.fillRate, newConfig, wap.reportingActive)
                waterfallAdProviderService.updateWithTransaction(newWapValues) match {
                  case 0 => {
                    unsuccessfulWaterfallAdProviderIDs = unsuccessfulWaterfallAdProviderIDs :+ wap.id
                    unsuccessfulWaterfallIDs = unsuccessfulWaterfallIDs + wap.waterfallID
                    Logger.error("Error updating WaterfallAdProvider ID: " + wap.id)
                  }
                  case _ => None
                }
              }
            } catch {
              case error: play.api.libs.json.JsResultException => {
                unsuccessfulWaterfallAdProviderIDs = unsuccessfulWaterfallAdProviderIDs :+ wap.id
                unsuccessfulWaterfallIDs = unsuccessfulWaterfallIDs + wap.waterfallID
                Logger.error("Error updating JSON configuration for WaterfallAdProvider ID: " + wap.id)
              }
            }
          }
          try {
            appConfigService.createWithWaterfallIDInTransaction(waterfallID, None) match {
              case Some(newGenerationNumber) => None
              case None => {
                unsuccessfulWaterfallIDs = unsuccessfulWaterfallIDs + waterfallID
                Logger.debug("AppConfig not updated for Waterfall ID: " + waterfallID)
              }
            }
          } catch {
            case error: org.postgresql.util.PSQLException => {
              rollbackWithError("Received the following error: " + error.getServerErrorMessage)
            }
            case error: IllegalArgumentException => {
              val message = "Received error message: " + error.getLocalizedMessage + "\n" +
                "Stack Trace: " + error.fillInStackTrace
              rollbackWithError(message)
            }
          }
        }
        if(unsuccessfulWaterfallAdProviderIDs.size == 0 && unsuccessfulWaterfallIDs.size == 0) {
          Logger.info(taskName + "All AppConfigs successfully updated!")
        } else {
          val errorMessage = "AppConfigs for the following Waterfall IDs were not updated successfully: [" + unsuccessfulWaterfallIDs.mkString(", ") + "]\n" +
            "The following WaterfallAdProvider IDs were not updated successfully: [" + unsuccessfulWaterfallAdProviderIDs.mkString(", ") + "]"
          Logger.error(taskName + errorMessage)
        }
      }
    } else {
      Logger.error(taskName + "Stopping update because there was a problem updating the Ad Providers. Please check the logs for more details.")
    }
  }

  /**
   * Rolls back transaction and logs an error
   * @param errorMessage The message to be logged
   * @param connection A shared database connection
   */
  def rollbackWithError(errorMessage: String)(implicit connection: Connection): Unit = {
    connection.rollback()
    Logger.error(errorMessage)
  }
}
