package ingot

import cats.data.{ EitherT, StateT }
import cats.syntax.either._
import cats.Id

object Clay {
  outer =>
  def pure[L, R](x: R): Clay[L, R] = rightT(x)

  def apply[L, R](
    x: StateWithLogs[Unit] => (StateWithLogs[Unit], Either[L, R])): Clay[L, R] =
    EitherT[StateT[Id, StateWithLogs[Unit], ?], L, R](StateT[Id, StateWithLogs[Unit], Either[L, R]](x))

  final class RightTPartiallyApplied[L] {
    def apply[R](x: R): Clay[L, R] = outer.apply[L, R](s => (s, Either.right[L, R](x)))
  }

  def rightT[L] = new RightTPartiallyApplied[L]

  final class LeftTPartiallyApplied[R] {
    def apply[L](x: L): Clay[L, R] = outer.apply[L, R](s => (s, Either.left[L, R](x)))
  }

  def leftT[R] = new LeftTPartiallyApplied[R]

  def lift[L, R](x: Either[L, R]): Clay[L, R] =
    apply[L, R](s => (s, x))

  def guard[F[_], R](x: F[R])(implicit G: Guard[F, Id]): Clay[Throwable, R] =
    apply[Throwable, R](s => (s, G(x)))

  def log[L](x: LogMessage): Clay[L, Unit] =
    apply[L, Unit](s => (s.combine(x), Either.right[L, Unit](())))

  def log[L](x: Logs): Clay[L, Unit] =
    apply[L, Unit](s => ((s.combine(x), Either.right[L, Unit](()))))

  def flushLogs[L](logger: Logger[Id]): Clay[L, Unit] = Clay[L, Unit] { s =>
    logger.log(s.logs)
    (StateWithLogs.init(()), Either.right[L, Unit](()))
  }

}
