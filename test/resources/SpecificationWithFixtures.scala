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
  val companyName = "Some Company"

  def insertData =
    DB.withConnection { implicit c =>
      val hashedPassword = password.bcrypt(generateSalt)

      val distributorID: Option[Long] = SQL("INSERT INTO distributors (name) VALUES ('TestDistributor')").executeInsert()

      SQL("INSERT INTO distributor_users (email, hashed_password, distributor_id) VALUES ({email}, {pwd}, {distributor_id})")
        .on("pwd" -> hashedPassword, "email" -> email, "distributor_id" -> distributorID.get).executeInsert()

      SQL("INSERT INTO apps (name, distributor_id) VALUES ('TestApp', {distributor_id})")
        .on("distributor_id" -> distributorID.get).executeInsert()
      SQL("INSERT INTO waterfalls (name, property_id) VALUES ('TestWaterfall', 1)").executeInsert()

      SQL("INSERT INTO ad_providers (name) VALUES ('AdProvider 1')").executeInsert()
      SQL("INSERT INTO ad_providers (name) VALUES ('AdProvider 2')").executeInsert()

      SQL("INSERT INTO waterfall_ad_providers (waterfall_id, ad_provider_id) VALUES (1, 1)").executeInsert()
      SQL("INSERT INTO waterfall_ad_providers (waterfall_id, ad_provider_id, waterfall_order) VALUES (1, 2, 0)").executeInsert()

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
