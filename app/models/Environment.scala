package models

import play.api.Play

//TODO due to using before application start, during testing we can not use "extends ConfigVars" here
object Environment {
  /**
    * Checks if the Play app is running in production mode (not necessarily in a real production environment).
    *
    * @return True if the app is running in production mode; otherwise, False.
    */
  def isInProdMode: Boolean = play.api.Play.isProd(play.api.Play.current)

  /**
    * Reads STAGING ENV variable.
    *
    * @return If found, a String containing 'true' or 'false'; otherwise, None.
    */
  def stagingEnvSetting: Option[String] = {
    // Can not use ConfigVars.staging due to testing restrictions
    val staging = Play.current.configuration.getString(Constants.AppConfig.Staging).getOrElse("")
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
  def isTest: Boolean = play.api.Play.isTest(play.api.Play.current)

  /**
    * Checks if the app is running in development mode.
    *
    * @return True if the app is running in development mode; otherwise, False.
    */
  def isDev: Boolean = play.api.Play.isDev(play.api.Play.current)

  /**
    * Checks current environment and returns the type as a String using the Rails environment naming convention.
    *
    * @return A String indicating the environment type.
    */
  def mode: String = {
    play.api.Play.mode(play.api.Play.current).toString match {
      case "Prod" if isStaging => "staging"
      case "Prod" if isProd => "production"
      case "Test" => "test"
      case _ => "development"
    }
  }

  /**
    * Checks if we are a review app
    *
    * @return true if this is a review app
    */
  def isReviewApp: Boolean = {
    val ParentName  = Play.current.configuration.getString(Constants.HerokuConfigVars.ParentName).getOrElse("")
    val AppName     = Play.current.configuration.getString(Constants.HerokuConfigVars.AppName).get
    ParentName != "" &&
      ParentName.length < AppName.length &&
      AppName.contains("_pr_")
  }
}
