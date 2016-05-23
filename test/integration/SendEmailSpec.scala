package integration

import akka.actor.Props
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import models._
import org.apache.http.client.methods.{HttpGet, HttpPatch}
import org.apache.http.impl.client.HttpClients
import org.specs2.mock.Mockito
import play.api.Logger
import play.api.db.DB
import play.api.libs.concurrent.Akka
import play.api.test.WithApplication
import resources.{SpecificationWithFixtures, WaterfallSpecSetup}

import scala.util.Random

/**
  * @author Steve Hermes
  *         Depends on active internet connection to get to https://mailtrap.io
  *         Removes all emails from mailtrap inbox
  *         Uses mailtrap account:
  *         Our company            ID 63020
  *         Mailbox                ID 97312
  *         website login          shermes@jungroup.com  PW  jungroup
  */
class SendEmailSpec extends SpecificationWithFixtures with WaterfallSpecSetup
  with UpdateHyprMarketplace with Mailer with Mockito with ConfigVars {
  final val MailTrapClean = "https://mailtrap.io/api/v1/inboxes/97312/clean?api_token=e4189cd37ee9cf6c15d2ce43c2838caf"
  final val MailTrapCnt = "https://mailtrap.io/api/v1/inboxes/97312?api_token=e4189cd37ee9cf6c15d2ce43c2838caf"
  final val MailTrapMsgs = "https://mailtrap.io/api/v1/inboxes/97312/messages?page=1&api_token=e4189cd37ee9cf6c15d2ce43c2838caf"

  "Send email service (if test fails, check your internet connection) " should {
    sequential

    "send an email to mailtrap.io " in
      new WithApplication(new ApplicationFake(Map("mode" -> play.api.Mode.Prod.toString, "staging" -> "true"))) {
        cleanMailTrapMailBox()
        val emailCnt = getMailTrapMailBoxMsgCnt
        val subject = "Email Test using mailtrap " + System.currentTimeMillis()
        sendEmail(ConfigVarsApp.domain,
          recipient = "steve@gmail.com",
          sender = PublishingEmail,
          subject = subject,
          body = "Just a test.",
          attachmentFileName = """public/images/email_logo.jpg""")

        wait4Email(emailCnt)
        val newCnt = getMailTrapMailBoxMsgCnt
        Logger.debug(emailCnt + " cnt newCnt " + newCnt)
        validateMailTrapMsgResponseSubject(getMailTrapMailBoxMsgs, subject) mustEqual true
        newCnt mustEqual (emailCnt + 1)
      }

    "reset password email to mailtrap.io " in
      new WithApplication(new ApplicationFake(Map("mode" -> play.api.Mode.Prod.toString, "staging" -> "true"))) {

        val resetPasswordActor = Akka.system.actorOf(Props(new PasswordResetActor))

        cleanMailTrapMailBox()
        val emailCnt = getMailTrapMailBoxMsgCnt
        resetPasswordActor ! "steve@gmail.com"

        wait4Email(emailCnt)
        val newCnt = getMailTrapMailBoxMsgCnt
        Logger.debug(emailCnt + " cnt newCnt " + newCnt)
        validateMailTrapMsgResponseSubject(getMailTrapMailBoxMsgs, "Your HyprMediate password has been changed") mustEqual true
        newCnt mustEqual (emailCnt + 1)
      }

    "send failure email to mailtrap.io " in
      new WithApplication(new ApplicationFake(Map("mode" -> play.api.Mode.Prod.toString, "staging" -> "true"))) {
        val randomCharacters = Random.alphanumeric.take(5).mkString
        val companyName = "Test Company-" + randomCharacters
        val email = "mediation-testing-" + randomCharacters + "@jungroup.com"
        val password = "testtest"

        val jApi = new JunGroupAPI
        val adProviders = AdProvider.findAll
        val distributorID = DistributorUser.create(email, password, companyName).get
        val appID = App.create(distributorID, "12345", 1).get
        val appp = App.find(appID).get

        val wap = DB.withTransaction { implicit connection =>
          val waterfallID = Waterfall.create(appID, "12345").get
          VirtualCurrency.createWithTransaction(appID, "Gold", 1, 10, None, Some(true))
          val adProviderID = ConfigVarsAdProviders.iosID
        val hyprWaterfallAdProviderID = WaterfallAdProvider.createWithTransaction(waterfallID, adProviderID, Option(0), Option(20), configurable = false, active = false, pending = true).get
          models.AppConfig.create(appID, appp.token, 0)
          val hyprWAP = WaterfallAdProvider.findWithTransaction(hyprWaterfallAdProviderID).get
          WaterfallAdProviderWithAppData(hyprWAP.id, waterfallID, adProviderID, hyprWAP.waterfallOrder, hyprWAP.cpm, hyprWAP.active, hyprWAP.fillRate,
            hyprWAP.configurationData, hyprWAP.reportingActive, hyprWAP.pending, appp.token, appp.name, companyName)
        }

        cleanMailTrapMailBox()
        val emailCnt = getMailTrapMailBoxMsgCnt
        val user = DistributorUser.find(distributorID).get
        jApi.sendFailureEmail(user, 2, appp.token, "Test error")

        wait4Email(emailCnt)
        val newCnt = getMailTrapMailBoxMsgCnt
        Logger.debug(emailCnt + " cnt newCnt " + newCnt)
        validateMailTrapMsgResponseSubject(getMailTrapMailBoxMsgs, "Player Ad Network Creation Failure") mustEqual true
        newCnt mustEqual (emailCnt + 1)
      }
  }

  /**
    * Returns the text content from a REST URL. Returns a blank String if there is a problem.
    *
    * @param url the url to get
    * @return the content
    */
  def getRestContent(url: String): String = {
    val httpclient = HttpClients.createDefault
    val https = new HttpGet(url)
    val response = httpclient.execute(https)
    try {
      val entity = response.getEntity
      val inputStream = entity.getContent
      val content = scala.io.Source.fromInputStream(inputStream).getLines().mkString
      response.close()
      content
    } catch {
      case ex: Exception => response.close(); ""
    }
  }

  //TODO using eventually causes emails to never get sent
  /**
    * Waits for an email or emails to arrive
    *
    * @param emailCnt Inbox email count
    */
  def wait4Email(emailCnt: Int) {
    getMailTrapMailBoxMsgCnt must eventually(be_>(emailCnt))
  }

  /**
    * Returns the text content from a REST URL. Returns a blank String if there is a problem.
    *
    * @param url The url to patch
    * @return The content
    */
  def patchRestContent(url: String): String = {
    val httpclient = HttpClients.createDefault
    val https = new HttpPatch(url)
    val response = httpclient.execute(https)
    try {
      val entity = response.getEntity
      val inputStream = entity.getContent
      val content = scala.io.Source.fromInputStream(inputStream).getLines().mkString
      response close()
      content
    } catch {
      case ex: Exception => response close(); ""
    }
  }

  /**
    * Remove all emails from the mailtrap inbox
    *
    * @param waitSecs Number of seconds to wait for empty mailbox
    */
  def cleanMailTrapMailBox(waitSecs: Int = 10) = {
    patchRestContent(MailTrapClean)
    getMailTrapMailBoxMsgCnt must eventually(be_==(0))
  }

  /**
    * Get count of emails in mailtrap inbox
    *
    * @return count of emails
    */
  def getMailTrapMailBoxMsgCnt: Int = {
    val content = getRestContent(MailTrapCnt)
    Logger.debug(s"getMailTrapMailBoxMsgCnt:\n$content")
    val mapper: ObjectMapper = new ObjectMapper
    val obj: JsonNode = mapper.readTree(content)
    val ec = obj.get("emails_count")
    ec.asInt
  }

  /**
    * Get 1 page of emails from the mailtrap inbox
    *
    * @return api content response
    */
  def getMailTrapMailBoxMsgs: String = {
    val content = getRestContent(MailTrapMsgs)
    Logger.debug(s"getMailTrapMailBoxMsgs:\n$content")
    content
  }

  /**
    * Verify an email subject contains a string
    *
    * @param content      getMailTrapMailBoxMsgs result
    * @param matchSubject string to match
    * @return true or false
    */
  def validateMailTrapMsgResponseSubject(content: String, matchSubject: String): Boolean = {
    val mapper: ObjectMapper = new ObjectMapper
    val obj: JsonNode = mapper.readTree(content)
    Logger.debug(s"getMailTrapMailBoxMsgs:\n$obj")
    val subject = obj.findValue("subject")
    subject.toString.contains(matchSubject)
  }
}
