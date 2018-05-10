### Composable data structures for logging, error handling and flow control

`result` is a small library that help you build composable programs.

The underlying is that you can build programs that define their own effects, state and errors, then
you can easily snap them together using the API the library provides.

The base library is built on top of [cats](https://typelevel.org/cats/) and the extension library
relies on [shapeless](https://github.com/milessabin/shapeless) to make creating composite state
data types easy.

The library is currently built against Scala 2.11.x and 2.12.x.

It's still in early development, a lot is going to change.

### Installation

```
libraryDependencies += "me.16s" %% "ingot" % "0.1.3"
```

or, the latest dev version is

```
libraryDependencies += "me.16s" %% "ingot" % "0.1.4-SNAPSHOT"
```


#### Usage

The simplest use case is when there is no state or effect monad, then you can just use the `Clay` data type after importing `ingot._`:

```tut:silent
import cats.syntax.either._
import ingot._
```

```tut
Clay.rightT[Int]("aaaa")
Clay.leftT[String](5)
Clay.lift(Either.right[Int, String]("b"))
```

you can even use guards against `Exception`s, for example you can automatically convert `scala.util.Try` to `Clay`.

```tut
Clay.guard(scala.util.Try("aaa"))
```


```tut:silent
import ingot._

sealed trait MyError
final case class ConnectionError(msg: String) extends MyError
final case class DataConsistencyError(id: Int) extends MyError

def getResponse(): Clay[MyError, String] = Clay.rightT("a")

def responseCheckSum(resp: String): Clay[MyError, Int] = Clay.rightT(5)

final case class ValidatedMessage(msg: String, checkSum: Int)

def service(): Clay[MyError, ValidatedMessage] = {
    for {
    resp <- getResponse()
    _ <- Clay.log("Loaded the response")
    cs <- responseCheckSum(resp)
    _ <- Clay.log("Got the checksum")
    } yield ValidatedMessage(resp, cs)
}
```


Then you can just run it:

```tut
service().runAL()
```

or, if you only want the results and discard the logs:

```tut
service().runA()
```

or just the logs:

```tut
service().runL()
```

If you want to mix in an effect monad you can switch to `Brick[F[_], L, R]`. `Clay` is a more specific version of `Brick`,
basically

```scala
import cats.Id
type Clay[L, R] = Brick[Id, L, R]
```

so everything that has a `Clay` data type will work with everything that is a `Brick`. `Brick` can be created the same
way as `Clay` with a few materializers added to support more input methods

```tut
import scala.concurrent.Future

sealed trait SendMessageError

final case object SendMessageTimeout extends SendMessageError

def sendMessage: Brick[Future, SendMessageError, Unit] = ??? 
```


