package im.tox.core.dht.handlers

import im.tox.core.dht.packets.dht.{NodesRequestPacket, PingPacket, PingResponsePacket}
import im.tox.core.dht.{Dht, NodeInfo}
import im.tox.core.error.DecoderError
import im.tox.core.io.IO

import scalaz.{\/, \/-}

object PingResponseHandler extends DhtPayloadHandler(PingResponsePacket) {

  override def apply(dht: Dht, sender: NodeInfo, packet: PingPacket): DecoderError \/ IO[Dht] = {
    \/- {
      for {
        // TODO(iphydf): This is temporary, just for load testing. It sends
        // node requests to every node that responds to a ping.
        () <- IO.sendTo(
          sender,
          makeResponse(
            dht.keyPair,
            sender.publicKey,
            NodesRequestPacket,
            NodesRequestPacket(dht.keyPair.publicKey, 0)
          )
        )
      } yield {
        dht.addNode(sender)
      }
    }
  }

}
