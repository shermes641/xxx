import play.api.Play
import play.api.mvc._
import scala.concurrent.Future
import play.api.mvc.Results.Unauthorized

// Filter to secure staging server. We should remove this before Mediation goes live.
object HTTPAuthFilter extends Filter {
  def apply(next: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {
    val isStaging = Play.current.configuration.getString("staging") match {
      case Some(_: String) if(play.api.Play.isProd(play.api.Play.current)) => true
      case None => false
    }
    request.tags.get("ROUTE_CONTROLLER") match {
      case Some(controller: String) if(controller != "controllers.APIController" && isStaging) => {
        val httpAuthUser = Play.current.configuration.getString("httpAuthUser").get
        val httpAuthPassword = Play.current.configuration.getString("httpAuthPassword").get

        request.headers.get("Authorization").flatMap { authorization =>
          authorization.split(" ").drop(1).headOption.filter { encoded =>
            new String(org.apache.commons.codec.binary.Base64.decodeBase64(encoded.getBytes)).split(":").toList match {
              case user :: password :: Nil if user == httpAuthUser && password == httpAuthPassword => true
              case _ => false
            }
          }.map(_ => next(request))
        }.getOrElse {
          Future.successful(Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="Secured""""))
        }
      }
      case _ => {
        next(request)
      }
    }
  }
}

object Global extends WithFilters(HTTPAuthFilter)
