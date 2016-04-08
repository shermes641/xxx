package models

import akka.actor.Actor
import javax.inject.Inject
import scala.language.postfixOps

/**
 * @param emailAddress The Email Address of the signed up user.
 * @param company The company of the signed up user.
 * @param userIPAddress The IP Address of the new user.
 */
case class sendUserCreationEmail(emailAddress: String, company: String, userIPAddress: String)

/**
  * Actor that sends users an email on sign up
  * @param mailer     A shared instance of the Mailer class
  * @param configVars Shared ENV config variables
  */
class WelcomeEmailActor @Inject() (mailer: Mailer, configVars: ConfigVars) extends Actor {
  def receive = {
    case sendUserCreationEmail(emailAddress: String, company: String, userIPAddress: String) =>
      sendWelcomeEmail(emailAddress)
      sendTeamNotification(emailAddress, company, userIPAddress)
  }

  /**
   * Sends email to new DistributorUser.  This is called on a successful sign up.
    *
    * @param email Email of the new DistributorUser.
   */
  def sendWelcomeEmail(email: String): Unit = {
    val subject = "Welcome to HyprMediate!"

    val body = views.html.Mails.welcomeEmailContent().toString
    val plain: String = { "Thank you for registering with HyprMediate.\r\n\r\n" +
      "HyprMediate is the easiest way to manage rewarded video monetization partners across your applications." +
      "Our platform is simple to use, and the setup is quick and seamless.\r\n\r\n " +
      "To get started, please visit our documentation.  https://documentation.hyprmx.com\r\n\r\n" +
      "If you have any questions, feel free to contact us by replying to this email.\r\n\r\n" +
      "We're looking forward to working with you!\r\n\r\nSincerely,\r\nYour HyprMediate\r\nPublisher Accounts Team" }

    mailer.sendEmail(host = configVars.ConfigVarsApp.domain, recipient = email, sender = mailer.PublishingEmail, subject = subject, body = body, plainText = plain)
  }

  /**
   * Sends email to Hyprmx team on successful user signup
    *
    * @param userEmail Email of the new DistributorUser.
   * @param userCompany Company of the new DistributorUser.
   * @param userIPAddress The IP Address of the new user.
   */
  def sendTeamNotification(userEmail: String, userCompany: String, userIPAddress: String): Unit = {
    val email = configVars.ConfigVarsApp.teamEmail
    val subject = "hyprMediate user has signed up - " + userEmail
    val body = userEmail + " has signed up for hyprMediate.\r\n\r\nCompany: " + userCompany + "\r\n\r\nIP Address: " + userIPAddress

    mailer.sendEmail(host = configVars.ConfigVarsApp.domain, recipient = email, sender = mailer.PublishingEmail, subject = subject, body = body)
  }
}
