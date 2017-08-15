name := "WsService"

organization := "interview.app"

organizationName := "Interview App"

version := "1.0"

scalaVersion := "2.12.1"


val akkaHttpV = "10.0.9"

libraryDependencies += "com.typesafe.akka" %% "akka-http" % akkaHttpV

libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

libraryDependencies += "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.0-SNAP9" % "test"

enablePlugins(JavaAppPackaging)
