package controllers

import javax.inject._
import java.sql.Connection
import models._
import play.api.db.Database
import play.api.libs.json._
import play.api.mvc._
import scala.language.implicitConversions

/**
  * Controller for all WaterfallAdProvider actions
  * @param models Helper class containing dependency-injected instances of service classes
  * @param db     A shared database
  */
@Singleton
class WaterfallAdProvidersController @Inject() (models: ModelService,
                                                db: Database) extends Controller with Secured with JsonToValueHelper {
  val distributorUserService = models.distributorUserService
  val waterfallAdProviderService = models.waterfallAdProviderService
  val appConfigService = models.appConfigServiceService
  val jsonBuilder = models.jsonBuilder
  val platform = models.platform

  override val authUser = distributorUserService // Used in Secured trait
  /**
   * Accepts AJAX call from Waterfall edit form.
   * @param distributorID ID of the Distributor who owns the Waterfall and WaterfallAdProviders.
   * @return Responds with 200 and the ID of the new WaterfallAdProvider if successful; otherwise, returns 400.
   */
  def create(distributorID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    request.body.asJson.map { wapData =>
      db.withTransaction { implicit connection =>
        try {
          val cpm: Option[Double] = (wapData \ "cpm").toOption match {
            case Some(cpmVal: JsNumber) => Some(cpmVal.as[Double])
            case Some(cpmVal: JsString) => Some(cpmVal.as[String].toDouble)
            case _ => None
          }
          val waterfallID = (wapData \ "waterfallID").as[String].toLong
          val wapID = waterfallAdProviderService.createWithTransaction(waterfallID, (wapData \ "adProviderID").as[Long], None, cpm, (wapData \ "configurable").as[Boolean], active = false)
          val appToken = (wapData \ "appToken").as[String]
          val generationNumber = (wapData \ "generationNumber").as[Long]
          val newGenerationNumber = appConfigService.createWithWaterfallIDInTransaction(waterfallID, Some(generationNumber))
          (wapID, newGenerationNumber) match {
            case (Some(wapIDVal), Some(newGenerationNumberVal)) => {
              val jsonParams = Some(Json.obj("status" -> "success", "message" -> "Ad Provider configuration updated!", "wapID" -> wapIDVal, "newGenerationNumber" -> newGenerationNumberVal))
              retrieveWaterfallAdProvider(wapIDVal, distributorID, Some((wapData \ "appToken").as[String]), jsonParams)
            }
            case (_, _) => {
              BadRequest(Json.obj("status" -> "error", "message" -> "Ad Provider was not created."))
            }
          }
        } catch {
          case error: org.postgresql.util.PSQLException => rollback
          case error: IllegalArgumentException => rollback
        }
      }
    }.getOrElse {
      BadRequest(Json.obj("status" -> "error", "message" -> "Invalid request."))
    }
  }

  /**
   * Renders form for editing WaterfallAdProviders.
   * @param distributorID ID of the Distributor who owns the current WaterfallAdProvider.
   * @param waterfallAdProviderID ID of the current WaterfallAdProvider.
   * @param appToken String identifier for the App to which the Waterfall belongs.
   * @return Form for editing WaterfallAdProvider.
   */
  def edit(distributorID: Long, waterfallAdProviderID: Long, appToken: Option[String]) = withAuth(Some(distributorID)) { username => implicit request =>
    db.withConnection { implicit connection =>
      retrieveWaterfallAdProvider(waterfallAdProviderID, distributorID, appToken, jsonParams = None)
    }
  }

  /**
   * Helper function to find WaterfallAdProvider configuration data in the create and edit actions.
   * @param waterfallAdProviderID ID of the current WaterfallAdProvider.
   * @param distributorID ID of the Distributor who owns the current WaterfallAdProvider.
   * @param appToken String identifier for the App to which the Waterfall belongs.
   * @param jsonParams Optional additional JSON params to be added to the response.
   * @param connection A shared database connection.
   * @return JSON containing WaterfallAdProvider data if a WaterfallAdProvider is found; otherwise, a JSON error message.
   */
  def retrieveWaterfallAdProvider(waterfallAdProviderID: Long, distributorID: Long, appToken: Option[String], jsonParams: Option[JsObject])(implicit connection: Connection) = {
    waterfallAdProviderService.findConfigurationData(waterfallAdProviderID) match {
      case Some(configData) => {
        val callbackUrl: Option[String] = configData.callbackUrlFormat match {
          case Some(callback) => {
            val token = appToken match {
              case Some(apiToken) => apiToken
              case None => None
            }
            Some(callback.format(token, configData.rewardMin))
          }
          case None => None
        }
        val appDomain: String = platform.find(configData.platformID).serverToServerDomain
        val response = Json.obj(
          "distributorID" -> JsString(distributorID.toString),
          "waterfallAdProviderID" -> JsNumber(waterfallAdProviderID),
          "adProviderName" -> JsString(configData.name),
          "reportingActive" -> JsBoolean(configData.reportingActive),
          "callbackUrl" -> JsString(callbackUrl.getOrElse("")),
          "callbackUrlDescription" -> JsString(configData.callbackUrlDescription),
          "cpm" -> configData.cpm,
          "appDomain" -> JsString(appDomain)
        )
          .deepMerge(jsonBuilder.buildWAPParams(jsonBuilder.buildWAPParamsForUI, configData))
        jsonParams match {
          case Some(params) => Ok(response.deepMerge(params))
          case None => Ok(response)
        }
      }
      case _ => {
        BadRequest(Json.obj("status" -> "error", "message" -> "Could not find ad provider."))
      }
    }
  }

  /**
   * Accepts AJAX call from WaterfallAdProvider edit form.
   * @param distributorID ID of the Distributor who owns the current WaterfallAdProvider.
   * @param waterfallAdProviderID ID of the current WaterfallAdProvider.
   * @return Responds with 200 if update is successful.  Otherwise, 400 is returned.
   */
  def update(distributorID: Long, waterfallAdProviderID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    val badResponse = Json.obj("status" -> "error", "message" -> "Ad Provider configuration was not updated.")
    request.body.asJson.map { jsonResponse =>
      db.withTransaction { implicit connection =>
        try {
          waterfallAdProviderService.find(waterfallAdProviderID) match {
            case Some(record) => {
              val appToken = (jsonResponse \ "appToken").as[String]
              val waterfallID = (jsonResponse \ "waterfallID").as[String].toLong
              val configData = jsonBuilder.buildWAPParams(jsonBuilder.buildWAPParamsForDB, jsonResponse)
              val reportingActive = (jsonResponse \ "reportingActive").as[Boolean]
              val generationNumber = (jsonResponse \ "generationNumber").as[Long]
              val eCPM = (jsonResponse \ "cpm").as[String].toDouble
              val newValues = new WaterfallAdProvider(record.id, record.waterfallID, record.adProviderID, record.waterfallOrder, Some(eCPM), record.active, record.fillRate, configData, reportingActive)
              waterfallAdProviderService.updateWithTransaction(newValues) match {
                case 1 => {
                  val newGenerationNumber: Long = appConfigService.createWithWaterfallIDInTransaction(waterfallID, Some(generationNumber)).getOrElse(0)
                  Ok(Json.obj("status" -> "success", "message" -> "Ad Provider configuration updated!", "newGenerationNumber" -> newGenerationNumber))
                }
                case _ => BadRequest(badResponse)
              }
            }
            case _ => BadRequest(badResponse)
          }
        } catch {
          case error: org.postgresql.util.PSQLException => rollback
          case error: IllegalArgumentException => rollback
        }
      }
    }.getOrElse {
      BadRequest(Json.obj("status" -> "error", "message" -> "Invalid request."))
    }
  }

  /**
   * Rolls back the transaction and sends a JSON error message to the user.
   * @param connection The shared connection for the database transaction.
   */
  def rollback(implicit connection: Connection) = {
    connection.rollback()
    BadRequest(Json.obj("status" -> "error", "message" -> "The Waterfall could not be edited. Please refresh the browser."))
  }
}
