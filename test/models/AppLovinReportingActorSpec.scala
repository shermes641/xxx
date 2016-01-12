package models

import akka.actor.Props
import org.specs2.mock.Mockito
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.concurrent.Future
import scala.collection.mutable.ListBuffer


class AppLovinReportingActorSpec extends SpecificationWithFixtures with Mockito {
  "AppLovinReportingActor" should {
    "call the updateRevenueData function for an AppLovin WaterfallAdProvider" in new WithDB {
      val appLovinActor = Akka.system(current).actorOf(Props(new AppLovinReportingActor))
      val apiCall = mock[AppLovinReportingAPI]
      apiCall.updateRevenueData returns Future { Option(1L) }
      appLovinActor ! apiCall
      eventually { there was one(apiCall).updateRevenueData }
    }

    "not call the updateRevenueData function for any ad provider other than AppLovin" in new WithDB {
      val appLovinActor = Akka.system(current).actorOf(Props(new AppLovinReportingActor))
      val reportingAPIs = List(mock[VungleReportingAPI], mock[AdColonyReportingAPI], mock[HyprMarketplaceReportingAPI])
      reportingAPIs.foreach { apiCall =>
        apiCall.updateRevenueData returns Future { Option(1L) }
        appLovinActor ! apiCall
        eventually { there was no(apiCall).updateRevenueData }
      }
    }

    "process each WaterfallAdProvider API call in order" in new WithDB {
      val appLovinActor = Akka.system(current).actorOf(Props(new AppLovinReportingActor))
      val apiCalls = List.fill(3)(mock[AppLovinReportingAPI])
      var completedCalls = new ListBuffer[AppLovinReportingAPI]()
      apiCalls.foreach { apiCall =>
        apiCall.updateRevenueData returns Future { completedCalls += apiCall; Option(0L) }
        appLovinActor ! apiCall
      }
      eventually {
        there was one(apiCalls(0)).updateRevenueData andThen(there was one(apiCalls(1)).updateRevenueData) andThen(there was one(apiCalls(2)).updateRevenueData)
      }
      (0 until 2).map(i => completedCalls(i) must beEqualTo(apiCalls(i)))
    }
  }
}
