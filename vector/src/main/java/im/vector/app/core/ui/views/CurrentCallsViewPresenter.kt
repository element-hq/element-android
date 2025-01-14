/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
