package result
import cats.Functor
import shapeless._
import shapeless.ops.hlist._

package object state {
  implicit class ResultTStateSyntax[F[_], S, L, R](x: ResultT[F, S, L, R]) {
    def transformSH[SS <: HList](implicit S: Selector[SS, S], R: Replacer.Aux[SS, S, S, (S, SS)], F: Functor[F]): ResultT[F, SS, L, R] = {
      val f: SS => S = x => x.select[S]
      val g: (SS, S) => SS = (ss, s) => ss.updatedElem(s)
      new ResultTSyntax(x).transformS(f, g)
    }

  }
}
