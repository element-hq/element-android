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

package im.vector.app.features.call.transfer

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.call.webrtc.WebRtcCall
import im.vector.app.features.call.webrtc.WebRtcCallManager
import org.matrix.android.sdk.api.session.call.CallState
import org.matrix.android.sdk.api.session.call.MxCall

class CallTransferViewModel @AssistedInject constructor(@Assisted initialState: CallTransferViewState,
                                                        private val callManager: WebRtcCallManager) :
    VectorViewModel<CallTransferViewState, EmptyAction, CallTransferViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<CallTransferViewModel, CallTransferViewState> {
        override fun create(initialState: CallTransferViewState): CallTransferViewModel
    }

    companion object : MavericksViewModelFactory<CallTransferViewModel, CallTransferViewState> by hiltMavericksViewModelFactory()

    private val call = callManager.getCallById(initialState.callId)
    private val callListener = object : WebRtcCall.Listener {
        override fun onStateUpdate(call: MxCall) {
            if (call.state is CallState.Ended) {
                _viewEvents.post(CallTransferViewEvents.Complete)
            }
        }
    }

    init {
        if (call == null) {
            _viewEvents.post(CallTransferViewEvents.Complete)
        } else {
            call.addListener(callListener)
        }
    }

    override fun onCleared() {
        super.onCleared()
        call?.removeListener(callListener)
    }

    override fun handle(action: EmptyAction) { }
}
