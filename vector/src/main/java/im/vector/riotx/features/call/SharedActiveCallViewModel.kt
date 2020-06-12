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

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import im.vector.matrix.android.api.session.call.MxCall
import im.vector.riotx.core.platform.VectorSharedAction
import javax.inject.Inject

sealed class CallActions : VectorSharedAction {
    data class GoToCallActivity(val mxCall: MxCall) : CallActions()
    data class ToggleVisibility(val visible: Boolean) : CallActions()
}

class SharedActiveCallViewModel @Inject constructor(
        private val webRtcPeerConnectionManager: WebRtcPeerConnectionManager
) : ViewModel() {

    val activeCall: MutableLiveData<MxCall?> = MutableLiveData()

    private val listener = object : WebRtcPeerConnectionManager.CurrentCallListener {
        override fun onCurrentCallChange(call: MxCall?) {
            activeCall.postValue(call)
        }
    }

    init {
        activeCall.postValue(webRtcPeerConnectionManager.currentCall?.mxCall)
        webRtcPeerConnectionManager.addCurrentCallListener(listener)
    }

    override fun onCleared() {
        webRtcPeerConnectionManager.removeCurrentCallListener(listener)
        super.onCleared()
    }
}
