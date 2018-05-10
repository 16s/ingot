package ingot.state

import ingot._

import scala.concurrent.{ Future }
import cats.instances.all._
import cats.~>
import cats.Id
import shapeless._

import scala.concurrent.ExecutionContext.Implicits.global

final case class Parsed(s: String)
trait Parser[S] {
  def parse(s: String): Ingot[cats.Id, S, String, Parsed]
}

final case class Response(url: String)
trait Client[S] {
  def fetch(url: String): Ingot[Future, S, String, Response]
}

object Implementation {
  final case class ParserState()
  final case class ClientState()

  implicit val testParser = new Parser[ParserState] {
    override def parse(s: String): Ingot[cats.Id, ParserState, String, Parsed] =
      Ingot.right[ParserState, String](Parsed(s"parsed:$s"): Id[Parsed])
  }

  implicit val testClient = new Client[ClientState] {
    def fetch(url: String): Ingot[Future, ClientState, String, Response] =
      Ingot.pure[Future, ClientState, String, Response](Response(s"url:$url"))
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
    CCS: CompositeState[ClientS, CombinedS]): Ingot[Future, CombinedS, String, Response] = for {
    parsed <- P.parse(s).withMonad[Future].transformS[CombinedS]
    response <- C.fetch(parsed.s).transformS[CombinedS]
  } yield response
}
