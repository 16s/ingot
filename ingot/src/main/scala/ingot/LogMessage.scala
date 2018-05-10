package ingot

final case class LogMessage(msg: String, level: LogLevel, context: Map[String, String])

object LogMessage {
  def apply(msg: String, level: LogLevel): LogMessage = apply(msg, level, Map.empty)
}

sealed trait LogLevel

object LogLevel {
  final case object Error extends LogLevel
  final case object Warning extends LogLevel
  final case object Info extends LogLevel
  final case object Debug extends LogLevel
}

sealed trait Loggable[A] {
  def toLogMessage(x: A, logLevel: LogLevel): LogMessage
}

object Loggable {
  def apply[A](implicit l: Loggable[A]): Loggable[A] = l

  def instance[A](f: (A, LogLevel) => LogMessage): Loggable[A] = new Loggable[A] {
    override def toLogMessage(x: A, logLevel: LogLevel): LogMessage = f(x, logLevel)
  }

  implicit val string = instance[String]((s, l) => LogMessage(s, l))
}
