package models

import akka.actor.Actor
import scala.language.postfixOps

class WelcomeEmailActor extends Actor with Mailer {
  def receive = {
    case email: String => {
      sendWelcomeEmail(email)
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
}
