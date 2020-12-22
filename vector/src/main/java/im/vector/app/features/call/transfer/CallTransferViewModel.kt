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

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.call.webrtc.WebRtcCall
import im.vector.app.features.call.webrtc.WebRtcCallManager
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.call.CallState
import org.matrix.android.sdk.api.session.call.MxCall
import timber.log.Timber

class CallTransferViewModel @AssistedInject constructor(@Assisted initialState: CallTransferViewState,
                                                        private val callManager: WebRtcCallManager)
    : VectorViewModel<CallTransferViewState, CallTransferAction, CallTransferViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: CallTransferViewState): CallTransferViewModel
    }

    companion object : MvRxViewModelFactory<CallTransferViewModel, CallTransferViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: CallTransferViewState): CallTransferViewModel? {
            val activity: CallTransferActivity = (viewModelContext as ActivityViewModelContext).activity()
            return activity.callTransferViewModelFactory.create(state)
        }
    }

    private var call: WebRtcCall? = null
    private val callListener = object : WebRtcCall.Listener {
        override fun onStateUpdate(call: MxCall) {
            if (call.state == CallState.Terminated) {
                _viewEvents.post(CallTransferViewEvents.Dismiss)
            }
        }
    }

    init {
        val webRtcCall = callManager.getCallById(initialState.callId)
        if (webRtcCall == null) {
            _viewEvents.post(CallTransferViewEvents.Dismiss)
        } else {
            call = webRtcCall
            webRtcCall.addListener(callListener)
        }
    }

    override fun onCleared() {
        super.onCleared()
        call?.removeListener(callListener)
    }

    override fun handle(action: CallTransferAction) {
        when (action) {
            is CallTransferAction.Connect -> transferCall(action)
        }
    }

    private fun transferCall(action: CallTransferAction.Connect) {
        viewModelScope.launch {
            try {
                call?.mxCall?.transfer(action.selectedUserId, null)
            } catch (failure: Throwable) {
                Timber.v("Fail to transfer call: $failure")
            }
        }
    }
}
