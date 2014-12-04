package functional

import models._
import play.api.test._
import play.api.test.Helpers._
import com.github.nscala_time.time.Imports._
import play.api.test.FakeApplication
import play.api.Play
import io.keen.client.java.ScopedKeys
import collection.JavaConversions._

class JavascriptsSpec extends SpecificationWithFixtures {

  "Javascript Tests" should {
    "Javascript QUnit Tests" in new WithFakeBrowser {
      println("##teamcity[testSuiteStarted name='Qunit Tests']")
      browser.goTo("/assets/javascripts/test/index.html")

      browser.await().atMost(10, java.util.concurrent.TimeUnit.SECONDS).until("#qunit-testresult").containsText("completed");
      browser.pageSource must contain("qunit-pass")

      println("##teamcity[testFailed name='#{result[ 'name' ]}' type='comparisonFailure' expected='#{firstFailedAssertion[ 'expected' ]}' actual='#{firstFailedAssertion[ 'actual' ]}' message='#{message}' details='#{source}']")
    }
  }

  step(clean)
}
