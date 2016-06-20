package controllers

import models._
import play.api.mvc._

/**
  * Display the Configuration variables if this is a review application
  */
object ConfigVarsController extends Controller with ConfigVars {
  /**
    * Review apps change configuration vars from what you would see on on the Heroku dashboard.
    * Therefore review apps are allowed to dump the configuration vars
    *
    * @return response with config variables
    */
  def serveConfigVars = Action { implicit request =>
    if (Environment.isReviewApp)
      Ok(
        s"""${ConfigVarsApp.toString}
           |${ConfigVarsHeroku.toString}
           |${ConfigVarsKeen.toString}
           |${ConfigVarsCallbackUrls.toString}
           |${ConfigVarsReporting.toString}
           |${ConfigVarsJunGroup.toString}
           |${ConfigVarsHmac.toString}
         """.stripMargin)
    else
      Ok(s"${ConfigVarsApp.appName} is not a review app")
  }
}
