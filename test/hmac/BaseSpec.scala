package hmac

import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, PropSpec, _}
import play.api.Play
import play.api.test.FakeApplication

// Set HMAC Play app config vars
trait TestAdditionalConf extends SuiteMixin { this: Suite =>

  val HmacTestConfig = Map(
    "db.default.url" -> "jdbc:postgresql://localhost/mediation_test?user=postgres&password=postgres",
    "play.evolutions.enabled" -> "true"
  )

  implicit lazy val app: FakeApplication = new FakeApplication(
    // Overwrite configuration settings
    additionalConfiguration = HmacTestConfig
  )

  abstract override def run(testName: Option[String], args: Args): Status = {
    Play.start(app)
    try {
      val newConfigMap = args.configMap + ("org.scalatestplus.play.app" -> app)
      val newArgs = args.copy(configMap = newConfigMap)
      val status = super.run(testName, newArgs)
      status.whenCompleted { _ => Play.stop(app) }
      status
    }
    catch {
      case ex: Throwable =>
        Play.stop(app)
        throw ex
    }
  }
}

trait BaseSpec extends PropSpec with PropertyChecks with Matchers with TestAdditionalConf {

  def notEmpty(s: String*) = !s.contains("")
}
