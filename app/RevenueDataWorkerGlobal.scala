import play.api.Application
import play.api.GlobalSettings
import models.RevenueDataActor
import scala.language.postfixOps

object RevenueDataWorkerGlobal extends GlobalSettings {
  /**
   * Overrides application start up to include the initialization of the revenue collection background job.
   * @param app The play application
   */
  override def onStart(app: Application) {
    if(play.api.Play.isProd(app)) {
      RevenueDataActor.startRevenueDataCollection(app)
    }
  }
}
