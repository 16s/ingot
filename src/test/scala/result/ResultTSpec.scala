package result

import cats.Id
import org.scalatest._
import Matchers._

class ResultTSpec extends FlatSpec {
  type SimpleTestResult = ResultT[Id, Unit, String, Int]
  "ResultT" should "correctly map" in {
    val res: SimpleTestResult = for {
      x <- ResultT.rightT(5)
      _ <- ResultT.log("This log message is ignored for now")
      y <- ResultT.rightT(3)
    } yield x + y

    val result: Either[String, Int] = res.runA(())
    result should equal(Right(8))
  }

  it should "correctly collect logs" in {
    val res: SimpleTestResult = for {
      _ <- ResultT.log("Something")
      _ <- ResultT.log("Something else")
      _ <- ResultT.log(Vector("3rd log", "4th log"))
    } yield 5

    val result: Logs = res.runL(())
    result should equal(Vector("Something", "Something else", "3rd log", "4th log"))
  }
}
