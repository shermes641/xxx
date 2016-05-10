package models

import org.specs2.mutable.Specification
import play.api.test.{FakeApplication, WithApplication}

class GlobalSpec extends Specification {
  sequential

  class ApplicationFake(additionalConfig: Map[String, _ <: Any] = Map("mode" -> play.api.Mode.Test.toString))
    extends FakeApplication(additionalConfiguration = additionalConfig) {
    override val mode = additionalConfig.getOrElse("mode", "No Mode").toString.toLowerCase() match {
      case "dev" => play.api.Mode.Dev
      case "prod" => play.api.Mode.Prod
      case _ => play.api.Mode.Test
    }
  }

  "Global" should {
    "Review app fails to start with bad Keen organization ID" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.KeenConfig.OrgID -> "123skjlkj56",
        Constants.HerokuConfigVars.AppName -> "mediation-staging_pr_17",
        Constants.HerokuConfigVars.ParentName -> "mediation-staging"))) {

        Environment.isReviewApp must_== true
        Environment.TestErrorCode must_== Constants.Errors.KeenConfigError
      }

    "Review app fails to start with bad adprovider ios ID" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.AdProviderConfig.IosAdProviderID -> 0,
        Constants.HerokuConfigVars.AppName -> "mediation-staging_pr_17",
        Constants.HerokuConfigVars.ParentName -> "mediation-staging"))) {

        Environment.isReviewApp must_== true
        Environment.TestErrorCode must_== Constants.Errors.AdProviderError
      }

    "Review app fails to start with bad Keen organization Key" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.KeenConfig.OrgKey -> "123skjlkj56",
        Constants.HerokuConfigVars.AppName -> "mediation-staging_pr_17",
        Constants.HerokuConfigVars.ParentName -> "mediation-staging"))) {

        Environment.isReviewApp must_== true
        Environment.TestErrorCode must_== Constants.Errors.KeenConfigError
      }

    "Review app fails to start with bad adprovider android ID" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.AdProviderConfig.AndroidAdProviderID -> 0,
        Constants.HerokuConfigVars.AppName -> "mediation-staging_pr_17",
        Constants.HerokuConfigVars.ParentName -> "mediation-staging"))) {

        Environment.isReviewApp must_== true
        Environment.TestErrorCode must_== Constants.Errors.AdProviderError
      }

    "Staging app starts with bad Keen organization ID" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.KeenConfig.OrgID -> "123skjlkj56",
        Constants.HerokuConfigVars.AppName -> "mediation-staging"))) {

        Environment.isStaging must_== true
        Environment.isReviewApp must_== false
        Environment.TestErrorCode must_== 0
      }

    "Staging app fails to start with bad adprovider ios ID" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.AdProviderConfig.IosAdProviderID -> 0,
        Constants.HerokuConfigVars.AppName -> "mediation-staging"))) {

        Environment.isStaging must_== true
        Environment.isReviewApp must_== false
        Environment.TestErrorCode must_== Constants.Errors.AdProviderError
      }

    "Staging app starts with bad Keen organization Key" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.KeenConfig.OrgKey -> "123skjlkj56",
        Constants.HerokuConfigVars.AppName -> "mediation-staging"))) {

        Environment.isStaging must_== true
        Environment.isReviewApp must_== false
        Environment.TestErrorCode must_== 0
      }

    "Staging app fails to start with bad adprovider android ID" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.AdProviderConfig.AndroidAdProviderID -> 0,
        Constants.HerokuConfigVars.AppName -> "mediation-staging"))) {

        Environment.isStaging must_== true
        Environment.isReviewApp must_== false
        Environment.TestErrorCode must_== Constants.Errors.AdProviderError
      }
  }
}
