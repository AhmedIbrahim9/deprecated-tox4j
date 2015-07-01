package im.tox.tox4j.core

import im.tox.tox4j.core.options.ToxOptions
import im.tox.tox4j.impl.jni.ToxCoreImpl
import org.scalatest.{ FlatSpec, Tag }

/**
 * These tests solely exist to exercise the C++ code paths that deal with closing and finalisation. If the C++ code has
 * errors in this area, these two tests can be used to single them out and debug them.
 *
 * Although [[System.gc()]] doesn't necessarily perform a GC, on the Oracle JVM it actually does reliably do so.
 * Thus, these tests don't formally test anything, but in reality they do.
 */
final class FinalizerTest extends FlatSpec {

  "Garbage collection" should "not crash the JVM when collecting a closed ToxCoreImpl" in {
    System.gc()
    new ToxCoreImpl[Unit](new ToxOptions).close()
    System.gc()
  }

  it should "not crash the JVM when collecting an unclosed ToxCoreImpl" in {
    System.gc()
    new ToxCoreImpl[Unit](new ToxOptions)
    System.gc()
  }
}

object FinalizerTest {
  val emptyTags: Seq[Tag] = Nil

  abstract class ToxFunction extends (ToxCore[Unit] => Unit) {
    override def apply(tox: ToxCore[Unit]): Unit
  }
}