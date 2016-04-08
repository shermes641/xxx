package controllers

import javax.inject.Inject
import models._
import play.api.mvc._

/**
  * Display the Configuration variables if this is a review application
  * @param configVars     Shared ENV configuration variables
  * @param appEnvironment THe environment in which the app is running
  */
class ConfigVarsController @Inject() (configVars: ConfigVars, appEnvironment: Environment) extends Controller {
  /**
    * Review apps change configuration vars from what you would see on on the Heroku dashboard.
    * Therefore review apps are allowed to dump the configuration vars
    *
    * @return response with config variables
    */
  def serveConfigVars = Action { implicit request =>
    if (appEnvironment.isReviewApp)
      Ok(
        s"""${configVars.ConfigVarsApp.toString}
           |${configVars.ConfigVarsHeroku.toString}
           |${configVars.ConfigVarsKeen.toString}
           |${configVars.ConfigVarsCallbackUrls.toString}
           |${configVars.ConfigVarsReporting.toString}
           |${configVars.ConfigVarsJunGroup.toString}
           |${configVars.ConfigVarsHmac.toString}
         """.stripMargin)
    else
      Ok(s"${configVars.ConfigVarsApp.appName} is not a review app")
  }
}
