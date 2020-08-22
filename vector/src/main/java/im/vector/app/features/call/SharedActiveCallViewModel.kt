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
import org.matrix.android.sdk.api.session.call.MxCall
import javax.inject.Inject

class SharedActiveCallViewModel @Inject constructor(
        private val webRtcPeerConnectionManager: WebRtcPeerConnectionManager
) : ViewModel() {

    val activeCall: MutableLiveData<MxCall?> = MutableLiveData()

    val callStateListener = object : MxCall.StateListener {

        override fun onStateUpdate(call: MxCall) {
            if (activeCall.value?.callId == call.callId) {
                activeCall.postValue(call)
            }
        }
    }

    private val listener = object : WebRtcPeerConnectionManager.CurrentCallListener {
        override fun onCurrentCallChange(call: MxCall?) {
            activeCall.value?.removeListener(callStateListener)
            activeCall.postValue(call)
            call?.addListener(callStateListener)
        }
    }

    init {
        activeCall.postValue(webRtcPeerConnectionManager.currentCall?.mxCall)
        webRtcPeerConnectionManager.addCurrentCallListener(listener)
    }

    override fun onCleared() {
        activeCall.value?.removeListener(callStateListener)
        webRtcPeerConnectionManager.removeCurrentCallListener(listener)
        super.onCleared()
    }
}
