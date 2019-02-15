lazy val scala212 = "2.12.8"
lazy val scala211 = "2.11.12"
lazy val supportedScalaVersions = List(scala212, scala211)

ThisBuild / organization := "io.github.CyberAgent"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := scala212

lazy val root = (project in file("."))
  .settings(
    name := "tunable-aws-ssm",
    crossScalaVersions := supportedScalaVersions,
    publishTo := sonatypePublishTo.value,
    libraryDependencies ++= Seq(
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
      "com.twitter" %% "util-tunable" % "19.1.0",
      "com.amazonaws" % "aws-java-sdk-core" % "1.11.498",
      "com.amazonaws" % "aws-java-sdk-ssm" % "1.11.498"
    )
  )
