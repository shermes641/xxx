package controllers

import javax.inject._
import models.DistributorUserService
import play.api.mvc._
import play.api.data.validation._

/**
  * Controller for index and not found actions
  * @param distributorUserService Shared instance of DistributorUserService
  */
@Singleton
class Application @Inject() (distributorUserService: DistributorUserService) extends Controller with Secured {
  override val authUser = distributorUserService // Used in Secured trait

  def index = withUser { user => implicit request =>
    Redirect(routes.AnalyticsController.show(user.distributorID.get, None, None))
  }

  /**
   * Renders the 404 - Not Found page
   * @return The view for our 404 page
   */
  def notFound = Action { implicit request =>
    NotFound(views.html.not_found())
  }
}

trait CustomFormValidation {
  /**
   * Form validation to check for empty field
   * @param fieldName Name of empty field to be displayed in error message
   * @return A valid or invalidated constraint containing error messages if necessary
   */
  def nonEmptyConstraint(fieldName: String): Constraint[String] = Constraint("constraints.nonEmptyCheck")({
    plainText => {
      val errors = plainText match {
        case "" => Seq(ValidationError(fieldName + " is required."))
        case _ => Nil
      }
      if (errors.isEmpty) {
        Valid
      } else {
        Invalid(errors)
      }
    }
  })
}
