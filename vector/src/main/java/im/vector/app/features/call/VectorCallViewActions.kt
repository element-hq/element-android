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

package im.vector.app.features.call

import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.features.call.audio.CallAudioManager
import im.vector.app.features.call.transfer.CallTransferResult

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
}
