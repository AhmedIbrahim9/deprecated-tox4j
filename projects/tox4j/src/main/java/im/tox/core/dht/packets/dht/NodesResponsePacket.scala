package im.tox.core.dht.packets.dht

import im.tox.core.dht.NodeInfo
import im.tox.core.error.CoreError
import im.tox.core.network.{PacketKind, PacketModuleCompanion}
import im.tox.core.typesafe.Security
import scodec.Attempt
import scodec.codecs._

import scala.collection.GenTraversableOnce
import scalaz.\/

/**
 * Send_nodes (response):
 *
 * [byte with value: 04]
 * [DHT public key of sender, length=32 bytes]
 * [random 24 byte nonce]
 * [Encrypted with the nonce and private key of the sender:
 *   [uint8_t number of nodes in this packet]
 *   [Nodes in packed node format, length = (39 bytes for ipv4, 41 bytes for ipv6) * (number of nodes (maximum of 4 nodes)) bytes]
 *   [ping_id, length=8 bytes]
 * ]
 *
 * The first byte of a send node response is a 4. This is then followed by the
 * DHT public key of the sender and a nonce. Inside the encrypted part of the
 * response there is a 1 byte number which contains the number of nodes stored
 * in the packet, up to 4 nodes in packed node format and the 8 byte ping id
 * that was sent in the request.
 */
final case class NodesResponsePacket private (
  nodes: List[NodeInfo]
) extends AnyVal

object NodesResponsePacket
    extends PacketModuleCompanion[NodesResponsePacket, PacketKind.NodesResponse.type, Security.Sensitive](PacketKind.NodesResponse) {

  val MaxNodes = 4

  override val codec =
    /**
     * [uint8_t number of nodes in this packet] (maximum of 4 nodes)
     * [Nodes in packed node format, length = (39 bytes for ipv4, 41 bytes for ipv6) * (number of nodes (maximum of 4 nodes)) bytes]
     */
    listOfN(uint8, NodeInfo.codec).exmap[NodesResponsePacket](
      { case nodes => CoreError.toAttempt(NodesResponsePacket(nodes)) },
      { case NodesResponsePacket(nodes) => Attempt.successful(nodes) }
    )

  def apply(nodes: GenTraversableOnce[NodeInfo]): CoreError \/ NodesResponsePacket = {
    for {
      _ <- CoreError.require(nodes.size <= MaxNodes, s"Too many nodes in $this: ${nodes.size} > $MaxNodes")
    } yield {
      NodesResponsePacket(nodes.toList)
    }
  }

}
