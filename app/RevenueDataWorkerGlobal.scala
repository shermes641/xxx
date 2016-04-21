import models.{RevenueDataActor, Environment}
import play.api.Application
import play.api.GlobalSettings
import scala.language.postfixOps
// $COVERAGE-OFF$
object RevenueDataWorkerGlobal extends GlobalSettings {
  /**
   * Overrides application start up to include the initialization of the revenue collection background job.
   * @param app The play application
   */
  override def onStart(app: Application) {
    if(Environment.isProdOrStaging) {
      RevenueDataActor.startRevenueDataCollection(app)
    }
  }
}
// $COVERAGE-ON$
