package im.tox.tox4j.av

import im.tox.core.typesafe.DiscreteValueCompanion
import scodec.codecs._
import scodec.{Attempt, Err}

import scala.concurrent.duration._
import scala.language.postfixOps

final class AudioLength private (val value: Duration) extends AnyVal

// scalastyle:off magic.number
object AudioLength extends DiscreteValueCompanion[Duration, AudioLength](
  2500 microseconds,
  5000 microseconds,
  10000 microseconds,
  20000 microseconds,
  40000 microseconds,
  60000 microseconds
) {

  protected override def unsafeFromValue(value: Duration): AudioLength = new AudioLength(value)
  protected override def toValue(self: AudioLength): Duration = self.value

  override val codec = uint16.exmap[AudioLength](
    { micros => Attempt.fromOption(fromValue(micros microseconds), new Err.General(s"Invalid value for $this: $micros")) },
    { self => Attempt.successful(self.value.toMicros.toInt) }
  )

  val Length2_5 = new AudioLength(values(0))
  val Length5 = new AudioLength(values(1))
  val Length10 = new AudioLength(values(2))
  val Length20 = new AudioLength(values(3))
  val Length40 = new AudioLength(values(4))
  val Length60 = new AudioLength(values(5))

}
