package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current

/**
 * Encapsulates information for third-party SDKs to be mediated.
 * @param id id field in the ad_providers table
 * @param name name field in the ad_providers table
 */
case class AdProvider(id: Long, name: String)

object AdProvider {
  // Used to convert SQL query result into instances of the AdProvider class.
  val adProviderParser: RowParser[AdProvider] = {
      get[Long]("id") ~
      get[String]("name") map {
      case id ~ name => AdProvider(id, name)
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
}
