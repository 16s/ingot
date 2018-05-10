package ingot

trait Consumer[A, B] {
  def consume(x: A): B
}
