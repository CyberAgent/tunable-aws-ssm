package jp.co.cyberagent.tunable.aws.ssm

import java.util.concurrent.RejectedExecutionException

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.model.{GetParametersByPathRequest, GetParametersByPathResult}
import com.twitter.concurrent.AsyncMutex
import com.twitter.util._
import com.twitter.util.tunable.{Tunable, TunableMap}
import com.typesafe.scalalogging.StrictLogging

import scala.collection.JavaConverters._

class AwsSsmTunableMap(
    path: String,
    client: AWSSimpleSystemsManagement,
    refreshInterval: Duration
) extends TunableMap
    with Closable
    with StrictLogging {

  override def apply[T](key: TunableMap.Key[T]): Tunable[T] = underlying(key)
  override def entries: Iterator[TunableMap.Entry[_]] = underlying.entries

  override def close(deadline: Time): Future[Unit] = {
    triggerTask.fold(Future.Unit)(_.close(deadline))
  }

  override def toString: String = s"AwsSsmTunableMap($path)"

  private[this] val normalizedPath = if (path.endsWith("/")) path else path + "/"
  private[this] val underlying = TunableMap.newMutable(s"AwsSsmTunableMap($path)")
  private[this] val timer = new JavaTimer(isDaemon = true, name = Some("AwsSsmTunableMapTimer"))

  private[this] var triggerTask: Option[TimerTask] = None

  // 更新タスクの実行に時間がかかっているとき次の更新がスケジュールされても無視するように
  // maxWaiters = 0 とする。タスク終了と同時に mutex queue に溜まっていたタスクがすぐに
  // 実行されてしまうと API Quota を使い切ってしまう恐れがあるため。
  private[this] val refreshMutex = new AsyncMutex(maxWaiters = 0)

  private[this] def triggerRefresh(): Unit = {
    refreshMutex
        .acquireAndRun(FuturePool.unboundedPool {
          refresh()
        })
        .respond {
          case Return(_) =>
          case Throw(e: RejectedExecutionException) =>
            logger.warn("refresh task is already running. something may be wrong")
          case Throw(e) =>
            logger.warn("failed to refresh", e)
        }
  }

  private[this] val changesWitnessMap = new java.util.concurrent.ConcurrentHashMap[String, Witness[Option[String]]]()
  private[this] var knownKeys = Set.empty[TunableMap.Key[_]]

  private[this] def getParametersByPath(req: GetParametersByPathRequest,
      xs: List[GetParametersByPathResult] = Nil): List[GetParametersByPathResult] = {
    val rep = client.getParametersByPath(req)
    Option(rep.getNextToken) match {
      case Some(nextToken) => getParametersByPath(req.withNextToken(nextToken), rep :: xs)
      case None            => rep :: xs
    }
  }

  // This method is not thread safe
  private[this] def refresh(): Unit = {
    val req = new GetParametersByPathRequest()
        .withPath(path)
        .withRecursive(false)
        .withWithDecryption(true)
        .withMaxResults(10)

    val params = getParametersByPath(req).flatMap(_.getParameters.asScala)
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
  refresh()

  logger.info(s"Starting TunableMap refreshment scheduler at $refreshInterval interval")
  triggerTask = Some(timer.schedule(refreshInterval)(triggerRefresh()))
}
