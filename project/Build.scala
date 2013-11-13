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

  val scalazDependencies = Seq("org.scalaz" %% "scalaz-core" % "7.1.0-M2")

  val testDependencies = Seq(
    "org.scalatest" %% "scalatest" % "1.9.1" % "test",
    "org.json4s" %% "json4s-native" % "3.2.4" % "test"
  )

  val core = Project("core", file("core"))
    .settings(commonSettings ++ publishSettings: _*)
    .settings(
      name := "json-zipper-core",
      libraryDependencies ++= scalazDependencies ++ testDependencies
    )

  val json4s = Project("json4s", file("json4s"))
    .dependsOn(core)
    .settings(commonSettings ++ publishSettings: _*)
    .settings(
      name := "json-zipper-json4s",
      libraryDependencies += "org.json4s" %% "json4s-core" % "3.2.4"
    )

  val test = Project("test", file("test"))
    .dependsOn(core, json4s)
    .settings(libraryDependencies ++= testDependencies)

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
