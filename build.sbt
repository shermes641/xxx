name := "MediationApi"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-feature") // show feature warnings in console

lazy val gatling = project.in(file("gatling"))
  .enablePlugins(GatlingPlugin)
  .configs(LoadTest).settings(inConfig(Gatling)(Defaults.testTasks): _*)
  .settings(
    libraryDependencies ++= Seq(
      "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.2.0" % "test",
      "io.gatling" % "gatling-test-framework" % "2.2.0" % "test"
    )
  )

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .configs(TaskTest).settings(inConfig(TaskTest)(Defaults.testTasks): _*)
  .configs(ItTest).settings(inConfig(ItTest)(Defaults.testTasks): _*)
  .configs(FunTest).settings(inConfig(FunTest)(Defaults.testTasks): _*)
  .configs(KeenTest).settings(inConfig(KeenTest)(Defaults.testTasks): _*)
  .configs(HmacTest).settings(inConfig(HmacTest)(Defaults.testTasks): _*)
  .configs(UnitTest).settings(inConfig(UnitTest)(Defaults.testTasks): _*)
  .configs(NoKeenTest).settings(inConfig(NoKeenTest)(Defaults.testTasks): _*)

lazy val TaskTest = config("task") extend Test
lazy val ItTest = config("it") extend Test
lazy val FunTest = config("fun") extend Test
lazy val KeenTest = config("keen") extend Test
lazy val HmacTest = config("hmac") extend Test
lazy val UnitTest = config("unit") extend Test
lazy val NoKeenTest = config("nokeen") extend Test
lazy val LoadTest = config("load") extend Gatling

def hmacTestFilter(name: String): Boolean = {
  if (name startsWith "hmac.") {
    println("HMAC test: " + name)
    true
  } else
    false
}

def taskTestFilter(name: String): Boolean = {
  if (name startsWith "tasks.") {
    println("TASK test: " + name)
    true
  } else
    false
}

def notKeenTestFilter(name: String): Boolean = {
  if ((name startsWith "functional.Keen") ||
    (name startsWith "integration.Keen") ||
    (name startsWith "models.Keen")) {
    false
  } else
    true
}

def keenTestFilter(name: String): Boolean = {
  if ((name startsWith "functional.Keen") ||
    (name startsWith "models.Keen")) {
    println("KEEN test: " + name)
    true
  } else
    false
}

def itTestFilter(name: String): Boolean = {
  if (name startsWith "integration.") {
    println("INTEGRATION test: " + name)
    true
  } else
    false
}

def funTestFilter(name: String): Boolean = {
  if ((name startsWith "functional.") && notKeenTestFilter(name)) {
    println("FUNCTIONAL test: " + name)
    true
  } else
    false
}

def unitTestFilter(name: String): Boolean = {
  if ((name startsWith "models.") && notKeenTestFilter(name)) {
    println("UNIT test: " + name)
    true
  } else
    false
}

def noKeenTestFilter(name: String): Boolean = {
  if (notKeenTestFilter(name)) {
    println("NO KEEN test: " + name)
    true
  } else
    false
}

testOptions in TaskTest := Seq(Tests.Filter(taskTestFilter))

testOptions in ItTest := Seq(Tests.Filter(itTestFilter))

testOptions in FunTest := Seq(Tests.Filter(funTestFilter))

testOptions in KeenTest := Seq(Tests.Filter(keenTestFilter))

testOptions in HmacTest := Seq(Tests.Filter(hmacTestFilter))

testOptions in UnitTest := Seq(Tests.Filter(unitTestFilter))

testOptions in NoKeenTest := Seq(Tests.Filter(noKeenTestFilter))

logLevel in Test := Level.Info

logLevel in TaskTest := Level.Info

logLevel in ItTest := Level.Info

logLevel in FunTest := Level.Info

logLevel in KeenTest := Level.Info

logLevel in HmacTest := Level.Info

logLevel in UnitTest := Level.Info

logLevel in NoKeenTest := Level.Info

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "anorm" % "2.4.0",
  cache,
  evolutions,
  "org.clapper" %% "grizzled-slf4j" % "1.0.4",
  jdbc,
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "org.apache.httpcomponents" % "httpclient" % "4.5.2",
  "io.keen" %% "keenclient-scala" % "0.7.0",
  "com.newrelic.agent.java" % "newrelic-agent" % "3.14.0",
  "com.newrelic.agent.java" % "newrelic-api" % "3.14.0",
  "com.github.nscala-time" %% "nscala-time" % "1.8.0",
  "com.typesafe.play" %% "play-mailer" % "4.0.0",
  "org.postgresql" % "postgresql" % "9.4.1208.jre7",
  "com.github.t3hnar" %% "scala-bcrypt" % "2.4",
  "org.scala-lang" % "scala-compiler" % "2.11.8",
  "com.github.tototoshi" %% "scala-csv" % "1.1.2",
  "org.scala-lang" % "scala-library" % "2.11.8",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
  "com.netaporter" %% "scala-uri" % "0.4.14",
  ws,
  // Test only dependencies
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.4" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.4" % "test",
  "org.mockito" % "mockito-core" % "1.10.19" % "test",
  "org.scalatestplus" %% "play" % "1.4.0" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.1" % "test",
  "org.scalatest" %% "scalatest" % "3.0.0-M16-SNAP6" % "test",
  "org.seleniumhq.selenium" % "selenium-java" % "2.48.2" % "test",
  specs2 % Test
)

//TODO normally you would set this for all tests
//TODO but there are issues with not removing the instrumentation code from runtime classes
//TODO Therefore if you want coverage, run tests with "clean coverage"   ie: "./activator clean coverage hmac:test"
// coverageEnabled := true

coverageMinimum := sys.env.getOrElse("COVERAGE_PERCENT", "87.0").toDouble

coverageFailOnMinimum := true

coverageOutputTeamCity := true

//TODO Eventually we will want coverage on some of these classes, like the Actors
coverageExcludedPackages := ".*AppLovinReportingActor;.*KeenExportActor;.*RevenueDataActor;.*JunGroupAPIActor;.*JunGroupEmailActor;" +
  ".*JsonConversion;.*main;.*welcomeEmailContent;.*JsonToValueHelper;.*Routes;.*subHeader;.*Reverse.*;" +
  ".*edit;.*emailTemplate;.*forgot_password;.*formErrors;.*not_found;.*passwordChangedEmail;.*CustomFormValidation;.*passwordResetEmailContent;" +
  ".*Application;.*HTTP.*;.*Regenerate.*;" +
  ".*login.*;.*reset_password.*;.*signup.*;.*newApp.*;.*show.*"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

routesGenerator := InjectedRoutesGenerator

javaOptions in Test +="-Dconfig.file=conf/test.conf"
