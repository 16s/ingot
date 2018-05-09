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

import cats.Id
import org.scalatest._
import Matchers._
import cats.syntax.either._
import cats.~>

final case class SimpleState(id: Int)

object SimpleState {
  val empty = SimpleState(0)
}

class ResultTSpec extends FlatSpec {
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

  it should "correctly use inspects" in {
    val res: SimpleTestResult = for {
      _ <- ResultT.inspect[Id, SimpleState, String, String]((st: SimpleState) => Either.right[String, String](""))
      _ <- ResultT.inspectE((st: SimpleState) => st.id)
      _ <- ResultT.inspectF[Id, SimpleState, String, Int]((st: SimpleState) => Either.right[String, Int](st.id))
      _ <- ResultT.inspectL((st: SimpleState) => (Vector.empty[String], Either.right[String, String]("")))
      _ <- ResultT.inspectEL((st: SimpleState) => (Vector.empty[String], ""))
      _ <- ResultT.inspectFL[Id, SimpleState, String, String]((st: SimpleState) => (Vector.empty[String], Either.right[String, String]("")))
    } yield 5
    val result: Either[String, Int] = res.runA(SimpleState(1))
    result should equal(Either.right[String, Int](5))
  }

  trait DbConnection {
    def connNum: Int
  }

  trait HttpClient {
    def run: String => String
  }

  implicit val dbCompositeState =
    CompositeState.instance[DbConnection, (DbConnection, HttpClient)](_._1, { case (x, y) => (y, x._2) })

  implicit val httpCompositeState =
    CompositeState.instance[HttpClient, (DbConnection, HttpClient)](_._2, { case (x, y) => (x._1, y) })

  private def getIdFromDb(id: Int): ResultT[Id, DbConnection, String, String] = ResultT.pure(s"id:${id.toString}")

  private def getDataFromApi(url: String): ResultT[Id, HttpClient, String, String] = ResultT.pure(s"url:$url")

  private def getFromDbAndApi: ResultT[Id, (DbConnection, HttpClient), String, String] = {
    for {
      id <- getIdFromDb(5).transformS[(DbConnection, HttpClient)]
      url <- getDataFromApi("http://localhost").transformS[(DbConnection, HttpClient)]
    } yield s"$id$url"
  }

  it should "correctly combine multiple states" in {
    val st = (new DbConnection { override val connNum = 5 }, new HttpClient { override def run = identity })

    val result = getFromDbAndApi.runA(st)
    result should equal(Right("id:5url:http://localhost"))
  }

  it should "correctly transforms between monads" in {
    import cats.instances.all._

    implicit val idToList = new (List ~> Option) {
      def apply[A](x: List[A]): Option[A] = x.headOption
    }
    val lst = ResultT.right[List, Unit, String, Int](List(1, 2, 3))
    val opt = lst.withMonad[Option]
    opt.runA(()) should equal(Some(Right(1)))
  }
}
