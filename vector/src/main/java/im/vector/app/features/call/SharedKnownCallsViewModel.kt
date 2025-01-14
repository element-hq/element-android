/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import im.vector.app.features.call.webrtc.WebRtcCall
import im.vector.app.features.call.webrtc.WebRtcCallManager
import org.matrix.android.sdk.api.session.call.MxCall
import javax.inject.Inject

class SharedKnownCallsViewModel @Inject constructor(
        private val callManager: WebRtcCallManager
) : ViewModel() {

    val liveKnownCalls: MutableLiveData<List<WebRtcCall>> = MutableLiveData()

    val callListener = object : WebRtcCall.Listener {

        override fun onStateUpdate(call: MxCall) {
            liveKnownCalls.postValue(callManager.getCalls())
        }

        override fun onHoldUnhold() {
            super.onHoldUnhold()
            liveKnownCalls.postValue(callManager.getCalls())
        }
    }

    private val callManagerListener = object : WebRtcCallManager.Listener {
        override fun onCurrentCallChange(call: WebRtcCall?) {
            val knownCalls = callManager.getCalls()
            liveKnownCalls.postValue(knownCalls)
            knownCalls.forEach {
                it.removeListener(callListener)
                it.addListener(callListener)
            }
        }

        override fun onCallEnded(callId: String) {
            val knownCalls = callManager.getCalls()
            liveKnownCalls.postValue(knownCalls)
        }
    }

    init {
        val knownCalls = callManager.getCalls()
        liveKnownCalls.postValue(knownCalls)
        callManager.addListener(callManagerListener)
        knownCalls.forEach {
            it.addListener(callListener)
        }
    }

    override fun onCleared() {
        callManager.getCalls().forEach {
            it.removeListener(callListener)
        }
        callManager.removeListener(callManagerListener)
        super.onCleared()
    }
}
