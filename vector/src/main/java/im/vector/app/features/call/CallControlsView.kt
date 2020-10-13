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

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import im.vector.app.R
import kotlinx.android.synthetic.main.view_call_controls.view.*
import org.matrix.android.sdk.api.session.call.CallState
import org.webrtc.PeerConnection

class CallControlsView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var interactionListener: InteractionListener? = null

    @BindView(R.id.ringingControls)
    lateinit var ringingControls: ViewGroup

    @BindView(R.id.iv_icr_accept_call)
    lateinit var ringingControlAccept: ImageView

    @BindView(R.id.iv_icr_end_call)
    lateinit var ringingControlDecline: ImageView

    @BindView(R.id.connectedControls)
    lateinit var connectedControls: ViewGroup

    @BindView(R.id.iv_mute_toggle)
    lateinit var muteIcon: ImageView

    @BindView(R.id.iv_video_toggle)
    lateinit var videoToggleIcon: ImageView

    init {
        ConstraintLayout.inflate(context, R.layout.view_call_controls, this)
        // layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        ButterKnife.bind(this)
    }

    @OnClick(R.id.iv_icr_accept_call)
    fun acceptIncomingCall() {
        interactionListener?.didAcceptIncomingCall()
    }

    @OnClick(R.id.iv_icr_end_call)
    fun declineIncomingCall() {
        interactionListener?.didDeclineIncomingCall()
    }

    @OnClick(R.id.iv_end_call)
    fun endOngoingCall() {
        interactionListener?.didEndCall()
    }

    @OnClick(R.id.iv_mute_toggle)
    fun toggleMute() {
        interactionListener?.didTapToggleMute()
    }

    @OnClick(R.id.iv_video_toggle)
    fun toggleVideo() {
        interactionListener?.didTapToggleVideo()
    }

    @OnClick(R.id.iv_leftMiniControl)
    fun returnToChat() {
        interactionListener?.returnToChat()
    }

    @OnClick(R.id.iv_more)
    fun moreControlOption() {
        interactionListener?.didTapMore()
    }

    fun updateForState(state: VectorCallViewState) {
        val callState = state.callState.invoke()
        if (state.isAudioMuted) {
            muteIcon.setImageResource(R.drawable.ic_microphone_off)
            muteIcon.contentDescription = resources.getString(R.string.a11y_unmute_microphone)
        } else {
            muteIcon.setImageResource(R.drawable.ic_microphone_on)
            muteIcon.contentDescription = resources.getString(R.string.a11y_mute_microphone)
        }
        if (state.isVideoEnabled) {
            videoToggleIcon.setImageResource(R.drawable.ic_video)
            videoToggleIcon.contentDescription = resources.getString(R.string.a11y_stop_camera)
        } else {
            videoToggleIcon.setImageResource(R.drawable.ic_video_off)
            videoToggleIcon.contentDescription = resources.getString(R.string.a11y_start_camera)
        }

        when (callState) {
            is CallState.Idle,
            is CallState.Dialing,
            is CallState.Answering    -> {
                ringingControls.isVisible = true
                ringingControlAccept.isVisible = false
                ringingControlDecline.isVisible = true
                connectedControls.isVisible = false
            }
            is CallState.LocalRinging -> {
                ringingControls.isVisible = true
                ringingControlAccept.isVisible = true
                ringingControlDecline.isVisible = true
                connectedControls.isVisible = false
            }
            is CallState.Connected    -> {
                if (callState.iceConnectionState == PeerConnection.PeerConnectionState.CONNECTED) {
                    ringingControls.isVisible = false
                    connectedControls.isVisible = true
                    iv_video_toggle.isVisible = state.isVideoCall
                } else {
                    ringingControls.isVisible = true
                    ringingControlAccept.isVisible = false
                    ringingControlDecline.isVisible = true
                    connectedControls.isVisible = false
                }
            }
            is CallState.Terminated,
            null                      -> {
                ringingControls.isVisible = false
                connectedControls.isVisible = false
            }
        }
    }

    interface InteractionListener {
        fun didAcceptIncomingCall()
        fun didDeclineIncomingCall()
        fun didEndCall()
        fun didTapToggleMute()
        fun didTapToggleVideo()
        fun returnToChat()
        fun didTapMore()
    }
}
