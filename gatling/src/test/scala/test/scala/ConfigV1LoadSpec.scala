package test.scala

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Run: <./activator gatling/load:test> or
  * <./activator "gatling/gatling:testOnly *ConfigV1LoadSpec">
  * After running this test run <heroku ps:scale web=1 -a mediation-staging>
  * to reset the dynos to 1, as they may have scaled up.
  *
  * This test depends on DB data in staging as of 05/17/2016, if it changes this test may fail.
  * This test will cause the Addept scaling addon to scale of dynos depending on historical web traffic.
  * I have seen this test fail when dynos are starting up. If the test is started when there are 8 or more dynos
  * already started, then there is no problem. Adept reccomends a warm up period before running heavy load tests.
  *
  * TODO start a seeded version of the mediation app and use it for load testing
  */
class ConfigV1LoadSpec extends Simulation {
  //need the correct auth header, this can be found by using gatling recorder
  //TODO used mediation-smh to not pummel mediation-staging, but need to change this for
  val httpsProtocol = http
    .baseURL("https://mediation-staging.herokuapp.com")
    .inferHtmlResources()
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate, br")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .authorizationHeader("Basic bWVkaWF0aW9uOnRyaWVkIGluY29tZSBncmVlbiByb3V0ZQ==")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.11; rv:45.0) Gecko/20100101 Firefox/45.0")
    .acceptLanguageHeader("en-US,en;q=0.8")

  val scn = scenario("UnityCallbackV1LoadSpec")
    .exec(http("request_0")
      .get("/v1/app_configs/87551823-8d6f-475e-8ae9-bfae5ee40997")
      .check(status.is(200))
    )
    .exec(http("request_1")
      .get("/v1/reward_callbacks/87551823-8d6f-475e-8ae9-bfae5ee40997/unity_ads?sid=testuser1&oid=562832418&hmac=11be64e7d09fa8e69cf2a1d7570e864a")
      .check(status.is(200))
    )

  val scn1 = scenario("UserLoadSpec")
    .exec(http("request_3")
      .get("/v1/app_configs/8686055c-8759-4eff-b69f-442828f95e9f")
      .check(status.is(200)))

  /**
    * see http://gatling.io/docs/2.1.1/general/simulation_setup.html
    * and http://www.anthony-galea.com/blog/post/an-introduction-to-load-testing-with-gatling/
    *
    * This is an example of several of the features for ramping users
    * Note that scn and scn1 will run in parallel
    * If you have more dynos running before running this test, the load can be bumped up.
    * This test sometimes fails when started with only 1 active dyno
    */
  setUp(
    scn.inject(
      constantUsersPerSec(2) during (30 seconds),
      constantUsersPerSec(2) during (30 seconds) randomized,
      rampUsersPerSec(4) to 100 during (30 seconds),
      rampUsersPerSec(2) to 100 during (60 seconds) randomized,
      splitUsers(30) into (rampUsers(10) over (30 seconds)) separatedBy (10 seconds),
      splitUsers(30) into (rampUsers(10) over (30 seconds)) separatedBy atOnceUsers(10)
    ),
    scn1.inject(rampUsers(700) over (3 minutes))
  )
    .assertions(
      // these values are based on limited testing starting the test with 1 dyno active
      global.responseTime.mean.lessThan(5000),
      global.responseTime.max.lessThan(35000),
      global.successfulRequests.percent.greaterThan(99)
    )
    .maxDuration(4 minutes)
    .protocols(httpsProtocol)
}
