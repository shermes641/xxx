package models

import play.api.Play

object Environment {
  /**
   * Checks if the Play app is running in production mode (not necessarily in a real production environment).
   * @return True if the app is running in production mode; otherwise, False.
   */
  def isInProdMode: Boolean = play.api.Play.isProd(play.api.Play.current)

  /**
   * Reads STAGING ENV variable.
   * @return If found, a String containing 'true' or 'false'; otherwise, None.
   */
  def stagingEnvSetting: Option[String] = Play.current.configuration.getString("staging")

  /**
   * Checks if the app is running in production mode and compares the string passed as an argument to the STAGING ENV variable.
   * @param targetStagingEnvSetting The expected string found in the STAGING ENV variable.
   * @return True if the app is running in production mode and STAGING ENV variable matches targetStagingEnvSetting; otherwise, False.
   */
  def prodOrStagingCheck(targetStagingEnvSetting: String): Boolean = {
    stagingEnvSetting match {
      case Some(setting: String) if(isInProdMode && setting == targetStagingEnvSetting) => true
      case _ => false
    }
  }

  /**
   * Checks if the app is running in production mode and our STAGING ENV var is set to 'true.'
   * @return True if the app is running in production mode and STAGING is set to 'true'; otherwise, False.
   */
  def isStaging: Boolean = prodOrStagingCheck("true")

  /**
   * Checks if the app is running in production mode and our STAGING ENV var is set to 'false.'
   * @return True if the app is running in production mode and STAGING is set to 'false'; otherwise, False.
   */
  def isProd: Boolean = prodOrStagingCheck("false")

  /**
   * Checks if the app is running in either the staging or production environment.
   * @return True if the app is running in staging or production; otherwise, False.
   */
  def isProdOrStaging: Boolean = isProd || isStaging

  /**
   * Checks if the app is running in test mode.
   * @return True if the app is in test mode; otherwise, False.
   */
  def isTest: Boolean = play.api.Play.isTest(play.api.Play.current)

  /**
   * Checks if the app is running in development mode.
   * @return True if the app is running in development mode; otherwise, False.
   */
  def isDev: Boolean = play.api.Play.isDev(play.api.Play.current)

  /**
   * Checks current environment and returns the type as a String using the Rails environment naming convention.
   * @return A String indicating the environment type.
   */
  def mode: String = {
    play.api.Play.mode(play.api.Play.current).toString match {
      case "Prod" if(isStaging) => "staging"
      case "Prod" if(isProd) => "production"
      case "Test" => "test"
      case _ => "development"
    }
  }
}
