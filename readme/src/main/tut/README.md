![Maven Central](https://maven-badges.herokuapp.com/maven-central/me.16s/ingot_2.12/badge.svg)

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

#### Synchronous operation

The simplest use case is when there is no state or effect monad, then you can just use the `Clay` data type after importing `ingot._`:

```tut:silent
import cats.syntax.either._
import ingot._
```

You can construct programs by calling the different materializers available for `Clay`.

```tut
Clay.rightT[Int]("aaaa")
Clay.leftT[String](5)
Clay.lift(Either.right[Int, String]("b"))
```

you can even use guards against `Exception`s, for example you can automatically convert `scala.util.Try` to `Clay`.

```tut
Clay.guard(scala.util.Try("aaa"))
```

There's also a special call that doesn't return a value but it adds a log message that can
later be printed:

```tut
val program = for {
    _ <- Clay.log[Int]("this is a log message".asInfo)
    _ <- Clay.log[Int]("this is a second log message".asError)
    } yield ()
```

To be able to print the logs you need an implementation of the `Logger[F[_]]` typeclasse,
for example


```tut:silent
val logger = new Logger[cats.Id] {
    private def printCtx(ctx: Map[String, String]): String =
        if (ctx.isEmpty) ""
        else ctx.map({case (k, v) => s"$k: $v"}).mkString("\n", "\n", "")

    override def log(x: Logs) = x.map { // Logs is just an alias for Vector[LogMessage]
        case LogMessage(msg, LogLevel.Error, ctx) => println(s"ERROR: $msg${printCtx(ctx)}") 
        case LogMessage(msg, LogLevel.Warning, ctx) => println(s"WARNING: $msg${printCtx(ctx)}") 
        case LogMessage(msg, LogLevel.Info, ctx) => println(s"INFO: $msg${printCtx(ctx)}") 
        case LogMessage(msg, LogLevel.Debug, ctx) => println(s"DEBUG: $msg${printCtx(ctx)}") 
    }
}

```

Now you can simply run:

```tut
program.flushLogs(logger).runA()
```

And the program will execute, as a last step printing out the log. Alternatively you can use
`runAL` that returns a tuple of the logs and the result of the program. Running `flushLogs` gets
rid of the logs stored in the data structure so `runAL` would return an empty list of log messages.

The `flushLogs` method also available as a method on the `Clay` object so it can be interleaved with
existing programs. Even though it's not recommended since logging is a side effect but sometimes it is
necessary:

```tut
val program2 = for {
    _ <- Clay.log[String]("This will be flushed".asInfo)
    _ <- Clay.log[String]("This will also be printed".asError)
    _ <- Clay.flushLogs[String](logger)
    _ <- Clay.log[String]("This will stay".asDebug)
} yield ()
program2.runAL()
```

Here's a slightly more involved example of combining programs:

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
    _ <- Clay.log("Loaded the response".asInfo)
    cs <- responseCheckSum(resp)
    _ <- Clay.log("Got the checksum".asDebug)
    } yield ValidatedMessage(resp, cs)
}
```


Then you can just run it:

```tut
service().runAL()
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

```tut:silent
import scala.concurrent.Future

sealed trait SendMessageError

final case object SendMessageTimeout extends SendMessageError

def sendMessage: Brick[Future, SendMessageError, Unit] = ??? 
```


