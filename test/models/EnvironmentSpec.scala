package models

import play.api.test.WithApplication
import resources.{ApplicationFake, SpecificationWithFixtures}

class EnvironmentSpec extends SpecificationWithFixtures {
  "Environment" should {
    "isReviewApp false in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString, Constants.AppConfig.Staging -> "true"))) {
        Environment.isReviewApp must_== false
      }

    "isReviewApp true in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.HerokuConfigVars.AppName -> "mediation-test-pr-000",
        Constants.HerokuConfigVars.ParentName -> "it-test"))) {
        Environment.isReviewApp must_== true
      }

    // no _pr_ in appName
    "isReviewApp false in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.HerokuConfigVars.AppName -> "mediation-test-pr-000"))) {
        Environment.isReviewApp must_== false
      }

    "isReviewApp false in test mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString, Constants.AppConfig.Staging -> "false"))) {
        Environment.isReviewApp must_== false
      }

    "isReviewApp true in test mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString,
        Constants.AppConfig.Staging -> "false",
        Constants.HerokuConfigVars.AppName -> "mediation-test-pr-000",
        Constants.HerokuConfigVars.ParentName -> "it-test"))) {
        Environment.isReviewApp must_== true
      }

    "isReviewApp false in dev mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Dev.toString, Constants.AppConfig.Staging -> "true"))) {
        Environment.isReviewApp must_== false
      }

    "isReviewApp true in dev mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Dev.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.HerokuConfigVars.AppName -> "mediation-test-pr-000",
        Constants.HerokuConfigVars.ParentName -> "it-test"))) {
        Environment.isReviewApp must_== true
      }
    "isReviewApp false in prod mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString, Constants.AppConfig.Staging -> "false"))) {
        Environment.isReviewApp must_== false
      }

    // these are misconfigured
    "isReviewApp true in prod mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString,
        Constants.AppConfig.Staging -> "false",
        Constants.HerokuConfigVars.AppName -> "mediation-test-pr-000",
        Constants.HerokuConfigVars.ParentName -> "it-test"))) {
        Environment.isReviewApp must_== true
      }

    // this is a misconfiguration, no _pr_ in appName
    "isReviewApp false in prod mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString,
        Constants.AppConfig.Staging -> "false",
        Constants.HerokuConfigVars.AppName -> "mediation-test_17"))) {
        Environment.isReviewApp must_== false
      }

    "isInProdMode false in test mode" in new TestApp {
      Environment.isInProdMode must_== false
    }

    "isInProdMode false in dev mode" in new DevApp {
      Environment.isInProdMode must_== false
    }

    "isInProdMode true  in prod mode" in new ProdApp {
      Environment.isInProdMode must_== true
    }

    "isTest false  in prod mode" in new ProdApp {
      Environment.isTest must_== false
    }

    "isTest true in test mode" in new TestApp {
      Environment.isTest must_== true
    }

    "isTest false in dev mode" in new DevApp {
      Environment.isTest must_== false
    }

    "isDev false in prod mode" in new ProdApp {
      Environment.isDev must_== false
    }

    "isDev false in test mode" in new TestApp {
      Environment.isDev must_== false
    }

    "isDev true in dev mode" in new DevApp {
      Environment.isDev must_== true
    }

    "stagingEnvSetting None in test mode no staging" in new TestApp {
      Environment.stagingEnvSetting must_== None
    }

    "stagingEnvSetting  true in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Staging -> "true"))) {
        Environment.stagingEnvSetting must_== Some("true")
      }

    "stagingEnvSetting true in prod mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString, Constants.AppConfig.Staging -> "true"))) {
        Environment.stagingEnvSetting must_== Some("true")
      }

    "stagingEnvSetting in false test mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Staging -> "false"))) {
        Environment.stagingEnvSetting must_== Some("false")
      }

    "isStaging false in test mode no staging" in new TestApp {
      Environment.isStaging must_== false
    }

    "isStaging false in test mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Staging -> "false"))) {
        Environment.isStaging must_== false
      }

    "isStaging  false in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Staging -> "true"))) {
        Environment.isStaging must_== false
      }

    "isStaging false in dev mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Dev.toString, Constants.AppConfig.Staging -> "true"))) {
        Environment.isStaging must_== false
      }

    "isStaging true in prod mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString, Constants.AppConfig.Staging -> "true"))) {
        Environment.isStaging must_== true
      }

    "isStaging false in prod mode no staging" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString))) {
        Environment.isStaging must_== false
      }

    "isStaging false in prod mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString, Constants.AppConfig.Staging -> "false"))) {
        Environment.isStaging must_== false
      }

    "isProd false in test mode no staging" in new TestApp {
      Environment.isProd must_== false
    }

    "isProd false in test mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Staging -> "false"))) {
        Environment.isProd must_== false
      }

    "isProd false in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Staging -> "true"))) {
        Environment.isProd must_== false
      }

    "isProd false in dev mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Dev.toString, Constants.AppConfig.Staging -> "true"))) {
        Environment.isProd must_== false
      }

    "isProd false in prod mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString, Constants.AppConfig.Staging -> "true"))) {
        Environment.isProd must_== false
      }

    "isProd false in prod mode no staging" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString))) {
        Environment.isProd must_== false
      }

    "isProd true in prod mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString, Constants.AppConfig.Staging -> "false"))) {
        Environment.isProd must_== true
      }

    "isProdOrStaging false in test mode no staging" in new TestApp {
      Environment.isProdOrStaging must_== false
    }

    "isProdOrStaging false in test mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Staging -> "false"))) {
        Environment.isProdOrStaging must_== false
      }

    "isProdOrStaging  false in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Staging -> "true"))) {
        Environment.isProdOrStaging must_== false
      }

    "isProdOrStaging false in dev mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Dev.toString, Constants.AppConfig.Staging -> "true"))) {
        Environment.isProdOrStaging must_== false
      }

    "isProdOrStaging true in prod mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString, Constants.AppConfig.Staging -> "true"))) {
        Environment.isProdOrStaging must_== true
      }

    "isProdOrStaging false in prod mode no staging" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString))) {
        Environment.isProdOrStaging must_== false
      }

    "isProdOrStaging true in prod mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString, Constants.AppConfig.Staging -> "false"))) {
        Environment.isProdOrStaging must_== true
      }

    "mode  in prod mode" in new ProdApp {
      Environment.mode must_== "development"
    }

    "mode in prod mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString, Constants.AppConfig.Staging -> "false"))) {
        Environment.mode must_== "production"
      }

    "mode in prod mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString, Constants.AppConfig.Staging -> "true"))) {
        Environment.mode must_== "staging"
      }

    "mode in test mode" in new TestApp {
      Environment.mode must_== "test"
    }

    "mode in test mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString, Constants.AppConfig.Staging -> "false"))) {
        Environment.mode must_== "test"
      }

    "mode in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString, Constants.AppConfig.Staging -> "true"))) {
        Environment.mode must_== "test"
      }

    "mode in dev mode" in new DevApp {
      Environment.mode must_== "development"
    }

    "mode in dev mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Dev.toString, Constants.AppConfig.Staging -> "false"))) {
        Environment.mode must_== "development"
      }

    "mode in dev mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Dev.toString, Constants.AppConfig.Staging -> "true"))) {
        Environment.mode must_== "development"
      }

    //  this is a misconfiguration, but it meets the review app requirements
    "isReviewApp true in prod mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString,
        Constants.AppConfig.Staging -> "false",
        Constants.HerokuConfigVars.AppName -> "mediation-test-pr-000",
        Constants.HerokuConfigVars.ParentName -> "it-test"))) {
        Environment.isReviewApp must_== true
      }

    // this is a misconfiguration, no _pr_ in appName and in prod mode
    "isReviewApp false in prod mode staging false" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString,
        Constants.AppConfig.Staging -> "false",
        Constants.HerokuConfigVars.AppName -> "mediation-test_17",
        Constants.HerokuConfigVars.ParentName -> "it-test"))) {
        Environment.isReviewApp must_== false
      }
  }
}
