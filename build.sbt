name := "scala_sql_insert_benchmark"

version := "0.1"

scalaVersion := "2.13.5"

lazy val root = (project in file("."))
  .settings(
    // postgres
    libraryDependencies += "org.postgresql" % "postgresql" % "42.2.19",
    // logging
    libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3",
  )
