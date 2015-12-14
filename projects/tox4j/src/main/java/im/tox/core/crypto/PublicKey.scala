package im.tox.core.crypto

import im.tox.core.crypto.Nonce._
import im.tox.core.random.RandomCore
import im.tox.core.typesafe.{KeyCompanion, Security}
import im.tox.tox4j.crypto.ToxCryptoConstants

import scala.reflect.ClassTag

final case class PublicKey private[crypto] (value: IndexedSeq[Byte]) extends AnyVal {
  def readable: String = PublicKey.toHexString(this)
  override def toString: String = {
    s"${getClass.getSimpleName}($readable)"
  }
}

case object PublicKey extends KeyCompanion[PublicKey, Security.NonSensitive](
  ToxCryptoConstants.PublicKeyLength,
  _.value.toArray
) {

  protected def unsafeFromValue(value: Array[Byte]): PublicKey = new PublicKey(value)

  def random(): PublicKey = {
    PublicKey(RandomCore.randomBytes(Size))
  }

}
