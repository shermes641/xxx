package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current
import play.api.libs.json._
import scala.language.postfixOps

/**
 * Encapsulates Waterfall configuration data along with generation number to be used in APIController.
 * @param generationNumber A number indicating how many times the Waterfall has been edited.
 * @param configuration The JSON API response expected by the SDK.
 */
case class WaterfallGeneration(generationNumber: Long, configuration: JsValue)

object WaterfallGeneration extends JsonConversion {
  /**
   * Creates a new WaterfallGeneration record in the waterfall_generations table.
   * @param waterfallID The ID of the Waterfall to which the WaterfallGeneration belongs.
   * @param waterfallToken The token of the Waterfall to which the WaterfallGeneration belongs.
   * @return 1 if the insert is successful; otherwise, 0;
   */
  def create(waterfallID: Long, waterfallToken: String): Option[Long] = {
    val configuration = Waterfall.responseV1(waterfallToken)
    DB.withConnection { implicit connection =>
      SQL(
        """
          INSERT INTO waterfall_generations (generation_number, waterfall_id, waterfall_token, configuration)
          VALUES ((SELECT COALESCE(MAX(generation_number), 0) + 1 FROM waterfall_generations WHERE waterfall_id={waterfall_id}),
          {waterfall_id}, {waterfall_token}, CAST({configuration} AS json));
        """
      ).on("waterfall_id" -> waterfallID, "waterfall_token" -> waterfallToken, "configuration" -> Json.stringify(configuration)).executeInsert()
    }
  }

  /**
   * Creates a WaterfallGeneration using on the Waterfall ID.
   * @param waterfallID The ID of the Waterfall to which the WaterfallGeneration belongs.
   * @return If successful, ID of the WaterfallGeneration; otherwise, None.
   */
  def createWithWaterfallID(waterfallID: Long): Option[Long] = {
    Waterfall.find(waterfallID) match {
      case Some(waterfall) => create(waterfallID, waterfall.token)
      case None => None
    }
  }

  /**
   * Retrieves the latest configuration JSON for a given Waterfall.
   * @param waterfallToken The identifier used to find a particular Waterfall response.
   * @return An instance of the WaterfallGeneration class if one is found; otherwise, None.
   */
  def findLatest(waterfallToken: String): Option[WaterfallGeneration] = {
    DB.withConnection { implicit connection =>
      val query = SQL(
        """
           SELECT generation_number, configuration FROM waterfall_generations
           WHERE waterfall_token={waterfall_token}
           ORDER BY generation_number DESC LIMIT 1
        """).on("waterfall_token" -> waterfallToken)
      query.as(waterfallGenerationParser*) match {
        case List(waterfallGeneration) => Some(waterfallGeneration)
        case _ => None
      }
    }
  }

  // Used to convert SQL row into an instance of the WaterfallGeneration class.
  val waterfallGenerationParser: RowParser[WaterfallGeneration] = {
    get[Long]("generation_number") ~
    get[JsValue]("configuration") map {
      case generation_number ~ configuration => WaterfallGeneration(generation_number, configuration)
    }
  }
}
