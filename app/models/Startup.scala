package models

import play.api.Logger

object Startup {
  def systemExit(msg: String, errorCode: Int, obj: Any, appEnvironment: Environment) = {
    Logger.error(msg)
    Logger.debug(obj.toString)
    if (!appEnvironment.isProd)
      appEnvironment.TestErrorCode = errorCode
    else
      sys.exit(errorCode)
  }

  //TODO when we move to Play 2.4 we will move this code to a dependency injected class
  def beforeStart(configVars: ConfigVars, appEnvironment: Environment) {
    appEnvironment.TestErrorCode = 0
    Logger.info(s"Before Application startup ... isReviewApp: ${appEnvironment.isReviewApp} ")
    // check for start up errors
    if (configVars.ConfigVarsKeen.error.isDefined) {
      systemExit(s"Keen configuration error: ${configVars.ConfigVarsKeen.error.get}", Constants.Errors.KeenConfigError, configVars.ConfigVarsKeen, appEnvironment)
    }
    if (configVars.ConfigVarsAdProviders.iosID != Constants.AdProviderConfig.IosID || configVars.ConfigVarsAdProviders.androidID != Constants.AdProviderConfig.AndroidID) {
      systemExit(s"AdProvider configuration error iosID: ${configVars.ConfigVarsAdProviders.iosID}   androidID: ${configVars.ConfigVarsAdProviders.androidID} ",
        Constants.Errors.AdProviderError,
        configVars.ConfigVarsAdProviders,
        appEnvironment)
    }
  }

  def onStart(models: ModelService, appEnvironment: Environment) {
    val adProviderService = models.adProviderService
    val platform = models.platform

    if (appEnvironment.isReviewApp || appEnvironment.isDev) {
      Logger.debug("Loading Ad Providers .....")
      // make sure all ad providers exist
      adProviderService.loadAll()
    }

    //TODO check required environ vars

    val numberOfAdProviders = adProviderService.findAll.length
    val expectedNumberOfAdProviders = platform.Android.allAdProviders.length + platform.Android.allAdProviders.length
    if (numberOfAdProviders != expectedNumberOfAdProviders) {
      Logger.warn(s"Expected number of ad providers does not match actual number of ad providers expected: $expectedNumberOfAdProviders   actual: $numberOfAdProviders")
    }
  }
}

