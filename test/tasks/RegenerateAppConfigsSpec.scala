package tasks

import models._
import play.api.db.DB
import play.api.Play.current
import play.api.test.FakeApplication
import play.api.test.Helpers._
import resources.{AppCreationHelper, SpecificationWithFixtures, DistributorUserSetup}
import scala.concurrent.duration.Duration

class RegenerateAppConfigsSpec extends SpecificationWithFixtures with AppCreationHelper with DistributorUserSetup {
  running(FakeApplication(additionalConfiguration = testDB)) {
    AdProvider.loadAll()
  }

  val (app1, app2) = running(FakeApplication(additionalConfiguration = testDB)) {
    def setup(email: String, appName: String) = {
      val (_, distributor) = newDistributorUser(email)
      val (app, waterfall, _, _) = setUpApp(distributorID = distributor.id.get, appName = Some(appName))
      Waterfall.update(id = waterfall.id, optimizedOrder = waterfall.optimizedOrder, testMode = false, paused = false)
      WaterfallAdProvider.create(
        waterfallID = waterfall.id,
        adProviderID = 2,
        waterfallOrder = None,
        cpm = None,
        configurable = true,
        active = true
      ).get
      DB.withTransaction { implicit connection =>
        AppConfig.createWithWaterfallIDInTransaction(waterfall.id, None).get
      }
      app
    }
    (setup("user1@gmail.com", "App1"), setup("user2@gmail.com", "App2"))
  }

  "run" should {
    "update the app config for all apps that have changes" in new WithDB {
      val originalGenerationNumber1 = generationNumber(app1.id)
      val originalGenerationNumber2 = generationNumber(app2.id)

      List(app1, app2).map { app =>
        App.update(
          new UpdatableApp(
            id = app.id,
            active = true,
            distributorID = app.distributorID,
            name = "New App Name" + app.id,
            callbackURL = None,
            serverToServerEnabled = false
          )
        ) must beEqualTo(1)
      }

      RegenerateAppConfigs.run()

      List(
        (app1, originalGenerationNumber1 + 1),
        (app2, originalGenerationNumber2 + 1)
      ).map { appInfo =>
        val app = appInfo._1
        val expectedGenerationNumber = appInfo._2

        generationNumber(app.id) must beEqualTo(expectedGenerationNumber)
          .eventually(3, Duration(1000, "millis"))

        val newAppNameFromConfig = (AppConfig.findLatest(app.token).get.configuration \ "appName").as[String]
        val newAppNameFromDB = App.find(app.id).get.name

        newAppNameFromConfig must beEqualTo(newAppNameFromDB)
      }
    }
  }
}
