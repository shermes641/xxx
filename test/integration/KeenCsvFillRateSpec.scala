package integration

import java.io.File

import models._
import play.api.test.FakeApplication
import play.api.test.Helpers._
import resources.SpecificationWithFixtures

import scala.concurrent.duration.Duration
import scala.io.Source

/**
  * Test CSV fillrate logic with real data from Keen
  * Requires internet connection
  */
class KeenCsvFillRateSpec extends SpecificationWithFixtures {

  // Load AdProviders required for tests
  running(FakeApplication(additionalConfiguration = testDB)) {
    AdProvider.create(
      name = "hyprMX",
      configurationData = "{\"required_params\":[{\"description\": \"Your Vungle App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}]}",
      platformID = Platform.Ios.PlatformID,
      callbackUrlFormat = None
    )

    AdProvider.create(
      name = "Vungle",
      configurationData = "{\"required_params\":[{\"description\": \"Your Vungle App Id\", \"key\": \"appID\", \"value\":\"\", \"dataType\": \"String\"}]}",
      platformID = Platform.Ios.PlatformID,
      callbackUrlFormat = None
    )
  }

  val distributorUser = running(FakeApplication(additionalConfiguration = testDB)) {
    DistributorUser.create(email, password, "Company Name")
    DistributorUser.findByEmail(email).get
  }

  val distributorID = distributorUser.distributorID.get

  val ContainNA = true
  val oneSecDur = Duration(1000, "millis")

  def waitForCsvData(fn: String) = {
    val bufferedSource = Source.fromFile(fn)
    val res = bufferedSource.getLines.length > 1
    bufferedSource.close
    res
  }

  def waitForAndValidateCsvFillRate(appName: String, csvList: List[String], checkForNA: Boolean) = {
    true must beEqualTo(new File("tmp").listFiles.count(_.getName.endsWith(".csv")) != csvList.length).eventually(30, oneSecDur)
    val csvNewList = new File("tmp").listFiles.filter(_.getName.endsWith(".csv")).map(_.getName).toList
    // assumes a single csv file was created
    val fn = new File((csvNewList diff csvList).head).getPath
    true must beEqualTo(waitForCsvData(s"tmp/$fn")).eventually(30, oneSecDur)
    val lines = Source.fromFile(s"tmp/$fn").getLines
    var res = true
    for (line <- lines) {
      if (!line.startsWith("Date")) {
        res = res || line.contains(appName)
        checkForNA match {
          case true => res = res && line.split(",")(4) == "N/A"

          case _ => res = res && line.split(",")(4) != "N/A"
        }
      }
    }
    res
  }

  "Analytics keen integration CSV tests" should {
    "Verify with all ad providers, CSV contains fillrate" in new WithAppBrowser(distributorID) {
      logInUser()
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id), None).url)
      // Verify analytics data has been loaded
      waitUntilContainsText("#analytics-loading-status", "Waiting...")
      // eCPM metric
      verifyAnalyticsHaveLoaded

      browser.click("#export-as-csv")
      browser.await().atMost(3000).until("#email-modal").areDisplayed()
      browser.fill("input[id=export-email]").`with`("s@s.com")
      browser.find("#export-submit").click()
      browser.await().atMost(500)

      val csvList = try {
        new File("tmp").listFiles.filter(_.getName.endsWith(".csv")).map(_.getName).toList
      } catch {
        case _: Throwable => List[String]()
      }
      browser.find(".close-button").click()
      waitForAndValidateCsvFillRate(currentApp.name, csvList, !ContainNA) must beEqualTo(true)
    }

    "Verify with one ad providers, CSV contains fillrate" in new WithAppBrowser(distributorID) {
      logInUser()
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id), None).url)
      verifyAnalyticsHaveLoaded

      clickAndWaitForAngular("#ad-providers-filter .add")
      clickAndWaitForAngular("#filter-ad_providers")
      // hyprMX must be part of dropdown
      waitUntilContainsText("#ad-providers-filter .add", "hyprMX")
      clickAndWaitForAngular("#ad-providers-filter .add .dropdown-menu .active")
      // Verify analytics data has been loaded
      waitUntilContainsText("#analytics-loading-status", "Waiting...")
      verifyAnalyticsHaveLoaded

      browser.click("#export-as-csv")
      browser.await().atMost(3000).until("#email-modal").areDisplayed()
      browser.fill("input[id=export-email]").`with`("s@s.com")
      browser.find("#export-submit").click()
      browser.await().atMost(500)

      val csvList = try {
        new File("tmp").listFiles.filter(_.getName.endsWith(".csv")).map(_.getName).toList
      } catch {
        case _: Throwable => List[String]()
      }
      browser.find(".close-button").click()
      waitForAndValidateCsvFillRate(currentApp.name, csvList, !ContainNA) must beEqualTo(true)
    }

    "Verify with more than one ad providers, CSV does not contain fillrate" in new WithAppBrowser(distributorID) {
      logInUser()
      goToAndWaitForAngular(controllers.routes.AnalyticsController.show(distributorID, Some(currentApp.id), None).url)
      verifyAnalyticsHaveLoaded

      clickAndWaitForAngular("#ad-providers-filter .add")
      clickAndWaitForAngular("#filter-ad_providers")

      fillAndWaitForAngular("#filter-ad_providers", "Vungl")
      waitUntilContainsText("#ad-providers-filter .add", "Vungle")
      clickAndWaitForAngular("#ad-providers-filter .add .dropdown-menu .active")

      clickAndWaitForAngular("#ad-providers-filter .add")
      fillAndWaitForAngular("#filter-ad_providers", "hyprMX")
      waitUntilContainsText("#ad-providers-filter .add", "hyprMX")
      clickAndWaitForAngular("#ad-providers-filter .add .dropdown-menu .active")

      // Verify analytics data has been loaded
      waitUntilContainsText("#analytics-loading-status", "Waiting...")
      verifyAnalyticsHaveLoaded

      browser.click("#export-as-csv")
      browser.await().atMost(3000).until("#email-modal").areDisplayed()
      browser.fill("input[id=export-email]").`with`("s@s.com")
      browser.find("#export-submit").click()
      browser.await().atMost(500)

      val csvList = try {
        new File("tmp").listFiles.filter(_.getName.endsWith(".csv")).map(_.getName).toList
      } catch {
        case _: Throwable => List[String]()
      }
      browser.find(".close-button").click()
      waitForAndValidateCsvFillRate(currentApp.name, csvList, ContainNA) must beEqualTo(true)
    }
  }
}

