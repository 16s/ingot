package result
import cats.Functor
import scala.language.higherKinds
import shapeless._
import shapeless.ops.hlist._

package object state {

  trait CompositeState[S, SS] {
    def inspect(ss: SS): S
    def update(ss: SS, s: S): SS

    def getTransform[F[_], L, R](x: ResultT[F, S, L, R])(implicit F: Functor[F]): ResultT[F, SS, L, R] = {
      x.transformS(inspect, update)
    }
  }

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

  implicit class ResultTStateSyntax[F[_], S, L, R](x: ResultT[F, S, L, R]) {
    def withState[SS](implicit CS: CompositeState[S, SS], F: Functor[F]): ResultT[F, SS, L, R] =
      CS.getTransform(x)

  }
}
