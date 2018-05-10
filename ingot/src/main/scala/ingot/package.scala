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

import cats.{ Applicative, FlatMap, Functor, ~> }
import cats.data.{ EitherT, StateT }
import cats.instances.all._
import cats.Id

import scala.language.higherKinds

package object ingot {
  type LogContainer[M] = Vector[M]
  type LogMessage = String
  type Logs = LogContainer[LogMessage]

  type ActionType[F[_], S, L, R] = StateWithLogs[S] => F[(StateWithLogs[S], Either[L, R])]
  type Ingot[F[_], S, L, R] = EitherT[StateT[F, StateWithLogs[S], ?], L, R]
  type Yoga[F[_], L, R] = Ingot[F, Unit, L, R]
  type Clay[L, R] = Ingot[Id, Unit, L, R]

  implicit class IngotSyntax[F[_], S, L, R](x: Ingot[F, S, L, R]) {
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

    def withMonad[G[_]](implicit F: FlatMap[F], A: Applicative[G], K: F ~> G): Ingot[G, S, L, R] =
      Ingot[G, S, L, R](st => K(x.value.run(st)))

    def transformS[SS](implicit F: Functor[F], CS: CompositeState[S, SS]): Ingot[F, SS, L, R] =
      EitherT[StateT[F, StateWithLogs[SS], ?], L, R](x.value.transformS(
        { case StateWithLogs(logs, state) => StateWithLogs(logs, CS.inspect(state)) },
        { case (StateWithLogs(_, ss), StateWithLogs(logs, s)) => StateWithLogs(logs, CS.update(ss, s)) }))
  }

  implicit class YogaSyntax[F[_], L, R](x: Ingot[F, Unit, L, R]) {
    def runA()(implicit F: FlatMap[F]): F[Either[L, R]] =
      F.map(x.value.run(StateWithLogs.init(())))({ case (_, r) => r })

    def runL()(implicit F: FlatMap[F]): F[Logs] =
      F.map(x.value.run(StateWithLogs.init(())))({ case (StateWithLogs(logs, _), _) => logs })

    def runAL()(implicit F: FlatMap[F]): F[(Logs, Either[L, R])] =
      F.map(x.value.run(StateWithLogs.init(())))({ case (StateWithLogs(logs, _), r) => (logs, r) })
  }

  implicit class ClaySyntax[L, R](x: Ingot[Id, Unit, L, R]) {
    def runA(): Either[L, R] =
      x.value.run(StateWithLogs.init(()))._2

    def runL(): Logs =
      x.value.run(StateWithLogs.init(()))._1.logs

    def runAL(): (Logs, Either[L, R]) = {
      val (st, res) = x.value.run(StateWithLogs.init(()))
      (st.logs, res)
    }
  }

}
