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

class SharedCurrentCallViewModel @Inject constructor(
        private val callManager: WebRtcCallManager
) : ViewModel() {

    val currentCall: MutableLiveData<WebRtcCall?> = MutableLiveData()

    val callStateListener = object : WebRtcCall.Listener {

        override fun onStateUpdate(call: MxCall) {
            //post it-self
            currentCall.postValue(currentCall.value)
        }

        override fun onHoldUnhold() {
            super.onHoldUnhold()
            //post it-self
            currentCall.postValue(currentCall.value)
        }

    }

    private val listener = object : WebRtcCallManager.CurrentCallListener {
        override fun onCurrentCallChange(call: WebRtcCall?) {
            currentCall.value?.mxCall?.removeListener(callStateListener)
            currentCall.postValue(call)
            call?.addListener(callStateListener)
        }
    }

    init {
        currentCall.postValue(callManager.currentCall)
        callManager.addCurrentCallListener(listener)
    }

    override fun onCleared() {
        currentCall.value?.removeListener(callStateListener)
        callManager.removeCurrentCallListener(listener)
        super.onCleared()
    }
}
