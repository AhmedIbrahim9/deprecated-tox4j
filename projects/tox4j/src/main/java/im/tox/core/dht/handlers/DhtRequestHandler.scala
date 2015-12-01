package im.tox.core.dht.handlers

import java.net.InetSocketAddress

import im.tox.core.crypto.PlainText
import im.tox.core.dht.packets.DhtRequestPacket
import im.tox.core.dht.{Dht, NodeInfo}
import im.tox.core.error.CoreError
import im.tox.core.io.IO
import im.tox.core.network.PacketKind
import im.tox.core.network.handlers.ToxPacketHandler
import im.tox.core.network.packets.ToxPacket

import scalaz.{\/, \/-}

final case class DhtRequestHandler[T](handler: ToxPacketHandler[T])
    extends ToxPacketHandler(DhtRequestPacket.Make(handler.module)) {

  private val adapter = DhtEncryptedHandler(new DhtPayloadHandler(handler.module) {
    override def apply(dht: Dht, sender: NodeInfo, packet: T): CoreError \/ IO[Dht] = {
      handler(dht, sender.address, packet)
    }
  })

  override def apply(dht: Dht, origin: InetSocketAddress, requestPacket: DhtRequestPacket[T]): CoreError \/ IO[Dht] = {
    /**
     * A DHT node that receives a DHT request packet will check whether the node
     * with the receivers public key is their DHT public key and
     */
    if (requestPacket.receiverPublicKey == dht.keyPair.publicKey) {
      /**
       * If it is, they will decrypt and handle the packet.
       */
      adapter(dht, origin, requestPacket.payload)
    } else {
      /**
       * If it is not they will check whether they
       * know that DHT public key (if it's in their list of close nodes).
       */
      dht.getNode(requestPacket.receiverPublicKey) match {
        case None =>
          /**
           * If it isn't, they will drop the packet.
           */
          \/-(IO(dht))
        case Some(receiver) =>
          /**
           * If it is they will resend the exact packet to that
           * DHT node.
           */
          for {
            forwardedData <- module.toBytes(requestPacket)
          } yield {
            for {
              () <- IO.sendTo(
                receiver,
                ToxPacket(
                  PacketKind.DhtRequest,
                  PlainText(forwardedData)
                )
              )
            } yield {
              dht
            }
          }
      }
    }
  }

}
