import cats.{ Applicative, FlatMap }
import cats.data.{ EitherT, StateT }
import cats.syntax.all._

package object result {
  type Logs = Vector[String]

  val emptyLogs: Logs = Vector.empty[String]

  final case class StateWithLogs[S](logs: Logs, state: S)
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
  }

  implicit class ResultTSyntax[F[_], S, L, R](x: ResultT[F, S, L, R]) {
    def run(st: S)(implicit F: FlatMap[F]): F[(Logs, S, Either[L, R])] =
      F.map(x.value.run(StateWithLogs(emptyLogs, st)))({ case (StateWithLogs(logs, st), r) => (logs, st, r) })
    def runA(st: S)(implicit F: FlatMap[F]): F[Either[L, R]] =
      F.map(x.value.run(StateWithLogs(emptyLogs, st)))({ case (_, r) => r })
    def runS(st: S)(implicit F: FlatMap[F]): F[S] =
      F.map(x.value.run(StateWithLogs(emptyLogs, st)))({ case (StateWithLogs(_, st), _) => st })
    def runL(st: S)(implicit F: FlatMap[F]): F[Logs] =
      F.map(x.value.run(StateWithLogs(emptyLogs, st)))({ case (StateWithLogs(logs, _), _) => logs })
    def runAL(st: S)(implicit F: FlatMap[F]): F[(Logs, Either[L, R])] =
      F.map(x.value.run(StateWithLogs(emptyLogs, st)))({ case (StateWithLogs(logs, _), r) => (logs, r) })
  }
}
