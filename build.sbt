lazy val scala212 = "2.12.8"
lazy val scala211 = "2.11.12"
lazy val supportedScalaVersions = List(scala212, scala211)

ThisBuild / organization := "io.github.CyberAgent"
ThisBuild / scalaVersion := scala212

lazy val root = (project in file("."))
  .settings(
    name := "tunable-aws-ssm",
    crossScalaVersions := supportedScalaVersions,
    releaseCrossBuild := true,
    publishTo := sonatypePublishTo.value,
    libraryDependencies ++= Seq(
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
      "com.twitter" %% "util-tunable" % "19.1.0",
      "com.amazonaws" % "aws-java-sdk-core" % "1.11.498",
      "com.amazonaws" % "aws-java-sdk-ssm" % "1.11.498"
    )
  )

import ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("publishSigned", _)),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeRelease", _)),
  pushChanges
)
