lazy val root = (project in file("."))
  .settings(
    name := "tunable-aws-ssm",
    scalaVersion := "2.12.7",
    libraryDependencies ++= Seq(
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
      "com.twitter" %% "util-tunable" % "19.1.0",
      "com.amazonaws" % "aws-java-sdk-core" % "1.11.498",
      "com.amazonaws" % "aws-java-sdk-ssm" % "1.11.498"
    )
  )
