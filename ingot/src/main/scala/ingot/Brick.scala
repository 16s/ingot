package ingot

import cats.Applicative
import cats.data.{ EitherT, StateT }
import cats.syntax.either._

object Brick {
  outer =>

  def pure[F[_], L, R](x: R)(implicit A: Applicative[F]): Brick[F, L, R] = rightT(x)

  def apply[F[_], L, R](
    x: StateWithLogs[Unit] => F[(StateWithLogs[Unit], Either[L, R])])(
    implicit
    A: Applicative[F]): Brick[F, L, R] =
    EitherT[StateT[F, StateWithLogs[Unit], ?], L, R](StateT(x))

  def liftF[L] = new RightPartiallyApplied[L]

  final class RightTPartiallyApplied[F[_], L] {
    def apply[R](x: R)(implicit A: Applicative[F]): Brick[F, L, R] =
      outer.apply[F, L, R](s => A.pure((s, Either.right[L, R](x))))
  }

  def rightT[F[_], L] = new RightTPartiallyApplied[F, L]

  final class LeftTPartiallyApplied[F[_], R] {
    def apply[L](x: L)(implicit A: Applicative[F]): Brick[F, L, R] =
      outer.apply[F, L, R](s => A.pure((s, Either.left[L, R](x))))
  }

  def leftT[F[_], R] = new LeftTPartiallyApplied[F, R]

  final class RightPartiallyApplied[L] {
    def apply[F[_], R](x: F[R])(implicit A: Applicative[F]): Brick[F, L, R] =
      outer.apply[F, L, R](s => A.map(x)(x => (s, Either.right[L, R](x))))
  }

  def right[L] = new RightPartiallyApplied[L]

  final class LeftPartiallyApplied[R] {
    def apply[F[_], L](x: F[L])(implicit A: Applicative[F]): Brick[F, L, R] =
      outer.apply[F, L, R](s => A.map(x)(x => (s, Either.left[L, R](x))))
  }

  def left[R] = new LeftPartiallyApplied[R]

  def lift[F[_], L, R](x: F[Either[L, R]])(implicit A: Applicative[F]): Brick[F, L, R] =
    apply[F, L, R](s => A.map(x)(x => (s, x)))

  final class GuardPartiallyApplied[G[_]] {
    def apply[F[_], R](x: F[R])(implicit A: Applicative[G], G: Guard[F, G]): Brick[G, Throwable, R] =
      outer.apply[G, Throwable, R](s => A.map(G(x))(x => (s, x)))
  }

  def guard[G[_]] = new GuardPartiallyApplied[G]

  def log[F[_], L](x: LogMessage)(implicit A: Applicative[F]): Brick[F, L, Unit] =
    apply[F, L, Unit](s => A.pure((s.combine(x), Either.right[L, Unit](()))))

  def log[F[_], L](x: Logs)(implicit A: Applicative[F]): Brick[F, L, Unit] =
    apply[F, L, Unit](s => A.pure(((s.combine(x), Either.right[L, Unit](())))))
}
