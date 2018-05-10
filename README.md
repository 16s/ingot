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

```scala
import cats.syntax.either._
import ingot._
```

```scala
scala> Clay.rightT[Int]("aaaa")
<console>:10: warning: Unused import
       import cats.syntax.either._
                                 ^
res0: ingot.Clay[Int,String] = EitherT(cats.data.IndexedStateT@4d3bdb3c)

scala> Clay.leftT[String](5)
<console>:10: warning: Unused import
       import cats.syntax.either._
                                 ^
res1: ingot.Clay[Int,String] = EitherT(cats.data.IndexedStateT@9924f5d)

scala> Clay.lift(Either.right[Int, String]("b"))
res2: ingot.Clay[Int,String] = EitherT(cats.data.IndexedStateT@7b86a9b8)
```

you can even use guards against `Exception`s, for example you can automatically convert `scala.util.Try` to `Clay`.

```scala
scala> Clay.guard(scala.util.Try("aaa"))
<console>:10: warning: Unused import
       import cats.syntax.either._
                                 ^
res3: ingot.Clay[Throwable,String] = EitherT(cats.data.IndexedStateT@918b621)
```


```scala
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

```scala
scala> service().runAL()
<console>:10: warning: Unused import
       import cats.syntax.either._
                                 ^
<console>:13: warning: Unused import
       import ingot._
                    ^
res4: (ingot.Logs, Either[MyError,ValidatedMessage]) = (Vector(Loaded the response, Got the checksum),Right(ValidatedMessage(a,5)))
```

or, if you only want the results and discard the logs:

```scala
scala> service().runA()
<console>:10: warning: Unused import
       import cats.syntax.either._
                                 ^
<console>:13: warning: Unused import
       import ingot._
                    ^
res5: Either[MyError,ValidatedMessage] = Right(ValidatedMessage(a,5))
```

or just the logs:

```scala
scala> service().runL()
<console>:10: warning: Unused import
       import cats.syntax.either._
                                 ^
<console>:13: warning: Unused import
       import ingot._
                    ^
res6: ingot.Logs = Vector(Loaded the response, Got the checksum)
```

If you want to mix in an effect monad you can switch to `Brick[F[_], L, R]`. `Clay` is a more specific version of `Brick`,
basically

```scala
import cats.Id
type Clay[L, R] = Brick[Id, L, R]
```

so everything that has a `Clay` data type will work with everything that is a `Brick`. `Brick` can be created the same
way as `Clay` with a few materializers added to support more input methods

```scala
scala> import scala.concurrent.Future
<console>:10: warning: Unused import
       import cats.syntax.either._
                                 ^
<console>:13: warning: Unused import
       import ingot._
                    ^
<console>:16: warning: Unused import
       import ingot._
                    ^
<console>:21: warning: Unused import
       import scala.concurrent.Future
                               ^
import scala.concurrent.Future

scala> sealed trait SendMessageError
<console>:10: warning: Unused import
       import cats.syntax.either._
                                 ^
<console>:13: warning: Unused import
       import ingot._
                    ^
<console>:16: warning: Unused import
       import ingot._
                    ^
<console>:18: warning: Unused import
       import scala.concurrent.Future
                               ^
defined trait SendMessageError

scala> final case object SendMessageTimeout extends SendMessageError
<console>:10: warning: Unused import
       import cats.syntax.either._
                                 ^
<console>:13: warning: Unused import
       import ingot._
                    ^
<console>:16: warning: Unused import
       import ingot._
                    ^
<console>:18: warning: Unused import
       import scala.concurrent.Future
                               ^
defined object SendMessageTimeout

scala> def sendMessage: Brick[Future, SendMessageError, Unit] = ??? 
<console>:10: warning: Unused import
       import cats.syntax.either._
                                 ^
<console>:13: warning: Unused import
       import ingot._
                    ^
sendMessage: ingot.Brick[scala.concurrent.Future,SendMessageError,Unit]
```


