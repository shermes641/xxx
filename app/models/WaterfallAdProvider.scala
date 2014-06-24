package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.libs.json._
import play.api.Play.current

case class WaterfallAdProvider (
	id:Pk[Long], waterfall_id:Long, ad_provider_id:Long, waterfall_order: Option[Long], cpm: Option[Float], active: Option[Boolean], fill_rate: Option[Float]
)

object WaterfallAdProvider {

	implicit object WaterfallAdProviderFormatter extends Format[WaterfallAdProvider] {
 
        // convert from Waterfall object to JSON (serializing to JSON)
        def writes(wap: WaterfallAdProvider): JsValue = {
			Json.obj(
				"id" -> JsNumber(wap.id.get),
				"waterfall_id" -> JsNumber(wap.waterfall_id),
				"ad_provider_id" -> JsNumber(wap.ad_provider_id),
				"waterfall_order" -> (wap.waterfall_order match {
					case None => JsNull
					case _ => JsNumber(wap.waterfall_order.get)
				}),
				"cpm" -> (wap.cpm match {
					case None => JsNull
					case _ => JsNumber(wap.cpm.get)
				}),
				"active" -> (wap.active match {
					case None => JsNull
					case _ => JsBoolean(wap.active.get)
				}),
				"fill_rate" -> (wap.fill_rate match {
					case None => JsNull
					case _ => JsNumber(wap.fill_rate.get)
				})
	        )
        }
 
 		// Convert to Watferfall object form JSON (serializing from JSON)
 		// TODO: Currently mocked out.
        def reads(json: JsValue): JsResult[WaterfallAdProvider] = {
            JsError()
        }
 
    }

	val waterfallAdProviderParser: RowParser[WaterfallAdProvider] = {
		get[Pk[Long]]("WaterfallAdProvider.id") ~
		get[Long]("waterfall_id") ~
		get[Long]("ad_provider_id") ~
		get[Option[Long]]("waterfall_order") ~
		get[Option[Float]]("cpm") ~
		get[Option[Boolean]]("active") ~
		get[Option[Float]]("fill_rate") map {
			case id ~ waterfall_id ~ ad_provider_id ~ waterfall_order ~ cpm ~ active ~ fill_rate => WaterfallAdProvider(id, waterfall_id, ad_provider_id, waterfall_order, cpm, active, fill_rate)
		}
	}

	

    
}