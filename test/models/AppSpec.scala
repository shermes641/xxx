package models

import org.junit.runner._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class AppSpec extends SpecificationWithFixtures {
  val appName = "App 1"

  def withDistributor[A](test: Long => A): Long = {
    val distributorID = Distributor.create("Company Name")
    distributorID.get
  }

  def withApp[A](appName: String = appName, distributorID: Long = withDistributor(_ => {}))(test: App => A): App = {
    val appID = App.create(distributorID, appName)
    val app = App.find(appID.get)
    app.get
  }

  "App.create" should {
    "properly save a new App in the database" in new WithFakeDB {
      withApp()(app => {
        app.name must beEqualTo(appName)
      })
    }
  }

  "App.findAll" should {
    "return a list of all apps associated with the distributor ID" in new WithFakeDB {
      withDistributor(distributorID => {
        App.create(distributorID, "App 1")
        App.create(distributorID, "App 2")
        val apps = App.findAll(distributorID)
        apps.size must beEqualTo(2)
      })
    }
  }

  "App.update" should {
    "update the field(s) for a given App" in new WithFakeDB{
      withApp()(app => {
        val newAppName = "New App Name"
        val updatedApp = new App(app.id, app.distributorID, newAppName)
        App.update(updatedApp)
        App.find(app.id).get.name must beEqualTo(newAppName)
      })
    }
  }

  "App.destroy" should {
    "remove the App from the database" in new WithFakeDB {
      withApp()(app => {
        App.destroy(app.id) must beEqualTo(1)
      })
    }
  }
}

