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

package im.vector.riotx.features.call

import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.call.CallsListener
import im.vector.matrix.android.api.session.call.MxCall
import im.vector.matrix.android.api.session.room.model.call.CallAnswerContent
import im.vector.matrix.android.api.session.room.model.call.CallHangupContent
import im.vector.matrix.android.api.session.room.model.call.CallInviteContent
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.platform.VectorViewEvents
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.platform.VectorViewModelAction

data class VectorCallViewState(
        val callId: String? = null,
        val roomId: String = ""
) : MvRxState

sealed class VectorCallViewActions : VectorViewModelAction {

    object EndCall : VectorCallViewActions()
}

sealed class VectorCallViewEvents : VectorViewEvents {

    data class CallAnswered(val content: CallAnswerContent) : VectorCallViewEvents()
    data class CallHangup(val content: CallHangupContent) : VectorCallViewEvents()
}

class VectorCallViewModel @AssistedInject constructor(
        @Assisted initialState: VectorCallViewState,
        val session: Session,
        val webRtcPeerConnectionManager: WebRtcPeerConnectionManager
) : VectorViewModel<VectorCallViewState, VectorCallViewActions, VectorCallViewEvents>(initialState) {

    private val callServiceListener: CallsListener = object : CallsListener {
        override fun onCallAnswerReceived(callAnswerContent: CallAnswerContent) {
            withState { state ->
                if (callAnswerContent.callId == state.callId) {
                    _viewEvents.post(VectorCallViewEvents.CallAnswered(callAnswerContent))
                }
            }
        }

        override fun onCallInviteReceived(mxCall: MxCall, callInviteContent: CallInviteContent) {
        }

        override fun onCallHangupReceived(callHangupContent: CallHangupContent) {
            withState { state ->
                if (callHangupContent.callId == state.callId) {
                    _viewEvents.post(VectorCallViewEvents.CallHangup(callHangupContent))
                }
            }
        }
    }

    init {
        session.callService().addCallListener(callServiceListener)
    }

    override fun onCleared() {
        session.callService().removeCallListener(callServiceListener)
        super.onCleared()
    }

    override fun handle(action: VectorCallViewActions) = withState {
        when (action) {
            VectorCallViewActions.EndCall -> webRtcPeerConnectionManager.endCall()
        }.exhaustive
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: VectorCallViewState): VectorCallViewModel
    }

    companion object : MvRxViewModelFactory<VectorCallViewModel, VectorCallViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: VectorCallViewState): VectorCallViewModel? {
            val callActivity: VectorCallActivity = viewModelContext.activity()
            return callActivity.viewModelFactory.create(state)
        }

        override fun initialState(viewModelContext: ViewModelContext): VectorCallViewState? {
            //val args: CallArgs = viewModelContext.args()
            return VectorCallViewState()
        }
    }
}
