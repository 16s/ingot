package result

import cats.Id
import org.scalatest._
import Matchers._

class ResultTSpec extends FlatSpec {
  final case class SimpleState(id: Int)
  object SimpleState {
    val empty = SimpleState(0)
  }
  type SimpleTestResult = ResultT[Id, SimpleState, String, Int]
  "ResultT" should "correctly map" in {
    val res: SimpleTestResult = for {
      x <- ResultT.rightT(5)
      _ <- ResultT.log("This log message is ignored for now")
      y <- ResultT.rightT(3)
    } yield x + y

    val result: Either[String, Int] = res.runA(SimpleState.empty)
    result should equal(Right(8))
  }

  it should "correctly collect logs" in {
    val res: SimpleTestResult = for {
      _ <- ResultT.log("Something")
      _ <- ResultT.log("Something else")
      _ <- ResultT.log(Vector("3rd log", "4th log"))
    } yield 5

    val result: Logs = res.runL(SimpleState.empty)
    result should equal(Vector("Something", "Something else", "3rd log", "4th log"))
  }

  it should "correctly transform the state" in {
    val res: SimpleTestResult = for {
      _ <- ResultT.log[Id, SimpleState, String]("Starting off")
      st <- ResultT.get
      _ <- ResultT.set(SimpleState(st.id * 2))
      _ <- ResultT.modify((x: SimpleState) => SimpleState(x.id + 3))
      st <- ResultT.get
    } yield st.id
    val result: SimpleState = res.runS(SimpleState(1))
    result should equal(SimpleState(5))
  }
}
