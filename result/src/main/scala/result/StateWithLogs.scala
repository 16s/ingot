package result

import cats.Applicative
import cats.instances.all._

final case class StateWithLogs[+S](logs: Logs, state: S) {
  def combine(x: LogMessage): StateWithLogs[S] =
    copy(logs = cats.Monoid[Logs].combine(logs, Applicative[LogContainer].pure(x)))
  def combine(x: Logs): StateWithLogs[S] =
    copy(logs = cats.Monoid[Logs].combine(logs, x))
}

object StateWithLogs {
  def init[S](s: S)(implicit M: cats.Monoid[Logs]): StateWithLogs[S] = StateWithLogs(cats.Monoid[Logs].empty, s)
}
