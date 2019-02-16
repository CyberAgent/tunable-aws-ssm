# tunable-aws-ssm

[![CircleCI](https://circleci.com/gh/CyberAgent/tunable-aws-ssm.svg?style=svg)](https://circleci.com/gh/CyberAgent/tunable-aws-ssm)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.CyberAgent/tunable-aws-ssm_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.CyberAgent/tunable-aws-ssm_2.12)

This is `com.twitter.util.tunable.Tunable` implementation for AWS SSM.


## Testing

### Preparation

    docker pull localstack/localstack

If docker is installed on non-standard location, please set `DOCKER_LOCATION` environment variable to point it.

### Run

    sbt test
