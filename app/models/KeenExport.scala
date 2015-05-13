package models

import akka.actor.{Props, Actor}
import play.api.db.DB
import play.api.libs.concurrent.Akka
import play.api.libs.json._
import play.api.libs.ws._
import play.api.Play
import play.api.Play.current
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import io.keen.client.java.{KeenClient, KeenProject, JavaKeenClientBuilder}
import com.github.tototoshi.csv._
import java.io.File
import java.util.Date

/**
 * Get data from keen
 */
case class GetDataFromKeen()

case class KeenResult(value: JsValue, timeframe: JsObject)

/**
 * Encapsulates interactions with Player.
 */
case class KeenExport() {
  /**
   * Sends request to Keen API exporting all app information for the given distributor ID
   *
   * Columns: App, Platform, Earnings, Fill, Requests, Impressions, Completions, Completion Rate
   *
   * @param distributorID The DistributorID with the apps needed for export.
   * @param email The email to send the export to.
   */
  def exportToCSV(distributorID: Long, email: String, filters: JsArray, timeframe: JsObject, selectedApps: List[String]) = {
    val actor = Akka.system(current).actorOf(Props(new KeenExportActor(distributorID, email, filters, timeframe, selectedApps)))
    actor ! GetDataFromKeen()
  }

  /**
   * Creates requests to keen.
   * @param action Type of request ex. count, sum
   * @param filter The keen filter to query
   * @return Future[WSResponse]
   */
  def createRequest(action: String, filter: JsObject): Future[WSResponse] = {
    val config = Play.current.configuration
    println(filter)
    WS.url(KeenClient.client().getBaseUrl + "/3.0/projects/" + config.getString("keen.project").get + "/queries/" + action).withRequestTimeout(60000).withQueryString("api_key" -> config.getString("keen.readKey").get).post(filter)
  }

  /**
    * Creates requests to keen.
    * @param collection The keen collection to query
    * @param appID The app ID
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
 * @param distributorID The ID of the distributor
 * @param email The Email address to send the final CSV
 */
class KeenExportActor(distributorID: Long, email: String, filters: JsArray, timeframe: JsObject, selectedApps: List[String]) extends Actor with Mailer {
  private var counter = 0
  val fileName = "tmp/" + distributorID.toString + "-" + System.currentTimeMillis.toString + ".csv"
  /**
   * Parses the Keen response
   * @param body The keen response body
   * @return The result
   */
  def parseResponse(body: String): List[KeenResult] = {

    implicit val keenReader = Json.reads[KeenResult]

    Json.parse(body) match {
      case results => {
        (results \ "result").as[List[KeenResult]]
      }
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
    case GetDataFromKeen() => {
      val client = new JavaKeenClientBuilder().build()
      val project = new KeenProject(Play.current.configuration.getString("keen.project").get, Play.current.configuration.getString("keen.writeKey").get, Play.current.configuration.getString("keen.readKey").get)
      client.setDefaultProject(project)
      KeenClient.initialize(client)
      val writer = createCSVFile()
      createCSVHeader(writer)
      getData(writer)
    }
  }

  /**
   * Build csv rows for each date returned called per App
   * @param name App Name
   * @param requestsResponse requests per day
   * @param dauResponse daily active users per day
   * @param responsesResponse responses per day (for fill rate)
   * @param impressionsResponse impressions per day
   * @param completionsResponse completions per day
   * @param eCPMResponse average eCPM per day
   * @param earningsResponse sum of eCPMs per day
   * @param writer the previously opened csv file
   */
  def buildAppRows(name: String, requestsResponse: WSResponse, dauResponse: WSResponse, responsesResponse: WSResponse, impressionsResponse: WSResponse, completionsResponse: WSResponse, eCPMResponse: WSResponse, earningsResponse: WSResponse, writer: CSVWriter) = {
    // Count of all requests to from each ad provider
    val requestList = parseResponse(requestsResponse.body)
    // Count of all active users
    val dauList = parseResponse(dauResponse.body)
    // The count of all available responses from all ad providers
    val responseList = parseResponse(responsesResponse.body)
    // The count of impressions
    val impressionList = parseResponse(impressionsResponse.body)
    // The number of completions based on the SDK events
    val completionList = parseResponse(completionsResponse.body)
    // The sum of all the eCPMs reported on each completion
    val eCPMList = parseResponse(eCPMResponse.body)
    // The sum of all the eCPMs reported on each completion
    val earningList = parseResponse(earningsResponse.body)

    for(i <- requestList.indices){
      val date = (requestList(i).timeframe \ "start").as[String]
      val requests = requestList(i).value.as[Long]
      val dau = dauList(i).value.as[Long]
      val impressions = impressionList(i).value.as[Long]
      val responses = responseList(i).value.as[Long]
      val completions = completionList(i).value.as[Long]
      val eCPM = eCPMList(i).value.asOpt[Long].getOrElse("")
      val earnings = earningList(i).value.as[Long]

      // The fill rate based on the number of responses divided by requests
      val fillRate = requests match {
        case 0 => 0
        case _ => responses.toFloat/requests
      }

      // Completions per DAU
      val completionsPerDau = requests match {
        case 0 => 0
        case _ => completions.toFloat/dau
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
        earnings.toFloat/1000
      )

      println(appRow)
      createCSVRow(writer, appRow)
    }
  }

  def getData(writer: CSVWriter) = {
    for (appID <- selectedApps) {
      val name = App.find(appID.toLong).get.name

      // Clean way to make sure all requests are complete before moving on.  This also sends user an error email if export fails.
      val futureResponse: Future[(WSResponse, WSResponse, WSResponse, WSResponse, WSResponse, WSResponse, WSResponse)] = for {
        requestsResponse <- KeenExport().createRequest("count", KeenExport().createFilter(timeframe, filters, "mediate_availability_requested", appID))
        dauResponse <- KeenExport().createRequest("count_unique", KeenExport().createFilter(timeframe, filters, "mediate_availability_requested", appID, "device_unique_id"))
        responsesResponse <- KeenExport().createRequest("count", KeenExport().createFilter(timeframe, filters, "mediate_availability_response_true", appID))
        impressionsResponse <- KeenExport().createRequest("count", KeenExport().createFilter(timeframe, filters, "ad_displayed", appID))
        completionsResponse <- KeenExport().createRequest("count", KeenExport().createFilter(timeframe, filters, "ad_completed", appID))
        eCPMResponse <- KeenExport().createRequest("average", KeenExport().createFilter(timeframe, filters, "ad_completed", appID, "ad_provider_eCPM"))
        earningsResponse <- KeenExport().createRequest("sum", KeenExport().createFilter(timeframe, filters, "ad_completed", appID, "ad_provider_eCPM"))
      } yield (requestsResponse, dauResponse, responsesResponse, impressionsResponse, completionsResponse, eCPMResponse, earningsResponse)

      futureResponse.recover {
        case _ =>
          sendEmail(email, "Error Exporting CSV", "There was a problem exporting your data.  Please try again.")
      }

      futureResponse.onSuccess {
        case (requestsResponse, dauResponse, responsesResponse, impressionsResponse, completionsResponse, eCPMResponse, earningsResponse) => {
          buildAppRows(name, requestsResponse, dauResponse, responsesResponse, impressionsResponse, completionsResponse, eCPMResponse, earningsResponse, writer)

          counter += 1
          if(selectedApps.length <= counter) {
            println("Exported CSV: " + fileName)
            // Sends email after all apps have received their stats
            val content = "Attached is your requested CSV file."
            sendEmail(email, "Exported CSV from HyprMediate", content, "", fileName)
            writer.close()
          }
        }
      }
    }
  }
}
