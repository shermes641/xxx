package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current
import play.api.libs.json._
import scala.language.postfixOps

/**
 * Encapsulates information for a record in the waterfall_ad_providers table.
 * @param id id field in the waterfall_ad_providers table
 * @param waterfallID ID of the waterfall to which this waterfall_ad_provider belongs
 * @param adProviderID ID of the ad provider record to which this waterfall_ad_provider belongs
 * @param waterfallOrder position in the waterfall
 * @param cpm cost per thousand impressions
 * @param active determines if the waterfall_ad_provider should be included in a waterfall
 * @param fillRate the ratio of ads shown to inventory checks
 */
case class WaterfallAdProvider (
	id:Long, waterfallID:Long, adProviderID:Long, waterfallOrder: Option[Long], cpm: Option[Double], active: Option[Boolean], fillRate: Option[Float], configurationData: JsValue, reportingActive: Boolean
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
    get[Boolean]("reporting_active") map {
      case id ~ waterfall_id ~ ad_provider_id ~ waterfall_order ~ cpm ~ active ~ fill_rate ~ configuration_data ~ reporting_active => WaterfallAdProvider(id, waterfall_id, ad_provider_id, waterfall_order, cpm, active, fill_rate, configuration_data, reporting_active)
    }
  }

  /**
   * Updates the fields for a particular record in waterfall_ad_providers table.
   * @param waterfallAdProvider WaterfallAdProvider instance with updated attributes
   * @return Number of rows updated
   */
  def update(waterfallAdProvider: WaterfallAdProvider): Int = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          UPDATE waterfall_ad_providers
          SET waterfall_order={waterfall_order}, cpm={cpm}, active={active}, fill_rate={fill_rate},
          configuration_data=CAST({configuration_data} AS json), reporting_active={reporting_active}
          WHERE id={id};
        """
      ).on(
          "waterfall_order" -> waterfallAdProvider.waterfallOrder, "cpm" -> waterfallAdProvider.cpm,
          "active" -> waterfallAdProvider.active, "fill_rate" -> waterfallAdProvider.fillRate,
          "configuration_data" -> Json.stringify(waterfallAdProvider.configurationData),
          "reporting_active" -> waterfallAdProvider.reportingActive, "id" -> waterfallAdProvider.id
        ).executeUpdate()
    }
  }

  /**
   * Updates the eCPM field for a given WaterfallAdProvider ID.
   * @param id The ID of the WaterfallAdProvider to be updated.
   * @param eCPM The new eCPM value calculated in RevenueDataActor.
   * @return 1 if the update is successful; otherwise, 0.
   */
  def updateEcpm(id: Long, eCPM: Double): Int = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          UPDATE waterfall_ad_providers
          SET cpm={cpm}
          WHERE id={id};
        """
      ).on("cpm" -> eCPM, "id" -> id).executeUpdate()
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
          val updatedValues = new WaterfallAdProvider(record.id, record.waterfallID, record.adProviderID, Some(index + 1), record.cpm, record.active, record.fillRate, record.configurationData, record.reportingActive)
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
   * Creates a new WaterfallAdProvider record in the database unless a similar record exists.
   * @param waterfallID ID of the Waterfall to which the new WaterfallAdProvider belongs
   * @param adProviderID ID of the Ad Provider to which the new WaterfallAdProvider belongs
   * @return ID of new record if insert is successful, otherwise None.
   */
  def create(waterfallID: Long, adProviderID: Long, waterfallOrder: Option[Long] = None): Option[Long] = {
    DB.withConnection { implicit connection =>
      try{
        SQL(
          """
          INSERT INTO waterfall_ad_providers (waterfall_id, ad_provider_id, waterfall_order)
          VALUES ({waterfall_id}, {ad_provider_id}, {waterfall_order});
          """
        ).on("waterfall_id" -> waterfallID, "ad_provider_id" -> adProviderID, "waterfall_order" -> waterfallOrder).executeInsert()
      } catch {
        case exception: org.postgresql.util.PSQLException => {
          None
        }
      }
    }
  }

  /**
   * Finds waterfall_ad_provider record by ID.
   * @param waterfallAdProviderID ID of WaterfallAdProvider
   * @return WaterfallAdProvider instance if one exists. Otherwise, returns None.
   */
  def find(waterfallAdProviderID: Long): Option[WaterfallAdProvider] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT waterfall_ad_providers.*
          FROM waterfall_ad_providers
          WHERE id = {id};
        """
      ).on("id" -> waterfallAdProviderID)
      query.as(waterfallAdProviderParser*) match {
        case List(waterfallAdProvider) => Some(waterfallAdProvider)
        case _ => None
      }
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
          SELECT name, waterfall_ad_providers.id as id, cpm, active, waterfall_order FROM ad_providers
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
          SELECT name, ad_providers.configuration_data as ad_provider_configuration, wap.configuration_data as wap_configuration, wap.reporting_active
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

  // Used to convert result of orderedByCPM SQL query.
  val waterfallAdProviderOrderParser: RowParser[OrderedWaterfallAdProvider] = {
    get[String]("name") ~
    get[Long]("id") ~
    get[Option[Double]]("cpm") ~
    get[Boolean]("active") ~
    get[Option[Long]]("waterfall_order") map {
      case name ~ id ~ cpm ~ active ~ waterfall_order => OrderedWaterfallAdProvider(name, id, cpm, active, waterfall_order)
    }
  }

  // Used to convert result of findConfigurationData SQL query.
  val waterfallAdProviderConfigParser: RowParser[WaterfallAdProviderConfig] = {
    get[String]("name") ~
    get[JsValue]("ad_provider_configuration") ~
    get[JsValue]("wap_configuration") ~
    get[Boolean]("reporting_active") map {
      case name ~ ad_provider_configuration ~ wap_configuration ~ reporting_active => WaterfallAdProviderConfig(name, ad_provider_configuration, wap_configuration, reporting_active)
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
 */
case class OrderedWaterfallAdProvider(name: String, waterfallAdProviderID: Long, cpm: Option[Double], active: Boolean, waterfallOrder: Option[Long], newRecord: Boolean = false)

/**
 * Encapsulates data configuration information from a WaterfallAdProvider and corresponding AdProvider record.
 * @param name name field from ad_providers table.
 * @param adProviderConfiguration Configuration data from AdProvider record.
 * @param waterfallAdProviderConfiguration Configuration data form WaterfallAdProvider record.
 * @param reportingActive Boolean value indicating if we are collecting revenue data from third-parties.
 */
case class WaterfallAdProviderConfig(name: String, adProviderConfiguration: JsValue, waterfallAdProviderConfiguration: JsValue, reportingActive: Boolean) {
  /**
   * Maps required info from AdProviders to actual values stored in WaterfallAdProviders.
   * @return List of tuples where the first element is the name of the required key and the second element is the value for that key if any exists.
   */
  def mappedFields(paramType: String): List[RequiredParam] = {
    val reqParams = (this.adProviderConfiguration \ paramType).as[List[Map[String, String]]].map(el =>
      new RequiredParam(el.get("key"), el.get("dataType"), el.get("description"), el.get("value"))
    )
    val waterfallAdProviderParams = this.waterfallAdProviderConfiguration \ paramType
    // A JsUndefined value (when a key is not found in the JSON object) will pattern match to JsValue.
    // For this reason, the JsUndefined case must come before JsValue to avoid a JSON error when converting a JsValue to any other type.
    reqParams.map( param =>
      (waterfallAdProviderParams \ param.key.get) match {
        case _: JsUndefined => new RequiredParam(param.key, param.dataType, param.description, None)
        case value: JsValue => {
          var paramValue: Option[String] = None
          if(param.dataType.get == "Array") {
            paramValue = Some(value.as[List[String]].mkString(","))
          } else {
            paramValue = Some(value.as[String])
          }
          new RequiredParam(param.key, param.dataType, param.description, paramValue)
        }
        case _ => new RequiredParam(param.key, param.dataType, param.description, None)
      }
    )
  }
}

/**
 * Encapsulates information for configuration_data fields in ad_providers and waterfall_ad_providers tables.
 * @param key Name of param required by Ad Provider.
 * @param dataType Data type of param.  Currently, only Strings and Arrays are supported.
 * @param description Description of the required param.  This is used in the UI to direct the distributor user on how to configure an ad provider.
 * @param value The actual value of the param.
 */
case class RequiredParam(key: Option[String], dataType: Option[String], description: Option[String], value: Option[String] = None)
