package ingot.state

import ingot._

import scala.concurrent.{ Future }
import cats.instances.all._
import cats.~>
import shapeless._

import scala.concurrent.ExecutionContext.Implicits.global

final case class Parsed(s: String)
trait Parser[S] {
  def parse(s: String): IngotT[cats.Id, S, String, Parsed]
}

final case class Response(url: String)
trait Client[S] {
  def fetch(url: String): IngotT[Future, S, String, Response]
}

object Implementation {
  final case class ParserState()
  final case class ClientState()

  implicit val testParser = new Parser[ParserState] {
    override def parse(s: String): IngotT[cats.Id, ParserState, String, Parsed] =
      IngotT.right[cats.Id, ParserState, String, Parsed](Parsed(s"parsed:$s"))
  }

  implicit val testClient = new Client[ClientState] {
    def fetch(url: String): IngotT[Future, ClientState, String, Response] =
      IngotT.pure[Future, ClientState, String, Response](Response(s"url:$url"))
  }

}

object TestService {
  implicit val idToFuture: (cats.Id ~> Future) = new (cats.Id ~> Future) {
    override def apply[A](fa: Id[A]): Future[A] = Future.successful(fa)
  }

  def run[ParserS, ClientS, CombinedS](
    s: String)(
    implicit
    P: Parser[ParserS],
    C: Client[ClientS],
    PCS: CompositeState[ParserS, CombinedS],
    CCS: CompositeState[ClientS, CombinedS]): IngotT[Future, CombinedS, String, Response] = for {
    parsed <- P.parse(s).withMonad[Future].transformS[CombinedS]
    response <- C.fetch(parsed.s).transformS[CombinedS]
  } yield response
}
