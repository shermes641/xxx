package models

import java.io.File

import akka.actor.{Actor, Props}
import com.github.tototoshi.csv._
import io.keen.client.java.{JavaKeenClientBuilder, KeenClient, KeenProject}
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.json._
import play.api.libs.ws._
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

/**
  * Get data from keen
  */
case class GetDataFromKeen()

case class KeenResult(value: JsValue, timeframe: JsObject)

class KeenRequest(action: String = "", val post: JsObject = JsObject(Seq())) extends ConfigVars {

  val base = KeenClient.client().getBaseUrl + "/3.0/projects/" + ConfigVarsKeen.projectID + "/queries/"

  def function(action: String): KeenRequest = new KeenRequest(action, post)

  def select(collection: String): KeenRequest = new KeenRequest(action, post +("event_collection", JsString(collection)))

  def targetProperty(target: String): KeenRequest = new KeenRequest(action, post +("target_property", JsString(target)))

  def filterWith(property: String, operator: String, propertyValue: String): KeenRequest = {
    val filters = (post \ "filters").asOpt[JsArray].getOrElse(JsArray())
    new KeenRequest(action, post +("filters", filters :+ JsObject(Seq("property_name" -> JsString(property),
      "operator" -> JsString(operator),
      "property_value" -> JsString(propertyValue)))))
  }

  def groupBy(group: String) = new KeenRequest(action, post +("group_by", JsString(group)))

  def thisDays(daysAgo: Int): KeenRequest = new KeenRequest(action, post +("timeframe", JsString("this_%s_days".format(daysAgo))))

  def interval(interval: String): KeenRequest = new KeenRequest(action, post +("interval", JsString(interval)))

  def collect(): Future[WSResponse] = {
    if (Environment.isTest) {
      debug()
    }
    WS.url(base + action).withRequestTimeout(300000).withQueryString("api_key" -> KeenRequest.project.getReadKey).post(post)
  }

  def debug() = Logger.debug("-- KeenRequest Debug: Posting to %s, data: %s".format(base + action, post.toString))

}

object KeenRequest extends ConfigVars {

  // move this to startup.
  val client = new JavaKeenClientBuilder().build()
  val project = new KeenProject(ConfigVarsKeen.projectID, ConfigVarsKeen.writeKey, ConfigVarsKeen.readKey)
  client.setDefaultProject(project)
  KeenClient.initialize(client)

}

/**
  * Encapsulates interactions with Player.
  */
case class KeenExport() {
  /**
    * Sends request to Keen API exporting all app information for the given distributor ID
    *
    * Columns: App, Platform, Earnings, Fill, Requests, Impressions, Completions, Completion Rate
    *
    * @param distributorID       The DistributorID with the apps needed for export.
    * @param displayFillRate     If false display "N/A", not the actual fillrate
    * @param email               The email to send the export to.
    * @param filters             Filters for the analytic events
    * @param timeframe           Timeframe for the analytics events
    * @param selectedApps        Apps to loop through for events
    * @param adProvidersSelected true if ad providers are selected individually
    * @param scopedReadKey       A Keen API read key scoped exclusively to the Distributor who is currently logged in
    */
  def exportToCSV(distributorID: Long,
                  displayFillRate: Boolean,
                  email: String,
                  filters: JsArray,
                  timeframe: JsObject,
                  selectedApps: List[String],
                  adProvidersSelected: Boolean,
                  scopedReadKey: String) = {
    val actor = Akka.system(current).actorOf(Props(new KeenExportActor(distributorID, displayFillRate, email, filters, timeframe, selectedApps, adProvidersSelected, scopedReadKey)))
    actor ! GetDataFromKeen()
  }
}

object KeenExport extends ConfigVars {
  /**
    * Posts requests to keen.
    *
    * @param action Type of request ex. count, sum
    * @param filter The keen filter to query
    * @return Future[WSResponse]
    */
  def createRequest(action: String, filter: JsObject): Future[WSResponse] = {
    WS.url(KeenClient.client().getBaseUrl + "/3.0/projects/" + ConfigVarsKeen.projectID + "/queries/" + action)
      .withRequestTimeout(300000)
      .withQueryString("api_key" -> ConfigVarsKeen.readKey).post(filter)
  }

  /**
    * Build request to keen.
    *
    * @param collection The keen collection to query
    * @param appID      The app ID
    * @return JsObject
    */
  def createFilter(timeframe: JsObject, filters: JsArray, collection: String, appID: String, targetProperty: String = "") = {
    val allFilters = filters :+ JsObject(Seq(
      "property_name" -> JsString("app_id"),
      "operator" -> JsString("eq"),
      "property_value" -> JsString(appID)
    ))

    JsObject(
      Seq(
        "event_collection" -> JsString(collection),
        "target_property" -> JsString(targetProperty),
        "filters" -> allFilters,
        "timeframe" -> timeframe,
        "interval" -> JsString("daily")
      )
    )
  }

}

/**
  * Actor that makes the long running requests to keen
  *
  * @param distributorID        The ID of the distributor
  * @param displayFillrate      If false display "N/A", not the actual fillrate
  * @param email                The Email address to send the final CSV
  * @param filters              Filters for the analytic events
  * @param timeframe            Timeframe for the analytics events
  * @param selectedApps         Apps to loop through for events
  * @param adProvidersSelected  true if ad providers are selected individually
  * @param scopedReadKey       A Keen API read key scoped exclusively to the Distributor who is currently logged in
  */
class KeenExportActor(distributorID: Long,
                      displayFillrate: Boolean,
                      email: String,
                      filters: JsArray,
                      timeframe: JsObject,
                      selectedApps: List[String],
                      adProvidersSelected: Boolean,
                      scopedReadKey: String) extends Actor with Mailer with ConfigVars {
  private var counter = 0

  val fileName = "tmp/" + distributorID.toString + "-" + System.currentTimeMillis.toString + ".csv"

  /**
    * Parses the Keen response
    *
    * @param body The keen response body
    * @return The result
    */
  def parseResponse(body: String): List[KeenResult] = {

    implicit val keenReader = Json.reads[KeenResult]
    Json.parse(body) match {
      case results =>
        (results \ "result").as[List[KeenResult]]
    }
  }

  /**
    * Create CSV for writing
    */
  def createCSVFile(): CSVWriter = {
    val f = new File(fileName)
    f.getParentFile.mkdirs()
    CSVWriter.open(f)
  }

  /**
    * Write CSV header
    *
    * @param writer the previously opened csv file
    */
  def createCSVHeader(writer: CSVWriter) = {
    val headerRow = List(
      "Date",
      "App",
      "DAU",
      "Requests",
      "Fill",
      "Impressions",
      "Completions",
      "Completion Per DAU",
      "eCPM",
      "Estimated Revenue"
    )

    writer.writeRow(headerRow)
  }

  /**
    * Write CSV Row
    *
    * @param writer the previously opened csv file
    * @param appRow The row to append
    */
  def createCSVRow(writer: CSVWriter, appRow: List[Any]) = {
    writer.writeRow(appRow)
  }

  def receive = {
    /**
      * Gets the data from keen.  Waterfalls through each API request until all data is found.  Needs to be done
      * in order as some calculations require previous variables to be set.  It does this on each app in the distributors
      * account
      */
    case GetDataFromKeen() =>
      val client = new JavaKeenClientBuilder().build()
      val project = new KeenProject(ConfigVarsKeen.projectID, ConfigVarsKeen.writeKey, scopedReadKey)
      client.setDefaultProject(project)
      KeenClient.initialize(client)
      val writer = createCSVFile()
      createCSVHeader(writer)
      requestData(writer)
  }

  /**
    * Build csv rows for each date returned called per App
    *
    * @param name                            App Name
    * @param requestsResponse                requests per day
    * @param dauResponse                     daily active users per day
    * @param responsesResponse               responses per day (for fill rate)
    * @param impressionsResponse             impressions per day
    * @param adCompletedResponse             completions per day (using ad_completed events for iOS 1.0)
    * @param rewardDeliveredResponse         completions per day (using reward_delivered events for Android 1.0, iOS 2.0, and newer)
    * @param adCompletedEcpmResponse         weighted average eCPM per day (using ad_completed events for iOS 1.0)
    * @param rewardDeliveredEcpmResponse     weighted average eCPM per day (using reward_delivered events for Android 1.0, iOS 2.0, and newer)
    * @param adCompletedEarningsResponse     sum of eCPMs per day (using ad_completed events for iOS 1.0)
    * @param rewardDeliveredEarningsResponse sum of eCPMs per day (using reward_delivered events for Android 1.0, iOS 2.0, and newer)
    * @param writer                          the previously opened csv file
    */
  def buildAppRows(name: String,
                   requestsResponse: WSResponse,
                   dauResponse: WSResponse,
                   responsesResponse: WSResponse,
                   impressionsResponse: WSResponse,
                   adCompletedResponse: WSResponse,
                   rewardDeliveredResponse: WSResponse,
                   adCompletedEcpmResponse: WSResponse,
                   rewardDeliveredEcpmResponse: WSResponse,
                   adCompletedEarningsResponse: WSResponse,
                   rewardDeliveredEarningsResponse: WSResponse,
                   writer: CSVWriter) = {
    // Count of all requests to from each ad provider
    val requestList = parseResponse(requestsResponse.body)
    // Count of all active users
    val dauList = parseResponse(dauResponse.body)
    // The count of all available responses from all ad providers
    val responseList = parseResponse(responsesResponse.body)
    // The count of impressions
    val impressionList = parseResponse(impressionsResponse.body)
    // The number of completions based on the SDK events (using ad_completed event for iOS 1.0)
    val adCompletedList = parseResponse(adCompletedResponse.body)
    // The number of completions based on the SDK events (using reward_delivered event for Android 1.0, iOS 2.0, and newer)
    val rewardDeliveredList = parseResponse(rewardDeliveredResponse.body)
    // List of weighted average eCPMs based on the SDK events (using ad_completed event for iOS 1.0)
    val adCompletedEcpmList = parseResponse(adCompletedEcpmResponse.body)
    // List of weighted average eCPMs based on the SDK events (using reward_delivered event for Android 1.0, iOS 2.0, and newer)
    val rewardDeliveredEcpmList = parseResponse(rewardDeliveredEcpmResponse.body)
    // The sum of all the eCPMs reported on each completion (using ad_completed event for iOS 1.0)
    val adCompletedEarningsList = parseResponse(adCompletedEarningsResponse.body)
    // The sum of all the eCPMs reported on each completion (using reward_delivered event for Android 1.0, iOS 2.0, and newer)
    val rewardDeliveredEarningsList = parseResponse(rewardDeliveredEarningsResponse.body)

    for (i <- requestList.indices) {
      val date: String = (requestList(i).timeframe \ "start").as[String].split("T")(0)
      val requests: Long = requestList(i).value.as[Long]
      val dau: Long = dauList(i).value.as[Long]
      val impressions: Long = impressionList(i).value.as[Long]
      val responses: Long = responseList(i).value.as[Long]
      val adCompletedCount: Long = adCompletedList(i).value.as[Long]
      val rewardDeliveredCount: Long = rewardDeliveredList(i).value.as[Long]
      val completions: Long = adCompletedCount + rewardDeliveredCount
      val adCompletedAverageEcpm: Double = adCompletedEcpmList(i).value.asOpt[Double].getOrElse(0)
      val rewardDeliveredAverageEcpm: Double = rewardDeliveredEcpmList(i).value.asOpt[Double].getOrElse(0)
      val eCPM: Double = {
        if (completions > 0) {
          val sumTotal: Double = (adCompletedCount * adCompletedAverageEcpm) + (rewardDeliveredCount * rewardDeliveredAverageEcpm)
          sumTotal / completions.toDouble
        } else {
          0.0
        }
      }

      /**
        * Since we only log eCPM to Keen for ad_completed/reward_delivered events, we need to use
        * the eCPM sum from our completion events along with the completion rate to calculate total revenue based on impressions.
        */
      val earnings = {
        val completionRate: Double = completions.toDouble / impressions.toDouble
        val eCPMSum: Double = eCPM * completions
        (eCPMSum / completionRate).toFloat / 1000
      }

      // The fill rate based on the number of responses divided by requests
      val fillRate =
        if (displayFillrate) {
          requests match {
            case 0 => 0
            case _ => responses.toFloat / requests
          }
        } else
          "N/A"

      // Completions per DAU
      val completionsPerDau = requests match {
        case 0 => 0
        case _ => completions.toFloat / dau
      }

      // The row to append to the CSV
      val appRow = List(
        date,
        name,
        dau,
        requests,
        fillRate,
        impressions,
        completions,
        completionsPerDau,
        eCPM,
        earnings
      )

      println(appRow)
      createCSVRow(writer, appRow)
    }
  }

  def requestData(writer: CSVWriter) = {
    val (requestCollection, responseCollection) = if (adProvidersSelected) ("availability_requested", "availability_response_true") else ("mediate_availability_requested", "mediate_availability_response_true")

    for (appID <- selectedApps) {
      val name = App.findAppWithVirtualCurrency(appID.toLong, distributorID).get.appName
      // Clean way to make sure all requests are complete before moving on.  This also sends user an error email if export fails.
      val futureResponse: Future[(WSResponse, WSResponse, WSResponse, WSResponse, WSResponse, WSResponse, WSResponse, WSResponse, WSResponse, WSResponse)] = for {
        requestsResponse <- KeenExport.createRequest("count", KeenExport.createFilter(timeframe, filters, requestCollection, appID))
        dauResponse <- KeenExport.createRequest("count_unique", KeenExport.createFilter(timeframe, filters, requestCollection, appID, "device_unique_id"))
        responsesResponse <- KeenExport.createRequest("count", KeenExport.createFilter(timeframe, filters, responseCollection, appID))
        impressionsResponse <- KeenExport.createRequest("count", KeenExport.createFilter(timeframe, filters, "ad_displayed", appID))
        adCompletedResponse <- KeenExport.createRequest("count", KeenExport.createFilter(timeframe, filters, "ad_completed", appID))
        rewardDeliveredResponse <- KeenExport.createRequest("count", KeenExport.createFilter(timeframe, filters, "reward_delivered", appID))
        adCompletedEcpmResponse <- KeenExport.createRequest("average", KeenExport.createFilter(timeframe, filters, "ad_completed", appID, "ad_provider_eCPM"))
        rewardDeliveredEcpmResponse <- KeenExport.createRequest("average", KeenExport.createFilter(timeframe, filters, "reward_delivered", appID, "ad_provider_eCPM"))
        adCompletedEarningsResponse <- KeenExport.createRequest("sum", KeenExport.createFilter(timeframe, filters, "ad_completed", appID, "ad_provider_eCPM"))
        rewardDeliveredEarningsResponse <- KeenExport.createRequest("sum", KeenExport.createFilter(timeframe, filters, "reward_delivered", appID, "ad_provider_eCPM"))
      } yield (
        requestsResponse,
        dauResponse,
        responsesResponse,
        impressionsResponse,
        adCompletedResponse,
        rewardDeliveredResponse,
        adCompletedEcpmResponse,
        rewardDeliveredEcpmResponse,
        adCompletedEarningsResponse,
        rewardDeliveredEarningsResponse
        )

      futureResponse.recover {
        case e: Exception =>
          Logger.error("Error Exporting CSV for " + email + " Message: " + e.getMessage)
          sendEmail(host = ConfigVarsApp.domain, recipient = email, sender = PublishingEmail, subject = "Error Exporting CSV", body = "There was a problem exporting your data.  Please try again.")
      }

      futureResponse.onSuccess {
        case (
          requestsResponse,
          dauResponse,
          responsesResponse,
          impressionsResponse,
          adCompletedResponse,
          rewardDeliveredResponse,
          adCompletedEcpmResponse,
          rewardDeliveredEcpmResponse,
          adCompletedEarningsResponse,
          rewardDeliveredEarningsResponse) =>
          buildAppRows(
            name,
            requestsResponse,
            dauResponse,
            responsesResponse,
            impressionsResponse,
            adCompletedResponse,
            rewardDeliveredResponse,
            adCompletedEcpmResponse,
            rewardDeliveredEcpmResponse,
            adCompletedEarningsResponse,
            rewardDeliveredEarningsResponse,
            writer
          )

          counter += 1
          if (selectedApps.length <= counter) {
            println("Exported CSV: " + fileName)
            // Sends email after all apps have received their stats
            val content = "Attached is your requested CSV file."
            sendEmail(host = ConfigVarsApp.domain, recipient = email, sender = PublishingEmail, subject = "Exported CSV from HyprMediate", body = content, "", attachmentFileName = fileName)
            writer.close()
          }
      }
    }
  }
}
