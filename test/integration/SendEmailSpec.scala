package integration

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import models.Mailer
import org.apache.http.client.methods.{HttpGet, HttpPatch}
import org.apache.http.impl.client.HttpClients
import play.api.Logger
import play.api.test.WithApplication
import resources.SpecificationWithFixtures

/**
  * @author Steve Hermes
  *         Depends on active internet connection to get to https://mailtrap.io
  *         Removes all emails from mailtrap inbox
  *         Uses mailtrap account:
  *         Our company            ID 63020
  *         Mailbox                ID 97312
  *         website login          shermes@jungroup.com  PW  jungroup
  */
class SendEmailSpec extends SpecificationWithFixtures with Mailer {
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
        sendEmail(recipient = "steve@gmail.com", sender = PublishingEmail, subject = subject, body = "Just a test.")

        def wait4Email(cnt: Int) {
          Thread.sleep(1000)
          if (cnt != 0 && getMailTrapMailBoxMsgCnt <= emailCnt)
            wait4Email(cnt - 1)
        }

        //wait up to 20 secs for emails to get delivered
        wait4Email(20)
        Thread.sleep(3000)
        val newCnt = getMailTrapMailBoxMsgCnt
        Logger.debug(emailCnt + " cnt newCnt " + newCnt)
        validateMailTrapMsgResponseSubject(getMailTrapMailBoxMsgs, subject) mustEqual true
        newCnt mustEqual (emailCnt + 1)
      }
  }

  /**
    * Returns the text content from a REST URL. Returns a blank String if there
    * is a problem.
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

  /**
    * Returns the text content from a REST URL. Returns a blank String if there
    * is a problem.
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

  def cleanMailTrapMailBox(waitSecs: Int = 10) = {
    patchRestContent(MailTrapClean)
    wait4Empty(waitSecs)

    def wait4Empty(secs: Int) {
      Thread.sleep(1000)
      val cnt = getMailTrapMailBoxMsgCnt
      if (secs != 0 && cnt != 0)
        wait4Empty(secs - 1)
    }
  }

  def getMailTrapMailBoxMsgCnt: Int = {
    val content = getRestContent(MailTrapCnt)
    Logger.debug(s"getMailTrapMailBoxMsgCnt:\n$content")
    val mapper: ObjectMapper = new ObjectMapper
    val obj: JsonNode = mapper.readTree(content)
    val ec = obj.get("emails_count")
    ec.asInt
  }

  def getMailTrapMailBoxMsgs: String = {
    val content = getRestContent(MailTrapMsgs)
    Logger.debug(s"getMailTrapMailBoxMsgs:\n$content")
    content
  }

  def validateMailTrapMsgResponseSubject(content: String, matchSubject: String): Boolean = {
    val mapper: ObjectMapper = new ObjectMapper
    val obj: JsonNode = mapper.readTree(content)
    Logger.debug(s"getMailTrapMailBoxMsgs:\n$obj")
    val subject = obj.findValue("subject")
    subject.toString.contains(matchSubject)
  }
}
