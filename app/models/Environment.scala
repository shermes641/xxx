package models

import javax.inject.Inject
import play.api.Configuration

//TODO due to using before application start, during testing we can not use "extends ConfigVars" here
class Environment @Inject() (environmentConfig: Configuration, appMode: AppMode) {

  // used for testing Global.scala
  var TestErrorCode = 0

  /**
    * Checks if the Play app is running in production mode (not necessarily in a real production environment).
    *
    * @return True if the app is running in production mode; otherwise, False.
    */
  def isInProdMode: Boolean = play.api.Mode.Prod == appMode.contextMode

  /**
    * Reads STAGING ENV variable.
    *
    * @return If found, a String containing 'true' or 'false'; otherwise, None.
    */
  def stagingEnvSetting: Option[String] = {
    // Can not use ConfigVars.staging due to testing restrictions
    val staging = environmentConfig.getString(Constants.AppConfig.Staging).getOrElse("")
    if (staging != "") Some(staging) else None
  }

  /**
    * Checks if the app is running in production mode and our STAGING ENV var is set to 'true.'
    *
    * @return True if the app is running in production mode and STAGING is set to 'true'; otherwise, False.
    */
  def isStaging: Boolean = {
    stagingEnvSetting match {
      case Some(setting) => isInProdMode && setting == "true"
      case None => false
    }
  }

  /**
    * Checks if the app is running in production mode and our STAGING ENV var is set to 'false.'
    *
    * @return True if the app is running in production mode and STAGING is set to 'false'; otherwise, False.
    */
  def isProd: Boolean = {
    stagingEnvSetting match {
      case Some(setting) => isInProdMode && setting == "false"
      case None => false
    }
  }

  /**
    * Checks if the app is running in either the staging or production environment.
    *
    * @return True if the app is running in staging or production; otherwise, False.
    */
  def isProdOrStaging: Boolean = isProd || isStaging

  /**
    * Checks if the app is running in test mode.
    *
    * @return True if the app is in test mode; otherwise, False.
    */
  def isTest: Boolean = play.api.Mode.Test == appMode.contextMode

  /**
    * Checks if the app is running in development mode.
    *
    * @return True if the app is running in development mode; otherwise, False.
    */
  def isDev: Boolean = play.api.Mode.Dev == appMode.contextMode

  /**
    * Checks current environment and returns the type as a String using the Rails environment naming convention.
    *
    * @return A String indicating the environment type.
    */
  def mode: String = {
    appMode.contextMode match {
      case play.api.Mode.Prod if isStaging => "staging"
      case play.api.Mode.Prod if isProd => "production"
      case play.api.Mode.Test => "test"
      case _ => "development"
    }
  }

  /**
    * Checks if we are a review app
    *
    * @return true if this is a review app
    */
  def isReviewApp: Boolean = {
    val ParentName  = environmentConfig.getString(Constants.HerokuConfigVars.ParentName).getOrElse("")
    val AppName     = environmentConfig.getString(Constants.HerokuConfigVars.AppName).get
    ParentName != "" &&
      ParentName.length < AppName.length &&
      AppName.contains("-pr-")
  }
}
