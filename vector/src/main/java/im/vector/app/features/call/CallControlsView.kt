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
import android.widget.FrameLayout
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.databinding.ViewCallControlsBinding
import org.matrix.android.sdk.api.session.call.CallState
import org.matrix.android.sdk.api.session.call.MxPeerConnectionState

class CallControlsView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val views: ViewCallControlsBinding

    var interactionListener: InteractionListener? = null

    init {
        inflate(context, R.layout.view_call_controls, this)
        views = ViewCallControlsBinding.bind(this)
        views.audioSettingsIcon.setOnClickListener { didTapAudioSettings() }
        views.ringingControlAccept.setOnClickListener { acceptIncomingCall() }
        views.ringingControlDecline.setOnClickListener { declineIncomingCall() }
        views.endCallIcon.setOnClickListener { endOngoingCall() }
        views.muteIcon.setOnClickListener { toggleMute() }
        views.videoToggleIcon.setOnClickListener { toggleVideo() }
        views.moreIcon.setOnClickListener { moreControlOption() }
    }

    private fun didTapAudioSettings() {
        interactionListener?.didTapAudioSettings()
    }

    private fun acceptIncomingCall() {
        interactionListener?.didAcceptIncomingCall()
    }

    private fun declineIncomingCall() {
        interactionListener?.didDeclineIncomingCall()
    }

    private fun endOngoingCall() {
        interactionListener?.didEndCall()
    }

    private fun toggleMute() {
        interactionListener?.didTapToggleMute()
    }

    private fun toggleVideo() {
        interactionListener?.didTapToggleVideo()
    }

    private fun moreControlOption() {
        interactionListener?.didTapMore()
    }

    fun updateForState(state: VectorCallViewState) {
        val callState = state.callState.invoke()
        if (state.isAudioMuted) {
            views.muteIcon.setImageResource(R.drawable.ic_mic_off)
            views.muteIcon.contentDescription = resources.getString(R.string.a11y_unmute_microphone)
        } else {
            views.muteIcon.setImageResource(R.drawable.ic_mic_on)
            views.muteIcon.contentDescription = resources.getString(R.string.a11y_mute_microphone)
        }
        if (state.isVideoEnabled) {
            views.videoToggleIcon.setImageResource(R.drawable.ic_video)
            views.videoToggleIcon.contentDescription = resources.getString(R.string.a11y_stop_camera)
        } else {
            views.videoToggleIcon.setImageResource(R.drawable.ic_video_off)
            views.videoToggleIcon.contentDescription = resources.getString(R.string.a11y_start_camera)
        }

        when (callState) {
            is CallState.LocalRinging -> {
                views.ringingControls.isVisible = true
                views.ringingControlAccept.isVisible = true
                views.ringingControlDecline.isVisible = true
                views.connectedControls.isVisible = false
            }
            CallState.CreateOffer,
            CallState.Idle,
            is CallState.Connected,
            is CallState.Dialing,
            is CallState.Answering    -> {
                views.ringingControls.isVisible = false
                views.connectedControls.isVisible = true
                views.videoToggleIcon.isVisible = state.isVideoCall
                views.moreIcon.isVisible = callState is CallState.Connected && callState.iceConnectionState == MxPeerConnectionState.CONNECTED
            }
            is CallState.Ended        -> {
                views.ringingControls.isVisible = false
                views.connectedControls.isVisible = false
            }
            null                      -> Unit
        }
    }

    interface InteractionListener {
        fun didTapAudioSettings()
        fun didAcceptIncomingCall()
        fun didDeclineIncomingCall()
        fun didEndCall()
        fun didTapToggleMute()
        fun didTapToggleVideo()
        fun didTapMore()
    }
}
