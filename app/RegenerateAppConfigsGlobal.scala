import play.api.Application
import play.api.GlobalSettings
import tasks.RegenerateAppConfigs

object RegenerateAppConfigsGlobal extends GlobalSettings {
  /**
   * Overrides application start up to include the initialization of the script to regenerate AppConfigs.
   * @param app The play application
   */
  override def onStart(app: Application): Unit = {
    RegenerateAppConfigs.run
  }
}
