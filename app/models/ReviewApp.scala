package models

import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.{CloseableHttpClient, HttpClientBuilder}
import play.api.Logger

import scala.util.parsing.json._

/**
  * Created by shermes on 4/19/16.
  */
class ReviewApp {

  /**
    * Get data from existing Keen project
    *
    * @param client         Http client
    * @param url            url to GET
    * @param reviewAppName  name of Heroku review app
    * @param KeenOrgKey     Jun Group Keen organization key
    * @return Map with name id and apikeys for the Keen project, or Map  with error key if project can not be found
    */
  private def getKeenProject(client: CloseableHttpClient, url: String, reviewAppName: String, KeenOrgKey: String): Map[String, String] = {
    val getProjects  = new HttpGet(url)
    getProjects.addHeader("Authorization", KeenOrgKey)
    getProjects.addHeader("Content-Type", "application/json")
    val getResponse = client.execute(getProjects)
    val getEntity = getResponse.getEntity
    val content = if (getEntity != null) {
      val inputStream = getEntity.getContent
      val content = scala.io.Source.fromInputStream(inputStream).getLines().mkString("\n").toString
      inputStream.close()
      content
    } else
      ""
    val contentMap = JSON.parseFull(content.toString).getOrElse(List(Map())).asInstanceOf[List[Map[String, Any]]]
    if (contentMap.nonEmpty) {
      val existingProjectMap = contentMap.map(projectMap => {
        if (projectMap.getOrElse("name", "") == reviewAppName)
          projectMap
        else
          Map()
      }).find(_ != Map()).getOrElse(Map()).asInstanceOf[Map[String, Any]]

      if (existingProjectMap.nonEmpty) {
        val apiKeys = existingProjectMap.get("apiKeys")
        Logger.debug(s"API    : $apiKeys\n\n\n\n\n\n")
          apiKeys.get.asInstanceOf[Map[String, String]] ++ Map("name" -> existingProjectMap.getOrElse("name", "").toString, "id" -> existingProjectMap.getOrElse("id", "").toString)
      } else
          Map("error" -> s"project not found: $reviewAppName", "response" -> content.toString)
    } else {
      Map("error" -> "no data returned", "response" -> content.toString)
    }
  }

  /**
    * Create a Keen project for a review app
    *
    * @param client         Http client
    * @param url            url to GET
    * @param reviewAppName  Name of Heroku review app -- Note: it is assumed the caller provided a valid Heroku review app name
    * @param opsEmail       Operations email
    * @param KeenOrgKey     Jun Group Keen organization key
    * @return Map with name id and apikeys for the created Keen project, or Map with error key if creation fails
    */
  private def createKeenProject(client: CloseableHttpClient, url: String, reviewAppName: String, opsEmail: String, KeenOrgKey: String): Map[String, String] = {
    val data = s"""{"name": "$reviewAppName","users": [{"email": "$opsEmail"}]}"""

    val post = new HttpPost(url)
    post.addHeader("Authorization", KeenOrgKey)
    post.addHeader("Content-Type", "application/json")
    post.setEntity(new StringEntity(data))
    // send the post request
    val response = client.execute(post)
    val entity = response.getEntity
    val content = if (entity != null) {
      val inputStream = entity.getContent
      val content = scala.io.Source.fromInputStream(inputStream).getLines().mkString("\n").toString
      inputStream.close()
      content
    } else
      ""

    client.close()
    val contentMap = JSON.parseFull(content.toString).getOrElse(Map()).asInstanceOf[Map[String, Any]]

    Logger.info(s"STATUS: ${response.getStatusLine}")
    response.getStatusLine match {
      case null =>
        Logger.error(s"Status not set, failed to create review app Keen project\nCONTENT: $content\nJSON: $contentMap")
        Map("error" -> "Missing status on Keen response")

      case status =>
        if (status.getStatusCode == 200) {
          val apiKeys = contentMap.getOrElse("apiKeys", Map()).asInstanceOf[Map[String, String]]
          val name = contentMap.get("name")
          val id = contentMap.get("id")
          val writeKey = apiKeys.get("writeKey")
          val readKey = apiKeys.get("readKey")
          val masterKey = apiKeys.get("masterKey")
          if (name.isEmpty || id.isEmpty || writeKey.isEmpty || readKey.isEmpty || masterKey.isEmpty) {
            Logger.error(s"Keen project creation error one or more values are missing: name, id, writeKey, readKey, masterKey\nRESPONSE: ${content.toString}")
            Map("error" -> "Missing values on Keen response")
          } else {
            apiKeys ++ Map("name" -> name.get.toString, "id" -> id.get.toString)
          }
        } else if (status.getStatusCode == 409) {
          val msg = contentMap.getOrElse("message", "").toString
          if (msg.contains("Duplicate")) {
            // this ok
            Logger.debug(s"Keen project already exists, this should never happen\nRESPONSE: ${content.toString}")
            Map("error" -> s"Keen project exists project name: $reviewAppName")
          } else {
            Logger.error(s"Unknown Keen error STATUS: $status \nRESPONSE${content.toString}")
            Map("error" -> s"Bad status on Keen response STATUS: $status")
          }
        } else {
          Map("error" -> s"Unexpected status on Keen response STATUS: $status \nRESPONSE${content.toString}")
        }
    }
  }

  /**
    * Create a Keen project for a review app
    *
    * @param reviewApp  review app name
    * @param opsEmail   email for operations
    * @param KeenOrgId  Jun Group organization ID
    * @param KeenOrgKey Jun Group organization KEY
    * @return Map with name id and apikeys keys for the Keen project if created, if the project existed an existing key is included, or Map with error key if creation fails
    */
  def createOrGetKeenProject(reviewApp: String, opsEmail: String, KeenOrgId: String, KeenOrgKey: String): Map[String, String] = {
    val url = s"https://api.keen.io/3.0/organizations/$KeenOrgId/projects"
    val client = HttpClientBuilder.create.build
    try {
      // see if project exists
      val getExistingProjectData = getKeenProject(client, url, reviewApp, KeenOrgKey)

      if (getExistingProjectData.getOrElse("error", None) == None) {
        client.close()
        Logger.debug(s"Found existing project: $getExistingProjectData")
        // return the project name, id, and api keys
        getExistingProjectData ++ Map("existing" -> "true")
      } else {
        // project does not exists, let's create one
        val getCreatedProjectData = createKeenProject(client, url, reviewApp, opsEmail, KeenOrgKey)
        client.close()
        getCreatedProjectData
      }
    } catch {
      case ex: Throwable =>
        client.close()
        Logger.error(s"Error accessing / creating Keen project \nerror: $ex")
        Map(("error", ex.getLocalizedMessage))
    }
  }
}
