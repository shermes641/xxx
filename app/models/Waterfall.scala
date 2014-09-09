package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current
import controllers.ConfigInfo
import play.api.libs.json.{JsValue}

case class Waterfall(id: Long, name: String, token: String)

object Waterfall extends JsonConversion {
  // Used to convert SQL row into an instance of the Waterfall class.
  val waterfallParser: RowParser[Waterfall] = {
    get[Long]("waterfalls.id") ~
    get[String]("name") ~
    get[String]("token") map {
      case id ~ name  ~ token => Waterfall(id, name, token)
    }
  }

  /**
   * Creates a new Waterfall record in the database.
   * @param appID ID of the App to which the new Waterfall belongs
   * @param name Name of the new Waterfall
   * @return ID of new record if insert is successful, otherwise None.
   */
  def create(appID: Long, name: String): Option[Long] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          INSERT INTO waterfalls (app_id, name, token)
          VALUES ({app_id}, {name}, {token});
        """
      ).on("app_id" -> appID, "name" -> name, "token" -> generateToken).executeInsert()
    }
  }

  /**
   * Updates the fields for a particular record in waterfalls table.
   * @param id ID field of the waterfall to be updated.
   * @param name name field of the waterfall to be updated.
   * @return Number of rows updated
   */
  def update(id: Long, name: String): Int = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          UPDATE waterfalls
          SET name={name}
          WHERE id={id};
        """
      ).on("name" -> name, "id" -> id).executeUpdate()
    }
  }

  /**
   * Finds a record in the Waterfall table by ID.
   * @param waterfallID ID of current Waterfall
   * @return Waterfall instance
   */
  def find(waterfallID: Long): Option[Waterfall] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT waterfalls.*
          FROM waterfalls
          WHERE id={id};
        """
      ).on("id" -> waterfallID)
      query.as(waterfallParser*) match {
        case List(waterfall) => Some(waterfall)
        case _ => None
      }
    }
  }

  /**
   * Finds a record in the Waterfall table by ID.
   * @param appID ID of current App
   * @return List of Waterfall instances if query is successful.  Otherwise, returns an empty list.
   */
  def findByAppID(appID: Long): List[Waterfall] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT waterfalls.*
          FROM waterfalls
          WHERE app_id={app_id};
        """
      ).on("app_id" -> appID)
      query.as(waterfallParser*).toList
    }
  }

  /**
   * Retrieves the order of ad providers with their respective configuration data.
   * @param token API token used to authenticate a request and find a particular waterfall.
   * @return A list containing instances of AdProviderInfo if any active WaterfallAdProviders exist.
   */
  def order(token: String): List[AdProviderInfo] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT ap.name, wap.configuration_data
          FROM waterfalls w
          JOIN waterfall_ad_providers wap on wap.waterfall_id = w.id
          JOIN ad_providers ap on ap.id = wap.ad_provider_id
          WHERE w.token={id} AND wap.active = true
          ORDER BY wap.waterfall_order ASC
        """
      ).on("id" -> token)
      query.as(adProviderParser*).toList
    }
  }

  // Used to convert SQL row into an instance of the AdProviderInfo class in Waterfall.order.
  val adProviderParser: RowParser[AdProviderInfo] = {
    get[String]("name") ~
    get[JsValue]("configuration_data") map {
      case name ~ configuration_data => AdProviderInfo(name, configuration_data)
    }
  }

  /**
   * Encapsulates necessary information returned from SQL query in Waterfall.order.
   * @param providerName Maps to the name field in the ad_providers table.
   * @param configurationData Maps to the configuration_data field in the waterfall_ad_providers table.
   */
  case class AdProviderInfo(providerName: String, configurationData: JsValue)

  /**
   * Updates WaterfallAdProvider records according to the configuration in the Waterfall edit view.
   * @param waterfallID ID of the Waterfall to which all WaterfallAdProviders belong.
   * @param adProviderConfigList List of attributes to update for each WaterfallAdProvider.
   * @return True if the update is successful; otherwise, false.
   */
  def reconfigureAdProviders(waterfallID: Long, adProviderConfigList: List[ConfigInfo]): Boolean = {
    var successful = true
    adProviderConfigList.map { adProviderConfig =>
      if(adProviderConfig.active && adProviderConfig.newRecord) {
        // If a Distributor wants to add a new AdProvider to the current waterfall, create a new WaterfallAdProvider record.
        WaterfallAdProvider.create(waterfallID, adProviderConfig.id, Some(adProviderConfig.waterfallOrder)) match {
          case Some(id) => {}
          case None => successful = false
        }
      } else if(!adProviderConfig.newRecord) {
        //  Otherwise, find and update the existing WaterfallAdProvider record.
        WaterfallAdProvider.find(adProviderConfig.id) match {
          case Some(record) => {
            val newOrder = if(adProviderConfig.active) Some(adProviderConfig.waterfallOrder) else None
            val updatedValues = new WaterfallAdProvider(record.id, record.waterfallID, record.adProviderID, newOrder, record.cpm, Some(adProviderConfig.active), record.fillRate, record.configurationData)
            WaterfallAdProvider.update(updatedValues)
          }
          case _ => {
            successful = false
          }
        }
      }
    }
    successful
  }

  /**
   * Generates token field for waterfall.  This is called once on waterfall creation.
   * @return Random string to be saved as token.
   */
  def generateToken = {
    java.util.UUID.randomUUID.toString
  }
}
