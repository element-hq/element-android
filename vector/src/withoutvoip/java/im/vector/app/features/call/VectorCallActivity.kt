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
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.WindowManager
import com.airbnb.mvrx.MvRx
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityCallBinding
import im.vector.app.features.call.webrtc.WebRtcCall
import im.vector.app.features.home.room.detail.RoomDetailActivity
import im.vector.app.features.home.room.detail.RoomDetailArgs
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.logger.LoggerTag
import timber.log.Timber
import javax.inject.Inject

@Parcelize
data class CallArgs(
        val signalingRoomId: String,
        val callId: String,
        val participantUserId: String,
        val isIncomingCall: Boolean,
        val isVideoCall: Boolean
) : Parcelable

private val loggerTag = LoggerTag("VectorCallActivity", LoggerTag.VOIP)

class VectorCallActivity : VectorBaseActivity<ActivityCallBinding>(), CallControlsView.InteractionListener {

    override fun getBinding() = ActivityCallBinding.inflate(layoutInflater)

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    private lateinit var callArgs: CallArgs

    @Inject lateinit var viewModelFactory: VectorCallViewModel.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.BLACK
        super.onCreate(savedInstanceState)

        if (intent.hasExtra(MvRx.KEY_ARG)) {
            callArgs = intent.getParcelableExtra(MvRx.KEY_ARG)!!
        } else {
            Timber.tag(loggerTag.value).e("missing callArgs for VectorCall Activity")
            finish()
        }
    }

    companion object {
        private const val EXTRA_MODE = "EXTRA_MODE"

        const val INCOMING_RINGING = "INCOMING_RINGING"
        const val INCOMING_ACCEPT = "INCOMING_ACCEPT"

        fun newIntent(context: Context, call: WebRtcCall, mode: String?): Intent {
            return Intent(context, VectorCallActivity::class.java).apply {
                // what could be the best flags?
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(MvRx.KEY_ARG, CallArgs(call.nativeRoomId, call.callId, call.mxCall.opponentUserId, !call.mxCall.isOutgoing, call.mxCall.isVideoCall))
                putExtra(EXTRA_MODE, mode)
            }
        }

        fun newIntent(context: Context,
                      callId: String,
                      signalingRoomId: String,
                      otherUserId: String,
                      isIncomingCall: Boolean,
                      isVideoCall: Boolean,
                      mode: String?): Intent {
            return Intent(context, VectorCallActivity::class.java).apply {
                // what could be the best flags?
                flags = FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MvRx.KEY_ARG, CallArgs(signalingRoomId, callId, otherUserId, isIncomingCall, isVideoCall))
                putExtra(EXTRA_MODE, mode)
            }
        }
    }

    override fun didAcceptIncomingCall() = Unit

    override fun didDeclineIncomingCall() = Unit

    override fun didEndCall() = Unit

    override fun didTapToggleMute() = Unit

    override fun didTapToggleVideo() = Unit

    override fun returnToChat() {
        val args = RoomDetailArgs(callArgs.signalingRoomId)
        val intent = RoomDetailActivity.newIntent(this, args).apply {
            flags = FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        // is it needed?
        finish()
    }

    override fun didTapMore() = Unit
}
