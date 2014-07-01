package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.libs.json._
import play.api.Play.current

case class Waterfall (id: Pk[Long] = NotAssigned, name:String)

object Waterfall {

	implicit object WaterfallFormat extends Format[Waterfall] {
 
        // convert from Waterfall object to JSON (serializing to JSON)
        def writes(waterfall: Waterfall): JsValue = {
        	waterfall.id match {
        		case NotAssigned => {
        			Json.obj(
		            	"name" -> JsString(waterfall.name)
		            )
        		}
        		case _ => {
        			Json.obj(
    					"id" -> JsNumber(waterfall.id.get),
		            	"name" -> JsString(waterfall.name)
		            )
        		}

        	}
        }
 
 		// Convert to Waterfall object form JSON (serializing from JSON)
 		// TODO: Currently mocked out.
        def reads(json: JsValue): JsResult[Waterfall] = {
            JsError()
        }
 
    }

    val waterfallParser: RowParser[Waterfall] = {
    	get[Pk[Long]]("Waterfall.id") ~
      get[String]("name") map {
        case id ~ name => Waterfall(id, name)
      }
    }

	def withWaterfallAdProviders(id: Long): Map[Waterfall, List[models.WaterfallAdProvider]] = {
		DB.withConnection { implicit c =>
			val query = SQL(
				"""
					SELECT Waterfall.*, WaterfallAdProvider.*
					FROM Waterfall JOIN WaterfallAdProvider on WaterfallAdProvider.waterfall_id = Waterfall.id
					WHERE Waterfall.id = {id}
				"""
				).on("id" -> id)
			query.as(waterfallParser ~ WaterfallAdProvider.waterfallAdProviderParser *).groupBy(_._1).mapValues(_.map(_._2))
			
		}
	}

}
