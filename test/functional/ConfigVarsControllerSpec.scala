package functional


import models.Constants
import org.specs2.mock.Mockito
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import resources.{ApplicationFake, SpecificationWithFixtures}
import scala.concurrent.Future

class ConfigVarsControllerSpec extends SpecificationWithFixtures with Results with Mockito {

  "ConfigVarsContoller" should {
    "not return config vars if not review app" in {
      running(FakeApplication()) {
        val appEnv = mock[models.Environment]
        val fakeConfigVars = mock[models.ConfigVars]
        appEnv.isReviewApp returns false
        lazy val configVarsController = new controllers.ConfigVarsController(configVars, appEnv)
        val result: Future[Result] = configVarsController.serveConfigVars.apply(FakeRequest())
        val bodyText: String = contentAsString(result)
        bodyText must beEqualTo("mediation-staging is not a review app")
      }
    }

    "isReviewApp true in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.HerokuConfigVars.AppName -> "mediation-test-pr-000",
        Constants.HerokuConfigVars.ParentName -> "it-test"))) {

        val appEnv = mock[models.Environment]
        val fakeConfigVars = mock[models.ConfigVars]
        appEnv.isReviewApp returns true
        lazy val configVarsController = new controllers.ConfigVarsController(configVars, appEnv)
        val result: Future[Result] = configVarsController.serveConfigVars.apply(FakeRequest())
        val bodyText: String = contentAsString(result)
        bodyText.contains("APPLICATION CONFIG") must beTrue
      }
  }
}
