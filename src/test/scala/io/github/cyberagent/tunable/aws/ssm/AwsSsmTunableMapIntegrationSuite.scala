package io.github.cyberagent.tunable.aws.ssm

import cloud.localstack.{Localstack, TestUtils}
import cloud.localstack.docker.LocalstackDockerTestRunner
import cloud.localstack.docker.annotation.{LocalstackDockerConfiguration, LocalstackDockerProperties}
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.simplesystemsmanagement.model.{ParameterType, PutParameterRequest}
import com.amazonaws.services.simplesystemsmanagement.{AWSSimpleSystemsManagement, AWSSimpleSystemsManagementClient}
import com.twitter.util.tunable.TunableMap
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import cloud.localstack.docker.LocalstackDocker

@RunWith(classOf[LocalstackDockerTestRunner])
@LocalstackDockerProperties(randomizePorts = true, services = Array("ssm"))
class AwsSsmTunableMapIntegrationSuite extends FunSuite with BeforeAndAfterAll {

  val isLocal = !sys.env.contains("CI")
  val localstackDocker = LocalstackDocker.INSTANCE

  override def beforeAll(): Unit = {
    if (isLocal) {
      Localstack.teardownInfrastructure()

      val dockerConfig = {
        val b = LocalstackDockerConfiguration.builder()
        b.randomizePorts(true)
        b.build()
      }
      localstackDocker.startup(dockerConfig)
    }
  }

  override def afterAll(): Unit = {
    if (isLocal) localstackDocker.stop()
  }

  lazy val ssmEndpoint =
    if (isLocal) LocalstackDocker.INSTANCE.getEndpointSSM
    else "localhost:4583"

  lazy val client: AWSSimpleSystemsManagement = {
    val endpoint = new AwsClientBuilder.EndpointConfiguration(ssmEndpoint, "us-east-1")
    AWSSimpleSystemsManagementClient.builder()
        .withEndpointConfiguration(endpoint)
        .withCredentials(TestUtils.getCredentialsProvider)
        .build()
  }

  def putParameter(name: String, value: String, typ: ParameterType = ParameterType.String): Unit = {
    val req = new PutParameterRequest()
        .withName(name)
        .withValue(value)
        .withType(typ)
    client.putParameter(req)
  }

  test("AwsSsmTunableMap should return value") {
    putParameter("/foo/bar/k1", "v1")

    val testee = AwsSsmTunableMap("/foo/bar", client)
    val actual = testee(TunableMap.Key[String]("k1"))()
    assert(actual === Some("v1"))
  }
}
