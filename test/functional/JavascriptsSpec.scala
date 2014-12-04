package functional

import models._
import play.api.test._
import play.api.test.Helpers._
import com.github.nscala_time.time.Imports._
import play.api.test.FakeApplication
import play.api.Play
import io.keen.client.java.ScopedKeys
import collection.JavaConversions._
import org.fluentlenium.core.domain.FluentList
import org.fluentlenium.core.domain.FluentWebElement

class JavascriptsSpec extends SpecificationWithFixtures {

  "Javascript Tests" should {
    "Javascript QUnit Tests" in new WithFakeBrowser {
      println("##teamcity[testSuiteStarted name='Qunit Tests']")
      browser.goTo("/assets/javascripts/test/index.html")

      browser.await().atMost(10, java.util.concurrent.TimeUnit.SECONDS).until("#qunit-testresult").containsText("completed")

      val fls = browser.$("#qunit-tests > li > strong")
      val iterator = fls.iterator()
      val index = 0
      while (iterator.hasNext) {
        val testName = iterator.next.getText
        println("##teamcity[testStarted name='" + testName + "']")
        println("##teamcity[testFinished name='" + testName + "' duration='0']")
      }

      browser.pageSource must contain("qunit-pass")
    }
  }

  step(clean)
}
