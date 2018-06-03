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

package ingot

import cats.Eval
import cats.data.{ EitherT, StateT }
import cats.syntax.either._

object Rebar {
  outer =>

  def pure[S, L, R](x: R): Rebar[S, L, R] = rightT(x)

  def apply[S, L, R](
    x: StateWithLogs[S] => (StateWithLogs[S], Either[L, R])): Rebar[S, L, R] =
    EitherT[StateT[Eval, StateWithLogs[S], ?], L, R](StateT(x.andThen(Eval.now)))

  def applyF[S, L, R](
    x: StateWithLogs[S] => Eval[(StateWithLogs[S], Either[L, R])]): Rebar[S, L, R] =
    EitherT[StateT[Eval, StateWithLogs[S], ?], L, R](StateT(x))

  def rightT[S, L, R](x: R): Rebar[S, L, R] =
    apply[S, L, R](s => (s, Either.right[L, R](x)))

  def leftT[S, L, R](x: L): Rebar[S, L, R] =
    apply[S, L, R](s => (s, Either.left[L, R](x)))

  final class LiftPartiallyApplied[S] {
    def apply[L, R](x: Either[L, R]): Rebar[S, L, R] =
      outer.apply[S, L, R](s => (s, x))
  }

  def lift[S] = new LiftPartiallyApplied[S]

  final class GuardPartiallyApplied[S] {
    def apply[F[_], R](x: F[R])(implicit G: Guard[F, Eval]): Rebar[S, Throwable, R] =
      outer.applyF(st => G(x).map(x => (st, x)))
  }

  def guard[S] = new GuardPartiallyApplied[S]

  def log[S, L](x: LogMessage): Rebar[S, L, Unit] =
    apply[S, L, Unit](s => (s.combine(x), Either.right[L, Unit](())))

  def log[S, L](x: Logs): Rebar[S, L, Unit] =
    apply[S, L, Unit](s => ((s.combine(x), Either.right[L, Unit](()))))

  def get[S, L]: Rebar[S, L, S] =
    apply[S, L, S](s => (s, Either.right[L, S](s.state)))

  def getL[S, L]: Rebar[S, L, Logs] =
    apply[S, L, Logs](s => (s, Either.right[L, Logs](s.logs)))

  def set[S, L](x: S): Rebar[S, L, Unit] =
    apply[S, L, Unit](s => ((s.copy(state = x), Either.right[L, Unit](()))))

  def modify[S, L](f: S => S): Rebar[S, L, Unit] =
    apply[S, L, Unit](s => ((s.copy(state = f(s.state)), Either.right[L, Unit](()))))

  def inspect[S, L, R](f: S => Either[L, R]): Rebar[S, L, R] =
    apply[S, L, R](s => (s, f(s.state)))

  final class InspectEPartiallyApplied[L] {
    def apply[S, R](f: S => R): Rebar[S, L, R] =
      outer.apply[S, L, R](s => (s, Either.right[L, R](f(s.state))))
  }

  def inspectE[L] = new InspectEPartiallyApplied[L]

  def inspectG[F[_], S, R](
    f: S => F[R])(
    implicit
    G: Guard[F, Eval]): Rebar[S, Throwable, R] =
    applyF[S, Throwable, R](s => G(f(s.state)).map(r => (s, r)))

  def inspectL[S, L, R](f: S => (Logs, Either[L, R])): Rebar[S, L, R] =
    outer.apply[S, L, R] { s =>
      val (logs, r) = f(s.state)
      (s.combine(logs), r)
    }

  final class InspectELPartiallyApplied[L] {
    def apply[S, R](f: S => (Logs, R)): Rebar[S, L, R] =
      outer.apply[S, L, R] { s =>
        val (logs, r) = f(s.state)
        (s.combine(logs), Either.right[L, R](r))
      }
  }

  def inspectEL[L] = new InspectELPartiallyApplied[L]

  def flushLogs[S, L](
    logger: Logger[Eval]): Rebar[S, L, Unit] = applyF { swl =>
    logger.log(swl.logs).map(_ => (StateWithLogs.init(swl.state), Either.right[L, Unit](())))
  }
}
