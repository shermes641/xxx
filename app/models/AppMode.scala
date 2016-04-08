package models

import javax.inject.Inject

case class AppMode @Inject() (environment: play.api.Environment) {
  lazy val contextMode = environment.mode
}
