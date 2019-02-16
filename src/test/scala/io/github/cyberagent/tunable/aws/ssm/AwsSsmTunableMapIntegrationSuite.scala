package io.github.cyberagent.tunable.aws.ssm

import cloud.localstack.docker.{LocalstackDocker, LocalstackDockerTestRunner}
import cloud.localstack.docker.annotation.{LocalstackDockerConfiguration, LocalstackDockerProperties}
import cloud.localstack.{Localstack, TestUtils}
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.simplesystemsmanagement.model.{DeleteParametersRequest, ParameterType, PutParameterRequest}
import com.amazonaws.services.simplesystemsmanagement.{AWSSimpleSystemsManagement, AWSSimpleSystemsManagementClient}
import com.twitter.util.Duration
import com.twitter.util.tunable.TunableMap
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfterAll, FunSuite}


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
    else "http://localhost:4583"

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
        .withOverwrite(true)
    client.putParameter(req)
  }

  def deleteParameters(names: String*): Unit = {
    val req = new DeleteParametersRequest()
            .withNames(names: _*)
    client.deleteParameters(req)
  }

  test("AwsSsmTunableMap should return value") {
    putParameter("/foo/bar/k1", "v1")

    val testee = AwsSsmTunableMap("/foo/bar", client, Duration.fromMilliseconds(100))
    assert(testee(TunableMap.Key[String]("k1"))() === Some("v1"))

    Thread.sleep(500)
    putParameter("/foo/bar/k1", "v2")
    Thread.sleep(500)

    assert(testee(TunableMap.Key[String]("k1"))() === Some("v2"))

    Thread.sleep(500)
    deleteParameters("/foo/bar/k1")
    Thread.sleep(500)

    assert(testee(TunableMap.Key[String]("k1"))() === None)

    testee.close(Duration.Top)
  }
}
