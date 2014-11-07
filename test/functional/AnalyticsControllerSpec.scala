package functional

import models._
import play.api.test._
import play.api.test.Helpers._
import com.github.nscala_time.time._
import com.github.nscala_time.time.Imports._
import play.api.test.FakeApplication
import play.api.Play
import io.keen.client.java.ScopedKeys
import collection.JavaConversions._

class AnalyticsControllerSpec extends SpecificationWithFixtures {

  val distributorUser = running(FakeApplication(additionalConfiguration = testDB)) {
    DistributorUser.create(email, password, "Company Name")
    DistributorUser.findByEmail(email).get
  }

  val distributorID = distributorUser.distributorID.get

  "Analytics show action" should {
    def userLogin(browser: TestBrowser) = {
      browser.goTo(controllers.routes.DistributorUsersController.login().url)
      browser.fill("#email").`with`(email)
      browser.fill("#password").`with`(password)
      browser.click("button")
    }

    "show analytics for an app" in new WithFakeBrowser {
      val app2Name = "App 2"
      val appID = App.create(distributorID, app2Name).get

      userLogin(browser)

      browser.goTo(controllers.routes.AnalyticsController.show(distributorID, Some(appID)).url)
      browser.pageSource must contain("Analytics")
    }

    "populate ad networks for show page" in new WithFakeBrowser {
      val app2Name = "App 2"
      val appID = App.create(distributorID, app2Name).get

      userLogin(browser)

      val adProviderID = AdProvider.create("hyprMX", "{\"required_params\":[{\"description\": \"Your Vungle App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}]}", None)
      browser.goTo(controllers.routes.AnalyticsController.show(distributorID, Some(appID)).url)

      // Verify first option defaults to "all"
      browser.$("#ad_providers").getValue() must beEqualTo("all")
    }

    "country select box should exist and not be empty" in new WithFakeBrowser {
      val app2Name = "App 2"
      val appID = App.create(distributorID, app2Name).get

      userLogin(browser)

      browser.goTo(controllers.routes.AnalyticsController.show(distributorID, Some(appID)).url)

      // Verify first option defaults to "all"
      browser.$("#countries").getValue() must beEqualTo("all")
    }

    "date picker should be setup correctly" in new WithFakeBrowser {
      val app2Name = "App 2"
      val appID = App.create(distributorID, app2Name).get

      userLogin(browser)

      browser.goTo(controllers.routes.AnalyticsController.show(distributorID, Some(appID)).url)

      var date = DateTime.now
      // End date must be todays date
      browser.$("#end_date").getValue() must beEqualTo(date.toString("MM/dd/yyyy"))

      // Start date must be todays date minus 1 month
      browser.$("#start_date").getValue() must beEqualTo(date.minusMonths(1).toString("MM/dd/yyyy"))
    }

    "app ID should be set correctly in hidden field" in new WithFakeBrowser {
      val app2Name = "App 2"
      val appID = App.create(distributorID, app2Name).get

      userLogin(browser)

      browser.goTo(controllers.routes.AnalyticsController.show(distributorID, Some(appID)).url)

      // eCPM must be set correctly (placeholder for now)
      browser.$("#app_id").getValue() must beEqualTo(appID.toString)
    }

    "Keen project should be set correctly in hidden field" in new WithFakeBrowser {
      val app2Name = "App 2"
      val appID = App.create(distributorID, app2Name).get

      userLogin(browser)
      browser.goTo(controllers.routes.AnalyticsController.show(distributorID, Some(appID)).url)

      // eCPM must be set correctly (placeholder for now)
      browser.$("#keen_project").getValue() must beEqualTo(Play.current.configuration.getString("keen.project").get)
    }

    "Scoped key has been created correctly" in new WithFakeBrowser {
      val app2Name = "App 2"
      val appID = App.create(distributorID, app2Name).get

      userLogin(browser)

      browser.goTo(controllers.routes.AnalyticsController.show(distributorID, Some(appID)).url)

      val decrypted = ScopedKeys.decrypt(Play.current.configuration.getString("keen.masterKey").get, browser.$("#scoped_key").getValue()).toMap.toString()
      decrypted must contain("property_value="+distributorID.toString)
    }


    "Javascript Unit Tests" in new WithFakeBrowser {
      browser.goTo("/assets/javascripts/test/index.html")

      browser.await().atMost(10, java.util.concurrent.TimeUnit.SECONDS).until("#qunit-testresult").containsText("completed");
      browser.pageSource must contain("qunit-pass")
    }
  }

  step(clean)
}
