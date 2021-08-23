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

package im.vector.app.core.ui.views

import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import im.vector.app.core.epoxy.onClick
import im.vector.app.features.call.utils.EglUtils
import im.vector.app.features.call.webrtc.WebRtcCall
import org.matrix.android.sdk.api.session.call.CallState
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

class KnownCallsViewHolder {

    private var activeCallPiP: SurfaceViewRenderer? = null
    private var currentCallsView: CurrentCallsView? = null
    private var pipWrapper: MaterialCardView? = null
    private var currentCall: WebRtcCall? = null
    private var calls: List<WebRtcCall> = emptyList()

    private var activeCallPipInitialized = false

    private val tickListener = object : WebRtcCall.Listener {
        override fun onTick(formattedDuration: String) {
            currentCallsView?.render(calls, formattedDuration)
        }
    }

    fun updateCall(currentCall: WebRtcCall?, calls: List<WebRtcCall>) {
        activeCallPiP?.let {
            this.currentCall?.detachRenderers(listOf(it))
        }
        this.currentCall?.removeListener(tickListener)
        this.currentCall = currentCall
        this.currentCall?.addListener(tickListener)
        this.calls = calls
        val hasActiveCall = currentCall?.mxCall?.state is CallState.Connected
        if (hasActiveCall) {
            val isVideoCall = currentCall?.mxCall?.isVideoCall == true
            if (isVideoCall) initIfNeeded()
            currentCallsView?.isVisible = !isVideoCall
            currentCallsView?.render(calls, currentCall?.formattedDuration() ?: "")
            pipWrapper?.isVisible = isVideoCall
            activeCallPiP?.isVisible = isVideoCall
            activeCallPiP?.let {
                currentCall?.attachViewRenderers(null, it, null)
            }
        } else {
            currentCallsView?.isVisible = false
            activeCallPiP?.isVisible = false
            pipWrapper?.isVisible = false
            activeCallPiP?.let {
                currentCall?.detachRenderers(listOf(it))
            }
        }
    }

    private fun initIfNeeded() {
        if (!activeCallPipInitialized && activeCallPiP != null) {
            activeCallPiP?.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            EglUtils.rootEglBase?.let { eglBase ->
                activeCallPiP?.init(eglBase.eglBaseContext, null)
                activeCallPiP?.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED)
                activeCallPiP?.setEnableHardwareScaler(true /* enabled */)
                activeCallPiP?.setZOrderMediaOverlay(true)
                activeCallPipInitialized = true
            }
        }
    }

    fun bind(activeCallPiP: SurfaceViewRenderer,
             activeCallView: CurrentCallsView,
             pipWrapper: MaterialCardView,
             interactionListener: CurrentCallsView.Callback) {
        this.activeCallPiP = activeCallPiP
        this.currentCallsView = activeCallView
        this.pipWrapper = pipWrapper
        this.currentCallsView?.callback = interactionListener
        pipWrapper.onClick {
            interactionListener.onTapToReturnToCall()
        }
        this.currentCall?.addListener(tickListener)
    }

    fun unBind() {
        activeCallPiP?.let {
            currentCall?.detachRenderers(listOf(it))
        }
        if (activeCallPipInitialized) {
            activeCallPiP?.release()
        }
        this.currentCallsView?.callback = null
        this.currentCall?.removeListener(tickListener)
        pipWrapper?.setOnClickListener(null)
        activeCallPiP = null
        currentCallsView = null
        pipWrapper = null
    }
}
