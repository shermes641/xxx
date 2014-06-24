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
    		Ok(Json.obj(
    				"waterfall" -> Json.toJson(waterfall),
    				"waterfall_ad_providers" -> Json.arr(list.map { i => Json.toJson(i)})
    			)
    		)
    	}
    }
  }

}

