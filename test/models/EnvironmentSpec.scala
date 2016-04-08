package models

import play.api.Configuration
import play.api.test.WithApplication
import resources.{ApplicationFake, SpecificationWithFixtures}

class EnvironmentSpec extends SpecificationWithFixtures {

  val prodEnv = play.api.Environment.simple(mode = play.api.Mode.Prod)
  val devEnv = play.api.Environment.simple(mode = play.api.Mode.Dev)
  val testEnv = play.api.Environment.simple(mode = play.api.Mode.Test)

  def appEnv(config: Configuration, env: play.api.Environment): Environment = {
    val appMode = new AppMode(env)
    new models.Environment(config, appMode)
  }

  "Environment" should {
    "isReviewApp false in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString, Constants.AppConfig.Staging -> "true"))) {
        appEnv(app.configuration, testEnv).isReviewApp must_== false
      }

    "isReviewApp true in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.HerokuConfigVars.AppName -> "mediation-test-pr-000",
        Constants.HerokuConfigVars.ParentName -> "it-test"))) {
        appEnv(app.configuration, testEnv).isReviewApp must_== true
      }

    // no _pr_ in appName
    "isReviewApp false in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.HerokuConfigVars.AppName -> "mediation-test-pr-000"))) {
        appEnv(app.configuration, testEnv).isReviewApp must_== false
      }

    "isReviewApp false in test mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString, Constants.AppConfig.Staging -> "false"))) {
        appEnv(app.configuration, testEnv).isReviewApp must_== false
      }

    "isReviewApp true in test mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString,
        Constants.AppConfig.Staging -> "false",
        Constants.HerokuConfigVars.AppName -> "mediation-test-pr-000",
        Constants.HerokuConfigVars.ParentName -> "it-test"))) {
        appEnv(app.configuration, testEnv).isReviewApp must_== true
      }

    "isReviewApp false in dev mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Dev.toString, Constants.AppConfig.Staging -> "true"))) {
        appEnv(app.configuration, devEnv).isReviewApp must_== false
      }

    "isReviewApp true in dev mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Dev.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.HerokuConfigVars.AppName -> "mediation-test-pr-000",
        Constants.HerokuConfigVars.ParentName -> "it-test"))) {
        appEnv(app.configuration, devEnv).isReviewApp must_== true
      }
    "isReviewApp false in prod mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString, Constants.AppConfig.Staging -> "false"))) {
        appEnv(app.configuration, prodEnv).isReviewApp must_== false
      }

    // these are misconfigured
    "isReviewApp true in prod mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString,
        Constants.AppConfig.Staging -> "false",
        Constants.HerokuConfigVars.AppName -> "mediation-test-pr-000",
        Constants.HerokuConfigVars.ParentName -> "it-test"))) {
        appEnv(app.configuration, prodEnv).isReviewApp must_== true
      }

    // this is a misconfiguration, no _pr_ in appName
    "isReviewApp false in prod mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString,
        Constants.AppConfig.Staging -> "false",
        Constants.HerokuConfigVars.AppName -> "mediation-test_17"))) {
        appEnv(app.configuration, prodEnv).isReviewApp must_== false
      }

    "isInProdMode false in test mode" in new TestApp {
      appEnv(app.configuration, testEnv).isInProdMode must_== false
    }

    "isInProdMode false in dev mode" in new DevApp {
      appEnv(app.configuration, devEnv).isInProdMode must_== false
    }

    "isInProdMode true  in prod mode" in new ProdApp {
      appEnv(app.configuration, prodEnv).isInProdMode must_== true
    }

    "isTest false  in prod mode" in new ProdApp {
      appEnv(app.configuration, prodEnv).isTest must_== false
    }

    "isTest true in test mode" in new TestApp {
      appEnv(app.configuration, testEnv).isTest must_== true
    }

    "isTest false in dev mode" in new DevApp {
      appEnv(app.configuration, devEnv).isTest must_== false
    }

    "isDev false in prod mode" in new ProdApp {
      appEnv(app.configuration, prodEnv).isDev must_== false
    }

    "isDev false in test mode" in new TestApp {
      appEnv(app.configuration, testEnv).isDev must_== false
    }

    "isDev true in dev mode" in new DevApp {
      appEnv(app.configuration, devEnv).isDev must_== true
    }

    "stagingEnvSetting None in test mode no staging" in new TestApp {
      appEnv(app.configuration, testEnv).stagingEnvSetting must_== None
    }

    "stagingEnvSetting  true in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Staging -> "true"))) {
        appEnv(app.configuration, testEnv).stagingEnvSetting must_== Some("true")
      }

    "stagingEnvSetting true in prod mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString, Constants.AppConfig.Staging -> "true"))) {
        appEnv(app.configuration, prodEnv).stagingEnvSetting must_== Some("true")
      }

    "stagingEnvSetting in false test mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Staging -> "false"))) {
        appEnv(app.configuration, testEnv).stagingEnvSetting must_== Some("false")
      }

    "isStaging false in test mode no staging" in new TestApp {
      appEnv(app.configuration, testEnv).isStaging must_== false
    }

    "isStaging false in test mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Staging -> "false"))) {
        appEnv(app.configuration, testEnv).isStaging must_== false
      }

    "isStaging  false in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Staging -> "true"))) {
        appEnv(app.configuration, testEnv).isStaging must_== false
      }

    "isStaging false in dev mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Dev.toString, Constants.AppConfig.Staging -> "true"))) {
        appEnv(app.configuration, devEnv).isStaging must_== false
      }

    "isStaging true in prod mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString, Constants.AppConfig.Staging -> "true"))) {
        appEnv(app.configuration, prodEnv).isStaging must_== true
      }

    "isStaging false in prod mode no staging" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString))) {
        appEnv(app.configuration, prodEnv).isStaging must_== false
      }

    "isStaging false in prod mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString, Constants.AppConfig.Staging -> "false"))) {
        appEnv(app.configuration, prodEnv).isStaging must_== false
      }

    "isProd false in test mode no staging" in new TestApp {
      appEnv(app.configuration, testEnv).isProd must_== false
    }

    "isProd false in test mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Staging -> "false"))) {
        appEnv(app.configuration, testEnv).isProd must_== false
      }

    "isProd false in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Staging -> "true"))) {
        appEnv(app.configuration, testEnv).isProd must_== false
      }

    "isProd false in dev mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Dev.toString, Constants.AppConfig.Staging -> "true"))) {
        appEnv(app.configuration, devEnv).isProd must_== false
      }

    "isProd false in prod mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString, Constants.AppConfig.Staging -> "true"))) {
        appEnv(app.configuration, prodEnv).isProd must_== false
      }

    "isProd false in prod mode no staging" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString))) {
        appEnv(app.configuration, prodEnv).isProd must_== false
      }

    "isProd true in prod mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString, Constants.AppConfig.Staging -> "false"))) {
        appEnv(app.configuration, prodEnv).isProd must_== true
      }

    "isProdOrStaging false in test mode no staging" in new TestApp {
      appEnv(app.configuration, testEnv).isProdOrStaging must_== false
    }

    "isProdOrStaging false in test mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Staging -> "false"))) {
        appEnv(app.configuration, testEnv).isProdOrStaging must_== false
      }

    "isProdOrStaging  false in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Staging -> "true"))) {
        appEnv(app.configuration, testEnv).isProdOrStaging must_== false
      }

    "isProdOrStaging false in dev mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Dev.toString, Constants.AppConfig.Staging -> "true"))) {
        appEnv(app.configuration, devEnv).isProdOrStaging must_== false
      }

    "isProdOrStaging true in prod mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString, Constants.AppConfig.Staging -> "true"))) {
        appEnv(app.configuration, prodEnv).isProdOrStaging must_== true
      }

    "isProdOrStaging false in prod mode no staging" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString))) {
        appEnv(app.configuration, prodEnv).isProdOrStaging must_== false
      }

    "isProdOrStaging true in prod mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString, Constants.AppConfig.Staging -> "false"))) {
        appEnv(app.configuration, prodEnv).isProdOrStaging must_== true
      }

    "mode  in prod mode" in new ProdApp {
      appEnv(app.configuration, prodEnv).mode must_== "development"
    }

    "mode in prod mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString, Constants.AppConfig.Staging -> "false"))) {
        appEnv(app.configuration, prodEnv).mode must_== "production"
      }

    "mode in prod mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString, Constants.AppConfig.Staging -> "true"))) {
        appEnv(app.configuration, prodEnv).mode must_== "staging"
      }

    "mode in test mode" in new TestApp {
      appEnv(app.configuration, testEnv).mode must_== "test"
    }

    "mode in test mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString, Constants.AppConfig.Staging -> "false"))) {
        appEnv(app.configuration, testEnv).mode must_== "test"
      }

    "mode in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString, Constants.AppConfig.Staging -> "true"))) {
        appEnv(app.configuration, testEnv).mode must_== "test"
      }

    "mode in dev mode" in new DevApp {
      appEnv(app.configuration, devEnv).mode must_== "development"
    }

    "mode in dev mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Dev.toString, Constants.AppConfig.Staging -> "false"))) {
        appEnv(app.configuration, devEnv).mode must_== "development"
      }

    "mode in dev mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Dev.toString, Constants.AppConfig.Staging -> "true"))) {
        appEnv(app.configuration, devEnv).mode must_== "development"
      }

    //  this is a misconfiguration, but it meets the review app requirements
    "isReviewApp true in prod mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString,
        Constants.AppConfig.Staging -> "false",
        Constants.HerokuConfigVars.AppName -> "mediation-test-pr-000",
        Constants.HerokuConfigVars.ParentName -> "it-test"))) {
        appEnv(app.configuration, prodEnv).isReviewApp must_== true
      }

    // this is a misconfiguration, no _pr_ in appName and in prod mode
    "isReviewApp false in prod mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString,
        Constants.AppConfig.Staging -> "false",
        Constants.HerokuConfigVars.AppName -> "mediation-test_17",
        Constants.HerokuConfigVars.ParentName -> "it-test"))) {
        appEnv(app.configuration, prodEnv).isReviewApp must_== false
      }
  }
}
