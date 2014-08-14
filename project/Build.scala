import java.util.jar._
import sbt._
import sbt.Keys._
import sbtrelease.ReleasePlugin._
import sbtrelease._
import ReleaseStateTransformations._
import xerial.sbt.Sonatype._
import com.typesafe.sbt.pgp._

object Build extends Build {

  val commonSettings = Seq(
    organization := "com.gu",
    scalaVersion := "2.10.4",
    crossScalaVersions := Seq("2.10.4", "2.11.2"),
    scalacOptions ++= Seq("-feature", "-deprecation", "-language:higherKinds", "-Xfatal-warnings")
  )

  val publishSettings = releaseSettings ++ sonatypeSettings ++ Seq(
    scmInfo := Some(ScmInfo(
      url("https://github.com/bmjames/json4s-zipper"),
      "scm:git:git@github.com:bmjames/json4s-zipper.git"
    )),
    pomExtra := (
      <url>https://github.com/bmjames/json4s-zipper</url>
          <developers>
        <developer>
        <id>bmjames</id>
        <name>Ben James</name>
        <url>https://github.com/bmjames</url>
          </developer>
        </developers>
    ),
    licenses := Seq(
      "Apache V2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")
    ),
    ReleaseKeys.crossBuild := true,
    ReleaseKeys.releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      ReleaseStep(
        action = state => Project.extract(state).runTask(PgpKeys.publishSigned, state)._1,
        enableCrossBuild = true
      ),
      setNextVersion,
      commitNextVersion,
      ReleaseStep(state => Project.extract(state).runTask(SonatypeKeys.sonatypeReleaseAll, state)._1),
      pushChanges
    )
  )

  val scalazVersion = "7.1.0"
  val scalacheckVersion = "1.11.0"
  val scalatestVersion = "2.2.1"
  val json4sVersion = "3.2.10"
  val playVersion = "2.3.3"

  val core = Project("core", file("core"))
    .settings(commonSettings ++ publishSettings: _*)
    .settings(
      name := "json-zipper-core",
      description := "JSON Zipper core library",
      libraryDependencies += "org.scalaz" %% "scalaz-core" % scalazVersion
    )

  val scalacheckBinding = Project("scalacheck-binding", file("scalacheck-binding"))
    .dependsOn(core)
    .settings(commonSettings ++ publishSettings: _*)
    .settings(
      name := "json-zipper-scalacheck-binding",
      description := "JSON Zipper ScalaCheck binding",
      libraryDependencies += "org.scalacheck" %% "scalacheck" % scalacheckVersion
    )

  val json4s = Project("json4s", file("json4s"))
    .dependsOn(core, scalacheckBinding)
    .settings(commonSettings ++ publishSettings: _*)
    .settings(
      name := "json-zipper-json4s",
      description := "JSON Zipper Json4s binding",
      libraryDependencies ++= Seq(
        "org.json4s" %% "json4s-core" % json4sVersion,
        "org.scalacheck" %% "scalacheck" % scalacheckVersion % "test"
      ))

  val play = Project("play", file("play"))
    .dependsOn(core, scalacheckBinding)
    .settings(commonSettings ++ publishSettings: _*)
    .settings(
      name := "json-zipper-play",
      description := "JSON Zipper Play binding",
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
        "org.scalatest" %% "scalatest" % scalatestVersion % "test",
        "org.json4s" %% "json4s-native" % json4sVersion % "test"
      ))

  val root = Project("root", file("."))
    .settings(commonSettings: _*)
    .aggregate(core, json4s, play, scalacheckBinding, test)
}
