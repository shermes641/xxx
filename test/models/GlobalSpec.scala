package models

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.Configuration
import play.api.test.{FakeApplication, WithApplication}

class GlobalSpec extends Specification with Mockito {
  sequential

  class ApplicationFake(additionalConfig: Map[String, _ <: Any] = Map("mode" -> play.api.Mode.Test.toString))
    extends FakeApplication(additionalConfiguration = additionalConfig) {
    override val mode = additionalConfig.getOrElse("mode", "No Mode").toString.toLowerCase() match {
      case "dev" => play.api.Mode.Dev
      case "prod" => play.api.Mode.Prod
      case _ => play.api.Mode.Test
    }
  }

  val prodEnv = play.api.Environment.simple(mode = play.api.Mode.Prod)
  val devEnv = play.api.Environment.simple(mode = play.api.Mode.Dev)
  val testEnv = play.api.Environment.simple(mode = play.api.Mode.Test)

  def appEnv(config: Configuration, env: play.api.Environment): Environment = {
    val appMode = new AppMode(env)
    new models.Environment(config, appMode)
  }

  "Global" should {
    "Review app fails to start with bad Keen organization ID" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.KeenConfig.OrgID -> "123skjlkj56",
        Constants.HerokuConfigVars.AppName -> "mediation-test-pr-000",
        Constants.HerokuConfigVars.ParentName -> "mediation-staging"))) {

        val appEnvironment = appEnv(app.configuration, testEnv)
        val currentConfig = new ConfigVars(app.configuration, appEnvironment)
        Startup.beforeStart(currentConfig, appEnvironment)

        appEnvironment.isReviewApp must_== true
        appEnvironment.TestErrorCode must_== Constants.Errors.KeenConfigError
      }

    "Review app fails to start with bad adprovider ios ID" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.AdProviderConfig.IosAdProviderID -> 0,
        Constants.HerokuConfigVars.AppName -> "mediation-test-pr-000",
        Constants.HerokuConfigVars.ParentName -> "mediation-staging"))) {

        val appEnvironment = appEnv(app.configuration, testEnv)
        val currentConfig = new ConfigVars(app.configuration, appEnvironment)
        Startup.beforeStart(currentConfig, appEnvironment)

        appEnvironment.isReviewApp must_== true
        appEnvironment.TestErrorCode must_== Constants.Errors.AdProviderError
      }

    "Review app fails to start with bad Keen organization Key" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.KeenConfig.OrgKey -> "123skjlkj56",
        Constants.HerokuConfigVars.AppName -> "mediation-test-pr-000",
        Constants.HerokuConfigVars.ParentName -> "mediation-staging"))) {

        val appEnvironment = appEnv(app.configuration, testEnv)
        val currentConfig = new ConfigVars(app.configuration, appEnvironment)
        Startup.beforeStart(currentConfig, appEnvironment)

        appEnvironment.isReviewApp must_== true
        appEnvironment.TestErrorCode must_== Constants.Errors.KeenConfigError
      }

    "Review app fails to start with bad adprovider android ID" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.AdProviderConfig.AndroidAdProviderID -> 0,
        Constants.HerokuConfigVars.AppName -> "mediation-test-pr-000",
        Constants.HerokuConfigVars.ParentName -> "mediation-staging"))) {

        val appEnvironment = appEnv(app.configuration, testEnv)
        val currentConfig = new ConfigVars(app.configuration, appEnvironment)
        Startup.beforeStart(currentConfig, appEnvironment)

        appEnvironment.isReviewApp must_== true
        appEnvironment.TestErrorCode must_== Constants.Errors.AdProviderError
      }

    "Staging app starts with bad Keen organization ID" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.KeenConfig.OrgID -> "123skjlkj56",
        Constants.HerokuConfigVars.AppName -> "mediation-staging"))) {

        val appEnvironment = appEnv(app.configuration, prodEnv)
        val currentConfig = new ConfigVars(app.configuration, appEnvironment)
        Startup.beforeStart(currentConfig, appEnvironment)

        appEnvironment.isStaging must_== true
        appEnvironment.isReviewApp must_== false
        appEnvironment.TestErrorCode must_== 0
      }

    "Staging app fails to start with bad adprovider ios ID" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.AdProviderConfig.IosAdProviderID -> 0,
        Constants.HerokuConfigVars.AppName -> "mediation-staging"))) {

        val appEnvironment = appEnv(app.configuration, prodEnv)
        val currentConfig = new ConfigVars(app.configuration, appEnvironment)
        Startup.beforeStart(currentConfig, appEnvironment)

        appEnvironment.isStaging must_== true
        appEnvironment.isReviewApp must_== false
        appEnvironment.TestErrorCode must_== Constants.Errors.AdProviderError
      }

    "Staging app starts with bad Keen organization Key" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.KeenConfig.OrgKey -> "123skjlkj56",
        Constants.HerokuConfigVars.AppName -> "mediation-staging"))) {

        val appEnvironment = appEnv(app.configuration, prodEnv)
        val currentConfig = new ConfigVars(app.configuration, appEnvironment)
        Startup.beforeStart(currentConfig, appEnvironment)

        appEnvironment.isStaging must_== true
        appEnvironment.isReviewApp must_== false
        appEnvironment.TestErrorCode must_== 0
      }

    "Staging app fails to start with bad adprovider android ID" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Prod.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.AdProviderConfig.AndroidAdProviderID -> 0,
        Constants.HerokuConfigVars.AppName -> "mediation-staging"))) {

        val appEnvironment = appEnv(app.configuration, prodEnv)
        val currentConfig = new ConfigVars(app.configuration, appEnvironment)
        Startup.beforeStart(currentConfig, appEnvironment)

        appEnvironment.isStaging must_== true
        appEnvironment.isReviewApp must_== false
        appEnvironment.TestErrorCode must_== Constants.Errors.AdProviderError
      }
  }
}
