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
    "properly save a new App in the database" in new WithDB {
      withApp()(app => {
        app.name must beEqualTo(appName)
      })
    }
  }

  "App.findAll" should {
    "return a list of all apps associated with the distributor ID" in new WithDB {
      withDistributor(distributorID => {
        App.create(distributorID, "App 1")
        App.create(distributorID, "App 2")
        val apps = App.findAll(distributorID)
        apps.size must beEqualTo(2)
      })
    }
  }

  "App.update" should {
    "update the field(s) for a given App" in new WithDB{
      withApp()(app => {
        val newAppName = "New App Name"
        val updatedAppClass = new App(app.id, false, app.distributorID, newAppName)
        App.update(updatedAppClass)
        val updatedApp = App.find(app.id).get
        updatedApp.name must beEqualTo(newAppName)
        updatedApp.active must beEqualTo(false)
      })
    }
  }
  step(clean)
}

