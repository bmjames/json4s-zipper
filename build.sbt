name := "json4s-zipper"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.0"

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-core" % "3.1.0"
, "org.scalaz" %% "scalaz-core" % "7.0.0-M8"
, "org.scalatest" %% "scalatest" % "1.9.1" % "test"
, "org.json4s" %% "json4s-native" % "3.1.0" % "test"
)
