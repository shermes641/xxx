package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current
import controllers.ConfigInfo

case class Waterfall(id: Long, name: String)

object Waterfall {
  // Used to convert SQL row into an instance of the Waterfall class.
  val waterfallParser: RowParser[Waterfall] = {
    get[Long]("waterfalls.id") ~
    get[String]("name") map {
      case id ~ name => Waterfall(id, name)
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
          INSERT INTO waterfalls (app_id, name)
          VALUES ({app_id}, {name});
        """
      ).on("app_id" -> appID, "name" -> name).executeInsert()
    }
  }

  /**
   * Updates the fields for a particular record in waterfalls table.
   * @param waterfall Waterfall instance with updated attributes
   * @return Number of rows updated
   */
  def update(waterfall: Waterfall): Int = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          UPDATE waterfalls
          SET name={name}
          WHERE id={id};
        """
      ).on("name" -> waterfall.name, "id" -> waterfall.id).executeUpdate()
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
        WaterfallAdProvider.create(waterfallID, adProviderConfig.id, Some(adProviderConfig.waterfallOrder))
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
}
