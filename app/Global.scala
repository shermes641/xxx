import models.Environment
import play.api.mvc._
import play.api.mvc.Results._
import play.api.Play
import scala.concurrent.Future

// Filter to secure staging server. We should remove this before Mediation goes live.
object HTTPAuthFilter extends Filter {
  def apply(next: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {
    request.tags.get("ROUTE_CONTROLLER") match {
      case Some(controller: String) if(controller != "controllers.APIController" && controller != "controllers.Assets" && Environment.isStaging) => {
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

// Redirect to HTTPS in production
object HTTPSFilter extends Filter {
  def apply(next: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {
    if(Environment.isProdOrStaging && !request.headers.get("x-forwarded-proto").getOrElse("").contains("https")) {
      Future.successful(MovedPermanently("https://" + request.host + request.uri))
    } else {
      next(request)
    }
  }
}

object Global extends WithFilters(HTTPSFilter, HTTPAuthFilter)
