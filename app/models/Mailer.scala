package models

import com.github.nscala_time.time.Imports._
import com.typesafe.plugin._
import play.api.Logger
import play.api.Play.current
import java.io.File

/** Encapsulates methods which send email. */
trait Mailer {
  val PublishingEmail = "HyprMediate <publishing@hyprmx.com>"
  val NoReplyEmail = "HyprMediate <noreply@hyprmx.com>"

  /**
   * Sends a generic email with a configurable subject and body.
   * @param recipient Email for recipient
   * @param sender The 'from' email address
   * @param subject Email subject
   * @param body Email body
   * @param attachmentFileName Email attachment file name
   */
  // $COVERAGE-OFF$ covered in it tests
  def sendEmail(host: String,
                recipient: String,
                sender: String = PublishingEmail,
                subject: String,
                body: String,
                plainText: String = "",
                attachmentFileName: String = ""): Unit = {
    if(Environment.isProdOrStaging) {
      val mail = use[MailerPlugin].email
      mail.setRecipient(recipient)
      mail.setFrom(sender)
      mail.setSubject(subject)
      if(attachmentFileName != "") {
        val dateFormat = DateTimeFormat.forPattern("y-M-d")
        val currentTime = new DateTime(DateTimeZone.UTC)
        val date = currentTime.toString(dateFormat)
        mail.addAttachment(date + "-UTC-export.csv", new File(attachmentFileName))
      }
      // Logging to help email debugging
      Logger.debug("Email Sent - Subject: " + subject + "\nBody: " + body + "\nRecipient: " + recipient)
      val template = views.html.Mails.emailTemplate(subject, body, host).toString()
      val text = if(plainText == "") body else plainText
      mail.send(text, template)
    }
  }
  // $COVERAGE-ON$
}
