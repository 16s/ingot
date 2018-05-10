package ingot

import cats.data.{ EitherT, StateT }
import cats.Applicative
import cats.syntax.either._

object Ingot {
  def pure[F[_], S, L, R](x: R)(implicit A: Applicative[F]): Ingot[F, S, L, R] = rightT(x)

  def apply[F[_], S, L, R](
    x: StateWithLogs[S] => F[(StateWithLogs[S], Either[L, R])])(
    implicit
    A: Applicative[F]): Ingot[F, S, L, R] =
    EitherT[StateT[F, StateWithLogs[S], ?], L, R](StateT(x))

  def liftF[F[_], S, L, R](x: F[R])(implicit A: Applicative[F]): Ingot[F, S, L, R] = right(x)

  def rightT[F[_], S, L, R](x: R)(implicit A: Applicative[F]): Ingot[F, S, L, R] =
    apply[F, S, L, R](s => A.pure((s, Either.right[L, R](x))))

  def leftT[F[_], S, L, R](x: L)(implicit A: Applicative[F]): Ingot[F, S, L, R] =
    apply[F, S, L, R](s => A.pure((s, Either.left[L, R](x))))

  def right[F[_], S, L, R](x: F[R])(implicit A: Applicative[F]): Ingot[F, S, L, R] =
    apply[F, S, L, R](s => A.map(x)(x => (s, Either.right[L, R](x))))

  def left[F[_], S, L, R](x: F[L])(implicit A: Applicative[F]): Ingot[F, S, L, R] =
    apply[F, S, L, R](s => A.map(x)(x => (s, Either.left[L, R](x))))

  def lift[F[_], S, L, R](x: F[Either[L, R]])(implicit A: Applicative[F]): Ingot[F, S, L, R] =
    apply[F, S, L, R](s => A.map(x)(x => (s, x)))

  def guard[F[_], G[_], S, R](x: F[R])(implicit A: Applicative[G], G: Guard[F, G]): Ingot[G, S, Throwable, R] =
    apply[G, S, Throwable, R](s => A.map(G(x))(x => (s, x)))

  def log[F[_], S, L](x: LogMessage)(implicit A: Applicative[F]): Ingot[F, S, L, Unit] =
    apply[F, S, L, Unit](s => A.pure((s.combine(x), Either.right[L, Unit](()))))

  def log[F[_], S, L](x: Logs)(implicit A: Applicative[F]): Ingot[F, S, L, Unit] =
    apply[F, S, L, Unit](s => A.pure(((s.combine(x), Either.right[L, Unit](())))))

  def get[F[_], S, L](implicit A: Applicative[F]): Ingot[F, S, L, S] =
    apply[F, S, L, S](s => A.pure((s, Either.right[L, S](s.state))))

  def getL[F[_], S, L](implicit A: Applicative[F]): Ingot[F, S, L, Logs] =
    apply[F, S, L, Logs](s => A.pure((s, Either.right[L, Logs](s.logs))))

  def set[F[_], S, L](x: S)(implicit A: Applicative[F]): Ingot[F, S, L, Unit] =
    apply[F, S, L, Unit](s => A.pure(((s.copy(state = x), Either.right[L, Unit](())))))

  def setF[F[_], S, L](x: F[S])(implicit A: Applicative[F]): Ingot[F, S, L, Unit] =
    apply[F, S, L, Unit](s => A.map(x)(x => (s.copy(state = x), Either.right[L, Unit](()))))

  def modify[F[_], S, L](f: S => S)(implicit A: Applicative[F]): Ingot[F, S, L, Unit] =
    apply[F, S, L, Unit](s => A.pure(((s.copy(state = f(s.state)), Either.right[L, Unit](())))))

  def modifyF[F[_], S, L](f: S => F[S])(implicit A: Applicative[F]): Ingot[F, S, L, Unit] =
    apply[F, S, L, Unit](s => A.map(f(s.state))(x => (s.copy(state = x), Either.right[L, Unit](()))))

  def inspect[F[_], S, L, R](f: S => Either[L, R])(implicit A: Applicative[F]): Ingot[F, S, L, R] =
    apply[F, S, L, R](s => A.pure((s, f(s.state))))

  def inspectE[F[_], S, L, R](f: S => R)(implicit A: Applicative[F]): Ingot[F, S, L, R] =
    apply[F, S, L, R](s => A.pure((s, Either.right[L, R](f(s.state)))))

  def inspectF[F[_], S, L, R](f: S => F[Either[L, R]])(implicit A: Applicative[F]): Ingot[F, S, L, R] =
    apply[F, S, L, R](s => A.map(f(s.state))(r => (s, r)))

  def inspectG[F[_], G[_], S, R](
    f: S => F[R])(
    implicit
    A: Applicative[G],
    G: Guard[F, G]): Ingot[G, S, Throwable, R] =
    apply[G, S, Throwable, R](s => A.map(G(f(s.state)))(r => (s, r)))

  def inspectL[F[_], S, L, R](f: S => (Logs, Either[L, R]))(implicit A: Applicative[F]): Ingot[F, S, L, R] =
    apply[F, S, L, R](s => A.pure {
      val (logs, r) = f(s.state)
      (s.combine(logs), r)
    })

  def inspectEL[F[_], S, L, R](f: S => (Logs, R))(implicit A: Applicative[F]): Ingot[F, S, L, R] =
    apply[F, S, L, R](s => A.pure {
      val (logs, r) = f(s.state)
      (s.combine(logs), Either.right[L, R](r))
    })

  def inspectFL[F[_], S, L, R](f: S => F[(Logs, Either[L, R])])(implicit A: Applicative[F]): Ingot[F, S, L, R] =
    apply[F, S, L, R](s => A.map(f(s.state)) { case (logs, r) => (s.combine(logs), r) })

}
