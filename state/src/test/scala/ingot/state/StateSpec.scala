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

package ingot.state

import org.scalatest._
import Matchers._
import cats.Id
import ingot._
import shapeless._
import scala.concurrent.ExecutionContext.Implicits.global
import cats.instances.future._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait DbConnection {
  def connNum: Int
}

trait HttpClient {
  def run: String => String
}

final case class CombinedState(c: DbConnection, h: HttpClient)

class StateSpec extends FlatSpec {

  private def getIdFromDb(id: Int): IngotT[Id, DbConnection, String, String] = IngotT.pure(s"id:${id.toString}")

  private def getDataFromApi(url: String): IngotT[Id, HttpClient, String, String] = IngotT.pure(s"url:$url")

  private def getFromDbAndApi: IngotT[Id, DbConnection :: HttpClient :: HNil, String, String] = {
    for {
      id <- getIdFromDb(5).transformS[DbConnection :: HttpClient :: HNil]
      url <- getDataFromApi("http://localhost").transformS[DbConnection :: HttpClient :: HNil]
    } yield s"$id$url"
  }

  private def getFromDbAndApiWithCombinedState: IngotT[Id, CombinedState, String, String] = {
    for {
      id <- getIdFromDb(5).transformS[CombinedState]
      url <- getDataFromApi("http://localhost").transformS[CombinedState]
    } yield s"$id$url"
  }

  "ingot-state" should "correctly combine multiple states into one HList" in {
    val st: DbConnection :: HttpClient :: HNil = new DbConnection { override val connNum = 5 } :: new HttpClient { override def run = identity } :: HNil

    val result = getFromDbAndApi.runA(st)
    result should equal(Right("id:5url:http://localhost"))
  }

  it should "correctly combine multiple states into one case class" in {
    val st: CombinedState = CombinedState(new DbConnection { override val connNum = 5 }, new HttpClient { override def run = identity })

    val result = getFromDbAndApiWithCombinedState.runA(st)
    result should equal(Right("id:5url:http://localhost"))
  }

  it should "properly call invoke implementation" in {
    import Implementation._
    val result = Await.result(TestService.run[ParserState, ClientState, (ParserState, ClientState)]("a").runA((ParserState(), ClientState())), Duration.Inf)
    result should equal(Right(Response("url:parsed:a")))
  }

}
