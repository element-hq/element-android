/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.call.audio.CallAudioManager
import org.matrix.android.sdk.api.session.call.CallState
import org.matrix.android.sdk.api.util.MatrixItem

data class VectorCallViewState(
        val callId: String,
        val roomId: String,
        val isVideoCall: Boolean,
        val isRemoteOnHold: Boolean = false,
        val isLocalOnHold: Boolean = false,
        val isAudioMuted: Boolean = false,
        val isVideoEnabled: Boolean = true,
        val isVideoCaptureInError: Boolean = false,
        val isHD: Boolean = false,
        val isFrontCamera: Boolean = true,
        val canSwitchCamera: Boolean = true,
        val device: CallAudioManager.Device = CallAudioManager.Device.Phone,
        val availableDevices: Set<CallAudioManager.Device> = emptySet(),
        val callState: Async<CallState> = Uninitialized,
        val otherKnownCallInfo: CallInfo? = null,
        val callInfo: CallInfo? = null,
        val formattedDuration: String = "",
        val canOpponentBeTransferred: Boolean = false,
        val transferee: TransfereeState = TransfereeState.NoTransferee,
        val isSharingScreen: Boolean = false
) : MavericksState {

    sealed class TransfereeState {
        object NoTransferee : TransfereeState()
        data class KnownTransferee(val name: String) : TransfereeState()
        object UnknownTransferee : TransfereeState()
    }

    data class CallInfo(
            val callId: String,
            val opponentUserItem: MatrixItem? = null
    )

    constructor(callArgs: CallArgs) : this(
            callId = callArgs.callId,
            roomId = callArgs.signalingRoomId,
            isVideoCall = callArgs.isVideoCall
    )
}
