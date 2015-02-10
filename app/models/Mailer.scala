package models

import com.typesafe.plugin._
import play.api.Play.current
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar

/** Encapsulates methods which send email. */
trait Mailer {
  /**
   * Sends a generic email with a configurable subject and body.
   * @param recipient Email for recipient
   * @param subject Email subject
   * @param body Email body
   */
  def sendEmail(recipient: String, subject: String, body: String, attachment: String = ""): Unit = {
    if(play.api.Play.isProd(play.api.Play.current)) {
      val mail = use[MailerPlugin].email
      mail.setRecipient(recipient)
      mail.setFrom("noreply@hyprMediate.com")
      mail.setSubject(subject)
      if(attachment != "") {
        val format = new SimpleDateFormat("d-M-y")
        mail.addAttachment(format.format(Calendar.getInstance().getTime()) + "-export.csv", new File(attachment))
      }
      val template = views.html.Mails.emailTemplate(subject, body).toString()
      mail.sendHtml(template)
    }
  }
}
