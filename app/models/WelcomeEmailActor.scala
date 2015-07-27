package models

import akka.actor.Actor
import scala.language.postfixOps
import play.api.Play

/**
 * @param emailAddress The Email Address of the signed up user.
 * @param company The company of the signed up user.
 * @param userIPAddress The IP Address of the new user.
 */
case class sendUserCreationEmail(emailAddress: String, company: String, userIPAddress: String)

class WelcomeEmailActor extends Actor with Mailer {
  def receive = {
    case sendUserCreationEmail(emailAddress: String, company: String, userIPAddress: String) => {
      sendWelcomeEmail(emailAddress)
      sendTeamNotification(emailAddress, company, userIPAddress)
    }
  }

  /**
   * Sends email to new DistributorUser.  This is called on a successful sign up.
   * @param email Email of the new DistributorUser.
   */
  def sendWelcomeEmail(email: String): Unit = {
    val subject = "Welcome to HyprMediate!"

    val body = views.html.Mails.welcomeEmailContent().toString()
    val plain: String = { "Thank you for registering with HyprMediate.\r\n\r\n" +
      "HyprMediate is the easiest way to manage rewarded video monetization partners across your applications." +
      "Our platform is simple to use, and the setup is quick and seamless.\r\n\r\n " +
      "To get started, please visit our documentation.  http://documentation.hyprmx.com\r\n\r\n" +
      "If you have any questions, feel free to contact us by replying to this email.\r\n\r\n" +
      "We're looking forward to working with you!\r\n\r\nSincerely,\r\nYour HyprMediate\r\nPublisher Accounts Team" }

    sendEmail(email, subject, body, plain)
  }

  /**
   * Sends email to Hyprmx team on successful user signup
   * @param userEmail Email of the new DistributorUser.
   * @param userCompany Company of the new DistributorUser.
   * @param userIPAddress The IP Address of the new user.
   */
  def sendTeamNotification(userEmail: String, userCompany: String, userIPAddress: String): Unit = {
    val email = Play.current.configuration.getString("hyprmarketplace.team_email").get
    val subject = "hyprMediate user has signed up - " + userEmail
    val body = userEmail + " has signed up for hyprMediate.\r\n\r\nCompany: " + userCompany + "\r\n\r\nIP Address: " + userIPAddress
    sendEmail(email, subject, body)
  }
}
