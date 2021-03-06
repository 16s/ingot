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

import cats.{ Applicative, Eval, FlatMap, Functor, ~> }
import cats.data.{ EitherT, StateT }

import scala.language.higherKinds

package object ingot {
  type LogContainer[M] = Vector[M]
  type Logs = LogContainer[LogMessage]

  type ActionType[F[_], S, L, R] = StateWithLogs[S] => F[(StateWithLogs[S], Either[L, R])]
  type Ingot[F[_], S, L, R] = EitherT[StateT[F, StateWithLogs[S], ?], L, R]
  type Brick[F[_], L, R] = Ingot[F, Unit, L, R]
  type Rebar[S, L, R] = Ingot[Eval, S, L, R]
  type Clay[L, R] = Ingot[Eval, Unit, L, R]

  implicit class ClaySyntax[L, R](x: Ingot[Eval, Unit, L, R]) {
    def runA(): Either[L, R] =
      x.value.run(StateWithLogs.init(())).value._2

    def runL(): Logs =
      x.value.run(StateWithLogs.init(())).value._1.logs

    def runAL(): (Logs, Either[L, R]) = {
      val (st, res) = x.value.run(StateWithLogs.init(())).value
      (st.logs, res)
    }

    def renderA[A]()(implicit fail: Consumer[L, A], success: Consumer[R, A]): A = runAL() match {
      case (_, Right(result)) => success.consume(result)
      case (_, Left(err)) => fail.consume(err)
    }

    def renderAL[A]()(implicit fail: Consumer[L, A], success: Consumer[R, A]): (Logs, A) = runAL() match {
      case (l, Right(result)) => (l, success.consume(result))
      case (l, Left(err)) => (l, fail.consume(err))
    }

    def flushLogs(logger: Logger[Eval]): Clay[L, R] = Clay[L, R] { _ =>
      val (logs, result) = runAL()
      logger.log(logs).value
      (StateWithLogs.init(()), result)
    }
  }

  implicit class BrickSyntax[F[_], L, R](x: Ingot[F, Unit, L, R]) {
    def runA()(implicit F: FlatMap[F]): F[Either[L, R]] =
      F.map(x.value.run(StateWithLogs.init(())))({ case (_, r) => r })

    def runL()(implicit F: FlatMap[F]): F[Logs] =
      F.map(x.value.run(StateWithLogs.init(())))({ case (StateWithLogs(logs, _), _) => logs })

    def runAL()(implicit F: FlatMap[F]): F[(Logs, Either[L, R])] =
      F.map(x.value.run(StateWithLogs.init(())))({ case (StateWithLogs(logs, _), r) => (logs, r) })

    def renderA[A]()(implicit F: FlatMap[F], fail: Consumer[L, A], success: Consumer[R, A]): F[A] = F.map(runAL())({
      case (_, Right(result)) => success.consume(result)
      case (_, Left(err)) => fail.consume(err)
    })

    def renderAL[A]()(implicit F: FlatMap[F], fail: Consumer[L, A], success: Consumer[R, A]): F[(Logs, A)] = F.map(runAL())({
      case (l, Right(result)) => (l, success.consume(result))
      case (l, Left(err)) => (l, fail.consume(err))
    })
  }

  implicit class RebarSyntax[S, L, R](x: Ingot[Eval, S, L, R]) {
    def run(st: S): (Logs, S, Either[L, R]) =
      x.value.run(StateWithLogs.init(st)).map({ r => (r._1.logs, r._1.state, r._2) }).value

    def runA(st: S): Either[L, R] =
      x.value.run(StateWithLogs.init(st)).map({ case (_, r) => r }).value

    def runS(st: S): S =
      x.value.run(StateWithLogs.init(st)).map({ r => r._1.state }).value

    def runL(st: S): Logs =
      x.value.run(StateWithLogs.init(st)).map({ case (StateWithLogs(logs, _), _) => logs }).value

    def runAL(st: S): (Logs, Either[L, R]) =
      x.value.run(StateWithLogs.init(st)).map({ case (StateWithLogs(logs, _), r) => (logs, r) }).value

    def transformS[SS](implicit CS: CompositeState[S, SS]): Rebar[SS, L, R] =
      EitherT[StateT[Eval, StateWithLogs[SS], ?], L, R](x.value.transformS(
        { case StateWithLogs(logs, state) => StateWithLogs(logs, CS.inspect(state)) },
        { case (StateWithLogs(_, ss), StateWithLogs(logs, s)) => StateWithLogs(logs, CS.update(ss, s)) }))

    def render[A](st: S)(implicit fail: Consumer[L, A], success: Consumer[R, A]): (Logs, S, A) = run(st) match {
      case (l, st, Right(result)) => (l, st, success.consume(result))
      case (l, st, Left(err)) => (l, st, fail.consume(err))
    }

    def renderA[A](st: S)(implicit fail: Consumer[L, A], success: Consumer[R, A]): A = run(st) match {
      case (_, _, Right(result)) => success.consume(result)
      case (_, _, Left(err)) => fail.consume(err)
    }

    def renderAL[A](st: S)(implicit fail: Consumer[L, A], success: Consumer[R, A]): (Logs, A) = run(st) match {
      case (l, _, Right(result)) => (l, success.consume(result))
      case (l, _, Left(err)) => (l, fail.consume(err))
    }
  }

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

    def render[A](st: S)(implicit F: FlatMap[F], fail: Consumer[L, A], success: Consumer[R, A]): F[(Logs, S, A)] = F.map(run(st))({
      case (l, st, Right(result)) => (l, st, success.consume(result))
      case (l, st, Left(err)) => (l, st, fail.consume(err))
    })

    def renderA[A](st: S)(implicit F: FlatMap[F], fail: Consumer[L, A], success: Consumer[R, A]): F[A] = F.map(run(st))({
      case (_, _, Right(result)) => success.consume(result)
      case (_, _, Left(err)) => fail.consume(err)
    })

    def renderAL[A](st: S)(implicit F: FlatMap[F], fail: Consumer[L, A], success: Consumer[R, A]): F[(Logs, A)] = F.map(run(st))({
      case (l, _, Right(result)) => (l, success.consume(result))
      case (l, _, Left(err)) => (l, fail.consume(err))
    })

    def flushLogs(logger: Logger[F])(implicit F: FlatMap[F], A: Applicative[F]): Ingot[F, S, L, R] = Ingot[F, S, L, R] { swl =>
      F.flatMap(run(swl.state)) {
        case (logs, st, result) =>
          F.map(logger.log(logs)) { _ =>
            (StateWithLogs.init(st), result)
          }
      }
    }

    def log(log: LogMessage)(implicit F: FlatMap[F], A: Applicative[F]): Ingot[F, S, L, R] = Ingot[F, S, L, R] { swl =>
      A.map(run(swl.state)) {
        case (logs, st, result) =>
          (StateWithLogs(logs, st).combine(log), result)
      }
    }

    def leftLog(log: LogMessage)(implicit F: FlatMap[F], A: Applicative[F]): Ingot[F, S, L, R] = Ingot[F, S, L, R] { swl =>
      A.map(run(swl.state)) {
        case (logs, st, err @ Left(_)) =>
          (StateWithLogs(logs, st).combine(log), err)
        case (logs, st, result) =>
          (StateWithLogs(logs, st), result)
      }
    }

    def rightLog(log: LogMessage)(implicit F: FlatMap[F], A: Applicative[F]): Ingot[F, S, L, R] = Ingot[F, S, L, R] { swl =>
      A.map(run(swl.state)) {
        case (logs, st, result @ Right(_)) =>
          (StateWithLogs(logs, st).combine(log), result)
        case (logs, st, err) =>
          (StateWithLogs(logs, st), err)
      }
    }
  }

  implicit class LoggableSyntax[A](x: A)(implicit L: Loggable[A]) {
    def asError: LogMessage = L.toLogMessage(x, LogLevel.Error)
    def asWarning: LogMessage = L.toLogMessage(x, LogLevel.Warning)
    def asInfo: LogMessage = L.toLogMessage(x, LogLevel.Info)
    def asDebug: LogMessage = L.toLogMessage(x, LogLevel.Debug)
  }

}
