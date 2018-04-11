import cats.data.{ EitherT, StateT }

package object result {
  final case class StateWithLogs[S](logs: List[String], state: S)
  type ActionType[F[_], S, L, R] = StateWithLogs[S] => F[(StateWithLogs[S], Either[L, R])]
  type ResultT[F[_], S, L, R] = EitherT[StateT[F, StateWithLogs[S], ?], L, R]
}
