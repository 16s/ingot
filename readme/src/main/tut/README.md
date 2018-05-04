### A simple return type to manage dependencies and logging

This is a tiny library with the primary goal to create a standard return type that handles
errors and logging (for motivation see my [ny-scala talk](https://youtu.be/xoJcLDOa98M)) 


#### Usage

First, import the library

```tut:silent
import result._
import scala.concurrent.Future
```

create an error type (or use an existing one), a simple ADT should do it 

```tut:silent
sealed trait MyError
final case class ConnectionError(msg: String) extends MyError
final case class DataConsistencyError(id: Int) extends MyError 
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