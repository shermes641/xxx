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
    val distributor = Distributor.find(DistributorUser.findByEmail(userEmail).get.distributorID.get)
    val subject = "Mediation user has signed up - " + userEmail
    val body = userEmail + " has signed up for mediation. For company " + distributor.get.name
    sendEmail(email, subject, body)
  }
}
