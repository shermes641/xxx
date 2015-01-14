package models

import com.typesafe.plugin._
import play.api.Play.current
import java.io.File

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
        mail.addAttachment("out.csv", new File(attachment))
      }
      mail.send(body)
    }
  }
}
