package im.tox.tox4j.core.exceptions;

import im.tox.tox4j.ToxCoreTestBase;
import im.tox.tox4j.core.ToxCore;
import org.junit.Test;
import scala.runtime.BoxedUnit;

import static org.junit.Assert.assertEquals;

public class ToxGetPortExceptionTest extends ToxCoreTestBase {

  @Test
  public void testGetTcpPort_NotBound() throws Exception {
    try (ToxCore<BoxedUnit> tox = newTox()) {
      tox.getTcpPort();
      fail();
    } catch (ToxGetPortException e) {
      assertEquals(ToxGetPortException.NOT_BOUND$.MODULE$, e.code());
    }
  }

}
