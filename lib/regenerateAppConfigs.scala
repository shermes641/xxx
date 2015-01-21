import models.AppConfig
import anorm._
import play.api.Play.current
import play.api.db.DB

new play.core.StaticApplication(new java.io.File("."))

/**
 * Script to update AppConfigs for all Waterfalls.
 */
def updateAllAppConfigs: Unit = {
  var unsuccessfulIDs: List[Long] = List()
  DB.withTransaction { implicit connection =>
    val waterfallIDs: List[Long] = SQL(
      """
          SELECT id from waterfalls;
      """
    )().map(row => row[Long]("id")).toList
    waterfallIDs.map { waterfallID =>
      AppConfig.createWithWaterfallIDInTransaction(waterfallID, None) match {
        case Some(newGenerationNumber) => None
        case None => {
          unsuccessfulIDs = unsuccessfulIDs :+ waterfallID
          println("AppConfig not updated for Waterfall ID: " + waterfallID)
        }
      }
    }
  }
  unsuccessfulIDs match {
    case list: List[Long] if(list.size == 0)  => {
      println("All AppConfigs successfully updated!")
    }
    case list: List[Long] => {
      println("AppConfigs for the following Waterfall IDs were not updated successfully: [" + list.mkString(", ") + "]")
    }
  }
}

updateAllAppConfigs