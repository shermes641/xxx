package integration

import models.{ConfigVars, Constants, Environment, ReviewApp}
import play.api.Play
import play.api.test.WithApplication
import resources.SpecificationWithFixtures

class ReviewAppSpec extends SpecificationWithFixtures {
  "ReviewApp" should {
    "isReviewApp true in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.HerokuConfigVars.AppName -> "it-test_pr_118",
        Constants.HerokuConfigVars.ParentName -> "it-test"))) {

        val TestErrorProjectID          = Play.current.configuration.getString(Constants.KeenConfig.ErrorProjectID).get
        val TestErrorProjectKey         = Play.current.configuration.getString(Constants.KeenConfig.ErrorProjectKey).get
        val TestReviewErrorProjectID    = Play.current.configuration.getString(Constants.KeenConfig.ReviewErrorProjectID).get
        val TestReviewErrorProjectKey   = Play.current.configuration.getString(Constants.KeenConfig.ReviewErrorProjectKey).get

        object Vars extends ConfigVars

        val res = new ReviewApp().createOrGetKeenProject(Vars.ConfigVarsApp.appName,
          Vars.ConfigVarsApp.opsEmail,
          Vars.ConfigVarsKeen.orgID,
          Vars.ConfigVarsKeen.orgKey)

        Environment.isReviewApp must_== true
        Vars.ConfigVarsKeen.error match {
          case Some(err: String) => // we were unable to contact keen.io to find or create the review app project
            res.get("error").get      == err.split(":").head must_== true
            ConfigVarsKeen.projectID  == Vars.ConfigVarsKeen.projectID must_== true
            ConfigVarsKeen.readKey    == Vars.ConfigVarsKeen.readKey must_== true
            ConfigVarsKeen.writeKey   == Vars.ConfigVarsKeen.writeKey must_== true
            ConfigVarsKeen.masterKey  == Vars.ConfigVarsKeen.masterKey must_== true
            TestReviewErrorProjectID  == Vars.ConfigVarsKeen.errorProjectID must_== true
            TestReviewErrorProjectKey == Vars.ConfigVarsKeen.errorProjectKey must_== true

          case _ =>
            res.get("name").get       == Vars.ConfigVarsApp.appName must_== true
            res.get("id").get         == Vars.ConfigVarsKeen.projectID must_== true
            res.get("readKey").get    == Vars.ConfigVarsKeen.readKey must_== true
            res.get("writeKey").get   == Vars.ConfigVarsKeen.writeKey must_== true
            res.get("masterKey").get  == Vars.ConfigVarsKeen.masterKey must_== true
            ConfigVarsKeen.projectID  == Vars.ConfigVarsKeen.projectID must_== true
            ConfigVarsKeen.readKey    == Vars.ConfigVarsKeen.readKey must_== true
            ConfigVarsKeen.writeKey   == Vars.ConfigVarsKeen.writeKey must_== true
            ConfigVarsKeen.masterKey  == Vars.ConfigVarsKeen.masterKey must_== true
            TestReviewErrorProjectID  == Vars.ConfigVarsKeen.errorProjectID must_== true
            TestReviewErrorProjectKey == Vars.ConfigVarsKeen.errorProjectKey must_== true
        }
      }
  }
}
