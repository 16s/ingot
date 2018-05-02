package result.state

import org.scalatest._
import Matchers._
import cats.Id
import result._
import shapeless._
import shapeless.ops.hlist._

class StateSpec extends FlatSpec {
  trait DbConnection {
    def connNum: Int
  }
  trait HttpClient {
    def run: String => String
  }

  private def getIdFromDb(id: Int): ResultT[Id, DbConnection, String, String] = ResultT.pure(s"id:${id.toString}")

  private def getDataFromApi(url: String): ResultT[Id, HttpClient, String, String] = ResultT.pure(s"url:$url")

  private def getFromDbAndApi: ResultT[Id, DbConnection :: HttpClient :: HNil, String, String] = {
    for {
      id <- getIdFromDb(5).transformSH[DbConnection :: HttpClient :: HNil]
      url <- getDataFromApi("http://localhost").transformSH[DbConnection :: HttpClient :: HNil]
    } yield s"$id$url"
  }

  "result-state" should "correctly combine multiple states" in {
    val st: DbConnection :: HttpClient :: HNil = new DbConnection { override val connNum = 5 } :: new HttpClient { override def run = identity } :: HNil

    val result = getFromDbAndApi.runA(st)
    result should equal(Right("id:5url:http://localhost"))
  }

}
