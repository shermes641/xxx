package functional


import controllers.ConfigVarsController
import models.Constants

import scala.concurrent.Future
import org.scalatestplus.play._
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import resources.ApplicationFake

class ConfigVarsControllerSpec extends PlaySpec with Results {

  "ConfigVarsContoller" should {
    "not return config vars if not review app" in {
      running(FakeApplication()) {
        val result: Future[Result] = ConfigVarsController.serveConfigVars.apply(FakeRequest())
        val bodyText: String = contentAsString(result)
        bodyText mustBe "mediation-staging is not a review app"
      }
    }

    "isReviewApp true in test mode staging true" in
      new WithApplication(new ApplicationFake(Map(Constants.AppConfig.Mode -> play.api.Mode.Test.toString,
        Constants.AppConfig.Staging -> "true",
        Constants.HerokuConfigVars.AppName -> "mediation-test-pr-000",
        Constants.HerokuConfigVars.ParentName -> "it-test"))) {
        val result: Future[Result] = ConfigVarsController.serveConfigVars.apply(FakeRequest())
        val bodyText: String = contentAsString(result)
        bodyText.contains("APPLICATION CONFIG") mustBe true
      }
  }
}
