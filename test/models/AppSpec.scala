package models

import anorm._
import org.junit.runner._
import org.specs2.runner._
import play.api.db.DB
import play.api.libs.json.{JsNumber, JsObject}
import resources.{DistributorUserSetup, WaterfallSpecSetup}

@RunWith(classOf[JUnitRunner])
class AppSpec extends SpecificationWithFixtures with WaterfallSpecSetup with DistributorUserSetup {
  val appName = "App 1"

  "App.findAppWithVirtualCurrency" should {
    "return an instance of the AppWithVirtualCurrency class" in new WithAppDB(distributor.id.get) {
      App.findAppWithVirtualCurrency(currentApp.id, distributor.id.get).get must haveClass[AppWithVirtualCurrency]
    }

    "return attributes for both App and VirtualCurrency" in new WithAppDB(distributor.id.get) {
      val appWithCurrency = App.findAppWithVirtualCurrency(currentApp.id, distributor.id.get).get
      appWithCurrency.appName must beEqualTo(currentApp.name)
      appWithCurrency.currencyID must beEqualTo(currentVirtualCurrency.id)
      appWithCurrency.currencyName must beEqualTo(currentVirtualCurrency.name)
    }
  }

  "App.findAllAppsWithWaterfalls" should {
    "return a list of Apps containing the Waterfall ID for each App" in new WithDB {
      val (newUser, newDistributor) = newDistributorUser("newemail@gmail.com")
      val appID = App.create(newDistributor.id.get, "New App").get
      val waterfallID = DB.withTransaction { implicit connection => Waterfall.create(appID, "New Waterfall") }.get
      val appWithWaterfallID = App.findAllAppsWithWaterfalls(newDistributor.id.get)(0)
      appWithWaterfallID.id must beEqualTo(appID)
      appWithWaterfallID.waterfallID must beEqualTo(waterfallID)
    }

    "return an empty list if no Apps could be found" in new WithDB {
      val fakeDistributorID = 12345
      App.findAllAppsWithWaterfalls(fakeDistributorID).size must beEqualTo(0)
    }
  }

  "App.findAppWithWaterfalls" should {
    "return an App containing the Waterfall ID for the App" in new WithAppDB(distributor.id.get) {
      val appWithWaterfallID = App.findAppWithWaterfalls(currentApp.id, distributor.id.get).get
      appWithWaterfallID.id must beEqualTo(currentApp.id)
      appWithWaterfallID.waterfallID must beEqualTo(currentWaterfall.id)
    }

    "return an empty list if no Apps could be found" in new WithDB {
      val fakeAppID = 12345
      App.findAppWithWaterfalls(fakeAppID, distributor.id.get) must beNone
    }
  }

  "App.create" should {
    "properly save a new App in the database" in new WithDB {
      val appID = App.create(distributor.id.get, appName).get
      App.find(appID).get.name must beEqualTo(appName)
    }

    "should set the app_config_refresh_interval to 0 by default" in new WithDB {
      val appID = App.create(distributor.id.get, randomAppName).get
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
    "return a list of all apps associated with the Distributor ID" in new WithDB {
      val newEmail = "testemail@mail.com"
      val (_, currentDistributor) = newDistributorUser(newEmail)
      App.create(currentDistributor.id.get, "App 1")
      App.create(currentDistributor.id.get, "App 2")
      val apps = App.findAll(currentDistributor.id.get)
      apps.size must beEqualTo(2)
    }

    "return an empty list if the Distributor ID is not found" in new WithDB {
      val fakeDistributorID = 12345
      App.findAll(fakeDistributorID).size must beEqualTo(0)
    }
  }

  "App.find" should {
    "return an instance of the App class if the ID is found" in new WithAppDB(distributor.id.get) {
      App.find(currentApp.id).get must haveClass[App]
    }

    "return None if the App ID is not found" in new WithDB {
      val fakeAppID = 12345
      App.find(fakeAppID) must beNone
    }
  }

  "App.findByWaterfallID" should {
    "return an instance of the App class if the Waterfall ID is found" in new WithAppDB(distributor.id.get) {
      App.findByWaterfallID(currentWaterfall.id).get must beEqualTo(currentApp)
    }

    "return None if the Waterfall ID is not found" in new WithDB {
      val fakeWaterfallID = 12345
      App.findByWaterfallID(fakeWaterfallID) must beNone
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
    "update the appConfigRefreshInterval attribute for an App" in new WithAppDB(distributor.id.get) {
      val newAppConfigRefreshInterval = 500
      Waterfall.update(currentWaterfall.id, optimizedOrder = false, testMode = false, paused = false)
      WaterfallAdProvider.create(currentWaterfall.id, adProviderID1.get, None, Some(5.0), true, true)
      DB.withTransaction { implicit connection => AppConfig.createWithWaterfallIDInTransaction(currentWaterfall.id, None) }
      val originalConfig = AppConfig.findLatest(currentApp.token).get
      val originalConfigData = originalConfig.configuration.as[JsObject]
      (originalConfigData \ "appConfigRefreshInterval").as[JsNumber].as[Long] must beEqualTo(0)
      App.updateAppConfigRefreshInterval(currentApp.id, newAppConfigRefreshInterval) must beEqualTo(true)
      val latestConfig = AppConfig.findLatest(currentApp.token).get
      latestConfig.generationNumber must beEqualTo(originalConfig.generationNumber + 1)
      (latestConfig.configuration.as[JsObject] \ "appConfigRefreshInterval").as[JsNumber].as[Long] must beEqualTo(newAppConfigRefreshInterval)
    }

    "not update the appConfigRefreshInterval if the App is in test mode" in new WithAppDB(distributor.id.get) {
      WaterfallAdProvider.create(currentWaterfall.id, adProviderID1.get, None, Some(5.0), true, true)
      DB.withTransaction { implicit connection => AppConfig.createWithWaterfallIDInTransaction(currentWaterfall.id, None) }
      val originalConfig = AppConfig.findLatest(currentApp.token).get
      App.updateAppConfigRefreshInterval(currentApp.id, 500) must beEqualTo(false)
      val latestConfig = AppConfig.findLatest(currentApp.token).get
      latestConfig.generationNumber must beEqualTo(originalConfig.generationNumber)
      latestConfig.configuration.as[JsObject] must beEqualTo(originalConfig.configuration)
      val originalRefreshInterval = (originalConfig.configuration.as[JsObject] \ "appConfigRefreshInterval").as[JsNumber].as[Long]
      val latestRefreshInterval = (latestConfig.configuration.as[JsObject] \ "appConfigRefreshInterval").as[JsNumber].as[Long]
      originalRefreshInterval must beEqualTo(latestRefreshInterval)
    }

    "return false if the App ID is not found" in new WithDB {
      val fakeAppID = 12345
      App.updateAppConfigRefreshInterval(fakeAppID, 500) must beEqualTo(false)
    }
  }
}

