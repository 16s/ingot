/**
 * Copyright 2018 Tamas Neltz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import cats.{ Applicative, FlatMap, Functor }
import cats.data.{ EitherT, StateT }
import cats.syntax.all._
import cats.instances.all._
import scala.language.higherKinds

package object result {
  type LogContainer[M] = Vector[M]
  type LogMessage = String
  type Logs = LogContainer[LogMessage]

  type ActionType[F[_], S, L, R] = StateWithLogs[S] => F[(StateWithLogs[S], Either[L, R])]
  type ResultT[F[_], S, L, R] = EitherT[StateT[F, StateWithLogs[S], ?], L, R]

  object ResultT {
    def pure[F[_], S, L, R](x: R)(implicit A: Applicative[F]): ResultT[F, S, L, R] = rightT(x)

    def apply[F[_], S, L, R](
      x: StateWithLogs[S] => F[(StateWithLogs[S], Either[L, R])])(
      implicit
      A: Applicative[F]): ResultT[F, S, L, R] =
      EitherT[StateT[F, StateWithLogs[S], ?], L, R](StateT(x))

    def liftF[F[_], S, L, R](x: F[R])(implicit A: Applicative[F]): ResultT[F, S, L, R] = right(x)

    def rightT[F[_], S, L, R](x: R)(implicit A: Applicative[F]): ResultT[F, S, L, R] =
      apply[F, S, L, R](s => A.pure((s, Either.right[L, R](x))))

    def leftT[F[_], S, L, R](x: L)(implicit A: Applicative[F]): ResultT[F, S, L, R] =
      apply[F, S, L, R](s => A.pure((s, Either.left[L, R](x))))

    def right[F[_], S, L, R](x: F[R])(implicit A: Applicative[F]): ResultT[F, S, L, R] =
      apply[F, S, L, R](s => A.map(x)(x => (s, Either.right[L, R](x))))

    def left[F[_], S, L, R](x: F[L])(implicit A: Applicative[F]): ResultT[F, S, L, R] =
      apply[F, S, L, R](s => A.map(x)(x => (s, Either.left[L, R](x))))

    def lift[F[_], S, L, R](x: F[Either[L, R]])(implicit A: Applicative[F]): ResultT[F, S, L, R] =
      apply[F, S, L, R](s => A.map(x)(x => (s, x)))

    def log[F[_], S, L](x: LogMessage)(implicit A: Applicative[F]): ResultT[F, S, L, Unit] =
      apply[F, S, L, Unit](s => A.pure((s.combine(x), Either.right[L, Unit](()))))

    def log[F[_], S, L](x: Logs)(implicit A: Applicative[F]): ResultT[F, S, L, Unit] =
      apply[F, S, L, Unit](s => A.pure(((s.combine(x), Either.right[L, Unit](())))))

    def get[F[_], S, L](implicit A: Applicative[F]): ResultT[F, S, L, S] =
      apply[F, S, L, S](s => A.pure((s, Either.right[L, S](s.state))))

    def getL[F[_], S, L](implicit A: Applicative[F]): ResultT[F, S, L, Logs] =
      apply[F, S, L, Logs](s => A.pure((s, Either.right[L, Logs](s.logs))))

    def set[F[_], S, L](x: S)(implicit A: Applicative[F]): ResultT[F, S, L, Unit] =
      apply[F, S, L, Unit](s => A.pure(((s.copy(state = x), Either.right[L, Unit](())))))

    def setF[F[_], S, L](x: F[S])(implicit A: Applicative[F]): ResultT[F, S, L, Unit] =
      apply[F, S, L, Unit](s => A.map(x)(x => (s.copy(state = x), Either.right[L, Unit](()))))

    def modify[F[_], S, L](f: S => S)(implicit A: Applicative[F]): ResultT[F, S, L, Unit] =
      apply[F, S, L, Unit](s => A.pure(((s.copy(state = f(s.state)), Either.right[L, Unit](())))))

    def modifyF[F[_], S, L](f: S => F[S])(implicit A: Applicative[F]): ResultT[F, S, L, Unit] =
      apply[F, S, L, Unit](s => A.map(f(s.state))(x => (s.copy(state = x), Either.right[L, Unit](()))))

    def inspect[F[_], S, L, R](f: S => Either[L, R])(implicit A: Applicative[F]): ResultT[F, S, L, R] =
      apply[F, S, L, R](s => A.pure((s, f(s.state))))

    def inspectE[F[_], S, L, R](f: S => R)(implicit A: Applicative[F]): ResultT[F, S, L, R] =
      apply[F, S, L, R](s => A.pure((s, Either.right[L, R](f(s.state)))))

    def inspectF[F[_], S, L, R](f: S => F[Either[L, R]])(implicit A: Applicative[F]): ResultT[F, S, L, R] =
      apply[F, S, L, R](s => A.map(f(s.state))(r => (s, r)))

    def inspectL[F[_], S, L, R](f: S => (Logs, Either[L, R]))(implicit A: Applicative[F]): ResultT[F, S, L, R] =
      apply[F, S, L, R](s => A.pure {
        val (logs, r) = f(s.state)
        (s.combine(logs), r)
      })

    def inspectEL[F[_], S, L, R](f: S => (Logs, R))(implicit A: Applicative[F]): ResultT[F, S, L, R] =
      apply[F, S, L, R](s => A.pure {
        val (logs, r) = f(s.state)
        (s.combine(logs), Either.right[L, R](r))
      })

    def inspectFL[F[_], S, L, R](f: S => F[(Logs, Either[L, R])])(implicit A: Applicative[F]): ResultT[F, S, L, R] =
      apply[F, S, L, R](s => A.map(f(s.state)) { case (logs, r) => (s.combine(logs), r) })
  }

  implicit class ResultTSyntax[F[_], S, L, R](x: ResultT[F, S, L, R]) {
    def run(st: S)(implicit F: FlatMap[F]): F[(Logs, S, Either[L, R])] =
      F.map(x.value.run(StateWithLogs.init(st)))({ r => (r._1.logs, r._1.state, r._2) })

    def runA(st: S)(implicit F: FlatMap[F]): F[Either[L, R]] =
      F.map(x.value.run(StateWithLogs.init(st)))({ case (_, r) => r })

    def runS(st: S)(implicit F: FlatMap[F]): F[S] =
      F.map(x.value.run(StateWithLogs.init(st)))({ r => r._1.state })

    def runL(st: S)(implicit F: FlatMap[F]): F[Logs] =
      F.map(x.value.run(StateWithLogs.init(st)))({ case (StateWithLogs(logs, _), _) => logs })

    def runAL(st: S)(implicit F: FlatMap[F]): F[(Logs, Either[L, R])] =
      F.map(x.value.run(StateWithLogs.init(st)))({ case (StateWithLogs(logs, _), r) => (logs, r) })

    def transformS[SS](
      f: SS => S,
      g: (SS, S) => SS)(implicit F: Functor[F]): ResultT[F, SS, L, R] =
      EitherT[StateT[F, StateWithLogs[SS], ?], L, R](x.value.transformS(
        { case StateWithLogs(logs, state) => StateWithLogs(logs, f(state)) },
        { case (StateWithLogs(_, ss), StateWithLogs(logs, s)) => StateWithLogs(logs, g(ss, s)) }))
  }
}
