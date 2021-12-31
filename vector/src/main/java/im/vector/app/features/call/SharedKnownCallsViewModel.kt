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
