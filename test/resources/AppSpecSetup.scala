package models

import anorm._
import play.api.Play.current
import play.api.db.DB
import play.api.test.Helpers._
import play.api.test._

trait AppSpecSetup extends SpecificationWithFixtures {
  val distributor = running(FakeApplication(additionalConfiguration = testDB)) {
    val distributorID = Distributor.create("Company Name").get
    Distributor.find(distributorID).get
  }

  val currentApp = running(FakeApplication(additionalConfiguration = testDB)) {
    val appID = App.create(distributor.id.get, "App Name").get
    App.find(appID).get
  }
}
