package im.tox.tox4j.core.callbacks

import im.tox.tox4j.core.enums.ToxFileControl
import im.tox.tox4j.core.{ToxCore, ToxFileId}

final class FilePauseResumeWithControlTest extends FilePauseResumeTestBase {

  protected override def newChatClient(name: String, expectedFriendName: String) = new Alice(name, expectedFriendName)

  final class Alice(name: String, expectedFriendName: String) extends super.Alice(name, expectedFriendName) {

    protected override def addFriendMessageTask(
      friendNumber: Int,
      bobSentFileNumber: Int,
      fileId: ToxFileId,
      tox: ToxCore[ChatState]
    )(state: State): State = {
      debug("send resume control")
      if (isBob) {
        tox.fileControl(friendNumber, bobSentFileNumber, ToxFileControl.RESUME)
        state.copy(bobShouldPause = 1)
      } else if (isAlice) {
        tox.fileControl(friendNumber, state.aliceSentFileNumber, ToxFileControl.RESUME)
        state.copy(aliceShouldPause = 1)
      } else {
        fail("Unexpected client (not Alice or Bob)")
        state
      }
    }

    protected override def addFileRecvTask(
      friendNumber: Int,
      bobSentFileNumber: Int,
      bobOffset: Long,
      tox: ToxCore[ChatState]
    )(state: State): State = {
      debug(s"sending control RESUME for $bobSentFileNumber")
      tox.fileControl(friendNumber, bobSentFileNumber, ToxFileControl.RESUME)
      state
    }

  }

}
