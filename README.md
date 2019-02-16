# tunable-aws-ssm

[![CircleCI](https://circleci.com/gh/CyberAgent/tunable-aws-ssm.svg?style=svg)](https://circleci.com/gh/CyberAgent/tunable-aws-ssm)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.CyberAgent/tunable-aws-ssm_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.CyberAgent/tunable-aws-ssm_2.12)

This is `com.twitter.util.tunable.Tunable` implementation for AWS SSM.

## Install

```
libraryDependencies += "io.github.CyberAgent" %% "tunable-aws-ssm" % version
```

## Usage

```scala
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClient
import com.twitter.util.tunable.{Tunable, TunableMap}
import io.github.cyberagent.tunable.aws.ssm.AwsSsmTunableMap

val client = AWSSimpleSystemsManagementClient.builder().build()
val tunableMap = AwsSsmTunableMap("/path/to/tunable", client)

val tunable: Tunable[String] = tunableMap(TunableMap.Key[String]("key"))

tunable() match {
  case None        => println("tunable is not configured")
  case Some(value) => println(value)
}
```

## Testing

### Preparation

    docker pull localstack/localstack

If docker is installed on non-standard location, please set `DOCKER_LOCATION` environment variable to point it.

### Run

    sbt test
