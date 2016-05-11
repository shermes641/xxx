
import models._
import play.api.mvc.Results._
import play.api.mvc._
import play.api.{Application, Logger}

import scala.concurrent.Future
// $COVERAGE-OFF$
// Filter to secure staging server. We should remove this before Mediation goes live.
object HTTPAuthFilter extends Filter with ConfigVars {
  def apply(next: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {
    request.tags.get("ROUTE_CONTROLLER") match {
      case Some(controller: String) if controller != "controllers.APIController" && controller != "controllers.Assets" && Environment.isStaging =>
        val httpAuthUser = ConfigVarsApp.httpAuthUser
        val httpAuthPassword = ConfigVarsApp.httpAuthPw

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

      case _ =>
        next(request)
    }
  }
}

// Redirect to HTTPS in production
object HTTPSFilter extends Filter {
  def apply(next: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {
    if (Environment.isProdOrStaging && !request.headers.get("x-forwarded-proto").getOrElse("").contains("https")) {
      Future.successful(MovedPermanently("https://" + request.host + request.uri))
    } else {
      next(request)
    }
  }
}
// $COVERAGE-ON$

object Global extends WithFilters(HTTPSFilter, HTTPAuthFilter) {

  private def systemExit(msg: String, errorCode: Int, obj: Any) = {
    Logger.error(msg)
    Logger.debug(obj.toString)
    if (!Environment.isProd)
      Environment.TestErrorCode = errorCode
    else
      sys.exit(errorCode)
  }

  //TODO when we move to Play 2.4 we will move this code to a dependency injected class
  override def beforeStart(app: Application) {
    object Vars extends ConfigVars
    Environment.TestErrorCode = 0
    Logger.info(s"Before Application startup ... isReviewApp: ${Environment.isReviewApp} ")
    // check for start up errors
    if (Vars.ConfigVarsKeen.error.isDefined) {
      systemExit(s"Keen configuration error: ${Vars.ConfigVarsKeen.error.get}", Constants.Errors.KeenConfigError, Vars.ConfigVarsKeen)
    }
    if (Vars.ConfigVarsAdProviders.iosID != Constants.AdProviderConfig.IosID || Vars.ConfigVarsAdProviders.androidID != Constants.AdProviderConfig.AndroidID) {
      systemExit(s"AdProvider configuration error iosID: ${Vars.ConfigVarsAdProviders.iosID}   androidID: ${Vars.ConfigVarsAdProviders.androidID} ",
        Constants.Errors.AdProviderError,
        Vars.ConfigVarsAdProviders)
    }
  }

  override def onStart(app: Application) {
    if (!Environment.isProd) {
      Logger.debug("Loading Ad Providers .....")
      // make sure all ad providers exist
      AdProvider.loadAll()
    }

    //TODO check required environ vars

    val numberOfAdProviders = AdProvider.findAll.length
    val expectedNumberOfAdProviders = Platform.Android.allAdProviders.length + Platform.Android.allAdProviders.length
    if (numberOfAdProviders != expectedNumberOfAdProviders) {
      Logger.warn(s"Expected number of ad providers does not match actual number of ad providers expected: $expectedNumberOfAdProviders   actual: $numberOfAdProviders")
    }
  }
}
