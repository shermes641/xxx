package models

import play.Logger
import play.api.Configuration
import play.api.test.Helpers._
import play.api.test.WithApplication
import resources.{ApplicationFake, SpecificationWithFixtures}

class ConfigVarsSpec extends SpecificationWithFixtures {

  val testEnv = play.api.Environment.simple(mode = play.api.Mode.Test)

  def appEnv(config: Configuration, env: play.api.Environment): Environment = {
    val appMode = new AppMode(env)
    new models.Environment(config, appMode)
  }

  val unstartedConfig = running(stagingAppFake) {
    val unstartedAppEnvironment = appEnv(stagingAppFake.configuration, testEnv)
    new ConfigVars(stagingAppFake.configuration, unstartedAppEnvironment)
  }

  "Environment" should {
    "Not a Review app, configuration vars set correctly in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString, Constants.AppConfig.Staging -> "true"))) {

        val appEnvironment = appEnv(app.configuration, testEnv)
        val startedConfig = new ConfigVars(app.configuration, appEnvironment)
        Startup.beforeStart(startedConfig, appEnvironment)

        appEnvironment.isReviewApp must_== false
        startedConfig.ConfigVarsKeen.toString == unstartedConfig.ConfigVarsKeen.toString must_== true
        startedConfig.ConfigVarsApp.toString == unstartedConfig.ConfigVarsApp.toString must_== true
        startedConfig.ConfigVarsAdProviders.toString == unstartedConfig.ConfigVarsAdProviders.toString must_== true
        startedConfig.ConfigVarsCallbackUrls.toString == unstartedConfig.ConfigVarsCallbackUrls.toString must_== true
        startedConfig.ConfigVarsHeroku.toString == unstartedConfig.ConfigVarsHeroku.toString must_== true
        startedConfig.ConfigVarsHmac.toString == unstartedConfig.ConfigVarsHmac.toString must_== true
        startedConfig.ConfigVarsJunGroup.toString == unstartedConfig.ConfigVarsJunGroup.toString must_== true
        startedConfig.ConfigVarsReporting.toString == unstartedConfig.ConfigVarsReporting.toString must_== true
      }

    "Review app, configuration vars set correctly in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.HerokuConfigVars.AppName -> "mediation-test-pr-000",
        Constants.HerokuConfigVars.ParentName -> "mediation-staging"))) {

        val appEnvironment = appEnv(app.configuration, testEnv)
        val startedConfig = new ConfigVars(app.configuration, appEnvironment)
        Startup.beforeStart(startedConfig, appEnvironment)

        appEnvironment.isReviewApp must_== true
        startedConfig.ConfigVarsKeen.error match {
          case Some(err) => // we were unable to contact keen.io to find or create the review app project
            Logger.error(s"Error accessing Keen for Review apps error: $err")
            startedConfig.ConfigVarsKeen.projectID == unstartedConfig.ConfigVarsKeen.projectID must_== true
            startedConfig.ConfigVarsKeen.writeKey == unstartedConfig.ConfigVarsKeen.writeKey must_== true
            startedConfig.ConfigVarsKeen.readKey == unstartedConfig.ConfigVarsKeen.readKey must_== true
            startedConfig.ConfigVarsKeen.masterKey == unstartedConfig.ConfigVarsKeen.masterKey must_== true
            startedConfig.ConfigVarsKeen.errorProjectKey == unstartedConfig.ConfigVarsKeen.errorProjectKey must_== false
            startedConfig.ConfigVarsKeen.errorProjectID == unstartedConfig.ConfigVarsKeen.errorProjectID must_== false
            startedConfig.ConfigVarsKeen.orgID == unstartedConfig.ConfigVarsKeen.orgID must_== true
            startedConfig.ConfigVarsKeen.orgKey == unstartedConfig.ConfigVarsKeen.orgKey must_== true

            startedConfig.ConfigVarsApp.appName == unstartedConfig.ConfigVarsApp.appName must_== false
            startedConfig.ConfigVarsApp.appName.length > unstartedConfig.ConfigVarsApp.appName.length must_== true
            startedConfig.ConfigVarsApp.appName.length > 1 must_== true
            startedConfig.ConfigVarsApp.parentName == unstartedConfig.ConfigVarsApp.parentName must_== false
            startedConfig.ConfigVarsApp.parentName.length > unstartedConfig.ConfigVarsApp.parentName.length must_== true
            startedConfig.ConfigVarsApp.parentName.length > 1 must_== true
            startedConfig.ConfigVarsApp.domain.startsWith(unstartedConfig.ConfigVarsApp.appName) must_== false
            startedConfig.ConfigVarsApp.domain.equals(s"https://${unstartedConfig.ConfigVarsApp.appName}.herokuapp.com") must_== true

            val start = s"https://${startedConfig.ConfigVarsApp.appName}.herokuapp.com"
            unstartedConfig.ConfigVarsCallbackUrls.ios.startsWith(start) must_== false
            unstartedConfig.ConfigVarsCallbackUrls.android.startsWith(start) must_== false
            unstartedConfig.ConfigVarsCallbackUrls.player.startsWith(start) must_== false
            startedConfig.ConfigVarsCallbackUrls.ios.startsWith(start) must_== true
            startedConfig.ConfigVarsCallbackUrls.android.startsWith(start) must_== true
            startedConfig.ConfigVarsCallbackUrls.player.startsWith(start) must_== true

            startedConfig.ConfigVarsAdProviders.toString == unstartedConfig.ConfigVarsAdProviders.toString must_== true
            startedConfig.ConfigVarsHeroku.toString == unstartedConfig.ConfigVarsHeroku.toString must_== true
            startedConfig.ConfigVarsHmac.toString == unstartedConfig.ConfigVarsHmac.toString must_== true
            startedConfig.ConfigVarsJunGroup.toString == unstartedConfig.ConfigVarsJunGroup.toString must_== true

          case _ =>
            startedConfig.ConfigVarsKeen.projectID == unstartedConfig.ConfigVarsKeen.projectID must_== false
            startedConfig.ConfigVarsKeen.writeKey == unstartedConfig.ConfigVarsKeen.writeKey must_== false
            startedConfig.ConfigVarsKeen.readKey == unstartedConfig.ConfigVarsKeen.readKey must_== false
            startedConfig.ConfigVarsKeen.masterKey == unstartedConfig.ConfigVarsKeen.masterKey must_== false
            startedConfig.ConfigVarsKeen.errorProjectKey == unstartedConfig.ConfigVarsKeen.errorProjectKey must_== false
            startedConfig.ConfigVarsKeen.errorProjectID == unstartedConfig.ConfigVarsKeen.errorProjectID must_== false
            startedConfig.ConfigVarsKeen.orgID == unstartedConfig.ConfigVarsKeen.orgID must_== true
            startedConfig.ConfigVarsKeen.orgKey == unstartedConfig.ConfigVarsKeen.orgKey must_== true

            startedConfig.ConfigVarsApp.appName == unstartedConfig.ConfigVarsApp.appName must_== false
            startedConfig.ConfigVarsApp.appName.length > unstartedConfig.ConfigVarsApp.appName.length must_== true
            startedConfig.ConfigVarsApp.appName.length > 1 must_== true
            startedConfig.ConfigVarsApp.parentName == unstartedConfig.ConfigVarsApp.parentName must_== false
            startedConfig.ConfigVarsApp.parentName.length > unstartedConfig.ConfigVarsApp.parentName.length must_== true
            startedConfig.ConfigVarsApp.parentName.length > 1 must_== true
            unstartedConfig.ConfigVarsApp.domain.startsWith(startedConfig.ConfigVarsApp.appName) must_== false
            startedConfig.ConfigVarsApp.domain.equals(s"https://${startedConfig.ConfigVarsApp.appName}.herokuapp.com") must_== true

            val start = s"https://${startedConfig.ConfigVarsApp.appName}.herokuapp.com"
            unstartedConfig.ConfigVarsCallbackUrls.ios.startsWith(start) must_== false
            unstartedConfig.ConfigVarsCallbackUrls.android.startsWith(start) must_== false
            unstartedConfig.ConfigVarsCallbackUrls.player.startsWith(start) must_== false
            startedConfig.ConfigVarsCallbackUrls.ios.startsWith(start) must_== true
            startedConfig.ConfigVarsCallbackUrls.android.startsWith(start) must_== true
            startedConfig.ConfigVarsCallbackUrls.player.startsWith(start) must_== true

            startedConfig.ConfigVarsAdProviders.toString == unstartedConfig.ConfigVarsAdProviders.toString must_== true
            startedConfig.ConfigVarsHeroku.toString == unstartedConfig.ConfigVarsHeroku.toString must_== true
            startedConfig.ConfigVarsHmac.toString == unstartedConfig.ConfigVarsHmac.toString must_== true
            startedConfig.ConfigVarsJunGroup.toString == unstartedConfig.ConfigVarsJunGroup.toString must_== true
        }
      }

    "Not a Review app, configuration vars set correctly in prod mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString, Constants.AppConfig.Staging -> "false"))) {

        val appEnvironment = appEnv(app.configuration, testEnv)
        val currentConfig = new ConfigVars(app.configuration, appEnvironment)
        Startup.beforeStart(currentConfig, appEnvironment)

        appEnvironment.isReviewApp must_== false
        //remove header and staging from ConfigVarsApp as staging is the only thing that will be different
        configVars.ConfigVarsApp.toString.split("\n").drop(3) sameElements configVars.ConfigVarsApp.toString.split("\n").drop(3) must_== true

        unstartedConfig.ConfigVarsKeen.toString == configVars.ConfigVarsKeen.toString must_== true
        unstartedConfig.ConfigVarsAdProviders.toString == configVars.ConfigVarsAdProviders.toString must_== true
        unstartedConfig.ConfigVarsCallbackUrls.toString == configVars.ConfigVarsCallbackUrls.toString must_== true
        unstartedConfig.ConfigVarsHeroku.toString == configVars.ConfigVarsHeroku.toString must_== true
        unstartedConfig.ConfigVarsHmac.toString == configVars.ConfigVarsHmac.toString must_== true
        unstartedConfig.ConfigVarsJunGroup.toString == configVars.ConfigVarsJunGroup.toString must_== true
        unstartedConfig.ConfigVarsReporting.toString == configVars.ConfigVarsReporting.toString must_== true
      }
  }
}
