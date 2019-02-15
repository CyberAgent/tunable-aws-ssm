// Your profile name of the sonatype account. The default is the same with the organization value
sonatypeProfileName := "io.github.CyberAgent"

// To sync with Maven central, you need to supply the following information:
publishMavenStyle := true

// License of your choice
licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))

// Where is the source code hosted
homepage := Some(url("https://github.com/CyberAgent/tunable-aws-ssm"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/CyberAgent/tunable-aws-ssm"),
    "scm:git@github.com:CyberAgent/tunable-aws-ssm.git"
  )
)
developers := List(
  Developer(id="atty303", name="Koji AGAWA", email="agawa_koji@cyberagent.co.jp", url=url("http://github.com/atty303"))
)
