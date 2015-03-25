name := "MediationApi"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

scalacOptions ++= Seq("-feature") // show feature warnings in console

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
  "org.seleniumhq.selenium" % "selenium-java" % "2.45.0" % "test",
  "org.specs2" %% "specs2-junit" % "2.3.12",
  "com.newrelic.agent.java" % "newrelic-agent" % "3.14.0",
  "com.newrelic.agent.java" % "newrelic-api" % "3.14.0",
  "org.mockito" % "mockito-core" % "1.9.5" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.6" % "test",
  "com.github.tototoshi" %% "scala-csv" % "1.1.2"
)
