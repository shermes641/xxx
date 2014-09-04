package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current
import play.api.libs.json._

/**
 * Encapsulates information for third-party SDKs to be mediated.
 * @param id id field in the ad_providers table
 * @param name name field in the ad_providers table
 */
case class AdProvider(id: Long, name: String, configurationData: JsValue)

object AdProvider extends JsonConversion {
  // Used to convert SQL query result into instances of the AdProvider class.
  val adProviderParser: RowParser[AdProvider] = {
      get[Long]("id") ~
      get[String]("name") ~
      get[JsValue]("configuration_data") map {
      case id ~ name ~ configuration_data => AdProvider(id, name, configuration_data)
    }
  }

  /**
   * Finds all AdProvider records.
   * @return A list of all AdProvider records from the database if records exist. Otherwise, returns an empty list.
   */
  def findAll: List[AdProvider] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT ad_providers.*
          FROM ad_providers;
        """
      )
      query.as(adProviderParser*).toList
    }
  }

  /**
   * Retrieves a list of AdProviders who do not currently have an existing WaterfallAdProvider record for a given Waterfall ID.
   * @param waterfallID The Waterfall ID to which all integrated WaterfallAdProviders belong.
   * @return A list of AdProvider instances if any exist.  Otherwise, returns an empty list.
   */
  def findNonIntegrated(waterfallID: Long): List[AdProvider] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT name, id, configuration_data
          FROM ad_providers
          WHERE id NOT IN
          (SELECT DISTINCT ad_provider_id
           FROM waterfall_ad_providers wap
           WHERE wap.waterfall_id={waterfall_id})
        """
      ).on("waterfall_id" -> waterfallID)
      query.as(adProviderParser*).toList
    }
  }

  /**
   * Creates a new record in the AdProvider table
   * @param name Maps to name column in AdProvider table
   * @param configurationData Json configuration data for AdProvider
   * @return ID of newly created record
   */
  def create(name: String, configurationData: String): Option[Long] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          INSERT INTO apps (name, configuration_data)
          VALUES ({name}, {configurationData});
        """
      ).executeInsert()
    }
  }
}
