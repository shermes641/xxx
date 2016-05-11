package controllers

import play.api.mvc._

object LoaderIO extends Controller {
  def serveLoader = Action { implicit request =>
    Ok("loaderio-ed9370e06b0ea24844e632f00839662e")
  }
}
