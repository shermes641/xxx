package security

import admin.AdminService
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{AuthenticatedRequest, DeadboltHandler, DynamicResourceHandler, HandlerKey}
import javax.inject.Inject
import play.api.mvc.{Request, Result, Results}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AdminDeadboltHandler(adminService: AdminService,
                           dynamicResourceHandler: Option[DynamicResourceHandler] = None) extends DeadboltHandler {

  override def beforeAuthCheck[A](request: Request[A]): Future[Option[Result]] = Future {None}

  override def getDynamicResourceHandler[A](request: Request[A]): Future[Option[DynamicResourceHandler]] = Future {None}

  override def getSubject[A](request: AuthenticatedRequest[A]): Future[Option[be.objectify.deadbolt.core.models.Subject]] =
    Future {
      val email: String = request.session.get("username").getOrElse("")
      val distributorID: Long = request.session.get("distributorID").getOrElse("0").toLong
      adminService.find(email, distributorID)
    }

  override def onAuthFailure[A](request: AuthenticatedRequest[A]): Future[Result] = {
    Future { Results.Redirect(controllers.routes.Application.notFound()) }
  }
}

/**
  * Must be implemented for Deadbolt
  * @param adminService Encapsulates functions for Admin class
  */
class AdminHandlerCache @Inject() (adminService: AdminService) extends HandlerCache {
  val adminHandler: DeadboltHandler = new AdminDeadboltHandler(adminService)

  val handlers: Map[Any, DeadboltHandler] = Map("admin" -> adminHandler)

  // Get the default handler.
  override def apply(): DeadboltHandler = adminHandler

  // Get a named handler
  override def apply(handlerKey: HandlerKey): DeadboltHandler = handlers(handlerKey)
}
