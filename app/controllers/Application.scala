package controllers

import play.api.mvc._
import play.api.data.validation._

object Application extends Controller with Secured {

  def index = withUser { user => implicit request =>
    Redirect(routes.AppsController.index(user.distributorID.get))
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
