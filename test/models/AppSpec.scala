package models

import anorm._
import org.junit.runner._
import org.specs2.runner._
import play.api.db.DB
import play.api.libs.json.{JsNumber, JsObject}
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class AppSpec extends SpecificationWithFixtures with WaterfallSpecSetup {
  val appName = "App 1"

  val distributorID = running(FakeApplication(additionalConfiguration = testDB)) {
    DistributorUser.create(email, password, companyName)
    val user = DistributorUser.findByEmail(email).get
    user.distributorID.get
  }

  "App.create" should {
    "properly save a new App in the database" in new WithDB {
      val appID = App.create(distributorID, appName).get
      App.find(appID).get.name must beEqualTo(appName)
    }

    "should set the app_config_refresh_interval to 0 by default" in new WithDB {
      val appID = App.create(distributorID, appName).get
      val appConfigRefreshInterval = DB.withConnection { implicit connection =>
        SQL(
          """
             SELECT app_config_refresh_interval FROM apps WHERE id={id};
          """
        ).on("id" -> appID).apply().head
      }
      appConfigRefreshInterval[Long]("app_config_refresh_interval") must beEqualTo(0)
    }
  }

  "App.findAll" should {
    "return a list of all apps associated with the distributor ID" in new WithDB {
      val newEmail = "testemail@mail.com"
      DistributorUser.create(newEmail, "password", "New Company")
      val user = DistributorUser.findByEmail(newEmail).get
      val newDistributorID = user.distributorID.get
      App.create(newDistributorID, "App 1")
      App.create(newDistributorID, "App 2")
      val apps = App.findAll(newDistributorID)
      apps.size must beEqualTo(2)
    }
  }

  "App.update" should {
    "update the field(s) for a given App" in new WithDB {
      val newAppName = "New App Name"
      val updatedAppClass = new UpdatableApp(app1.id, false, app1.distributorID, newAppName, None, false)
      App.update(updatedAppClass)
      val updatedApp = App.find(app1.id).get
      updatedApp.name must beEqualTo(newAppName)
      updatedApp.active must beEqualTo(false)
    }
  }

  "App.updateAppConfigRefreshInterval" should {
    "update the appConfigRefreshInterval attribute for an App" in new WithDB {
      val newAppConfigRefreshInterval = 500
      val currentApp = App.find(App.create(distributorID, appName).get).get
      VirtualCurrency.create(currentApp.id, "Coins", 100, None, None, Some(true))
      val waterfallID = DB.withTransaction { implicit connection => createWaterfallWithConfig(currentApp.id, appName) }
      Waterfall.update(waterfallID, false, false)
      WaterfallAdProvider.create(waterfallID, adProviderID1.get, None, Some(5.0), true, true)
      DB.withTransaction { implicit connection => AppConfig.createWithWaterfallIDInTransaction(waterfallID, None) }
      val originalConfig = AppConfig.findLatest(currentApp.token).get
      val originalConfigData = originalConfig.configuration.as[JsObject]
      (originalConfigData \ "appConfigRefreshInterval").as[JsNumber].as[Long] must beEqualTo(0)
      App.updateAppConfigRefreshInterval(currentApp.id, newAppConfigRefreshInterval)
      val latestConfig = AppConfig.findLatest(currentApp.token).get
      latestConfig.generationNumber must beEqualTo(originalConfig.generationNumber + 1)
      (latestConfig.configuration.as[JsObject] \ "appConfigRefreshInterval").as[JsNumber].as[Long] must beEqualTo(newAppConfigRefreshInterval)
    }
  }
  step(clean)
}

