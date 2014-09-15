package models

import anorm._
import play.api.Play.current
import play.api.db.DB
import play.api.libs.json.{JsObject, JsString}
import play.api.test.Helpers._
import play.api.test._

trait WaterfallSpecSetup extends SpecificationWithFixtures {
  val distributor = running(FakeApplication(additionalConfiguration = testDB)) {
    val distributorID = Distributor.create("Company Name").get
    Distributor.find(distributorID).get
  }

  val app1 = running(FakeApplication(additionalConfiguration = testDB)) {
    val appID = App.create(distributor.id.get, "App 1").get
    App.find(appID).get
  }

  val waterfall = running(FakeApplication(additionalConfiguration = testDB)) {
    val waterfallID = Waterfall.create(app1.id, app1.name).get
    Waterfall.find(waterfallID)
  }

  val adProviders = List("test ad provider 1", "test ad provider 2")

  val adProviderID1 = running(FakeApplication(additionalConfiguration = testDB)) {
    DB.withConnection { implicit connection =>
      SQL("insert into ad_providers (name) values ({name})").on("name" -> adProviders(0)).executeInsert()
    }
  }

  val adProviderID2 = running(FakeApplication(additionalConfiguration = testDB)) {
    DB.withConnection { implicit connection =>
      SQL("insert into ad_providers (name) values ({name})").on("name" -> adProviders(1)).executeInsert()
    }
  }
}
