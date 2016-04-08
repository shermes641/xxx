package models

import akka.actor.Props
import org.specs2.mock.Mockito
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.json.{JsString, Json}
import resources.SpecificationWithFixtures

import scala.concurrent.Future
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global


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
      val configurationData = Json.obj("requiredParams" -> Json.obj(), "reportingParams" -> Json.obj("APIKey" -> JsString("some API Key"), "appName" -> JsString("some App Name")))
      val apiCalls = List.fill(3)(spy(AppLovinReportingAPI(1, configurationData, database, waterfallAdProviderService, configVars, ws)))
      var completedCalls = new ListBuffer[AppLovinReportingAPI]()
      def fakeUpdateRevenue(apiCall: AppLovinReportingAPI) = {
        completedCalls += apiCall
        Thread.sleep(1000)
        Future { Option(0L) }
      }
      apiCalls.foreach { apiCall =>
        org.mockito.Mockito.doReturn(fakeUpdateRevenue(apiCall)).when(apiCall).updateRevenueData
        appLovinActor ! apiCall
      }
      eventually {
        there was one(apiCalls.head).updateRevenueData andThen(there was one(apiCalls(1)).updateRevenueData) andThen(there was one(apiCalls(2)).updateRevenueData)
      }
      (0 until 2).map(i => completedCalls(i) must beEqualTo(apiCalls(i)))
    }
  }
}
