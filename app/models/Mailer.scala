package models

import com.typesafe.plugin._
import play.api.Play.current

trait Mailer {
  def sendEmail(recipient: String, subject: String, body: String): Unit = {
    val mail = use[MailerPlugin].email
    mail.setRecipient(recipient)
    mail.setFrom("tdepplito@jungroup.com")
    mail.setSubject(subject)
    mail.send(body)
  }
}
