name := "MediationApi"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.1"

scalacOptions ++= Seq("-feature") // show feature warnings in console

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .configs(ItTest).settings(inConfig(ItTest)(Defaults.testTasks): _*)
  .configs(FunTest).settings(inConfig(FunTest)(Defaults.testTasks): _*)
  .configs(KeenTest).settings(inConfig(KeenTest)(Defaults.testTasks): _*)
  .configs(HmacTest).settings(inConfig(HmacTest)(Defaults.testTasks): _*)
  .configs(UnitTest).settings(inConfig(UnitTest)(Defaults.testTasks): _*)
  .configs(NoKeenTest).settings(inConfig(NoKeenTest)(Defaults.testTasks): _*)

javaOptions in Test += "-Dconfig.file=conf/test.conf"

lazy val ItTest = config("it") extend Test
lazy val FunTest = config("fun") extend Test
lazy val KeenTest = config("keen") extend Test
lazy val HmacTest = config("hmac") extend Test
lazy val UnitTest = config("unit") extend Test
lazy val NoKeenTest = config("nokeen") extend Test

def hmacTestFilter(name: String): Boolean = {
  if (name startsWith "hmac.") {
    println("HMAC test: " + name)
    true
  } else
    false
}

def notKeenTestFilter(name: String): Boolean = {
  if ((name startsWith "functional.Keen") || (name startsWith "models.Keen")) {
    false
  } else
    true
}

def keenTestFilter(name: String): Boolean = {
  if ((name startsWith "functional.Keen") ||
    (name startsWith "models.Keen") ||
    (name startsWith "integration.Keen")) {
    println("KEEN test: " + name)
    true
  } else
    false
}

def itTestFilter(name: String): Boolean = {
  if ((name startsWith "integration.") && notKeenTestFilter(name)) {
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

testOptions in ItTest := Seq(Tests.Filter(itTestFilter))

testOptions in FunTest := Seq(Tests.Filter(funTestFilter))

testOptions in KeenTest := Seq(Tests.Filter(keenTestFilter))

testOptions in HmacTest := Seq(Tests.Filter(hmacTestFilter))

testOptions in UnitTest := Seq(Tests.Filter(unitTestFilter))

testOptions in NoKeenTest := Seq(Tests.Filter(noKeenTestFilter))

logLevel in Test := Level.Info

logLevel in ItTest := Level.Info

logLevel in FunTest := Level.Info

logLevel in KeenTest := Level.Info

logLevel in HmacTest := Level.Info

logLevel in UnitTest := Level.Info

logLevel in NoKeenTest := Level.Info

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "org.postgresql" % "postgresql" % "9.3-1100-jdbc4",
  "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.1",
  "com.github.t3hnar" %% "scala-bcrypt" % "2.4",
  "org.clapper" %% "grizzled-slf4j" % "1.0.2",
  "io.spray" %%  "spray-can"     % "1.3.1",
  "io.spray" %%  "spray-routing" % "1.3.1",
  "com.github.nscala-time" %% "nscala-time" % "1.8.0",
  "org.seleniumhq.selenium" % "selenium-java" % "2.48.2" % "test",
  "org.specs2" %% "specs2-junit" % "2.3.12",
  "com.newrelic.agent.java" % "newrelic-agent" % "3.14.0",
  "com.newrelic.agent.java" % "newrelic-api" % "3.14.0",
  "org.mockito" % "mockito-core" % "1.9.5" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.6" % "test",
  "com.github.tototoshi" %% "scala-csv" % "1.1.2",
  "org.scalatestplus" % "play_2.11" % "1.5.0-SNAP1",
  "org.scalacheck" %% "scalacheck" % "1.12.5" % "test",
  "io.spray" %% "spray-testkit" % "1.3.2" % "test",
  "com.netaporter" %% "scala-uri" % "0.4.12"
)

//TODO normally you would set this for all tests
//TODO but there are issues with not removing the instrumentation code from runtime classes
//TODO Therefore if you want coverage, run tests with "clean coverage"   ie: "./activator clean coverage test"
// coverageEnabled := true

coverageMinimum := 70

coverageFailOnMinimum := false

coverageOutputTeamCity := true
