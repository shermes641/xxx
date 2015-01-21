package models

import anorm._
import anorm.SqlParser._
import java.sql.Connection
import play.api.db.DB
import play.api.Play.current
import play.api.libs.json._
import scala.language.implicitConversions
import scala.language.postfixOps
import play.api.Play

/**
 * Encapsulates information for a record in the waterfall_ad_providers table.
 * @param id id field in the waterfall_ad_providers table
 * @param waterfallID ID of the waterfall to which this waterfall_ad_provider belongs
 * @param adProviderID ID of the ad provider record to which this waterfall_ad_provider belongs
 * @param waterfallOrder position in the waterfall
 * @param cpm cost per thousand impressions
 * @param active determines if the waterfall_ad_provider should be included in a waterfall
 * @param fillRate the ratio of ads shown to inventory checks
 * @param pending A boolean that determines if the waterfall_ad_provider is able to be activated.  This is used only in the case of HyprMarketplace while we wait for a Distributor ID.
 */
case class WaterfallAdProvider (
                                 id:Long, waterfallID:Long, adProviderID:Long, waterfallOrder: Option[Long], cpm: Option[Double], active: Option[Boolean],
                                 fillRate: Option[Float], configurationData: JsValue, reportingActive: Boolean, pending: Boolean = false
                                 )

object WaterfallAdProvider extends JsonConversion {
  // Used to convert SQL row into an instance of the WaterfallAdProvider class.
  val waterfallAdProviderParser: RowParser[WaterfallAdProvider] = {
    get[Long]("waterfall_ad_providers.id") ~
      get[Long]("waterfall_id") ~
      get[Long]("ad_provider_id") ~
      get[Option[Long]]("waterfall_order") ~
      get[Option[Double]]("cpm") ~
      get[Option[Boolean]]("active") ~
      get[Option[Float]]("fill_rate") ~
      get[JsValue]("configuration_data") ~
      get[Boolean]("reporting_active") ~
      get[Boolean]("pending") map {
      case id ~ waterfall_id ~ ad_provider_id ~ waterfall_order ~ cpm ~ active ~
        fill_rate ~ configuration_data ~ reporting_active ~ pending =>
        WaterfallAdProvider(id, waterfall_id, ad_provider_id, waterfall_order, cpm, active, fill_rate, configuration_data, reporting_active, pending)
    }
  }

  /**
   * SQL to update the fields for a particular record in the waterfall_ad_providers table.
   * @param waterfallAdProvider WaterfallAdProvider instance with update attributes.
   * @return SQL to be executed by update and updateWithTransaction methods.
   */
  def updateSQL(waterfallAdProvider: WaterfallAdProvider): SimpleSql[Row] = {
    SQL(
      """
          UPDATE waterfall_ad_providers
          SET waterfall_order={waterfall_order}, cpm={cpm}, active={active}, fill_rate={fill_rate},
          configuration_data=CAST({configuration_data} AS json), reporting_active={reporting_active},
          pending={pending}
          WHERE id={id};
      """
    ).on(
        "waterfall_order" -> waterfallAdProvider.waterfallOrder, "cpm" -> waterfallAdProvider.cpm,
        "active" -> waterfallAdProvider.active, "fill_rate" -> waterfallAdProvider.fillRate,
        "configuration_data" -> Json.stringify(waterfallAdProvider.configurationData),
        "reporting_active" -> waterfallAdProvider.reportingActive, "pending" -> waterfallAdProvider.pending, "id" -> waterfallAdProvider.id
      )
  }


  /**
   * Updates the fields for a particular record in waterfall_ad_providers table.
   * @param waterfallAdProvider WaterfallAdProvider instance with updated attributes
   * @return Number of rows updated
   */
  def update(waterfallAdProvider: WaterfallAdProvider): Int = {
    DB.withConnection { implicit connection =>
      updateSQL(waterfallAdProvider).executeUpdate()
    }
  }

  /**
   * Updates the fields for a particular record, within a transaction, in waterfall_ad_providers table.
   * @param waterfallAdProvider WaterfallAdProvider instance with updated attributes
   * @return Number of rows updated
   */
  def updateWithTransaction(waterfallAdProvider: WaterfallAdProvider)(implicit connection: Connection): Int = {
    updateSQL(waterfallAdProvider).executeUpdate()
  }

  /**
   * Updates the eCPM field for a given WaterfallAdProvider ID.
   * @param id The ID of the WaterfallAdProvider to be updated.
   * @param eCPM The new eCPM value calculated in RevenueDataActor.
   * @return the new generation number if the update is successful; otherwise, None.
   */
  def updateEcpm(id: Long, eCPM: Double)(implicit connection: Connection): Option[Long] = {
    SQL(
      """
      UPDATE waterfall_ad_providers
      SET cpm={cpm}
      WHERE id={id};
      """
    ).on("cpm" -> eCPM, "id" -> id).executeUpdate() match {
      case 1 => {
        WaterfallAdProvider.findWithTransaction(id) match {
          case Some(wap) => AppConfig.createWithWaterfallIDInTransaction(wap.waterfallID, None)
          case None => None
        }
      }
      case _ => None
    }
  }

  /**
   * Updates the waterfallOrder of all WaterfallAdProviders passed in.
   * @param adProviderList A list of WaterfallAdProvider IDs to be updated.
   * @return True if all updates were successful.  Otherwise, returns false.
   */
  def updateWaterfallOrder(adProviderList: List[String]): Boolean = {
    var successful = true
    for((adProviderID, index) <- adProviderList.view.zipWithIndex) {
      val updatableClass = WaterfallAdProvider.find(adProviderID.toLong) match {
        case Some(record) => {
          // The new waterfallOrder number is updated according to the position of each WaterfallAdProvider in the list.
          val updatedValues = new WaterfallAdProvider(record.id, record.waterfallID, record.adProviderID, Some(index + 1), record.cpm, record.active, record.fillRate, record.configurationData, record.reportingActive, record.pending)
          update(updatedValues) match {
            case 1 => {}
            case _ => successful = false
          }
        }
        case _ => successful = false
      }
    }
    successful
  }

  /**
   * SQL for inserting a new record in the waterfall_ad_providers table.
   * @param waterfallID ID of the Waterfall to which the new WaterfallAdProvider belongs
   * @param adProviderID ID of the Ad Provider to which the new WaterfallAdProvider belongs
   * @param cpm The estimated cost per thousand completions for an AdProvider.
   * @param configurable Determines if the cpm value can be edited for the WaterfallAdProvider.
   * @param active Boolean value determining if the WaterfallAdProvider can be included in the AppConfig list of AdProviders.
   * @param pending A boolean that determines if the waterfall_ad_provider is able to be activated.  This is used only in the case of HyprMarketplace while we wait for a Distributor ID.
   * @return SQL to be executed by create and createWithTransaction methods.
   */
  def insert(waterfallID: Long, adProviderID: Long, waterfallOrder: Option[Long] = None, cpm: Option[Double] = None, configurable: Boolean, active: Boolean = false, pending: Boolean = false): SimpleSql[Row] = {
    SQL(
      """
        INSERT INTO waterfall_ad_providers (waterfall_id, ad_provider_id, waterfall_order, cpm, configurable, active, pending)
        VALUES ({waterfall_id}, {ad_provider_id}, {waterfall_order}, {cpm}, {configurable}, {active}, {pending});
      """
    ).on("waterfall_id" -> waterfallID, "ad_provider_id" -> adProviderID, "waterfall_order" -> waterfallOrder, "cpm" -> cpm, "configurable" -> configurable, "active" -> active, "pending" -> pending)
  }

  /**
   * Creates a new WaterfallAdProvider record in the database unless a similar record exists.
   * @param waterfallID ID of the Waterfall to which the new WaterfallAdProvider belongs
   * @param adProviderID ID of the Ad Provider to which the new WaterfallAdProvider belongs
   * @param cpm The estimated cost per thousand completions for an AdProvider.
   * @param configurable Determines if the cpm value can be edited for the WaterfallAdProvider.
   * @param active Boolean value determining if the WaterfallAdProvider can be included in the AppConfig list of AdProviders.
   * @param pending A boolean that determines if the waterfall_ad_provider is able to be activated.  This is used only in the case of HyprMarketplace while we wait for a Distributor ID.
   * @return ID of new record if insert is successful, otherwise None.
   */
  def create(waterfallID: Long, adProviderID: Long, waterfallOrder: Option[Long] = None, cpm: Option[Double] = None, configurable: Boolean, active: Boolean = false, pending: Boolean = false): Option[Long] = {
    DB.withConnection { implicit connection =>
      insert(waterfallID, adProviderID, waterfallOrder, cpm, configurable, active, pending).executeInsert()
    }
  }

  /**
   * Creates a new WaterfallAdProvider record, within a transaction, unless a similar record exists.
   * @param waterfallID ID of the Waterfall to which the new WaterfallAdProvider belongs
   * @param adProviderID ID of the Ad Provider to which the new WaterfallAdProvider belongs
   * @param cpm The estimated cost per thousand completions for an AdProvider.
   * @param configurable Determines if the cpm value can be edited for the WaterfallAdProvider.
   * @param active Boolean value determining if the WaterfallAdProvider can be included in the AppConfig list of AdProviders.
   * @param pending A boolean that determines if the waterfall_ad_provider is able to be activated.  This is used only in the case of HyprMarketplace while we wait for a Distributor ID.
   * @return ID of new record if insert is successful, otherwise None.
   */
  def createWithTransaction(waterfallID: Long, adProviderID: Long, waterfallOrder: Option[Long] = None, cpm: Option[Double] = None, configurable: Boolean, active: Boolean = false, pending: Boolean = false)(implicit connection: Connection): Option[Long] = {
    insert(waterfallID, adProviderID, waterfallOrder, cpm, configurable, active, pending).executeInsert()
  }

  /**
   * Updates a HyprMarketplace WaterfallAdProvider instance with the distributor_channel_id from Player.
   * @param hyprWaterfallAdProvider The WaterfallAdProvider instance for HyprMarketplace.
   * @param distributionChannelID The AdNetwork ID created in Player.
   * @param connection A shared database connection.
   * @return 1 if the creation and update is successful; otherwise, returns 0.
   */
  def updateHyprMarketplaceConfig(hyprWaterfallAdProvider: WaterfallAdProvider, distributionChannelID: Long)(implicit connection: Connection) = {
    val hyprConfig = JsObject(
      Seq(
        "requiredParams" -> JsObject(
          Seq(
            "distributorID" -> JsString(distributionChannelID.toString),
            "eCPM" -> JsString(""),
            "appID" -> JsString("")
          )
        ),
        "reportingParams" -> JsObject(
          Seq(
            "APIKey" -> JsString(""),
            "placementID" -> JsString(""),
            "appID" -> JsString("")
          )
        ),
        "callbackParams" -> JsObject(
          Seq(
            "APIKey" -> JsString("")
          )
        )
      )
    )
    val newValues = new WaterfallAdProvider(hyprWaterfallAdProvider.id, hyprWaterfallAdProvider.waterfallID, hyprWaterfallAdProvider.adProviderID,
      hyprWaterfallAdProvider.waterfallOrder, hyprWaterfallAdProvider.cpm, Some(true), hyprWaterfallAdProvider.fillRate, hyprConfig, false, false)
    WaterfallAdProvider.updateWithTransaction(newValues)
  }

  /**
   * SQL to select a particular WaterfallAdProvider record from the database.
   * @param waterfallAdProviderID The ID of the WaterfallAdProvider for which to query.
   * @return SQL to be executed by find and findWithTransaction methods.
   */
  def findSQL(waterfallAdProviderID: Long): SimpleSql[Row] = {
    SQL(
      """
          SELECT waterfall_ad_providers.*
          FROM waterfall_ad_providers
          WHERE id = {id};
      """
    ).on("id" -> waterfallAdProviderID)
  }

  /**
   * Finds WaterfallAdProvider record by ID.
   * @param waterfallAdProviderID ID of WaterfallAdProvider
   * @return WaterfallAdProvider instance if one exists. Otherwise, returns None.
   */
  def find(waterfallAdProviderID: Long): Option[WaterfallAdProvider] = {
    DB.withConnection { implicit connection =>
      findSQL(waterfallAdProviderID).as(waterfallAdProviderParser*) match {
        case List(waterfallAdProvider) => Some(waterfallAdProvider)
        case _ => None
      }
    }
  }

  /**
   * Finds WaterfallAdProvider record, within transaction, by ID within a database transaction.
   * @param waterfallAdProviderID ID of WaterfallAdProvider
   * @return WaterfallAdProvider instance if one exists. Otherwise, returns None.
   */
  def findWithTransaction(waterfallAdProviderID: Long)(implicit connection: Connection): Option[WaterfallAdProvider] = {
    findSQL(waterfallAdProviderID).as(waterfallAdProviderParser*) match {
      case List(waterfallAdProvider) => Some(waterfallAdProvider)
      case _ => None
    }
  }

  /**
   * Finds all records in the waterfall_ad_providers table by waterfallID.
   * @param waterfallID ID of current Waterfall
   * @return List of WaterfallAdProvider instances if any exist.  Otherwise, returns an empty list.
   */
  def findAllByWaterfallID(waterfallID: Long): List[WaterfallAdProvider] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT waterfall_ad_providers.*
          FROM waterfall_ad_providers
          WHERE waterfall_id = {waterfall_id};
        """
      ).on("waterfall_id" -> waterfallID)
      query.as(waterfallAdProviderParser*).toList
    }
  }

  /**
   * Finds all WaterfallAdProvider records (active and inactive) for a Waterfall ID.
   * @param waterfallID ID of the Waterfall to which the WaterfallAdProvider records belong.
   * @return A list of OrderedWaterfallAdProvider instances if any exist.  Otherwise returns an empty list.
   */
  def findAllOrdered(waterfallID: Long, active: Boolean = false): List[OrderedWaterfallAdProvider] = {
    DB.withConnection { implicit connection =>
      val activeClause = if(active) " AND active = true " else ""
      val sqlStatement =
        """
          SELECT name, waterfall_ad_providers.id as id, cpm, active, waterfall_order, waterfall_ad_providers.configuration_data, waterfall_ad_providers.configurable, pending FROM ad_providers
          JOIN waterfall_ad_providers ON waterfall_ad_providers.ad_provider_id = ad_providers.id
          WHERE waterfall_id = {waterfall_id}
        """ + activeClause +
          """
          ORDER BY waterfall_order ASC;
          """
      val query = SQL(sqlStatement).on("waterfall_id" -> waterfallID)
      query.as(waterfallAdProviderOrderParser*).toList
    }
  }

  /**
   * Retrieves all active WaterfallAdProviders that have reporting enabled.
   * @return A list of WaterfallAdProviderRevenueData instances if any exist; otherwise, returns an empty list.
   */
  def findAllReportingEnabled: List[WaterfallAdProviderRevenueData] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT wap.id as id, ap.name, wap.configuration_data FROM ad_providers ap
          JOIN waterfall_ad_providers wap ON wap.ad_provider_id = ap.id
          WHERE wap.active = true AND wap.reporting_active = true;
        """
      )
      query.as(waterfallAdProviderRevenueDataParser*).toList
    }
  }

  /**
   * Retrieves configuration data for WaterfallAdProviders and AdProviders.
   * @param waterfallAdProviderID ID of the current WaterfallAdProvider.
   * @return Instance of WaterallAdProviderConfig class if records exist; otherwise, returns None.
   */
  def findConfigurationData(waterfallAdProviderID: Long): Option[WaterfallAdProviderConfig] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT name, cpm, ad_providers.configuration_data as ad_provider_configuration, ad_providers.callback_url_format, wap.configuration_data as wap_configuration, wap.reporting_active
          FROM waterfall_ad_providers wap
          JOIN ad_providers ON ad_providers.id = wap.ad_provider_id
          WHERE wap.id = {id};
        """
      ).on("id" -> waterfallAdProviderID)
      query.as(waterfallAdProviderConfigParser*) match {
        case List(waterfallAdProviderConfig) => Some(waterfallAdProviderConfig)
        case _ => None
      }
    }
  }

  /**
   * Find a WaterfallAdProvider record by AdProvider name and App token.
   * @param appToken The token for the App to which the WaterfallAdProvider belongs.
   * @param adProviderName The name of the AdProvider stored in the ad_providers table.
   * @return a WaterfallAdProvider instance if one exists; otherwise, returns None.
   */
  def findByAdProvider(appToken: String, adProviderName: String): Option[WaterfallAdProviderCallbackInfo] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
           SELECT wap.configuration_data, wap.cpm, vc.exchange_rate FROM waterfalls
           JOIN apps ON apps.id = waterfalls.app_id
           JOIN waterfall_ad_providers wap ON wap.waterfall_id = waterfalls.id
           JOIN ad_providers ON ad_providers.id = wap.ad_provider_id
           JOIN virtual_currencies vc ON vc.app_id = waterfalls.app_id
           WHERE apps.token={app_token} AND ad_providers.name={ad_provider_name};
        """
      ).on("app_token" -> appToken, "ad_provider_name" -> adProviderName)
      query.as(waterfallAdProviderCallbackInfoParser*) match {
        case List(waterfallAdProviderCallbackInfo) => Some(waterfallAdProviderCallbackInfo)
        case _ => None
      }
    }
  }

  /**
   * Checks if all fields in the configuration JSON are filled.
   * @param configurationData JSON from the configurationData attribute of the WaterfallAdProvider.
   * @param paramType the top level key to search for in the configurationData.
   * @return True if there is a blank field; False, otherwise.
   */
  def unconfigured(configurationData: JsValue, paramType: String = "requiredParams"): Boolean = {
    configurationData \ paramType match {
      case _: JsUndefined => return true
      case params: JsValue => {
        params.as[Map[String, JsValue]].map { param =>
          param._2 match {
            case paramValue: JsString if (paramValue == JsString("")) => return true
            case paramValue: JsArray if (paramValue == JsArray(Seq())) => return true
            case _ => None
          }
        }
      }
    }
    false
  }

  /**
   * Encapsulates information to be used in callbacks.
   * @param configurationData maps to the configuration_data field in the waterfall_ad_providers table.
   * @param cpm maps to the cpm field in the waterfall_ad_providers table.
   * @param exchangeRate maps to the exchange_rate field in the virtual_currencies table.
   */
  case class WaterfallAdProviderCallbackInfo(configurationData: JsValue, cpm: Option[Double], exchangeRate: Double)

  // Used to convert result of orderedByCPM SQL query.
  val waterfallAdProviderCallbackInfoParser: RowParser[WaterfallAdProviderCallbackInfo] = {
    get[JsValue]("configuration_data") ~
      get[Option[Double]]("cpm") ~
      get[Long]("exchange_rate") map {
      case configuration_data ~ cpm ~ exchange_rate => WaterfallAdProviderCallbackInfo(configuration_data, cpm, exchange_rate)
    }
  }

  // Used to convert result of orderedByCPM SQL query.
  val waterfallAdProviderOrderParser: RowParser[OrderedWaterfallAdProvider] = {
    get[String]("name") ~
      get[Long]("id") ~
      get[Option[Double]]("cpm") ~
      get[Boolean]("active") ~
      get[Option[Long]]("waterfall_order") ~
      get[JsValue]("configuration_data") ~
      get[Boolean]("configurable") ~
      get[Boolean]("pending") map {
      case name ~ id ~ cpm ~ active ~ waterfall_order ~ configuration_data ~ configurable ~ pending =>
        OrderedWaterfallAdProvider(name, id, cpm, active, waterfall_order, unconfigured(configuration_data.as[JsObject], "requiredParams"), false, configurable, pending)
    }
  }

  // Used to convert result of findConfigurationData SQL query.
  val waterfallAdProviderConfigParser: RowParser[WaterfallAdProviderConfig] = {
    get[String]("name") ~
      get[Option[Double]]("cpm") ~
      get[JsValue]("ad_provider_configuration") ~
      get[Option[String]]("callback_url_format") ~
      get[JsValue]("wap_configuration") ~
      get[Boolean]("reporting_active") map {
      case name ~ cpm ~ ad_provider_configuration ~ callback_url_format ~ wap_configuration ~ reporting_active => WaterfallAdProviderConfig(name, cpm, ad_provider_configuration, callback_url_format, wap_configuration, reporting_active)
    }
  }

  // Used to convert result of findAll SQL query.
  val waterfallAdProviderRevenueDataParser: RowParser[WaterfallAdProviderRevenueData] = {
    get[Long]("id") ~
      get[String]("name") ~
      get[JsValue]("configuration_data") map {
      case id ~ name ~ configuration_data => WaterfallAdProviderRevenueData(id, name, configuration_data)
    }
  }
}

/**
 * Encapsulates information from WaterfallAdProvider and AdProvider to be used in the RevenueDataActor background job.
 * @param waterfallAdProviderID Maps to the id field of the waterfall_ad_providers table.
 * @param name Maps to the name field of the ad_providers table.
 * @param configurationData Maps to the configuration_data field of the waterfall_ad_providers table.
 */
case class WaterfallAdProviderRevenueData(waterfallAdProviderID: Long, name: String, configurationData: JsValue)

/**
 * Encapsulates WaterfallAdProvider information used to determine the waterfall order.
 * @param name name field from ad_providers table
 * @param waterfallAdProviderID id field from waterfall_ad_providers table
 * @param cpm cpm field from waterfall_ad_providers table
 * @param waterfallOrder waterfall_order field from waterfall_ad_providers table
 * @param unconfigured Determines if all required params have been filled in.
 * @param newRecord Determines if there is a corresponding WaterfallAdProvider record.
 * @param configurable Determines if a DistributorUser can edit the cpm value for the WaterfallAdProvider.
 * @param pending Determines if the ad provider has been configured yet.  HyprMarketplace will remains in a pending state until we receive a response from Player with the distributor ID.
 */
case class OrderedWaterfallAdProvider(name: String, waterfallAdProviderID: Long, cpm: Option[Double], active: Boolean, waterfallOrder: Option[Long], unconfigured: Boolean, newRecord: Boolean = false, configurable: Boolean = true, pending: Boolean = false)

/**
 * Encapsulates data configuration information from a WaterfallAdProvider and corresponding AdProvider record.
 * @param name name field from ad_providers table.
 * @param cpm cpm field from waterfall_ad_providers table.
 * @param adProviderConfiguration Configuration data from AdProvider record.
 * @param callbackUrlFormat General format for an AdProvider's reward callback URL.
 * @param waterfallAdProviderConfiguration Configuration data form WaterfallAdProvider record.
 * @param reportingActive Boolean value indicating if we are collecting revenue data from third-parties.
 */
case class WaterfallAdProviderConfig(name: String, cpm: Option[Double], adProviderConfiguration: JsValue, callbackUrlFormat: Option[String], waterfallAdProviderConfiguration: JsValue, reportingActive: Boolean) {
  /**
   * Converts an optional String value to a Boolean.
   * @param param The original optional String value.
   * @return The String value converted to a Boolean if a String value is present; otherwise, returns false.
   */
  implicit def optionalStringToBoolean(param: Option[String]): Boolean = {
    param match {
      case Some(value) => value.toBoolean
      case None => false
    }
  }

  /**
   * Maps required info from AdProviders to actual values stored in WaterfallAdProviders.
   * @return List of tuples where the first element is the name of the required key and the second element is the value for that key if any exists.
   */
  def mappedFields(paramType: String): List[RequiredParam] = {
    val reqParams = (this.adProviderConfiguration \ paramType).as[List[Map[String, String]]].map(el =>
      new RequiredParam(el.get("displayKey"), el.get("key"), el.get("dataType"), el.get("description"), el.get("value"), el.get("refreshOnAppRestart"))
    )
    val waterfallAdProviderParams = this.waterfallAdProviderConfiguration \ paramType
    // A JsUndefined value (when a key is not found in the JSON object) will pattern match to JsValue.
    // For this reason, the JsUndefined case must come before JsValue to avoid a JSON error when converting a JsValue to any other type.
    reqParams.map( param =>
      (waterfallAdProviderParams \ param.key.get) match {
        case _: JsUndefined => new RequiredParam(param.displayKey, param.key, param.dataType, param.description, None, param.refreshOnAppRestart)
        case value: JsValue => {
          var paramValue: Option[String] = None
          if(param.dataType.get == "Array") {
            paramValue = Some(value.as[List[String]].mkString(","))
          } else {
            paramValue = Some(value.as[String])
          }
          new RequiredParam(param.displayKey, param.key, param.dataType, param.description, paramValue, param.refreshOnAppRestart)
        }
        case _ => new RequiredParam(param.displayKey, param.key, param.dataType, param.description, None, param.refreshOnAppRestart)
      }
    )
  }
}

/**
 * Encapsulates information for configuration_data fields in ad_providers and waterfall_ad_providers tables.
 * @param displayKey The name of the param displayed in the UI.
 * @param key Name of param required by Ad Provider.
 * @param dataType Data type of param.  Currently, only Strings and Arrays are supported.
 * @param description Description of the required param.  This is used in the UI to direct the distributor user on how to configure an ad provider.
 * @param value The actual value of the param.
 * @param refreshOnAppRestart Boolean value to indicate if changing the param only takes effect on app restart.
 */
case class RequiredParam(displayKey: Option[String], key: Option[String], dataType: Option[String], description: Option[String], value: Option[String], refreshOnAppRestart: Boolean)