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
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.call.dialpad.DialPadLookup
import im.vector.app.features.call.webrtc.WebRtcCall
import im.vector.app.features.call.webrtc.WebRtcCallManager
import im.vector.app.features.createdirect.DirectRoomHelper
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.call.CallState
import org.matrix.android.sdk.api.session.call.MxCall

class CallTransferViewModel @AssistedInject constructor(@Assisted initialState: CallTransferViewState,
                                                        private val dialPadLookup: DialPadLookup,
                                                        private val directRoomHelper: DirectRoomHelper,
                                                        private val callManager: WebRtcCallManager)
    : VectorViewModel<CallTransferViewState, CallTransferAction, CallTransferViewEvents>(initialState) {

    @AssistedFactory
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

    private val call = callManager.getCallById(initialState.callId)
    private val callListener = object : WebRtcCall.Listener {
        override fun onStateUpdate(call: MxCall) {
            if (call.state == CallState.Terminated) {
                _viewEvents.post(CallTransferViewEvents.Dismiss)
            }
        }
    }

    init {
        if (call == null) {
            _viewEvents.post(CallTransferViewEvents.Dismiss)
        } else {
            call.addListener(callListener)
        }
    }

    override fun onCleared() {
        super.onCleared()
        call?.removeListener(callListener)
    }

    override fun handle(action: CallTransferAction) {
        when (action) {
            is CallTransferAction.ConnectWithUserId      -> connectWithUserId(action)
            is CallTransferAction.ConnectWithPhoneNumber -> connectWithPhoneNumber(action)
        }.exhaustive
    }

    private fun connectWithUserId(action: CallTransferAction.ConnectWithUserId) {
        viewModelScope.launch {
            try {
                if (action.consultFirst) {
                    val dmRoomId = directRoomHelper.ensureDMExists(action.selectedUserId)
                    callManager.startOutgoingCall(
                            nativeRoomId = dmRoomId,
                            otherUserId = action.selectedUserId,
                            isVideoCall = call?.mxCall?.isVideoCall.orFalse(),
                            transferee = call
                    )
                } else {
                    call?.transferToUser(action.selectedUserId, null)
                }
                _viewEvents.post(CallTransferViewEvents.Dismiss)
            } catch (failure: Throwable) {
                _viewEvents.post(CallTransferViewEvents.FailToTransfer)
            }
        }
    }

    private fun connectWithPhoneNumber(action: CallTransferAction.ConnectWithPhoneNumber) {
        viewModelScope.launch {
            try {
                _viewEvents.post(CallTransferViewEvents.Loading)
                val result = dialPadLookup.lookupPhoneNumber(action.phoneNumber)
                if (action.consultFirst) {
                    callManager.startOutgoingCall(
                            nativeRoomId = result.roomId,
                            otherUserId = result.userId,
                            isVideoCall = call?.mxCall?.isVideoCall.orFalse(),
                            transferee = call
                    )
                } else {
                    call?.transferToUser(result.userId, result.roomId)
                }
                _viewEvents.post(CallTransferViewEvents.Dismiss)
            } catch (failure: Throwable) {
                _viewEvents.post(CallTransferViewEvents.FailToTransfer)
            }
        }
    }
}
