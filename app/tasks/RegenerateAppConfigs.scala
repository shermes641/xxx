package tasks

import anorm._
import java.sql.Connection
import models._
import play.api.db.DB
import play.api.Play
import play.api.Play.current
import scala.language.postfixOps

object RegenerateAppConfigs extends Mailer {
  val emailSubject = "AppConfigRegeneration Script Results"
  val recipient = Play.current.configuration.getString("hyprmediate_ops_email").get

  /**
   * Script to update AppConfigs for all Waterfalls.
   */
  def run = {
    if(AdProvider.updateAll == AdProvider.allProviders.size) {
      var unsuccessfulWaterfallIDs: Set[Long] = Set()
      var unsuccessfulWaterfallAdProviderIDs: Vector[Long] = Vector()

      DB.withTransaction { implicit connection =>
        val waterfallIDs: Vector[Long] = SQL("""SELECT id from waterfalls""")().map(row => row[Long]("id")).toVector
        waterfallIDs.map { waterfallID =>
          val waps = WaterfallAdProvider.findAllByWaterfallID(waterfallID)
          waps.map { wap =>
            try {
              val configData = WaterfallAdProvider.findConfigurationData(wap.id).get
              val newConfig = JsonBuilder.buildWAPParams(JsonBuilder.buildWAPParamsForDB, configData)
              val newWapValues = new WaterfallAdProvider(wap.id, waterfallID, wap.adProviderID, wap.waterfallOrder, wap.cpm, wap.active, wap.fillRate, newConfig, wap.reportingActive)
              WaterfallAdProvider.updateWithTransaction(newWapValues) match {
                case 0 => {
                  unsuccessfulWaterfallAdProviderIDs = unsuccessfulWaterfallAdProviderIDs :+ wap.id
                  unsuccessfulWaterfallIDs = unsuccessfulWaterfallIDs + wap.waterfallID
                  println("Error updating WaterfallAdProvider ID: " + wap.id)
                }
                case _ => None
              }
            } catch {
              case error: play.api.libs.json.JsResultException => {
                unsuccessfulWaterfallAdProviderIDs = unsuccessfulWaterfallAdProviderIDs :+ wap.id
                unsuccessfulWaterfallIDs = unsuccessfulWaterfallIDs + wap.waterfallID
                println("Error updating JSON configuration for WaterfallAdProvider ID: " + wap.id)
              }
            }
          }
          try {
            AppConfig.createWithWaterfallIDInTransaction(waterfallID, None) match {
              case Some(newGenerationNumber) => None
              case None => {
                unsuccessfulWaterfallIDs = unsuccessfulWaterfallIDs + waterfallID
                println("AppConfig not updated for Waterfall ID: " + waterfallID)
              }
            }
          } catch {
            case error: org.postgresql.util.PSQLException => {
              var message = "Received the following error: " + error.getServerErrorMessage
              rollbackWithError(message)
            }
            case error: IllegalArgumentException => {
              var message = "Received error message: " + error.getLocalizedMessage + "\n"
              message += "Stack Trace: " + error.fillInStackTrace
              rollbackWithError(message)
            }
          }
        }
        if(unsuccessfulWaterfallAdProviderIDs.size == 0 && unsuccessfulWaterfallIDs.size == 0) {
          val body = "All AppConfigs successfully updated!"
          println(body)
          sendEmail(recipient = recipient, subject = emailSubject, body = body)
        } else {
          var body = "AppConfigs for the following Waterfall IDs were not updated successfully: [" + unsuccessfulWaterfallIDs.mkString(", ") + "]\n"
          body += "The following WaterfallAdProvider IDs were not updated successfully: [" + unsuccessfulWaterfallAdProviderIDs.mkString(", ") + "]"
          println(body)
          sendEmail(recipient = recipient, subject = emailSubject, body = body)
        }
      }
    } else {
      var body = "Stopping update because there was a problem updating the Ad Providers. Please check the logs for more details."
      println(body)
      sendEmail(recipient = recipient, subject = emailSubject, body = body)
    }
  }

  /**
   * Rolls back transaction and sends notification email
   * @param body The body of the email
   * @param connection A shared database connection
   */
  def rollbackWithError(body: String)(implicit connection: Connection): Unit = {
    connection.rollback()
    sendEmail(recipient = recipient, subject = emailSubject, body = body)
  }
}
