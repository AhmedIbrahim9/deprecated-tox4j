package im.tox.client.callbacks

import java.util

import im.tox.client.commands.{AudioCommandHandler, ShowCommandHandler, VideoCommandHandler}
import im.tox.client.{Say, ToxClientState}
import im.tox.tox4j.OptimisedIdOps._
import im.tox.tox4j.ToxEventListener
import im.tox.tox4j.av.ToxAv
import im.tox.tox4j.av.data._
import im.tox.tox4j.av.enums.{ToxavCallControl, ToxavFriendCallState}
import im.tox.tox4j.core.ToxCore
import im.tox.tox4j.core.data._
import im.tox.tox4j.core.enums.ToxMessageType

import scalaz.Lens

/**
 * Handles audio/video calls.
 */
final class AudioVideoEventListener(id: Int)
    extends IdLogging(id) with ToxEventListener[ToxClientState] with Say {

  private val audioBitRate = BitRate.fromInt(8).get
  private val audioLength = AudioLength.Length60
  private val audioSamplingRate = SamplingRate.Rate8k
  private val audioFrameSize = (audioLength.value.toMillis * audioSamplingRate.value / 1000).toInt
  private val audioFramesPerIteration = 1

  private val videoBitRate = BitRate.fromInt(1).get

  override def friendMessage(
    friendNumber: ToxFriendNumber,
    messageType: ToxMessageType,
    timeDelta: Int,
    message: ToxFriendMessage
  )(state: ToxClientState): ToxClientState = {
    val AudioCommand = "audio\\s+(.+)".r
    val VideoCommand = "video\\s+(.+)".r
    val ShowCommand = "show\\s+(.+)".r

    val command = message.toString.toLowerCase
    command match {
      case "call me" =>
        state.addTask { (tox, av, state) =>
          logInfo(s"Ringing ${state.friends(friendNumber).name}")
          av.call(friendNumber, audioBitRate, videoBitRate)
          state
        }

      case AudioCommand(request) => AudioCommandHandler(audioSamplingRate)(friendNumber, state, request)
      case VideoCommand(request) => VideoCommandHandler(friendNumber, state, request)
      case ShowCommand(request)  => ShowCommandHandler(friendNumber, state, request)

      case _ =>
        say(friendNumber, s"unrecognised command: '$command'; try 'call me'")(state)
    }
  }

  override def call(
    friendNumber: ToxFriendNumber,
    audioEnabled: Boolean,
    videoEnabled: Boolean
  )(state: ToxClientState): ToxClientState = {
    state.addTask { (tox, av, state) =>
      logInfo(s"Answering call from $friendNumber")
      av.answer(friendNumber, audioBitRate, videoBitRate)
      av.callControl(friendNumber, ToxavCallControl.MUTE_AUDIO)
      av.callControl(friendNumber, ToxavCallControl.HIDE_VIDEO)

      val callState = new util.ArrayList[ToxavFriendCallState]
      if (audioEnabled) callState.add(ToxavFriendCallState.ACCEPTING_A)
      if (videoEnabled) callState.add(ToxavFriendCallState.ACCEPTING_V)
      av.invokeCallState(friendNumber, callState)

      state
    }
  }

  private def sendNextAudioFrame(
    friendNumber: ToxFriendNumber
  )(
    tox: ToxCore,
    av: ToxAv,
    state: ToxClientState
  ): ToxClientState = {
    val audioTime = ToxClientState.friendAudioTime(friendNumber)

    audioTime.get(state) match {
      case None =>
        state // finished
      case Some(t) =>
        // Get next audio frames and send them.
        val audio = ToxClientState.friendAudio(friendNumber).get(state)
        val nextT = t + audioFrameSize * audioFramesPerIteration

        for (t <- t until nextT by audioFrameSize) {
          val pcm = audio.nextFrame16(t, audioFrameSize)
          av.audioSendFrame(
            friendNumber,
            pcm,
            SampleCount(audioLength, audioSamplingRate),
            AudioChannels.Mono,
            audioSamplingRate
          )
        }
        audioTime.set(state, Some(nextT)).addTask(sendNextAudioFrame(friendNumber))
    }
  }

  private def sendNextVideoFrame(
    friendNumber: ToxFriendNumber
  )(
    tox: ToxCore,
    av: ToxAv,
    state: ToxClientState
  ): ToxClientState = {
    val videoFrame = ToxClientState.friendVideoFrame(friendNumber)

    videoFrame.get(state) match {
      case None =>
        state // finished
      case Some(t) =>
        // Get next frame and send it.
        val video = ToxClientState.friendVideo(friendNumber).get(state)
        val (y, u, v) = video.yuv(t)
        av.videoSendFrame(friendNumber, video.width.value, video.height.value, y, u, v)
        videoFrame.set(state, Some(t + 1)).addTask(sendNextVideoFrame(friendNumber))
    }
  }

  override def callState(
    friendNumber: ToxFriendNumber,
    callState: util.Collection[ToxavFriendCallState]
  )(state: ToxClientState): ToxClientState = {
    (state
      |> stopAvReceiving(friendNumber, callState)
      |> startStopAudioSending(friendNumber, callState)
      |> startStopVideoSending(friendNumber, callState))
  }

  private def stopAvReceiving(
    friendNumber: ToxFriendNumber,
    callState: util.Collection[ToxavFriendCallState]
  )(state: ToxClientState): ToxClientState = {
    state.addTask { (tox, av, state) =>
      if (callState.contains(ToxavFriendCallState.SENDING_A)) {
        av.callControl(friendNumber, ToxavCallControl.MUTE_AUDIO)
      }
      if (callState.contains(ToxavFriendCallState.SENDING_V)) {
        av.callControl(friendNumber, ToxavCallControl.HIDE_VIDEO)
      }
      state
    }
  }

  private def startStopAudioSending(
    friendNumber: ToxFriendNumber,
    callState: util.Collection[ToxavFriendCallState]
  )(state: ToxClientState): ToxClientState = {
    val audioTime = ToxClientState.friendAudioTime(friendNumber)

    if (callState.contains(ToxavFriendCallState.ACCEPTING_A)) {
      logInfo(s"Sending audio to friend $friendNumber")
      audioTime.set(state, Some(0))
        .addTask(sendNextAudioFrame(friendNumber))
    } else {
      audioTime.mod({
        case None => None
        case Some(_) =>
          logInfo(s"Stopped sending audio to friend $friendNumber")
          None
      }, state)
    }
  }

  private def startStopVideoSending(
    friendNumber: ToxFriendNumber,
    callState: util.Collection[ToxavFriendCallState]
  )(state: ToxClientState): ToxClientState = {
    val videoFrame = ToxClientState.friendVideoFrame(friendNumber)

    if (callState.contains(ToxavFriendCallState.ACCEPTING_V)) {
      logInfo(s"Sending video to friend $friendNumber")
      videoFrame.set(state, Some(0))
        .addTask(sendNextVideoFrame(friendNumber))
    } else {
      videoFrame.mod({
        case None => None
        case Some(_) =>
          logInfo(s"Stopped sending video to friend $friendNumber")
          None
      }, state)
    }

  }

  override def bitRateStatus(
    friendNumber: ToxFriendNumber,
    audioBitRate0: BitRate,
    videoBitRate0: BitRate
  )(state: ToxClientState): ToxClientState = {
    val audioBitRate = audioBitRate0.copy(audioBitRate0.value max 8)
    val videoBitRate = videoBitRate0.copy(videoBitRate0.value max 1)

    state.addTask { (tox, av, state) =>
      av.setBitRate(friendNumber, audioBitRate, videoBitRate)

      val audioTime = ToxClientState.friendAudioTime(friendNumber)
      val videoFrame = ToxClientState.friendVideoFrame(friendNumber)

      (state
        |> setBitRate("audio", audioTime, audioBitRate)
        |> setBitRate("video", videoFrame, videoBitRate))
    }
  }

  private def setBitRate(
    target: String,
    lens: Lens[ToxClientState, Option[Int]],
    bitRate: BitRate
  )(state: ToxClientState): ToxClientState = {
    if (bitRate == BitRate.Disabled) {
      logInfo(s"Stopping $target sending")
      lens.set(state, None)
    } else {
      lens.set(state, Some(0))
    }
  }

  override def audioReceiveFrame(
    friendNumber: ToxFriendNumber,
    pcm: Array[Short],
    channels: AudioChannels,
    samplingRate: SamplingRate
  )(state: ToxClientState): ToxClientState = {
    state
  }

  override def videoReceiveFrame(
    friendNumber: ToxFriendNumber,
    width: Int, height: Int,
    y: Array[Byte], u: Array[Byte], v: Array[Byte],
    yStride: Int, uStride: Int, vStride: Int
  )(state: ToxClientState): ToxClientState = {
    state
  }

}
