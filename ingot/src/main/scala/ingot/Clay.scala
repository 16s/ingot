package ingot

import cats.Eval
import cats.data.{ EitherT, StateT }
import cats.syntax.either._

object Clay {
  outer =>

  final class RightTPartiallyApplied[L] {
    def apply[R](x: R): Clay[L, R] = outer.apply[L, R](s => (s, Either.right[L, R](x)))
  }

  def pure[L] = new RightTPartiallyApplied[L]

  def apply[L, R](
    x: StateWithLogs[Unit] => (StateWithLogs[Unit], Either[L, R])): Clay[L, R] =
    EitherT[StateT[Eval, StateWithLogs[Unit], ?], L, R](StateT[Eval, StateWithLogs[Unit], Either[L, R]](x.andThen(Eval.now)))

  def rightT[L] = new RightTPartiallyApplied[L]

  final class LeftTPartiallyApplied[R] {
    def apply[L](x: L): Clay[L, R] = outer.apply[L, R](s => (s, Either.left[L, R](x)))
  }

  def leftT[R] = new LeftTPartiallyApplied[R]

  def lift[L, R](x: Either[L, R]): Clay[L, R] =
    apply[L, R](s => (s, x))

  def guard[F[_], R](x: F[R])(implicit G: Guard[F, Eval]): Clay[Throwable, R] =
    apply[Throwable, R](s => (s, G(x).value))

  def log[L](x: LogMessage): Clay[L, Unit] =
    apply[L, Unit](s => (s.combine(x), Either.right[L, Unit](())))

  def log[L](x: Logs): Clay[L, Unit] =
    apply[L, Unit](s => ((s.combine(x), Either.right[L, Unit](()))))

  def flushLogs[L](logger: Logger[Eval]): Clay[L, Unit] = Clay[L, Unit] { s =>
    logger.log(s.logs).value
    (StateWithLogs.init(()), Either.right[L, Unit](()))
  }

}
