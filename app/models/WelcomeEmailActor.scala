package models

import akka.actor.Actor
import scala.language.postfixOps
import play.api.Play

/**
 * @param emailAddress The Email Address of the signed up user.
 * @param company The company of the signed up user.
 */
case class sendUserCreationEmail(emailAddress: String, company: String)

class WelcomeEmailActor extends Actor with Mailer {
  def receive = {
    case sendUserCreationEmail(emailAddress: String, company: String) => {
      sendWelcomeEmail(emailAddress)
      sendTeamNotification(emailAddress, company)
    }
  }

  /**
   * Sends email to new DistributorUser.  This is called on a successful sign up.
   * @param email Email of the new DistributorUser.
   */
  def sendWelcomeEmail(email: String): Unit = {
    val subject = "Welcome to HyprMediate!"
    val body = views.html.Mails.welcomeEmailContent().toString()
    var plain = "Thank you for registering with HyprMediate.  To get started, please visit our documentation.  http://documentation.hyprmx.com"
    sendEmail(email, subject, body, plain)
  }

  /**
   * Sends email to Hyprmx team on successful user signup
   * @param userEmail Email of the new DistributorUser.
   * @param userCompany Company of the new DistributorUser.
   */
  def sendTeamNotification(userEmail: String, userCompany: String): Unit = {
    val email = Play.current.configuration.getString("hyprmarketplace.team_email").get
    val subject = "Mediation user has signed up - " + userEmail
    val body = userEmail + " has signed up for mediation. For company " + userCompany
    sendEmail(email, subject, body)
  }
}
