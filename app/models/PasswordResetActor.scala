package models

import akka.actor.Actor
import scala.language.postfixOps
import play.api.db.Database
import scala.language.postfixOps

/**
  * Actor for resetting user passwords
  * @param mailer     A shared instance of the Mailer class
  * @param database   A shared database
  * @param configVars Shared ENV configuration variables
  */
class PasswordResetActor(mailer: Mailer, database: Database, configVars: ConfigVars) extends Actor {
  def receive = {
    case user: DistributorUser =>
      lazy val link: String = resetLink(user)
      val body = views.html.Mails.passwordResetEmailContent(link).toString()
      val plain: String = {
        "We received a request to change the password for your HyprMediate account.\r\n\r\n" +
          "The link below will remain active for 1 hour.\r\n\r\n" +
          link
      }
      mailer.sendEmail(host = configVars.ConfigVarsApp.domain, recipient = user.email, sender = mailer.NoReplyEmail, subject = "Reset your HyprMediate password", body = body, plainText = plain)

    case completedEmail: String => {
      val supportEmail = configVars.ConfigVarsApp.teamEmail
      val body = views.html.Mails.passwordChangedEmail(supportEmail).toString()
      val plain: String = {
        "Your password has been changed\r\n\r\n" +
          "If you did not make this change and believe your HyprMediate account has been compromised, please contact support" +
          " at " + supportEmail
      }
      mailer.sendEmail(host = configVars.ConfigVarsApp.domain, recipient = completedEmail, sender = mailer.NoReplyEmail, subject = "Your HyprMediate password has been changed", body = body, plainText = plain)
    }
  }

  /**
    * Generates the link to the password reset page which is included in the email to the DistributorUser
    *
    * @param user The DistributorUser who initiated the password reset
    * @return The link that will be included in the password reset email
    */
  def resetLink(user: DistributorUser): String = {
    val token: Option[String] = PasswordReset.create(user.id.get, database)
    val pathAndQueryString = controllers.routes.DistributorUsersController.resetPassword(Some(user.email), token, user.id).url
    configVars.ConfigVarsApp.domain + pathAndQueryString
  }
}
