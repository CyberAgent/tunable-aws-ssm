package io.github.cyberagent.tunable.aws.ssm

import java.util.concurrent.RejectedExecutionException

import com.twitter.concurrent.AsyncMutex
import com.twitter.util.{Closable, Duration, Future, FuturePool, JavaTimer, Return, Throw, Time, TimerTask}
import com.typesafe.scalalogging.StrictLogging

trait Poller extends Closable {
  def register(f: () => Unit): Unit
}

object NullPoller extends Poller {
  override def register(f: () => Unit): Unit = ()
  override def close(deadline: Time): Future[Unit] = Future.Unit
}

class DefaultPoller(
    pollInterval: Duration
) extends Poller with StrictLogging {
  override def close(deadline: Time): Future[Unit] = {
    triggerTask.fold(Future.Unit)(_.close(deadline))
  }

  def register(f: () => Unit): Unit = {
    triggerTask = Some(timer.schedule(pollInterval)(triggerPoll(f)))
  }

  private[this] val timer = new JavaTimer(isDaemon = true, name = Some("AwsSsmTunableMapTimer"))
  private[this] var triggerTask: Option[TimerTask] = None

  // 更新タスクの実行に時間がかかっているとき次の更新がスケジュールされても無視するように
  // maxWaiters = 0 とする。タスク終了と同時に mutex queue に溜まっていたタスクがすぐに
  // 実行されてしまうと API Quota を使い切ってしまう恐れがあるため。
  private[this] val refreshMutex = new AsyncMutex(maxWaiters = 0)

  private[this] def triggerPoll(f: () => Unit): Unit = {
    refreshMutex
        .acquireAndRun(FuturePool.unboundedPool {
          f()
        })
        .respond {
          case Return(_) =>
          case Throw(e: RejectedExecutionException) =>
            logger.warn("refresh task is already running. something may be wrong")
          case Throw(e) =>
            logger.warn("failed to refresh", e)
        }
  }

  logger.info(s"Starting TunableMap refreshment scheduler at $pollInterval interval")
}
