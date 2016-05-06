package resources

import play.api.test.FakeApplication

class ApplicationFake(additionalConfig: Map[String, _ <: Any] = Map("mode" -> play.api.Mode.Test.toString))
  extends FakeApplication(additionalConfiguration = additionalConfig) {
  override val mode = additionalConfig.getOrElse("mode", "No Mode").toString.toLowerCase() match {
    case "dev" => play.api.Mode.Dev
    case "prod" => play.api.Mode.Prod
    case _ => play.api.Mode.Test
  }
}
