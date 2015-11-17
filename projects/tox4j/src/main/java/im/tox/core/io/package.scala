package im.tox.core

import im.tox.core.dht.{Dht, NodeInfo}
import im.tox.core.network.PacketKind
import im.tox.core.network.packets.ToxPacket

import scala.concurrent.duration.Duration
import scalaz.stream.udp.Packet
import scalaz.{StateFunctions, State}

package object io {

  private type S = Seq[IO.Action]
  type IO[A] = State[S, A]

  object IO {

    sealed trait Action
    object Action {
      case object Shutdown extends Action
      final case class SendTo(receiver: NodeInfo, packet: ToxPacket[PacketKind]) extends Action
      final case class StartTimer(delay: Duration, repeat: Option[Int] = None)(val event: Duration => Option[Event]) extends Action
    }

    sealed trait Event
    case object ShutdownEvent extends Event
    final case class TimedActionEvent(action: Dht => IO[Dht]) extends Event
    final case class NetworkEvent(packet: Packet) extends Event

    def apply[A](a: A): IO[A] = State.state(a)

    def sendTo(receiver: NodeInfo, packet: ToxPacket[PacketKind]): IO[Unit] = {
      addAction(Action.SendTo(receiver, packet))
    }

    def startTimer(delay: Duration, repeat: Option[Int] = None)(event: Duration => Option[Event]): IO[Unit] = {
      addAction(Action.StartTimer(delay, repeat)(event))
    }

    private def addAction(action: Action): IO[Unit] = {
      State.modify(actions => action +: actions)
    }

  }

}
