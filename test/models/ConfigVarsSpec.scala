package models

import play.Logger
import play.api.test.WithApplication
import resources.SpecificationWithFixtures

class ConfigVarsSpec extends SpecificationWithFixtures {

  class Vars extends ConfigVars

  object OriginalVars extends ConfigVars {}

  "Environment" should {
    "Not a Review app, configuration vars set correctly in test mode staging true" in
      new WithApplication(new ApplicationFake(Map("mode" -> play.api.Mode.Test.toString, "staging" -> "true"))) {

        object Vars extends ConfigVars {}

        Environment.isReviewApp must_== false
        OriginalVars.ConfigVarsKeen.toString == Vars.ConfigVarsKeen.toString must_== true
        OriginalVars.ConfigVarsApp.toString == Vars.ConfigVarsApp.toString must_== true
        OriginalVars.ConfigVarsAdProviders.toString == Vars.ConfigVarsAdProviders.toString must_== true
        OriginalVars.ConfigVarsCallbackUrls.toString == Vars.ConfigVarsCallbackUrls.toString must_== true
        OriginalVars.ConfigVarsHeroku.toString == Vars.ConfigVarsHeroku.toString must_== true
        OriginalVars.ConfigVarsHmac.toString == Vars.ConfigVarsHmac.toString must_== true
        OriginalVars.ConfigVarsJunGroup.toString == Vars.ConfigVarsJunGroup.toString must_== true
        OriginalVars.ConfigVarsReporting.toString == Vars.ConfigVarsReporting.toString must_== true
      }

    "Review app, configuration vars set correctly in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.HerokuConfigVars.AppName -> "mediation-staging_pr_17",
        Constants.HerokuConfigVars.ParentName -> "mediation-staging"))) {

        object Vars extends ConfigVars

        Environment.isReviewApp must_== true
        Vars.ConfigVarsKeen.error match {
          case Some(err) => // we were unable to contact keen.io to find or create the review app project
            Logger.error(s"Error accessing Keen for Review apps error: $err")
            OriginalVars.ConfigVarsKeen.projectID == Vars.ConfigVarsKeen.projectID must_== true
            OriginalVars.ConfigVarsKeen.writeKey == Vars.ConfigVarsKeen.writeKey must_== true
            OriginalVars.ConfigVarsKeen.readKey == Vars.ConfigVarsKeen.readKey must_== true
            OriginalVars.ConfigVarsKeen.masterKey == Vars.ConfigVarsKeen.masterKey must_== true
            OriginalVars.ConfigVarsKeen.errorProjectKey == Vars.ConfigVarsKeen.errorProjectKey must_== false
            OriginalVars.ConfigVarsKeen.errorProjectID == Vars.ConfigVarsKeen.errorProjectID must_== false
            OriginalVars.ConfigVarsKeen.orgID == Vars.ConfigVarsKeen.orgID must_== true
            OriginalVars.ConfigVarsKeen.orgKey == Vars.ConfigVarsKeen.orgKey must_== true

            OriginalVars.ConfigVarsApp.appName == Vars.ConfigVarsApp.appName must_== false
            OriginalVars.ConfigVarsApp.appName.length < Vars.ConfigVarsApp.appName.length must_== true
            Vars.ConfigVarsApp.appName.length > 1 must_== true
            OriginalVars.ConfigVarsApp.parentName == Vars.ConfigVarsApp.parentName must_== false
            OriginalVars.ConfigVarsApp.parentName.length < Vars.ConfigVarsApp.parentName.length must_== true
            Vars.ConfigVarsApp.parentName.length > 1 must_== true
            OriginalVars.ConfigVarsApp.domain.startsWith(Vars.ConfigVarsApp.appName) must_== false
            Vars.ConfigVarsApp.domain.equals(s"https://${Vars.ConfigVarsApp.appName}.herokuapp.com") must_== true

            val start = s"https://${Vars.ConfigVarsApp.appName}.herokuapp.com"
            OriginalVars.ConfigVarsCallbackUrls.ios.startsWith(start) must_== false
            OriginalVars.ConfigVarsCallbackUrls.android.startsWith(start) must_== false
            OriginalVars.ConfigVarsCallbackUrls.player.startsWith(start) must_== false
            Vars.ConfigVarsCallbackUrls.ios.startsWith(start) must_== true
            Vars.ConfigVarsCallbackUrls.android.startsWith(start) must_== true
            Vars.ConfigVarsCallbackUrls.player.startsWith(start) must_== true

            OriginalVars.ConfigVarsAdProviders.toString == Vars.ConfigVarsAdProviders.toString must_== true
            OriginalVars.ConfigVarsHeroku.toString == Vars.ConfigVarsHeroku.toString must_== true
            OriginalVars.ConfigVarsHmac.toString == Vars.ConfigVarsHmac.toString must_== true
            OriginalVars.ConfigVarsJunGroup.toString == Vars.ConfigVarsJunGroup.toString must_== true

          case _ =>
            OriginalVars.ConfigVarsKeen.projectID == Vars.ConfigVarsKeen.projectID must_== false
            OriginalVars.ConfigVarsKeen.writeKey == Vars.ConfigVarsKeen.writeKey must_== false
            OriginalVars.ConfigVarsKeen.readKey == Vars.ConfigVarsKeen.readKey must_== false
            OriginalVars.ConfigVarsKeen.masterKey == Vars.ConfigVarsKeen.masterKey must_== false
            OriginalVars.ConfigVarsKeen.errorProjectKey == Vars.ConfigVarsKeen.errorProjectKey must_== false
            OriginalVars.ConfigVarsKeen.errorProjectID == Vars.ConfigVarsKeen.errorProjectID must_== false
            OriginalVars.ConfigVarsKeen.orgID == Vars.ConfigVarsKeen.orgID must_== true
            OriginalVars.ConfigVarsKeen.orgKey == Vars.ConfigVarsKeen.orgKey must_== true

            OriginalVars.ConfigVarsApp.appName == Vars.ConfigVarsApp.appName must_== false
            OriginalVars.ConfigVarsApp.appName.length < Vars.ConfigVarsApp.appName.length must_== true
            Vars.ConfigVarsApp.appName.length > 1 must_== true
            OriginalVars.ConfigVarsApp.parentName == Vars.ConfigVarsApp.parentName must_== false
            OriginalVars.ConfigVarsApp.parentName.length < Vars.ConfigVarsApp.parentName.length must_== true
            Vars.ConfigVarsApp.parentName.length > 1 must_== true
            OriginalVars.ConfigVarsApp.domain.startsWith(Vars.ConfigVarsApp.appName) must_== false
            Vars.ConfigVarsApp.domain.equals(s"https://${Vars.ConfigVarsApp.appName}.herokuapp.com") must_== true

            val start = s"https://${Vars.ConfigVarsApp.appName}.herokuapp.com"
            OriginalVars.ConfigVarsCallbackUrls.ios.startsWith(start) must_== false
            OriginalVars.ConfigVarsCallbackUrls.android.startsWith(start) must_== false
            OriginalVars.ConfigVarsCallbackUrls.player.startsWith(start) must_== false
            Vars.ConfigVarsCallbackUrls.ios.startsWith(start) must_== true
            Vars.ConfigVarsCallbackUrls.android.startsWith(start) must_== true
            Vars.ConfigVarsCallbackUrls.player.startsWith(start) must_== true

            OriginalVars.ConfigVarsAdProviders.toString == Vars.ConfigVarsAdProviders.toString must_== true
            OriginalVars.ConfigVarsHeroku.toString == Vars.ConfigVarsHeroku.toString must_== true
            OriginalVars.ConfigVarsHmac.toString == Vars.ConfigVarsHmac.toString must_== true
            OriginalVars.ConfigVarsJunGroup.toString == Vars.ConfigVarsJunGroup.toString must_== true
        }
      }

    "Not a Review app, configuration vars set correctly in prod mode staging false" in
      new WithApplication(new ApplicationFake(Map("mode" -> play.api.Mode.Prod.toString, "staging" -> "false"))) {

        object Vars extends ConfigVars {}

        Environment.isReviewApp must_== false
        //remove header and staging from ConfigVarsApp as staging is the only thing that will be different
        OriginalVars.ConfigVarsApp.toString.split("\n").drop(3) sameElements Vars.ConfigVarsApp.toString.split("\n").drop(3) must_== true

        OriginalVars.ConfigVarsKeen.toString == Vars.ConfigVarsKeen.toString must_== true
        OriginalVars.ConfigVarsAdProviders.toString == Vars.ConfigVarsAdProviders.toString must_== true
        OriginalVars.ConfigVarsCallbackUrls.toString == Vars.ConfigVarsCallbackUrls.toString must_== true
        OriginalVars.ConfigVarsHeroku.toString == Vars.ConfigVarsHeroku.toString must_== true
        OriginalVars.ConfigVarsHmac.toString == Vars.ConfigVarsHmac.toString must_== true
        OriginalVars.ConfigVarsJunGroup.toString == Vars.ConfigVarsJunGroup.toString must_== true
        OriginalVars.ConfigVarsReporting.toString == Vars.ConfigVarsReporting.toString must_== true
      }
  }
}
