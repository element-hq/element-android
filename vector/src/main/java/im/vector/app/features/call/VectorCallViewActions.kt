/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call

import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.features.call.audio.CallAudioManager
import im.vector.app.features.call.transfer.CallTransferResult
import org.webrtc.VideoCapturer

sealed class VectorCallViewActions : VectorViewModelAction {
    object EndCall : VectorCallViewActions()
    object AcceptCall : VectorCallViewActions()
    object DeclineCall : VectorCallViewActions()
    object ToggleMute : VectorCallViewActions()
    object ToggleVideo : VectorCallViewActions()
    object ToggleHoldResume : VectorCallViewActions()
    data class ChangeAudioDevice(val device: CallAudioManager.Device) : VectorCallViewActions()
    object OpenDialPad : VectorCallViewActions()
    data class SendDtmfDigit(val digit: String) : VectorCallViewActions()
    data class SwitchCall(val callArgs: CallArgs) : VectorCallViewActions()

    object SwitchSoundDevice : VectorCallViewActions()
    object HeadSetButtonPressed : VectorCallViewActions()
    object ToggleCamera : VectorCallViewActions()
    object ToggleHDSD : VectorCallViewActions()
    object InitiateCallTransfer : VectorCallViewActions()
    object CallTransferSelectionCancelled : VectorCallViewActions()
    data class CallTransferSelectionResult(val callTransferResult: CallTransferResult) : VectorCallViewActions()
    object TransferCall : VectorCallViewActions()
    object ToggleScreenSharing : VectorCallViewActions()
    data class StartScreenSharing(val videoCapturer: VideoCapturer) : VectorCallViewActions()
}
