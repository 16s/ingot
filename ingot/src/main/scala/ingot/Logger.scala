package ingot

trait Logger[F[_]] {
  def log(logs: Logs): F[Unit]
}
