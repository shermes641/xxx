package models

import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.test.FakeApplication
import play.api.test.Helpers._
import org.junit.runner._
import org.specs2.mock.Mockito
import org.specs2.runner._
import com.github.tototoshi.csv._
import akka.testkit.TestActorRef
import resources.{DistributorUserSetup}
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import io.keen.client.java.{KeenClient, KeenProject, JavaKeenClientBuilder}
import play.api.Play

@RunWith(classOf[JUnitRunner])
class KeenExportSpec extends SpecificationWithFixtures with DistributorUserSetup with AppCreationHelper with Mockito {
  implicit val actorSystem = ActorSystem("testActorSystem", ConfigFactory.load())

  "GetDataFromKeen" should {

    "Returns the correct filters" in new WithDB {
      val collection_name = "test_collection"
      val property_name = "property_name"
      val property_value = 123123
      val filters = models.KeenExport().createFilter(collection_name, 123123, property_name)
      (filters \ "event_collection").as[String] must beEqualTo(collection_name)
      (filters \ "target_property").as[String] must beEqualTo(property_name)
      ((filters \ "filters").as[JsArray].as[List[JsObject]].head \ "property_value").as[String].toLong must beEqualTo(property_value)
    }

    "Builds CSV correctly" in new WithDB {
      val email = "test@jungroup.com"
      val (newUser, newDistributor) = newDistributorUser(email, "password", "company")
      val client = new JavaKeenClientBuilder().build()
      val project = new KeenProject(Play.current.configuration.getString("keen.project").get, Play.current.configuration.getString("keen.writeKey").get, Play.current.configuration.getString("keen.readKey").get)
      client.setDefaultProject(project)
      KeenClient.initialize(client)

      val keenExportActor = TestActorRef(new KeenExportActor(newDistributor.id.get, email)).underlyingActor
      setUpApp(newDistributor.id.get)
      val appList = App.findAllAppsWithWaterfalls(newDistributor.id.get)

      val writer = keenExportActor.createCSVFile()
      keenExportActor.GetData(appList, writer)
    }

    "Builds App Row correctly" in new WithDB {
      val email = "test2@jungroup.com"
      val (newUser, newDistributor) = newDistributorUser(email, "password", "company")
      val client = new JavaKeenClientBuilder().build()
      val project = new KeenProject(Play.current.configuration.getString("keen.project").get, Play.current.configuration.getString("keen.writeKey").get, Play.current.configuration.getString("keen.readKey").get)
      client.setDefaultProject(project)
      KeenClient.initialize(client)

      val keenExportActor = TestActorRef(new KeenExportActor(newDistributor.id.get, email)).underlyingActor
      setUpApp(newDistributor.id.get)
      val appList = App.findAllAppsWithWaterfalls(newDistributor.id.get)

      val requestsResponse = mock[WSResponse]
      val responsesResponse = mock[WSResponse]
      val impressionsResponse = mock[WSResponse]
      val completionsResponse = mock[WSResponse]
      val earningsResponse = mock[WSResponse]
      requestsResponse.body returns "{\"result\": 100}"
      responsesResponse.body returns "{\"result\": 50}"
      impressionsResponse.body returns "{\"result\": 30}"
      completionsResponse.body returns "{\"result\": 15}"
      earningsResponse.body returns "{\"result\": 20}"

      var appRow = keenExportActor.buildAppRow("App Name", "iOS", requestsResponse, responsesResponse, impressionsResponse, completionsResponse, earningsResponse)
      appRow must beEqualTo(List("App Name", "iOS", 20, 0.5, 100, 30, 15, 0.5))

      requestsResponse.body returns "{\"result\": 0}"
      responsesResponse.body returns "{\"result\": 50}"
      impressionsResponse.body returns "{\"result\": 0}"
      completionsResponse.body returns "{\"result\": 15}"
      earningsResponse.body returns "{\"result\": 20}"

      var appRowByZero = keenExportActor.buildAppRow("App Name", "iOS", requestsResponse, responsesResponse, impressionsResponse, completionsResponse, earningsResponse)
      appRowByZero must beEqualTo(List("App Name", "iOS", 20, 0.0, 0, 0, 15, 0.0))
    }

    "Can parse Keen responses correctly" in new WithDB {
      val email = "test3@jungroup.com"
      val (newUser, newDistributor) = newDistributorUser(email, "password", "company")
      val client = new JavaKeenClientBuilder().build()
      val project = new KeenProject(Play.current.configuration.getString("keen.project").get, Play.current.configuration.getString("keen.writeKey").get, Play.current.configuration.getString("keen.readKey").get)
      client.setDefaultProject(project)
      KeenClient.initialize(client)

      val keenExportActor = TestActorRef(new KeenExportActor(newDistributor.id.get, email)).underlyingActor
      setUpApp(newDistributor.id.get)
      val appList = App.findAllAppsWithWaterfalls(newDistributor.id.get)

      keenExportActor.parseResponse("{\"result\": 123456}") must beEqualTo(123456)
      keenExportActor.parseResponse("{\"result\": 3333333}") must beEqualTo(3333333)
    }
  }
}
