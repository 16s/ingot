package result.state

import org.scalatest._
import Matchers._
import cats.Id
import result._
import shapeless._

class StateSpec extends FlatSpec {
  trait DbConnection {
    def connNum: Int
  }
  trait HttpClient {
    def run: String => String
  }

  private def getIdFromDb(id: Int): ResultT[Id, DbConnection, String, String] = ResultT.pure(s"id:${id.toString}")

  private def getDataFromApi(url: String): ResultT[Id, HttpClient, String, String] = ResultT.pure(s"url:$url")

  private final case class CombinedState(c: DbConnection, h: HttpClient)

  private def getFromDbAndApi: ResultT[Id, DbConnection :: HttpClient :: HNil, String, String] = {
    for {
      id <- getIdFromDb(5).withState[DbConnection :: HttpClient :: HNil]
      url <- getDataFromApi("http://localhost").withState[DbConnection :: HttpClient :: HNil]
    } yield s"$id$url"
  }

  private def getFromDbAndApiWithCombinedState: ResultT[Id, CombinedState, String, String] = {
    for {
      id <- getIdFromDb(5).withState[CombinedState]
      url <- getDataFromApi("http://localhost").withState[CombinedState]
    } yield s"$id$url"
  }

  "result-state" should "correctly combine multiple states into one HList" in {
    val st: DbConnection :: HttpClient :: HNil = new DbConnection { override val connNum = 5 } :: new HttpClient { override def run = identity } :: HNil

    val result = getFromDbAndApi.runA(st)
    result should equal(Right("id:5url:http://localhost"))
  }

  it should "correctly combine multiple states into one case class" in {
    val st: CombinedState = CombinedState(new DbConnection { override val connNum = 5 }, new HttpClient { override def run = identity })

    val result = getFromDbAndApiWithCombinedState.runA(st)
    result should equal(Right("id:5url:http://localhost"))
  }

}
