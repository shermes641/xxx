package models

import anorm._
import com.github.t3hnar.bcrypt._
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable._
import play.api.Play.current
import play.api.db.DB
import play.api.test.Helpers._
import play.api.test._

/**
 * Created by jeremy on 7/1/14.
 */
abstract class SpecificationWithFixtures extends Specification {

  val email = "tdepplito@jungroup.com"
  val password = "password"

  def insertData =
    DB.withConnection { implicit c =>
      val hashedPassword = password.bcrypt(generateSalt)

      SQL("INSERT INTO DistributorUser (email, hashed_password) VALUES ({email}, {pwd})")
        .on("pwd" -> hashedPassword, "email" -> email).executeUpdate()

      SQL("INSERT INTO Distributor (name) VALUES ('TestDistributor')").executeUpdate()
      SQL("INSERT INTO Property (name, distributor_id) VALUES ('TestProperty', 1)").executeUpdate()
      SQL("INSERT INTO Waterfall (name, property_id) VALUES ('TestWaterfall', 1)").executeUpdate()

      SQL("INSERT INTO AdProvider (name) VALUES ('AdProvider 1')").executeUpdate()
      SQL("INSERT INTO AdProvider (name) VALUES ('AdProvider 2')").executeUpdate()

      SQL("INSERT INTO WaterfallAdProvider (waterfall_id, ad_provider_id) VALUES (1, 1)").executeUpdate()
      SQL("INSERT INTO WaterfallAdProvider (waterfall_id, ad_provider_id, waterfall_order) VALUES (1, 2, 0)").executeUpdate()

    }

  abstract class WithApplicationAndFixtures extends WithApplication(FakeApplication(additionalConfiguration = inMemoryDatabase())) {

    override def around[T: AsResult](t: => T): Result = super.around {
      insertData
      t
    }
  }

  abstract class WithFakeDB extends WithApplication(FakeApplication(additionalConfiguration = inMemoryDatabase())) {

    override def around[T: AsResult](t: => T): Result = super.around {
      t
    }
  }

  abstract class WithBrowserAndFixtures extends WithBrowser(app = FakeApplication(additionalConfiguration = inMemoryDatabase())) { //, webDriver = WebDriverFactory(Helpers.FIREFOX)

    override def around[T: AsResult](t: => T): Result = super.around {
      insertData
      t
    }
  }

  abstract class WithFakeBrowser extends WithBrowser(app = FakeApplication(additionalConfiguration = inMemoryDatabase())) { //, webDriver = WebDriverFactory(Helpers.FIREFOX)

    override def around[T: AsResult](t: => T): Result = super.around {
      t
    }
  }

}
