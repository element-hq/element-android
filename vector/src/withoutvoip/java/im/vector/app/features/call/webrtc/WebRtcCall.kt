/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.call.webrtc

import im.vector.app.core.utils.TextUtils.formatDuration
import im.vector.app.features.call.CameraType
import im.vector.app.features.call.CaptureFormat
import org.matrix.android.sdk.api.session.call.MxCall
import org.matrix.android.sdk.api.session.room.model.call.CallAssertedIdentityContent
import org.matrix.android.sdk.api.session.room.model.call.EndCallReason
import org.threeten.bp.Duration

private const val STREAM_ID = "userMedia"

@Suppress("UNUSED_PARAMETER")
class WebRtcCall(
        val mxCall: MxCall,
        // This is where the call is placed from an ui perspective.
        // In case of virtual room, it can differs from the signalingRoomId.
        val nativeRoomId: String
) : MxCall.StateListener {

    interface Listener : MxCall.StateListener {
        fun onCaptureStateChanged() {}
        fun onCameraChanged() {}
        fun onHoldUnhold() {}
        fun assertedIdentityChanged() {}
        fun onTick(formattedDuration: String) {}
        override fun onStateUpdate(call: MxCall) {}
    }

    fun addListener(listener: Listener) = Unit

    fun removeListener(listener: Listener) = Unit

    val callId = mxCall.callId

    // room where call signaling is placed. In case of virtual room it can differs from the nativeRoomId.
    val signalingRoomId = mxCall.roomId

    // Mute status
    var micMuted = false
        private set
    var videoMuted = false
        private set
    var isRemoteOnHold = false
        private set
    var isLocalOnHold = false
        private set

    var remoteAssertedIdentity: CallAssertedIdentityContent.AssertedIdentity? = null
        private set

    var videoCapturerIsInError = false

    init {
        mxCall.addListener(this)
    }

    fun formattedDuration(): String {
        return formatDuration(Duration.ofMillis(0L))
    }

    /**
     * Without consultation
     */
    fun transferToUser(targetUserId: String, targetRoomId: String?) = Unit

    /**
     * With consultation
     */
    fun transferToCall(transferTargetCall: WebRtcCall) = Unit

    fun acceptIncomingCall() = Unit

    /**
     * Sends a DTMF digit to the other party
     * @param digit The digit (nb. string - '#' and '*' are dtmf too)
     */
    fun sendDtmfDigit(digit: String) = Unit

    fun setCaptureFormat(format: CaptureFormat) = Unit

    fun updateRemoteOnHold(onHold: Boolean) = Unit

    fun muteCall(muted: Boolean) = Unit

    fun enableVideo(enabled: Boolean) = Unit

    fun canSwitchCamera(): Boolean = false

    fun switchCamera() = Unit

    fun currentCameraType(): CameraType? = null

    fun currentCaptureFormat(): CaptureFormat = CaptureFormat.HD

    fun endCall(reason: EndCallReason = EndCallReason.USER_HANGUP) = Unit

    // MxCall.StateListener

    override fun onStateUpdate(call: MxCall) = Unit
}
