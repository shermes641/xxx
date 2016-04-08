package models

import com.github.nscala_time.time.Imports._
import com.google.inject.Singleton
import play.api.Logger
import java.io.File
import org.apache.commons.mail.EmailAttachment
import play.api.libs.mailer._
import javax.inject.Inject

/**
  * Encapsulates methods which send email
  * @param mailerClient An instance of the MailerClient class
  */
@Singleton
class Mailer @Inject() (mailerClient: MailerClient, appEnvironment: Environment) {
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
    if(appEnvironment.isProdOrStaging) {
      val template = views.html.Mails.emailTemplate(subject, body, host).toString
      val text = if(plainText == "") body else plainText
      var attachments = Seq[AttachmentFile]()
      if(attachmentFileName != "") {
        val dateFormat = DateTimeFormat.forPattern("y-M-d")
        val currentTime = new DateTime(DateTimeZone.UTC)
        val date = currentTime.toString(dateFormat)
        attachments = Seq(AttachmentFile(date + "-UTC-export.csv", new File(attachmentFileName)))
      }
      val email = Email(
        subject = subject,
        from = sender,
        to = Seq(recipient),
        bodyText = Some(text),
        bodyHtml = Some(template),
        attachments = attachments
      )
      // Logging to help email debugging
      Logger.debug("Email Sent - Subject: " + subject + "\nBody: " + body + "\nRecipient: " + recipient)
      mailerClient.send(email)
    }
  }
  // $COVERAGE-ON$
}
