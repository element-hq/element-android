/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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

class CallTransferViewModel @AssistedInject constructor(
        @Assisted initialState: CallTransferViewState,
        private val callManager: WebRtcCallManager
) :
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

    override fun handle(action: EmptyAction) {}
}
