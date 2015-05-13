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
        "timeframe" -> timeframe
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
  def parseResponse(body: String): Long = {
    Json.parse(body) match {
      case results => {
        (results \ "result").as[Long]
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
      "App",
      "Platform",
      "Earnings",
      "Fill",
      "Requests",
      "Impressions",
      "Completions",
      "Completion Rate"
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

  def buildAppRow(name: String, platform: String, requestsResponse: WSResponse, responsesResponse: WSResponse, impressionsResponse: WSResponse, completionsResponse: WSResponse, earningsResponse: WSResponse): List[Any] = {
    // Count of all requests to from each ad provider
    val requests = parseResponse(requestsResponse.body)
    // The count of all available responses from all ad providers
    val responses = parseResponse(responsesResponse.body)
    // The count of impressions
    val impressions = parseResponse(impressionsResponse.body)
    // The number of completions based on the SDK events
    val completions = parseResponse(completionsResponse.body)
    // The sum of all the eCPMs reported on each completion
    val earnings = parseResponse(earningsResponse.body).toFloat/1000

    // The completion rate based on the completions divided by impressions
    val completionRate = impressions match {
      case 0 => 0
      case _ => completions.toFloat/impressions
    }

    // The fill rate based on the number of responses divided by requests
    val fillRate = requests match {
      case 0 => 0
      case _ => responses.toFloat/requests
    }
    // The row to append to the CSV
    List(
      name,
      platform,
      earnings,
      fillRate,
      requests,
      impressions,
      completions,
      completionRate
    )
  }

  def getData(writer: CSVWriter) = {
    for (appID <- selectedApps) {
      val name = App.find(appID.toLong).get.name
      // We currently do not store the app platform so this is hardcoded.
      val platform = "iOS"

      // Clean way to make sure all requests are complete before moving on.  This also sends user an error email if export fails.
      val futureResponse: Future[(WSResponse, WSResponse, WSResponse, WSResponse, WSResponse)] = for {
        requestsResponse <- KeenExport().createRequest("count", KeenExport().createFilter(timeframe, filters, "mediate_availability_requested", appID))
        responsesResponse <- KeenExport().createRequest("count", KeenExport().createFilter(timeframe, filters, "mediate_availability_response_true", appID))
        impressionsResponse <- KeenExport().createRequest("count", KeenExport().createFilter(timeframe, filters, "ad_displayed", appID))
        completionsResponse <- KeenExport().createRequest("count", KeenExport().createFilter(timeframe, filters, "ad_completed", appID))
        earningsResponse <- KeenExport().createRequest("sum", KeenExport().createFilter(timeframe, filters, "ad_completed", appID, "ad_provider_eCPM"))
      } yield (requestsResponse, responsesResponse, impressionsResponse, completionsResponse, earningsResponse)

      futureResponse.recover {
        case _ =>
          sendEmail(email, "Error Exporting CSV", "There was a problem exporting your data.  Please try again.")
      }

      futureResponse.onSuccess {
        case (requestsResponse, responsesResponse, impressionsResponse, completionsResponse, earningsResponse) => {
          val appRow = buildAppRow(name, platform, requestsResponse, responsesResponse, impressionsResponse, completionsResponse, earningsResponse)
          println(appRow)
          createCSVRow(writer, appRow)

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
