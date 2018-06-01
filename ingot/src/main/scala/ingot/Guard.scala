/**
 * Copyright 2018 Tamas Neltz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ingot

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Try, Failure, Success }
import cats.syntax.either._

trait Guard[F[_], G[_]] {
  def apply[A](x: F[A]): G[Either[Throwable, A]]
}

object Guard {
  def apply[F[_], G[_]](implicit g: Guard[F, G]): Guard[F, G] = g

  implicit val tryGuard: Guard[Try, cats.Eval] = new Guard[Try, cats.Eval] {
    override def apply[A](x: Try[A]): cats.Eval[Either[Throwable, A]] = x match {
      case Failure(t) => cats.Eval.now(Either.left[Throwable, A](t))
      case Success(s) => cats.Eval.now(Either.right[Throwable, A](s))
    }
  }

  implicit def futureGuard(implicit ec: ExecutionContext): Guard[Future, Future] = new Guard[Future, Future] {
    override def apply[A](x: Future[A]): Future[Either[Throwable, A]] =
      x.map(r => Either.right[Throwable, A](r))
        .recover({ case t => Either.left[Throwable, A](t) })
  }
}
