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

import android.view.View
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import im.vector.app.core.utils.DebouncedClickListener
import im.vector.app.features.call.WebRtcPeerConnectionManager
import org.matrix.android.sdk.api.session.call.CallState
import org.matrix.android.sdk.api.session.call.EglUtils
import org.matrix.android.sdk.api.session.call.MxCall
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

class ActiveCallViewHolder {

    private var activeCallPiP: SurfaceViewRenderer? = null
    private var activeCallView: ActiveCallView? = null
    private var pipWrapper: CardView? = null

    private var activeCallPipInitialized = false

    fun updateCall(activeCall: MxCall?, webRtcPeerConnectionManager: WebRtcPeerConnectionManager) {
        val hasActiveCall = activeCall?.state is CallState.Connected
        if (hasActiveCall) {
            val isVideoCall = activeCall?.isVideoCall == true
            if (isVideoCall) initIfNeeded()
            activeCallView?.isVisible = !isVideoCall
            pipWrapper?.isVisible = isVideoCall
            activeCallPiP?.isVisible = isVideoCall
            activeCallPiP?.let {
                webRtcPeerConnectionManager.attachViewRenderers(null, it, null)
            }
        } else {
            activeCallView?.isVisible = false
            activeCallPiP?.isVisible = false
            pipWrapper?.isVisible = false
            activeCallPiP?.let {
                webRtcPeerConnectionManager.detachRenderers(listOf(it))
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

    fun bind(activeCallPiP: SurfaceViewRenderer, activeCallView: ActiveCallView, pipWrapper: CardView, interactionListener: ActiveCallView.Callback) {
        this.activeCallPiP = activeCallPiP
        this.activeCallView = activeCallView
        this.pipWrapper = pipWrapper

        this.activeCallView?.callback = interactionListener
        pipWrapper.setOnClickListener(
                DebouncedClickListener(View.OnClickListener { _ ->
                    interactionListener.onTapToReturnToCall()
                })
        )
    }

    fun unBind(webRtcPeerConnectionManager: WebRtcPeerConnectionManager) {
        activeCallPiP?.let {
            webRtcPeerConnectionManager.detachRenderers(listOf(it))
        }
        if (activeCallPipInitialized) {
            activeCallPiP?.release()
        }
        this.activeCallView?.callback = null
        pipWrapper?.setOnClickListener(null)
        activeCallPiP = null
        activeCallView = null
        pipWrapper = null
    }
}
