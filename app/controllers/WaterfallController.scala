package controllers

import play.api._
import play.api.mvc._
import play.api.Play.current
import anorm._
import play.api.db.DB
import models._
import play.api.libs.json._

object WaterfallController extends Controller {

  def show(id: Long) = Action {
  	DB.withConnection { implicit c =>
  		val result = Waterfall.withWaterfallAdProviders(id)

    	if (result.size == 0) {
    		NotFound("No such waterfall")
    	} else {
    		val (waterfall, list) = result.head
    		val res: JsValue = Json.toJson(waterfall).as[JsObject] ++ Json.obj("waterfall_ad_providers" -> list)
    		Ok(res)
    	}
    }
  }

  def list(property_id: Long) = Action {
  	DB.withConnection { implicit connection =>
  		val query = SQL("SELECT * FROM Waterfall where property_id = {property_id}").on("property_id" -> property_id)
  		val waterfalls = query.as(Waterfall.waterfallParser*)

  		Ok(Json.toJson(waterfalls))
  	}
  }

}

