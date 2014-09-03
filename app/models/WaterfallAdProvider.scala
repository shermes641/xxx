package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current

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
	id:Long, waterfallID:Long, adProviderID:Long, waterfallOrder: Option[Long], cpm: Option[Double], active: Option[Boolean], fillRate: Option[Float]
)

object WaterfallAdProvider {
  // Used to convert SQL row into an instance of the WaterfallAdProvider class.
  val waterfallAdProviderParser: RowParser[WaterfallAdProvider] = {
    get[Long]("waterfall_ad_providers.id") ~
    get[Long]("waterfall_id") ~
    get[Long]("ad_provider_id") ~
    get[Option[Long]]("waterfall_order") ~
    get[Option[Double]]("cpm") ~
    get[Option[Boolean]]("active") ~
    get[Option[Float]]("fill_rate") map {
      case id ~ waterfall_id ~ ad_provider_id ~ waterfall_order ~ cpm ~ active ~ fill_rate => WaterfallAdProvider(id, waterfall_id, ad_provider_id, waterfall_order, cpm, active, fill_rate)
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
          SET waterfall_order={waterfall_order}, cpm={cpm}, active={active}, fill_rate={fill_rate}
          WHERE id={id};
        """
      ).on(
          "waterfall_order" -> waterfallAdProvider.waterfallOrder, "cpm" -> waterfallAdProvider.cpm,
          "active" -> waterfallAdProvider.active, "fill_rate" -> waterfallAdProvider.fillRate, "id" -> waterfallAdProvider.id
        ).executeUpdate()
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
          val updatedValues = new WaterfallAdProvider(record.id, record.waterfallID, record.adProviderID, Some(index + 1), record.cpm, record.active, record.fillRate)
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
  def create(waterfallID: Long, adProviderID: Long): Option[Long] = {
    DB.withConnection { implicit connection =>
      try {
        SQL(
          """
          INSERT INTO waterfall_ad_providers (waterfall_id, ad_provider_id)
          VALUES ({waterfall_id}, {ad_provider_id});
          """
        ).on("waterfall_id" -> waterfallID, "ad_provider_id" -> adProviderID).executeInsert()
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
   * Finds all WaterfallAdProvider records sorted by waterfall_order for a particular waterfallID.
   * @param waterfallID ID of current Waterfall
   * @return List of OrderedWaterfallAdProvider instances by waterfall_order if any exist.  Otherwise, returns an empty list.
   */
  def currentOrder(waterfallID: Long): List[OrderedWaterfallAdProvider] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
          SELECT name, waterfall_ad_providers.id as id, cpm, waterfall_order FROM ad_providers
          JOIN waterfall_ad_providers ON waterfall_ad_providers.ad_provider_id = ad_providers.id
          WHERE active = true AND waterfall_id = {waterfall_id}
          ORDER BY waterfall_order ASC;
        """
      ).on("waterfall_id" -> waterfallID)
      query.as(waterfallAdProviderOrderParser*).toList
    }
  }

  // Used to convert result of orderedByCPM SQL query.
  val waterfallAdProviderOrderParser: RowParser[OrderedWaterfallAdProvider] = {
    get[String]("name") ~
    get[Long]("id") ~
    get[Option[Double]]("cpm") ~
    get[Option[Long]]("waterfall_order") map {
      case name ~ id ~ cpm ~ waterfall_order => OrderedWaterfallAdProvider(name, id, cpm, waterfall_order)
    }
  }
}

/**
 * Encapsulates WaterfallAdProvider information used to determine the waterfall order.
 * @param name name field from ad_providers table
 * @param waterfallAdProviderID id field from waterfall_ad_providers table
 * @param cpm cpm field from waterfall_ad_providers table
 * @param waterfallOrder waterfall_order field from waterfall_ad_providers table
 */
case class OrderedWaterfallAdProvider(name: String, waterfallAdProviderID: Long, cpm: Option[Double], waterfallOrder: Option[Long])
