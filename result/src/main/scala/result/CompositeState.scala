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

package result

trait CompositeState[S, SS] {
  def inspect(ss: SS): S
  def update(ss: SS, s: S): SS
}

object CompositeState {
  def apply[S, SS](implicit gen: CompositeState[S, SS]): CompositeState[S, SS] = gen

  def instance[S, SS](f: SS => S, g: (SS, S) => SS): CompositeState[S, SS] = new CompositeState[S, SS] {
    override def inspect(ss: SS): S = f(ss)
    override def update(ss: SS, s: S): SS = g(ss, s)
  }
}
