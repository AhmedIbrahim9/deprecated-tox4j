package im.tox.core.error

import scodec.Err.General
import scodec.{Attempt, Err}

import scalaz.{-\/, \/, \/-}

sealed abstract class CoreError {
  val exception = new Throwable(toString)
}

object CoreError {

  final case class Unimplemented(message: String) extends CoreError
  final case class InvalidFormat(message: String) extends CoreError
  final case class CodecError(cause: Err) extends CoreError
  final case class DecryptionError() extends CoreError

  def apply[A](attempt: Attempt[A]): CoreError \/ A = {
    attempt match {
      case Attempt.Failure(cause)    => -\/(CodecError(cause))
      case Attempt.Successful(value) => \/-(value)
    }
  }

  def toAttempt[A](value: CoreError \/ A): Attempt[A] = {
    Attempt.fromEither(value.leftMap {
      case CodecError(cause) => cause
      case error             => new General(error.toString)
    }.toEither)
  }

}
