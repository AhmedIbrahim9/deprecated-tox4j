package im.tox.tox4j.testing

import im.tox.core.error.CoreError
import im.tox.tox4j.DhtNodeSelector._
import im.tox.tox4j.core.exceptions.{ToxBootstrapException, ToxFriendAddException, ToxNewException}
import im.tox.tox4j.core.{ToxCore, ToxCoreFactory}
import im.tox.tox4j.exceptions.ToxException
import org.jetbrains.annotations.NotNull
import org.scalatest.Assertions

import scala.language.implicitConversions
import scalaz.\/

trait ToxTestMixin extends Assertions {

  protected def intercept[E <: Enum[E]](code: E)(f: => Unit) = {
    try {
      f
      fail(s"Expected exception with code ${code.name}")
    } catch {
      case e: ToxException[_] =>
        assert(e.code eq code)
    }
  }

  protected def interceptWithTox[E <: Enum[E]](code: E)(f: ToxCore[Unit] => Unit) = {
    intercept(code) {
      ToxCoreFactory.withTox { tox =>
        addFriends(tox, 1)
        f(tox)
      }
    }
  }

  @throws[ToxNewException]
  @throws[ToxFriendAddException]
  protected def addFriends[ToxCoreState](@NotNull tox: ToxCore[ToxCoreState], count: Int): Int = {
    if (count < 1) {
      throw new IllegalArgumentException("Cannot add less than 1 friend: " + count)
    }
    (0 until count).map { (i: Int) =>
      ToxCoreFactory.withTox { friend =>
        tox.addFriendNorequest(friend.getPublicKey)
      }
    }.last
  }

  @throws[ToxBootstrapException]
  def bootstrap[ToxCoreState](useIPv6: Boolean, udpEnabled: Boolean, @NotNull tox: ToxCore[ToxCoreState]): ToxCore[ToxCoreState] = {
    if (!udpEnabled) {
      tox.addTcpRelay(node.ipv4, node.tcpPort, node.dhtId)
    }
    tox.bootstrap(
      if (useIPv6) node.ipv6 else node.ipv4,
      if (udpEnabled) node.udpPort else node.tcpPort,
      node.dhtId
    )
    tox
  }

  implicit final def toGetDisjunction[T](disjunction: CoreError \/ T): GetDisjunction[T] = {
    GetDisjunction.toGetDisjunction(disjunction)
  }

}
