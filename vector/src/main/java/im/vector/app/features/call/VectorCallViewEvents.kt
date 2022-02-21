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

import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.features.call.audio.CallAudioManager
import org.matrix.android.sdk.api.session.call.TurnServerResponse

sealed class VectorCallViewEvents : VectorViewEvents {

    data class ConnectionTimeout(val turn: TurnServerResponse?) : VectorCallViewEvents()
    data class ShowSoundDeviceChooser(
            val available: Set<CallAudioManager.Device>,
            val current: CallAudioManager.Device
    ) : VectorCallViewEvents()
    object ShowDialPad : VectorCallViewEvents()
    object ShowCallTransferScreen : VectorCallViewEvents()
    object FailToTransfer : VectorCallViewEvents()
//    data class CallAnswered(val content: CallAnswerContent) : VectorCallViewEvents()
//    data class CallHangup(val content: CallHangupContent) : VectorCallViewEvents()
//    object CallAccepted : VectorCallViewEvents()
}
