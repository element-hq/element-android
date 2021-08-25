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

import android.content.Context
import androidx.lifecycle.LifecycleObserver
import im.vector.app.features.call.audio.CallAudioManager
import im.vector.app.features.call.lookup.CallProtocolsChecker
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.call.CallListener
import org.matrix.android.sdk.api.session.call.CallState
import org.matrix.android.sdk.api.session.call.MxCall
import org.matrix.android.sdk.api.session.room.model.call.CallAnswerContent
import org.matrix.android.sdk.api.session.room.model.call.CallAssertedIdentityContent
import org.matrix.android.sdk.api.session.room.model.call.CallCandidatesContent
import org.matrix.android.sdk.api.session.room.model.call.CallHangupContent
import org.matrix.android.sdk.api.session.room.model.call.CallInviteContent
import org.matrix.android.sdk.api.session.room.model.call.CallNegotiateContent
import org.matrix.android.sdk.api.session.room.model.call.CallRejectContent
import org.matrix.android.sdk.api.session.room.model.call.CallSelectAnswerContent
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manage peerConnectionFactory & Peer connections outside of activity lifecycle to resist configuration changes
 * Use app context
 */
private val loggerTag = LoggerTag("WebRtcCallManager", LoggerTag.VOIP)

@Singleton
@Suppress("UNUSED_PARAMETER")
class WebRtcCallManager @Inject constructor(
        context: Context
) : CallListener, LifecycleObserver {

    interface CurrentCallListener {
        fun onCurrentCallChange(call: WebRtcCall?) {}
        fun onAudioDevicesChange() {}
    }

    val supportedPSTNProtocol: String? = null

    val supportsPSTNProtocol = false

    val supportsVirtualRooms = false

    fun addProtocolsCheckerListener(listener: CallProtocolsChecker.Listener) = Unit

    fun removeProtocolsCheckerListener(listener: CallProtocolsChecker.Listener) = Unit

    fun addCurrentCallListener(listener: CurrentCallListener) = Unit

    fun removeCurrentCallListener(listener: CurrentCallListener) = Unit

    val audioManager = CallAudioManager(context, null)

    fun getCallById(callId: String): WebRtcCall? = null

    fun getCallsByRoomId(roomId: String): List<WebRtcCall> = emptyList()

    fun getTransfereeForCallId(callId: String): WebRtcCall? = null

    fun getCurrentCall(): WebRtcCall? = null

    fun getCalls(): List<WebRtcCall> = emptyList()

    fun checkForProtocolsSupportIfNeeded() = Unit

    /**
     * @return a set of all advertised call during the lifetime of the app.
     */
    fun getAdvertisedCalls(): HashSet<String> = hashSetOf()

    fun headSetButtonTapped() {
        Timber.tag(loggerTag.value).v("headSetButtonTapped")
        val call = getCurrentCall() ?: return
        if (call.mxCall.state is CallState.LocalRinging) {
            call.acceptIncomingCall()
        }
        if (call.mxCall.state is CallState.Connected) {
            // end call?
            call.endCall()
        }
    }

    fun startOutgoingCall(nativeRoomId: String, otherUserId: String, isVideoCall: Boolean, transferee: WebRtcCall? = null) = Unit

    override fun onCallIceCandidateReceived(mxCall: MxCall, iceCandidatesContent: CallCandidatesContent) = Unit

    fun endCallForRoom(roomId: String) = Unit

    override fun onCallInviteReceived(mxCall: MxCall, callInviteContent: CallInviteContent) = Unit

    override fun onCallAnswerReceived(callAnswerContent: CallAnswerContent) = Unit

    override fun onCallHangupReceived(callHangupContent: CallHangupContent) = Unit

    override fun onCallRejectReceived(callRejectContent: CallRejectContent) = Unit

    override fun onCallSelectAnswerReceived(callSelectAnswerContent: CallSelectAnswerContent) = Unit

    override fun onCallNegotiateReceived(callNegotiateContent: CallNegotiateContent) = Unit

    override fun onCallManagedByOtherSession(callId: String) = Unit

    override fun onCallAssertedIdentityReceived(callAssertedIdentityContent: CallAssertedIdentityContent) = Unit
}
