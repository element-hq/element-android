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

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import im.vector.app.R
import im.vector.app.databinding.ViewCurrentCallsCardBinding
import im.vector.app.features.call.utils.EglUtils
import im.vector.app.features.call.webrtc.WebRtcCall
import im.vector.app.features.call.webrtc.getOpponentAsMatrixItem
import im.vector.app.features.home.AvatarRenderer
import io.github.hyuwah.draggableviewlib.DraggableView
import io.github.hyuwah.draggableviewlib.setupDraggable
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.call.CallState
import org.webrtc.RendererCommon

class CurrentCallsCardView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    interface Callback {
        fun onTapToReturnToCall()
    }

    private val views: ViewCurrentCallsCardBinding

    private var activeCallPipInitialized = false
    private var currentCall: WebRtcCall? = null
    private var draggableView: DraggableView<CurrentCallsCardView>? = null

    lateinit var avatarRenderer: AvatarRenderer
    lateinit var session: Session
    var callback: Callback? = null

    init {
        inflate(context, R.layout.view_current_calls_card, this)
        isVisible = false
        views = ViewCurrentCallsCardBinding.bind(this)
        draggableView = setupDraggable().build()
        setOnClickListener { callback?.onTapToReturnToCall() }
    }

    fun render(currentCall: WebRtcCall?, calls: List<WebRtcCall>) {
        views.activeCallPiP.let {
            this.currentCall?.detachRenderers(listOf(it))
        }
        this.currentCall = currentCall
        if (currentCall != null) {
            isVisible = true
            when (currentCall.mxCall.state) {
                is CallState.LocalRinging, CallState.Idle -> {
                    isVisible = false
                }
                is CallState.Connected    -> {
                    views.activeCallProgress.isVisible = false
                    val isVideoCall = currentCall.mxCall.isVideoCall
                    if (isVideoCall) {
                        renderVideoCall(currentCall)
                    } else {
                        renderVoiceCall(currentCall, calls)
                    }
                }
                else                      -> {
                    renderConnectingState(currentCall)
                }
            }
        } else {
            // NO ACTIVE CALL
            isVisible = false
        }
    }

    private fun renderConnectingState(currentCall: WebRtcCall) {
        //TODO show dots
        views.activeCallProgress.isVisible = true
        views.activeCallPiP.isVisible = false
        views.avatarViews.isVisible = false
        currentCall.detachRenderers(listOf(views.activeCallPiP))
    }

    private fun renderVideoCall(currentCall: WebRtcCall) {
        initIfNeeded()
        views.activeCallPiP.isVisible = true
        views.avatarViews.isVisible = false
        currentCall.attachViewRenderers(null, views.activeCallPiP, null)
    }

    private fun renderVoiceCall(currentCall: WebRtcCall, calls: List<WebRtcCall>) {
        views.activeCallPiP.isVisible = false
        views.avatarViews.isVisible = true
        val isActiveCallPaused = currentCall.isLocalOnHold || currentCall.isRemoteOnHold
        views.activeCallPausedIcon.isVisible = isActiveCallPaused
        val activeOpponentMatrixItem = currentCall.getOpponentAsMatrixItem(session)
        if (isActiveCallPaused) {
            val colorFilter = ContextCompat.getColor(context, R.color.bg_call_screen_blur)
            activeOpponentMatrixItem?.also {
                avatarRenderer.renderBlur(it, views.activeCallOpponentAvatar, sampling = 2, rounded = true, colorFilter = colorFilter, addPlaceholder = true)
            }
        } else {
            activeOpponentMatrixItem?.also {
                avatarRenderer.render(it, views.activeCallOpponentAvatar)
            }
        }

        val otherConnectedCall = calls.filter {
            it.mxCall.state is CallState.Connected
        }.firstOrNull {
            it != currentCall
        }
        if (otherConnectedCall != null) {
            views.otherCallOpponentAvatar.isVisible = true
            views.otherCallPausedIcon.isVisible = true
            otherConnectedCall.getOpponentAsMatrixItem(session)?.also { heldOpponentMatrixItem ->
                avatarRenderer.render(heldOpponentMatrixItem, views.activeCallOpponentAvatar)
            }
        } else {
            views.otherCallOpponentAvatar.isVisible = false
            views.otherCallPausedIcon.isVisible = false
        }
    }

    private fun initIfNeeded() {
        if (!activeCallPipInitialized) {
            EglUtils.rootEglBase?.let { eglBase ->
                views.activeCallPiP.apply {
                    init(eglBase.eglBaseContext, null)
                    setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED)
                    setEnableHardwareScaler(true)
                    setZOrderMediaOverlay(true)
                }
                activeCallPipInitialized = true
            }
        }
    }
}
