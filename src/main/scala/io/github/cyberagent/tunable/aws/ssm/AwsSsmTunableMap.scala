package io.github.cyberagent.tunable.aws.ssm

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.model.{GetParametersByPathRequest, GetParametersByPathResult, Parameter}
import com.twitter.util.tunable.{Tunable, TunableMap}
import com.twitter.util.{Closable, Duration, Future, Time, Witness}
import com.typesafe.scalalogging.StrictLogging

import scala.collection.JavaConverters._
import scala.collection.immutable

class AwsSsmTunableMap(
    prefix: String,
    client: AWSSimpleSystemsManagement,
    poller: Poller
) extends TunableMap
    with Closable
    with StrictLogging {

  override def apply[T](key: TunableMap.Key[T]): Tunable[T] = underlying(key)
  override def entries: Iterator[TunableMap.Entry[_]] = underlying.entries

  override def close(deadline: Time): Future[Unit] = poller.close(deadline)

  override def toString: String = s"AwsSsmTunableMap($prefix)"

  private[this] val normalizedPath = if (prefix.endsWith("/")) prefix else prefix + "/"
  private[this] val underlying = TunableMap.newMutable(s"AwsSsmTunableMap($prefix)")

  private[this] val changesWitnessMap = new java.util.concurrent.ConcurrentHashMap[String, Witness[Option[String]]]()
  private[this] var knownKeys = Set.empty[TunableMap.Key[_]]

  private[this] def getParametersByPath(req: GetParametersByPathRequest,
      xs: List[GetParametersByPathResult] = Nil): List[Parameter] = {
    val rep = client.getParametersByPath(req)
    Option(rep.getNextToken) match {
      case Some(nextToken) => getParametersByPath(req.withNextToken(nextToken), rep :: xs)
      case None            => (rep :: xs).flatMap(_.getParameters.asScala)
    }
  }

  private[this] def load(): Unit = synchronized {
    val req = new GetParametersByPathRequest()
        .withPath(prefix)
        .withRecursive(false)
        .withWithDecryption(true)
        .withMaxResults(10)

    val params: immutable.Seq[Parameter] = getParametersByPath(req)
    params.foreach { p =>
      for {
        name <- Option(p.getName)
        value <- Option(p.getValue)
      } yield {
        val shortName = name.stripPrefix(normalizedPath)
        val key = underlying.put[String](shortName, value)
        if (!knownKeys.contains(key)) {
          val tunable = underlying(key)
          tunable.asVar.changes.dedup.respond { value =>
            logger.info(s"${AwsSsmTunableMap.this} changed: $shortName -> $value")
          }
          knownKeys = knownKeys + key
        }
      }
    }
  }

  logger.info(s"Loading TunableMap from $this")
  load()

  poller.register(() => this.load())
}

object AwsSsmTunableMap {
  def apply(prefix: String, client: AWSSimpleSystemsManagement, pollInterval: Duration = Duration.fromSeconds(10)): AwsSsmTunableMap =
    new AwsSsmTunableMap(prefix, client, new DefaultPoller(pollInterval))
}
