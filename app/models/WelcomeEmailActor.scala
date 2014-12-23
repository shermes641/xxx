package models

import akka.actor.Actor
import scala.language.postfixOps
import play.api.Play

class WelcomeEmailActor extends Actor with Mailer {
  def receive = {
    case email: String => {
      sendWelcomeEmail(email)
      sendTeamNotification(email)
    }
  }

  /**
   * Sends email to new DistributorUser.  This is called on a successful sign up.
   * @param email Email of the new DistributorUser.
   */
  def sendWelcomeEmail(email: String): Unit = {
    val subject = "Welcome to HyprMediation"
    val body = "Welcome to HyprMediation!"
    sendEmail(email, subject, body)
  }

  /**
   * Sends email to Hyprmx team on successful user signup
   * @param userEmail Email of the new DistributorUser.
   */
  def sendTeamNotification(userEmail: String): Unit = {
    val email = Play.current.configuration.getString("hyprmarketplace.team_email").get
    val subject = "Mediation user has signed up - " + userEmail
    val body = userEmail + "Has signed up for mediation."
    sendEmail(email, subject, body)
  }
}
