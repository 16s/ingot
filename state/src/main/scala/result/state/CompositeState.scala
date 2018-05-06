package result.state

import result._
import cats.Functor

trait CompositeState[S, SS] {
  def inspect(ss: SS): S
  def update(ss: SS, s: S): SS

  def getTransform[F[_], L, R](x: ResultT[F, S, L, R])(implicit F: Functor[F]): ResultT[F, SS, L, R] = {
    x.transformS(inspect, update)
  }
}
