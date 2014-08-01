name := """MediationApi"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "mysql" % "mysql-connector-java" % "5.1.18",
  "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.0",
  "com.github.t3hnar" %% "scala-bcrypt" % "2.4"
)
