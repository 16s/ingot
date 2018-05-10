package ingot

import cats.Applicative
import cats.data.{ EitherT, StateT }
import cats.syntax.either._

object Yoga {
  def pure[F[_], L, R](x: R)(implicit A: Applicative[F]): Yoga[F, L, R] = rightT(x)

  def apply[F[_], L, R](
    x: StateWithLogs[Unit] => F[(StateWithLogs[Unit], Either[L, R])])(
    implicit
    A: Applicative[F]): Yoga[F, L, R] =
    EitherT[StateT[F, StateWithLogs[Unit], ?], L, R](StateT(x))

  def liftF[F[_], L, R](x: F[R])(implicit A: Applicative[F]): Yoga[F, L, R] = right(x)

  def rightT[F[_], L, R](x: R)(implicit A: Applicative[F]): Yoga[F, L, R] =
    apply[F, L, R](s => A.pure((s, Either.right[L, R](x))))

  def leftT[F[_], L, R](x: L)(implicit A: Applicative[F]): Yoga[F, L, R] =
    apply[F, L, R](s => A.pure((s, Either.left[L, R](x))))

  def right[F[_], L, R](x: F[R])(implicit A: Applicative[F]): Yoga[F, L, R] =
    apply[F, L, R](s => A.map(x)(x => (s, Either.right[L, R](x))))

  def left[F[_], L, R](x: F[L])(implicit A: Applicative[F]): Yoga[F, L, R] =
    apply[F, L, R](s => A.map(x)(x => (s, Either.left[L, R](x))))

  def lift[F[_], L, R](x: F[Either[L, R]])(implicit A: Applicative[F]): Yoga[F, L, R] =
    apply[F, L, R](s => A.map(x)(x => (s, x)))

  def guard[F[_], G[_], R](x: F[R])(implicit A: Applicative[G], G: Guard[F, G]): Yoga[G, Throwable, R] =
    apply[G, Throwable, R](s => A.map(G(x))(x => (s, x)))

  def log[F[_], L](x: LogMessage)(implicit A: Applicative[F]): Yoga[F, L, Unit] =
    apply[F, L, Unit](s => A.pure((s.combine(x), Either.right[L, Unit](()))))

  def log[F[_], L](x: Logs)(implicit A: Applicative[F]): Yoga[F, L, Unit] =
    apply[F, L, Unit](s => A.pure(((s.combine(x), Either.right[L, Unit](())))))
}
