### Composable data structures for logging, error handling and flow control

`result` is a small library that help you build composable programs.

The underlying is that you can build programs that define their own effects, state and errors, then
you can easily snap them together using the API the library provides.

The base library is built on top of [cats](https://typelevel.org/cats/) and the extension library
relies on [shapeless](https://github.com/milessabin/shapeless) to make creating composite state
data types easy.

The library is currently built against Scala 2.11.x and 2.12.x. Development

### Installation

```
libraryDependencies += "me.16s" %% "result" % "0.1.2"
```

#### Usage

To define a new program, for example a simple HTTP client 

```tut:silent
import result._
import scala.concurrent.Future
```

let's describe the error cases

```tut:silent
sealed trait MyError
final case class ConnectionError(msg: String) extends MyError
final case class DataConsistencyError(id: Int) extends MyError
```


```tut:silent
final case class Request(url: String)
final case class Response(body: String)

trait Client {
    // this method could potentially fail
    def request(url: Request): Future[Response] = ??? 
}

final case class HttpClientState(clientPool: List[Client])

object HttpClient {
    def request(req: Request): ResultT[Future, HttpClientState, MyError] = ???
}  
```

if you need a state, create it, otherwise just use `Unit`

```tut:silent
final case class MyState(connections: Int)
```

now you can create your own Return type

```tut:silent
type MyReturn[A] = ResultT[Future, MyState, MyError, A]
```

more to follow...