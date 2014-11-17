package models

import anorm._
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current

object WaterfallGeneration {
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
}
