package controllers

import models._
import play.api.mvc._

/**
  * Display the Configuration variables if this is a review application
  */
object ConfigVarsController extends Controller with ConfigVars {
  /**
    * Renders form to create a new DistributorUser.
    *
    * @return Form for sign up
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
