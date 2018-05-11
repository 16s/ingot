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

import cats.Applicative
import cats.instances.all._

final case class StateWithLogs[+S](logs: Logs, state: S) {
  def combine(x: LogMessage): StateWithLogs[S] =
    copy(logs = cats.Monoid[Logs].combine(logs, Applicative[LogContainer].pure(x)))
  def combine(x: Logs): StateWithLogs[S] =
    copy(logs = cats.Monoid[Logs].combine(logs, x))
}

object StateWithLogs {
  def init[S](s: S): StateWithLogs[S] = StateWithLogs(cats.Monoid[Logs].empty, s)
}
