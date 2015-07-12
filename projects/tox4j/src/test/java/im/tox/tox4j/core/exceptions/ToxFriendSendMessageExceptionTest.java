package im.tox.tox4j.core.exceptions;

import im.tox.tox4j.ToxCoreTestBase;
import im.tox.tox4j.core.ToxCore;
import im.tox.tox4j.core.enums.ToxMessageType;
import org.junit.Test;
import scala.runtime.BoxedUnit;

import static org.junit.Assert.assertEquals;

public class ToxFriendSendMessageExceptionTest extends ToxCoreTestBase {

  @Test
  public void testSendMessageNotFound() throws Exception {
    try (ToxCore<BoxedUnit> tox = newTox()) {
      try {
        tox.sendMessage(0, ToxMessageType.NORMAL, 0, "hello".getBytes());
        fail();
      } catch (ToxFriendSendMessageException e) {
        assertEquals(ToxFriendSendMessageException.FRIEND_NOT_FOUND$.MODULE$, e.code());
      }
    }
  }

  @Test
  public void testSendMessageNotConnected() throws Exception {
    try (ToxCore<BoxedUnit> tox = newTox()) {
      int friendNumber = addFriends(tox, 1);
      try {
        tox.sendMessage(friendNumber, ToxMessageType.ACTION, 0, "hello".getBytes());
        fail();
      } catch (ToxFriendSendMessageException e) {
        assertEquals(ToxFriendSendMessageException.FRIEND_NOT_CONNECTED$.MODULE$, e.code());
      }
    }
  }

  @Test
  public void testSendMessageNull() throws Exception {
    try (ToxCore<BoxedUnit> tox = newTox()) {
      int friendNumber = addFriends(tox, 1);
      try {
        tox.sendMessage(friendNumber, ToxMessageType.NORMAL, 0, null);
        fail();
      } catch (ToxFriendSendMessageException e) {
        assertEquals(ToxFriendSendMessageException.NULL$.MODULE$, e.code());
      }
    }
  }

  @Test
  public void testSendMessageEmpty() throws Exception {
    try (ToxCore<BoxedUnit> tox = newTox()) {
      int friendNumber = addFriends(tox, 1);
      try {
        tox.sendMessage(friendNumber, ToxMessageType.ACTION, 0, new byte[0]);
        fail();
      } catch (ToxFriendSendMessageException e) {
        assertEquals(ToxFriendSendMessageException.EMPTY$.MODULE$, e.code());
      }
    }
  }

}
