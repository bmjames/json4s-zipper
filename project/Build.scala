import java.util.jar._
import sbt._
import sbt.Keys._


object Build extends Build {

  val commonSettings = Seq(
    organization := "com.gu",
    scalaVersion := "2.10.3",
    version      := "0.1-SNAPSHOT",
    scalacOptions ++= Seq("-feature", "-deprecation", "-language:higherKinds", "-Xfatal-warnings")
  )

  val scalazVersion = "7.1.0-M3"
  val scalacheckVersion = "1.11.0"
  val json4sVersion = "3.2.6"
  val playVersion = "2.2.1"

  val core = Project("core", file("core"))
    .settings(commonSettings ++ publishSettings: _*)
    .settings(
      name := "json-zipper-core",
      libraryDependencies += "org.scalaz" %% "scalaz-core" % scalazVersion
    )

  val scalacheckBinding = Project("scalacheck-binding", file("scalacheck-binding"))
    .dependsOn(core)
    .settings(commonSettings ++ publishSettings: _*)
    .settings(name := "json-zipper-scalacheck-binding")
    .settings(libraryDependencies += "org.scalacheck" %% "scalacheck" % scalacheckVersion)

  val json4s = Project("json4s", file("json4s"))
    .dependsOn(core, scalacheckBinding)
    .settings(commonSettings ++ publishSettings: _*)
    .settings(
      name := "json-zipper-json4s",
      libraryDependencies ++= Seq(
        "org.json4s" %% "json4s-core" % json4sVersion,
        "org.scalacheck" %% "scalacheck" % scalacheckVersion % "test"
      ))

  val play = Project("play", file("play"))
    .dependsOn(core, scalacheckBinding)
    .settings(commonSettings ++ publishSettings: _*)
    .settings(
      name := "json-zipper-play",
      resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
      libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play-json" % playVersion,
        "org.scalacheck" %% "scalacheck" % scalacheckVersion % "test"
      ))

  val test = Project("test", file("test"))
    .dependsOn(core, json4s, play)
    .settings(
      publishArtifact := false,
      libraryDependencies ++= Seq(
        "org.scalatest" %% "scalatest" % "1.9.1" % "test",
        "org.json4s" %% "json4s-native" % json4sVersion % "test"
      ))

  def publishSettings = Seq(
    publishArtifact := true,
    packageOptions <+= (version, name) map { (v, n) =>
      Package.ManifestAttributes(
        Attributes.Name.IMPLEMENTATION_VERSION -> v,
        Attributes.Name.IMPLEMENTATION_TITLE -> n,
        Attributes.Name.IMPLEMENTATION_VENDOR -> "guardian.co.uk"
      )
    },
    publishTo <<= version { version: String =>
      val publishType = if (version.endsWith("SNAPSHOT")) "snapshots" else "releases"
      Some(Resolver.file(
        "guardian github " + publishType,
        file(System.getProperty("user.home") + "/guardian.github.com/maven/repo-" + publishType)
      ))
    }
  )

}
