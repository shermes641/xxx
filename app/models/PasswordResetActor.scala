package models

import akka.actor.{Props, Actor}
import play.api.db.DB
import play.api.libs.concurrent.Akka
import play.api.libs.json._
import play.api.libs.ws._
import play.api.{Logger, Play}
import play.api.Play.current
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{TimeoutException, Future}
import scala.language.postfixOps

class PasswordResetActor extends Actor with Mailer {
  def receive = {
    case user: DistributorUser => {
      lazy val link: String = resetLink(user)
      val body = views.html.Mails.passwordResetEmailContent(link).toString()
      val plain: String = {
        "We received a request to change the password for your HyprMediate account.\r\n\r\n" +
        "The link below will remain active for 1 hour.\r\n\r\n" +
        link
      }
      sendEmail(user.email, "Reset your HyprMediate password", body, plain)
    }
    case completedEmail: String => {
      val supportEmail = Play.current.configuration.getString("hyprmarketplace.team_email").getOrElse("")
      val body = views.html.Mails.passwordChangedEmail(supportEmail).toString()
      val plain: String = {
        "Your password has been changed\r\n\r\n" +
        "If you did not make this change and believe your HyprMediate account has been compromised, please contact support" +
        " at " + supportEmail
      }
      sendEmail(completedEmail, "Your HyprMediate password has been changed", body, plain)
    }
  }

  /**
   * Generates the link to the password reset page which is included in the email to the DistributorUser
   * @param user The DistributorUser who initiated the password reset
   * @return The link that will be included in the password reset email
   */
  def resetLink(user: DistributorUser): String = {
    val token: Option[String] = PasswordReset.create(user.id.get)
    val pathAndQueryString = controllers.routes.DistributorUsersController.resetPassword(Some(user.email), token, user.id).url
    Play.configuration.getString("app_domain").getOrElse("") + pathAndQueryString
  }
}
