package jp.co.cyberagent.tunable.aws.ssm

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.model.{GetParametersByPathResult, Parameter}
import com.twitter.util.{Await, Duration}
import com.twitter.util.tunable.TunableMap
import org.scalamock.scalatest.MockFactory
import org.scalatest.FlatSpec

class AwsSsmTunableMapSpec extends FlatSpec with MockFactory {
  def mkResult(nextToken: Option[String], params: (String, String)*): GetParametersByPathResult = {
    val ps = params.map { case (name, value) =>
      new Parameter().withName(name).withValue(value)
    }
    new GetParametersByPathResult()
        .withNextToken(nextToken.orNull)
        .withParameters(ps: _*)
  }

  "AwsSsmTunableMap#apply" should "returns tunable with some value when key is exists" in {
    val client = stub[AWSSimpleSystemsManagement]
    (client.getParametersByPath _).when(*).returns(
      mkResult(None, "key" -> "true")
    )

    val testee = new AwsSsmTunableMap("", client, NullPoller)
    val actual = testee(TunableMap.Key[String]("key"))()
    assert(actual === Some("true"))
  }

  it should "returns tunable with none value when key isn't exists " in {
    val client = stub[AWSSimpleSystemsManagement]
    (client.getParametersByPath _).when(*).returns(
      mkResult(None)
    )

    val testee = new AwsSsmTunableMap("", client, NullPoller)
    val actual = testee(TunableMap.Key[String]("not_existing"))()
    assert(actual === None)
  }

  it should "returns tunable when client result has paging" in {
    val client = stub[AWSSimpleSystemsManagement]
    (client.getParametersByPath _).when(*).returns(
      mkResult(Some("1"), "k1" -> "v1", "k2" -> "v2")
    ).returns(
      mkResult(None, "k3" -> "v3")
    )

    val testee = new AwsSsmTunableMap("", client, NullPoller)
    val actual = testee(TunableMap.Key[String]("k3"))()
    assert(actual === Some("v3"))
  }

  "AwsSsmTunableMap#entries" should "return entries" in {
    val client = stub[AWSSimpleSystemsManagement]
    (client.getParametersByPath _).when(*).returns(
      mkResult(None, "a" -> "A", "b" -> "B")
    )

    val testee = new AwsSsmTunableMap("", client, NullPoller)
    val actual = testee.entries.toVector
    assert(actual.size === 2)
    assert(actual.contains(TunableMap.Entry[String](TunableMap.Key[String]("a"), "A", "AwsSsmTunableMap()")))
    assert(actual.contains(TunableMap.Entry[String](TunableMap.Key[String]("b"), "B", "AwsSsmTunableMap()")))
  }

  "AwsSsmTunableMap#close" should "callable" in {
    val client = stub[AWSSimpleSystemsManagement]
    (client.getParametersByPath _).when(*).returns(
      mkResult(None)
    )

    val testee = new AwsSsmTunableMap("", client, NullPoller)
    val actual = Await.result(testee.close(Duration.Top))
    assert(actual === ())
  }

  "AwsSsmTunableMap#toString" should "contains prefix" in {
    val client = stub[AWSSimpleSystemsManagement]
    (client.getParametersByPath _).when(*).returns(
      mkResult(None)
    )

    val testee = new AwsSsmTunableMap("/foo/bar", client, NullPoller)
    val actual = testee.toString
    assert(actual === "AwsSsmTunableMap(/foo/bar)")
  }
}
