package result

import cats.Id
import org.scalatest._
import Matchers._

class ResultTSpec extends FlatSpec {
  type SimpleTestResult = ResultT[Id, Unit, String, Int]
  "ResultT" should "correctly map" in {
    val res: SimpleTestResult = for {
      x <- ResultT.rightT(5)
      y <- ResultT.rightT(3)
    } yield x + y

    val result: Either[String, Int] = res.runA(())
    result should equal(Right(8))
  }
}
