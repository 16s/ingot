# A simple return type to manage dependencies and logging

```sh
git clone git@github.com:16s/result.git
```

Usage:
```tut
import result._
import scala.concurrent.Future

def myFunc(): ResultT[Future, Unit, String, String] = ???

```