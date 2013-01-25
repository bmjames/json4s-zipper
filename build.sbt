name := "lift-json-zipper"

version := "1.0-SNAPSHOT"

scalaVersion := "2.9.2"

libraryDependencies ++= Seq(
  "net.liftweb" % "lift-json_2.9.1" % "2.4"
, "org.scalaz" %% "scalaz-core" % "7.0.0-M7"
, "org.scalatest" %% "scalatest" % "1.8" % "test"
)
