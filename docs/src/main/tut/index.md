---
layout: home
---

# result

```tut
import result._
import scala.concurrent.Future

sealed trait Error

type MyResult[A] = ResultT[Future, Unit, Error, A]
```