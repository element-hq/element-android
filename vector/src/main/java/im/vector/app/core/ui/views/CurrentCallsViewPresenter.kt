/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.core.ui.views

import androidx.core.view.isVisible
import im.vector.app.features.call.webrtc.WebRtcCall

class CurrentCallsViewPresenter {

    private var currentCallsView: CurrentCallsView? = null
    private var currentCall: WebRtcCall? = null
    private var calls: List<WebRtcCall> = emptyList()

    private val tickListener = object : WebRtcCall.Listener {
        override fun onTick(formattedDuration: String) {
            currentCallsView?.render(calls, formattedDuration)
        }
    }

    fun updateCall(currentCall: WebRtcCall?, calls: List<WebRtcCall>) {
        this.currentCall?.removeListener(tickListener)
        this.currentCall = currentCall
        this.currentCall?.addListener(tickListener)
        this.calls = calls
        val hasActiveCall = calls.isNotEmpty()
        currentCallsView?.isVisible = hasActiveCall
        currentCallsView?.render(calls, currentCall?.formattedDuration() ?: "")
    }

    fun bind(activeCallView: CurrentCallsView, interactionListener: CurrentCallsView.Callback) {
        this.currentCallsView = activeCallView
        this.currentCallsView?.callback = interactionListener
        this.currentCall?.addListener(tickListener)
    }

    fun unBind() {
        this.currentCallsView?.callback = null
        this.currentCall?.removeListener(tickListener)
        currentCallsView = null
    }
}
