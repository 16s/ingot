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
import shapeless._
import shapeless.ops.hlist._

package object state {

  implicit def genericCompositeState[S, SS, R](
    implicit
    gen: Generic.Aux[SS, R],
    cs: CompositeState[S, R]): CompositeState[S, SS] = new CompositeState[S, SS] {
    override def inspect(ss: SS): S =
      cs.inspect(gen.to(ss))

    override def update(ss: SS, s: S): SS =
      gen.from(cs.update(gen.to(ss), s))
  }

  implicit def hlistCompositeState[SS <: HList, S](implicit S: Selector[SS, S], replacer: shapeless.ops.hlist.Replacer.Aux[SS, S, S, (S, SS)]): CompositeState[S, SS] = new CompositeState[S, SS] {
    override def inspect(ss: SS): S = ss.select[S]
    override def update(ss: SS, s: S): SS = ss.updatedElem(s)
  }
}
