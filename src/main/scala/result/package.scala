import cats.{ Applicative, FlatMap }
import cats.data.{ EitherT, StateT }
import cats.kernel.Monoid
import cats.syntax.all._
import cats.instances.all._

package object result {
  type LogContainer[M] = Vector[M]
  type LogMessage = String
  type Logs = LogContainer[LogMessage]

  final case class StateWithLogs[S](logs: Logs, state: S) {
    def combine(x: LogMessage): StateWithLogs[S] =
      copy(logs = Monoid[Logs].combine(logs, Applicative[LogContainer].pure(x)))
    def combine(x: Logs): StateWithLogs[S] =
      copy(logs = Monoid[Logs].combine(logs, x))
  }

  object StateWithLogs {

    def init[S](s: S)(implicit M: Monoid[Logs]): StateWithLogs[S] = StateWithLogs(Monoid[Logs].empty, s)

  }

  type ActionType[F[_], S, L, R] = StateWithLogs[S] => F[(StateWithLogs[S], Either[L, R])]
  type ResultT[F[_], S, L, R] = EitherT[StateT[F, StateWithLogs[S], ?], L, R]

  object ResultT {
    def pure[F[_], S, L, R](x: R)(implicit A: Applicative[F]): ResultT[F, S, L, R] = rightT(x)

    def liftF[F[_], S, L, R](x: F[R])(implicit A: Applicative[F]): ResultT[F, S, L, R] = right(x)

    def rightT[F[_], S, L, R](x: R)(implicit A: Applicative[F]): ResultT[F, S, L, R] =
      EitherT[StateT[F, StateWithLogs[S], ?], L, R](StateT(s => A.pure((s, Either.right[L, R](x)))))

    def leftT[F[_], S, L, R](x: L)(implicit A: Applicative[F]): ResultT[F, S, L, R] =
      EitherT[StateT[F, StateWithLogs[S], ?], L, R](StateT(s => A.pure((s, Either.left[L, R](x)))))

    def right[F[_], S, L, R](x: F[R])(implicit A: Applicative[F]): ResultT[F, S, L, R] =
      EitherT[StateT[F, StateWithLogs[S], ?], L, R](StateT(s => A.map(x)(x => (s, Either.right[L, R](x)))))

    def left[F[_], S, L, R](x: F[L])(implicit A: Applicative[F]): ResultT[F, S, L, R] =
      EitherT[StateT[F, StateWithLogs[S], ?], L, R](StateT(s => A.map(x)(x => (s, Either.left[L, R](x)))))

    def lift[F[_], S, L, R](x: F[Either[L, R]])(implicit A: Applicative[F]): ResultT[F, S, L, R] =
      EitherT[StateT[F, StateWithLogs[S], ?], L, R](StateT(s => A.map(x)(x => (s, x))))

    def log[F[_], S, L](x: LogMessage)(implicit A: Applicative[F]): ResultT[F, S, L, Unit] =
      EitherT[StateT[F, StateWithLogs[S], ?], L, Unit](StateT(s => A.pure((s.combine(x), Either.right[L, Unit](())))))

    def log[F[_], S, L](x: Logs)(implicit A: Applicative[F]): ResultT[F, S, L, Unit] =
      EitherT[StateT[F, StateWithLogs[S], ?], L, Unit](StateT(s => A.pure(((s.combine(x), Either.right[L, Unit](()))))))

    def get[F[_], S, L](implicit A: Applicative[F]): ResultT[F, S, L, S] =
      EitherT[StateT[F, StateWithLogs[S], ?], L, S](StateT(s => A.pure((s, Either.right[L, S](s.state)))))

    def getL[F[_], S, L](implicit A: Applicative[F]): ResultT[F, S, L, Logs] =
      EitherT[StateT[F, StateWithLogs[S], ?], L, Logs](StateT(s => A.pure((s, Either.right[L, Logs](s.logs)))))

    def set[F[_], S, L](x: S)(implicit A: Applicative[F]): ResultT[F, S, L, Unit] =
      EitherT[StateT[F, StateWithLogs[S], ?], L, Unit](StateT(s => A.pure(((s.copy(state = x), Either.right[L, Unit](()))))))

    def setF[F[_], S, L](x: F[S])(implicit A: Applicative[F]): ResultT[F, S, L, Unit] =
      EitherT[StateT[F, StateWithLogs[S], ?], L, Unit](StateT(s => A.map(x)(x => (s.copy(state = x), Either.right[L, Unit](())))))

    def modify[F[_], S, L](f: S => S)(implicit A: Applicative[F]): ResultT[F, S, L, Unit] =
      EitherT[StateT[F, StateWithLogs[S], ?], L, Unit](StateT(s => A.pure(((s.copy(state = f(s.state)), Either.right[L, Unit](()))))))

    def modifyF[F[_], S, L](f: S => F[S])(implicit A: Applicative[F]): ResultT[F, S, L, Unit] =
      EitherT[StateT[F, StateWithLogs[S], ?], L, Unit](StateT(s => A.map(f(s.state))(x => (s.copy(state = x), Either.right[L, Unit](())))))
  }

  implicit class ResultTSyntax[F[_], S, L, R](x: ResultT[F, S, L, R]) {
    def run(st: S)(implicit F: FlatMap[F]): F[(Logs, S, Either[L, R])] =
      F.map(x.value.run(StateWithLogs.init(st)))({ case (StateWithLogs(logs, st), r) => (logs, st, r) })

    def runA(st: S)(implicit F: FlatMap[F]): F[Either[L, R]] =
      F.map(x.value.run(StateWithLogs.init(st)))({ case (_, r) => r })

    def runS(st: S)(implicit F: FlatMap[F]): F[S] =
      F.map(x.value.run(StateWithLogs.init(st)))({ case (StateWithLogs(_, st), _) => st })

    def runL(st: S)(implicit F: FlatMap[F]): F[Logs] =
      F.map(x.value.run(StateWithLogs.init(st)))({ case (StateWithLogs(logs, _), _) => logs })

    def runAL(st: S)(implicit F: FlatMap[F]): F[(Logs, Either[L, R])] =
      F.map(x.value.run(StateWithLogs.init(st)))({ case (StateWithLogs(logs, _), r) => (logs, r) })

  }
}
