# A simple return type to manage dependencies and logging

```sh
git clone git@github.com:16s/result.git
```

Usage:
```scala
import result._

def myFunc(): ResultT[Future, Unit, String, String] = ???

```