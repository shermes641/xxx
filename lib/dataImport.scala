/**
 * Creates seed data to bootstrap the database.
 */

import models._
import play.api.Logger
import play.api._
import play.api.ApplicationLoader.Context
import play.api.db.evolutions.Evolutions
import play.api.db.{DBComponents, Database, HikariCPComponents}
import play.api.libs.mailer._
import play.api.libs.ws.ning.NingWSClient
import play.api.mvc._
import play.api.mvc.Results._
import router.Routes
import scala.concurrent.Future
import Play.current

// The "correct" way to start the app
val env = Environment(new java.io.File("."), this.getClass.getClassLoader, Mode.Prod)
val context = ApplicationLoader.createContext(env)
val loader = ApplicationLoader(context)

val components = new MainComponents(context)
val adProviderService = components.adProviderService

adProviderService.loadAll()
