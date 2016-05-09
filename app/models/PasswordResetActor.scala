package models

import akka.actor.Actor

import scala.language.postfixOps

class PasswordResetActor extends Actor with Mailer with ConfigVars {
  def receive = {
    case user: DistributorUser =>
      lazy val link: String = resetLink(user)
      val body = views.html.Mails.passwordResetEmailContent(link).toString()
      val plain: String = {
        "We received a request to change the password for your HyprMediate account.\r\n\r\n" +
          "The link below will remain active for 1 hour.\r\n\r\n" +
          link
      }
      sendEmail(host = ConfigVarsApp.domain, recipient = user.email, sender = NoReplyEmail, subject = "Reset your HyprMediate password", body = body, plainText = plain)

    case completedEmail: String =>
      val supportEmail = ConfigVarsApp.teamEmail
      val body = views.html.Mails.passwordChangedEmail(supportEmail).toString()
      val plain: String = {
        "Your password has been changed\r\n\r\n" +
          "If you did not make this change and believe your HyprMediate account has been compromised, please contact support" +
          " at " + supportEmail
      }
      sendEmail(host = ConfigVarsApp.domain, recipient = completedEmail, sender = NoReplyEmail, subject = "Your HyprMediate password has been changed", body = body, plainText = plain)
  }

  /**
    * Generates the link to the password reset page which is included in the email to the DistributorUser
    *
    * @param user The DistributorUser who initiated the password reset
    * @return The link that will be included in the password reset email
    */
  def resetLink(user: DistributorUser): String = {
    val token: Option[String] = PasswordReset.create(user.id.get)
    val pathAndQueryString = controllers.routes.DistributorUsersController.resetPassword(Some(user.email), token, user.id).url
    ConfigVarsApp.domain + pathAndQueryString
  }
}
