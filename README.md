# A simple return type to manage dependencies and logging

```sh
git clone git@github.com:16s/result.git
```

Usage:
```scala
scala> import result._
import result._

scala> import scala.concurrent.Future
import scala.concurrent.Future

scala> def myFunc(): ResultT[Future, Unit, String, String] = ???
myFunc: ()result.ResultT[scala.concurrent.Future,Unit,String,String]
```
