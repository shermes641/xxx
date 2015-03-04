package controllers

import java.sql.Connection
import models._
import play.api.db.DB
import play.api.libs.json._
import play.api.mvc._
import play.api.Play
import play.api.Play.current
import scala.language.implicitConversions

object WaterfallAdProvidersController extends Controller with Secured with JsonToValueHelper {
  /**
   * Accepts AJAX call from Waterfall edit form.
   * @param distributorID ID of the Distributor who owns the Waterfall and WaterfallAdProviders.
   * @return Responds with 200 and the ID of the new WaterfallAdProvider if successful; otherwise, returns 400.
   */
  def create(distributorID: Long) = withAuth(Some(distributorID)) { username => implicit request =>
    request.body.asJson.map { wapData =>
      DB.withTransaction { implicit connection =>
        try {
          val cpm: Option[Double] = (wapData \ "cpm") match {
            case cpmVal: JsNumber => Some(cpmVal.as[Double])
            case _ => None
          }
          val wapID = WaterfallAdProvider.createWithTransaction((wapData \ "waterfallID").as[String].toLong, (wapData \ "adProviderID").as[String].toLong, None, cpm, (wapData \ "configurable").as[Boolean], active = false)
          val appToken = (wapData \ "appToken").as[String]
          val waterfallID = (wapData \ "waterfallID").as[String].toLong
          val generationNumber = (wapData \ "generationNumber").as[String].toLong
          val newGenerationNumber = AppConfig.createWithWaterfallIDInTransaction(waterfallID, Some(generationNumber))
          (wapID, newGenerationNumber) match {
            case (Some(wapIDVal), Some(newGenerationNumberVal)) => {
              val jsonParams = Some(Json.obj("status" -> "success", "message" -> "Ad Provider configuration updated!", "wapID" -> wapIDVal.toString, "newGenerationNumber" -> newGenerationNumberVal.toString))
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
    DB.withConnection { implicit connection =>
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
    WaterfallAdProvider.findConfigurationData(waterfallAdProviderID) match {
      case Some(configData) => {
        val callbackUrl: Option[String] = configData.callbackUrlFormat match {
          case Some(callback) => {
            val token = appToken match {
              case Some(apiToken) => apiToken
              case None => None
            }
            Some(callback.format(token))
          }
          case None => None
        }
        val response = Json.obj("distributorID" -> JsString(distributorID.toString), "waterfallAdProviderID" -> JsString(waterfallAdProviderID.toString),
          "adProviderName" -> JsString(configData.name), "reportingActive" -> JsBoolean(configData.reportingActive), "callbackUrl" -> JsString(callbackUrl.getOrElse("")),
          "cpm" -> configData.cpm, "appDomain" -> JsString(Play.current.configuration.getString("app_domain").get)).deepMerge(JsonBuilder.buildWAPParams(JsonBuilder.buildWAPParamsForUI, configData))
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
   * Converts RequiredParam class to JSON object.
   * @param param The an instance of the RequiredParam class to be converted.
   * @return JSON object to be used in the edit action.
   */
  implicit def requiredParamWrites(param: RequiredParam): JsObject = {
    JsObject(
      Seq(
        "displayKey" -> JsString(param.displayKey.getOrElse("")),
        "key" -> JsString(param.key.getOrElse("")),
        "dataType" -> JsString(param.dataType.getOrElse("")),
        "description" -> JsString(param.description.getOrElse("")),
        "value" -> JsString(param.value.getOrElse("")),
        "refreshOnAppRestart" -> JsBoolean(param.refreshOnAppRestart),
        "minLength" -> JsNumber(param.minLength)
      )
    )
  }

  /**
   * Assembles a JsArray from a list of RequiredParam JSON objects.
   * @param list The list of RequiredParams to be converted.
   * @return A JsArray containing RequiredParam JSON objects.
   */
  def requiredParamsListJs(list: List[RequiredParam]): JsArray = {
    list.foldLeft(JsArray(Seq()))((array, param) => array ++ JsArray(Seq(param)))
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
      DB.withTransaction { implicit connection =>
        try {
          WaterfallAdProvider.find(waterfallAdProviderID) match {
            case Some(record) => {
              val appToken = (jsonResponse \ "appToken").as[String]
              val waterfallID = (jsonResponse \ "waterfallID").as[String].toLong
              val configData = JsonBuilder.buildWAPParams(JsonBuilder.buildWAPParamsForDB, jsonResponse)
              val reportingActive = (jsonResponse \ "reportingActive").as[Boolean]
              val generationNumber = (jsonResponse \ "generationNumber").as[String].toLong
              val eCPM = (jsonResponse \ "cpm").as[String].toDouble
              val newValues = new WaterfallAdProvider(record.id, record.waterfallID, record.adProviderID, record.waterfallOrder, Some(eCPM), record.active, record.fillRate, configData, reportingActive)
              WaterfallAdProvider.updateWithTransaction(newValues) match {
                case 1 => {
                  val newGenerationNumber = AppConfig.createWithWaterfallIDInTransaction(waterfallID, Some(generationNumber)).getOrElse(0).toString
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
