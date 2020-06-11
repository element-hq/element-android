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

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import im.vector.matrix.android.api.session.call.CallState
import im.vector.riotx.R

class CallControlsView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var interactionListener: InteractionListener? = null

    @BindView(R.id.incomingRingingControls)
    lateinit var incomingRingingControls: ViewGroup
//    @BindView(R.id.iv_icr_accept_call)
//    lateinit var incomingRingingControlAccept: ImageView
//    @BindView(R.id.iv_icr_end_call)
//    lateinit var incomingRingingControlDecline: ImageView

    @BindView(R.id.connectedControls)
    lateinit var connectedControls: ViewGroup

    init {
        ConstraintLayout.inflate(context, R.layout.fragment_call_controls, this)
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

    @OnClick(R.id.iv_end_call)
    fun hangupCall() {
    }

    fun updateForState(callState: CallState?) {
        when (callState) {
            CallState.DIALING        -> {
            }
            CallState.ANSWERING      -> {
                incomingRingingControls.isVisible = false
                connectedControls.isVisible = false
            }
            CallState.REMOTE_RINGING -> {
            }
            CallState.LOCAL_RINGING  -> {
                incomingRingingControls.isVisible = true
                connectedControls.isVisible = false
            }
            CallState.CONNECTED      -> {
                incomingRingingControls.isVisible = false
                connectedControls.isVisible = true
            }
            CallState.TERMINATED,
            CallState.IDLE,
            null                     -> {
                incomingRingingControls.isVisible = false
                connectedControls.isVisible = false
            }
        }
    }

    interface InteractionListener {
        fun didAcceptIncomingCall()
        fun didDeclineIncomingCall()
        fun didEndCall()
    }
}
