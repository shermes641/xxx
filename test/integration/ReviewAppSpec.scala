package integration

import models._
import play.api.{Configuration, Play}
import play.api.test.WithApplication
import resources.{ApplicationFake, SpecificationWithFixtures}

class ReviewAppSpec extends SpecificationWithFixtures {
  val testEnv = play.api.Environment.simple(mode = play.api.Mode.Test)

  def appEnv(config: Configuration, env: play.api.Environment): Environment = {
    val appMode = new AppMode(env)
    new models.Environment(config, appMode)
  }

  "ReviewApp" should {
    "Find review app Keen project isReviewApp true in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.HerokuConfigVars.AppName -> "it-test-pr-000",
        Constants.HerokuConfigVars.ParentName -> "it-test"))) {

        val unstartedConfig = {
          val unstartedAppEnvironment = appEnv(app.configuration, testEnv)
          new ConfigVars(app.configuration, unstartedAppEnvironment)
        }

        val appEnvironment = appEnv(app.configuration, testEnv)
        val startedConfig = new ConfigVars(app.configuration, appEnvironment)
        Startup.beforeStart(startedConfig, appEnvironment)

        val TestErrorProjectID          = Play.current.configuration.getString(Constants.KeenConfig.ErrorProjectID).get
        val TestErrorProjectKey         = Play.current.configuration.getString(Constants.KeenConfig.ErrorProjectKey).get
        val TestReviewErrorProjectID    = Play.current.configuration.getString(Constants.KeenConfig.ReviewErrorProjectID).get
        val TestReviewErrorProjectKey   = Play.current.configuration.getString(Constants.KeenConfig.ReviewErrorProjectKey).get

        val res = new ReviewApp().createOrGetKeenProject(startedConfig.ConfigVarsApp.appName,
          startedConfig.ConfigVarsApp.opsEmail,
          startedConfig.ConfigVarsKeen.orgID,
          startedConfig.ConfigVarsKeen.orgKey)

        appEnvironment.isReviewApp must_== true
        startedConfig.ConfigVarsKeen.error match {
          case Some(err: String) => // we were unable to contact keen.io to find or create the review app project
            res.get("error").get                 == err.split(":").head must_== true
            unstartedConfig.ConfigVarsKeen.projectID  == startedConfig.ConfigVarsKeen.projectID must_== true
            unstartedConfig.ConfigVarsKeen.readKey    == startedConfig.ConfigVarsKeen.readKey must_== true
            unstartedConfig.ConfigVarsKeen.writeKey   == startedConfig.ConfigVarsKeen.writeKey must_== true
            unstartedConfig.ConfigVarsKeen.masterKey  == startedConfig.ConfigVarsKeen.masterKey must_== true
            TestReviewErrorProjectID                  == startedConfig.ConfigVarsKeen.errorProjectID must_== true
            TestReviewErrorProjectKey                 == startedConfig.ConfigVarsKeen.errorProjectKey must_== true

          case _ =>
            res.get("name").get                       == startedConfig.ConfigVarsApp.appName must_== true
            res.get("id").get                         == startedConfig.ConfigVarsKeen.projectID must_== true
            res.get("readKey").get                    == startedConfig.ConfigVarsKeen.readKey must_== true
            res.get("writeKey").get                   == startedConfig.ConfigVarsKeen.writeKey must_== true
            res.get("masterKey").get                  == startedConfig.ConfigVarsKeen.masterKey must_== true
            unstartedConfig.ConfigVarsKeen.projectID  == startedConfig.ConfigVarsKeen.projectID must_== true
            unstartedConfig.ConfigVarsKeen.readKey    == startedConfig.ConfigVarsKeen.readKey must_== true
            unstartedConfig.ConfigVarsKeen.writeKey   == startedConfig.ConfigVarsKeen.writeKey must_== true
            unstartedConfig.ConfigVarsKeen.masterKey  == startedConfig.ConfigVarsKeen.masterKey must_== true
            TestReviewErrorProjectID                  == startedConfig.ConfigVarsKeen.errorProjectID must_== true
            TestReviewErrorProjectKey                 == startedConfig.ConfigVarsKeen.errorProjectKey must_== true
        }
      }

    "Force create review app Keen project isReviewApp true in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.HerokuConfigVars.AppName -> "it-test-pr-000",
        Constants.HerokuConfigVars.ParentName -> "it-test"))) {

        val unstartedConfig = {
          val unstartedAppEnvironment = appEnv(app.configuration, testEnv)
          new ConfigVars(app.configuration, unstartedAppEnvironment)
        }

        val appEnvironment = appEnv(app.configuration, testEnv)
        val startedConfig = new ConfigVars(app.configuration, appEnvironment)
        Startup.beforeStart(startedConfig, appEnvironment)

        val TestErrorProjectID          = Play.current.configuration.getString(Constants.KeenConfig.ErrorProjectID).get
        val TestErrorProjectKey         = Play.current.configuration.getString(Constants.KeenConfig.ErrorProjectKey).get
        val TestReviewErrorProjectID    = Play.current.configuration.getString(Constants.KeenConfig.ReviewErrorProjectID).get
        val TestReviewErrorProjectKey   = Play.current.configuration.getString(Constants.KeenConfig.ReviewErrorProjectKey).get

        val res = new ReviewApp().createOrGetKeenProject(startedConfig.ConfigVarsApp.appName,
          startedConfig.ConfigVarsApp.opsEmail,
          startedConfig.ConfigVarsKeen.orgID,
          startedConfig.ConfigVarsKeen.orgKey,
          forceCreate = true)

        appEnvironment.isReviewApp must_== true
        startedConfig.ConfigVarsKeen.error match {
          case Some(err: String) => // we were unable to contact keen.io to find or create the review app project
            res.get("error").get                      == err.split(":").head must_== true
            unstartedConfig.ConfigVarsKeen.projectID  == startedConfig.ConfigVarsKeen.projectID must_== true
            unstartedConfig.ConfigVarsKeen.readKey    == startedConfig.ConfigVarsKeen.readKey must_== true
            unstartedConfig.ConfigVarsKeen.writeKey   == startedConfig.ConfigVarsKeen.writeKey must_== true
            unstartedConfig.ConfigVarsKeen.masterKey  == startedConfig.ConfigVarsKeen.masterKey must_== true
            TestReviewErrorProjectID                  == startedConfig.ConfigVarsKeen.errorProjectID must_== true
            TestReviewErrorProjectKey                 == startedConfig.ConfigVarsKeen.errorProjectKey must_== true

          case _ =>
            res.get("name").get                       == startedConfig.ConfigVarsApp.appName must_== true
            res.get("id").get                         == startedConfig.ConfigVarsKeen.projectID must_== true
            res.get("readKey").get                    == startedConfig.ConfigVarsKeen.readKey must_== true
            res.get("writeKey").get                   == startedConfig.ConfigVarsKeen.writeKey must_== true
            res.get("masterKey").get                  == startedConfig.ConfigVarsKeen.masterKey must_== true
            unstartedConfig.ConfigVarsKeen.projectID  == startedConfig.ConfigVarsKeen.projectID must_== true
            unstartedConfig.ConfigVarsKeen.readKey    == startedConfig.ConfigVarsKeen.readKey must_== true
            unstartedConfig.ConfigVarsKeen.writeKey   == startedConfig.ConfigVarsKeen.writeKey must_== true
            unstartedConfig.ConfigVarsKeen.masterKey  == startedConfig.ConfigVarsKeen.masterKey must_== true
            TestReviewErrorProjectID                  == startedConfig.ConfigVarsKeen.errorProjectID must_== true
            TestReviewErrorProjectKey                 == startedConfig.ConfigVarsKeen.errorProjectKey must_== true
        }
      }
  }
}
