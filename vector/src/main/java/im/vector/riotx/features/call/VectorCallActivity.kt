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

// import im.vector.riotx.features.call.service.CallHeadsUpService
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import butterknife.BindView
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.viewModel
import com.jakewharton.rxbinding3.view.clicks
import im.vector.matrix.android.api.session.call.CallState
import im.vector.matrix.android.api.session.call.EglUtils
import im.vector.matrix.android.api.session.call.MxCallDetail
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.core.services.CallService
import im.vector.riotx.core.utils.PERMISSIONS_FOR_AUDIO_IP_CALL
import im.vector.riotx.core.utils.PERMISSIONS_FOR_VIDEO_IP_CALL
import im.vector.riotx.core.utils.allGranted
import im.vector.riotx.core.utils.checkPermissions
import im.vector.riotx.features.home.AvatarRenderer
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_call.*
import kotlinx.android.synthetic.main.fragment_attachments_preview.*
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Parcelize
data class CallArgs(
        val roomId: String,
        val callId: String?,
        val participantUserId: String,
        val isIncomingCall: Boolean,
        val isVideoCall: Boolean,
        val autoAccept: Boolean
) : Parcelable

class VectorCallActivity : VectorBaseActivity(), CallControlsView.InteractionListener {

    override fun getLayoutRes() = R.layout.activity_call

    @Inject lateinit var avatarRenderer: AvatarRenderer

    override fun injectWith(injector: ScreenComponent) {
        super.injectWith(injector)
        injector.inject(this)
    }

    private val callViewModel: VectorCallViewModel by viewModel()
    private lateinit var callArgs: CallArgs

    @Inject lateinit var peerConnectionManager: WebRtcPeerConnectionManager

    @Inject lateinit var viewModelFactory: VectorCallViewModel.Factory

    @BindView(R.id.pip_video_view)
    lateinit var pipRenderer: SurfaceViewRenderer

    @BindView(R.id.fullscreen_video_view)
    lateinit var fullscreenRenderer: SurfaceViewRenderer

    @BindView(R.id.callControls)
    lateinit var callControlsView: CallControlsView

    private var rootEglBase: EglBase? = null

    var systemUiVisibility = false

    override fun doBeforeSetContentView() {
        // Set window styles for fullscreen-window size. Needs to be done before adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
            setShowWhenLocked(true)
            getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        hideSystemUI()
        setContentView(R.layout.activity_call)
    }

    private fun hideSystemUI() {
        systemUiVisibility = false
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    // Shows the system bars by removing all the flags
// except for the ones that make the content appear under the system bars.
    private fun showSystemUI() {
        systemUiVisibility = true
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    private fun toggleUiSystemVisibility() {
        if (systemUiVisibility) {
            hideSystemUI()
        } else {
            showSystemUI()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Rehide when bottom sheet is dismissed
        if (hasFocus) {
            hideSystemUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This will need to be refined
        ViewCompat.setOnApplyWindowInsetsListener(constraintLayout) { v, insets ->
            v.updatePadding(bottom = if (systemUiVisibility) insets.systemWindowInsetBottom else 0)
            insets
        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
// //            window.navigationBarColor = ContextCompat.getColor(this, R.color.riotx_background_light)
// //        }

        // for content intent when screen is locked
//        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
//        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (intent.hasExtra(MvRx.KEY_ARG)) {
            callArgs = intent.getParcelableExtra(MvRx.KEY_ARG)!!
        } else {
            finish()
        }

        constraintLayout.clicks()
                .throttleFirst(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { toggleUiSystemVisibility() }
                .disposeOnDestroy()

        if (isFirstCreation()) {
            // Reduce priority of notification as the activity is on screen
            CallService.onPendingCall(
                    this,
                    callArgs.isVideoCall,
                    callArgs.participantUserId,
                    callArgs.roomId,
                    "",
                    callArgs.callId ?: ""
            )
        }

        rootEglBase = EglUtils.rootEglBase ?: return Unit.also {
            finish()
        }

        configureCallViews()

        callViewModel.subscribe(this) {
            renderState(it)
        }

        callViewModel.viewEvents
                .observe()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    handleViewEvents(it)
                }
                .disposeOnDestroy()

        if (callArgs.isVideoCall) {
            if (checkPermissions(PERMISSIONS_FOR_VIDEO_IP_CALL, this, CAPTURE_PERMISSION_REQUEST_CODE, R.string.permissions_rationale_msg_camera_and_audio)) {
                start()
            }
        } else {
            if (checkPermissions(PERMISSIONS_FOR_AUDIO_IP_CALL, this, CAPTURE_PERMISSION_REQUEST_CODE, R.string.permissions_rationale_msg_record_audio)) {
                start()
            }
        }
    }

    private fun renderState(state: VectorCallViewState) {
        Timber.v("## VOIP renderState call $state")
        callControlsView.updateForState(state)
        when (state.callState.invoke()) {
            CallState.IDLE,
            CallState.DIALING       -> {
                callVideoGroup.isInvisible = true
                callInfoGroup.isVisible = true
                callStatusText.setText(R.string.call_ring)
                configureCallInfo(state)
            }

            CallState.LOCAL_RINGING -> {
                callVideoGroup.isInvisible = true
                callInfoGroup.isVisible = true
                callStatusText.text = null
                configureCallInfo(state)
            }

            CallState.ANSWERING     -> {
                callVideoGroup.isInvisible = true
                callInfoGroup.isVisible = true
                callStatusText.setText(R.string.call_connecting)
                configureCallInfo(state)
            }
            CallState.CONNECTING    -> {
                callVideoGroup.isInvisible = true
                callInfoGroup.isVisible = true
                configureCallInfo(state)
                callStatusText.setText(R.string.call_connecting)
            }
            CallState.CONNECTED     -> {
                if (callArgs.isVideoCall) {
                    callVideoGroup.isVisible = true
                    callInfoGroup.isVisible = false
                } else {
                    callVideoGroup.isInvisible = true
                    callInfoGroup.isVisible = true
                    configureCallInfo(state)
                    callStatusText.text = null
                }
            }
            CallState.TERMINATED    -> {
                finish()
            }
            null                    -> {
            }
        }
    }

    private fun configureCallInfo(state: VectorCallViewState) {
        state.otherUserMatrixItem.invoke()?.let {
            avatarRenderer.render(it, otherMemberAvatar)
            participantNameText.text = it.getBestName()
            callTypeText.setText(if (state.isVideoCall) R.string.action_video_call else R.string.action_voice_call)
        }
    }

    private fun configureCallViews() {
        callControlsView.interactionListener = this
//        if (callArgs.isVideoCall) {
//            iv_call_speaker.isVisible = false
//            iv_call_flip_camera.isVisible = true
//            iv_call_videocam_off.isVisible = true
//        } else {
//            iv_call_speaker.isVisible = true
//            iv_call_flip_camera.isVisible = false
//            iv_call_videocam_off.isVisible = false
//        }
//
//        iv_end_call.setOnClickListener {
//            callViewModel.handle(VectorCallViewActions.EndCall)
//            finish()
//        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == CAPTURE_PERMISSION_REQUEST_CODE && allGranted(grantResults)) {
            start()
        } else {
            // TODO display something
            finish()
        }
    }

    private fun start(): Boolean {
        // Init Picture in Picture renderer
        pipRenderer.init(rootEglBase!!.eglBaseContext, null)
        pipRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)

        // Init Full Screen renderer
        fullscreenRenderer.init(rootEglBase!!.eglBaseContext, null)
        fullscreenRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)

        pipRenderer.setZOrderMediaOverlay(true)
        pipRenderer.setEnableHardwareScaler(true /* enabled */)
        fullscreenRenderer.setEnableHardwareScaler(true /* enabled */)

        peerConnectionManager.attachViewRenderers(pipRenderer, fullscreenRenderer,
                intent.getStringExtra(EXTRA_MODE)?.takeIf { isFirstCreation() })
        return false
    }

    override fun onPause() {
        peerConnectionManager.detachRenderers()
        super.onPause()
    }

    private fun handleViewEvents(event: VectorCallViewEvents?) {
        Timber.v("handleViewEvents $event")
//        when (event) {
//            is VectorCallViewEvents.CallAnswered -> {
//            }
//            is VectorCallViewEvents.CallHangup   -> {
//                finish()
//            }
//        }
    }

    companion object {

        private const val CAPTURE_PERMISSION_REQUEST_CODE = 1
        private const val EXTRA_MODE = "EXTRA_MODE"

        const val OUTGOING_CREATED = "OUTGOING_CREATED"
        const val INCOMING_RINGING = "INCOMING_RINGING"
        const val INCOMING_ACCEPT = "INCOMING_ACCEPT"

        fun newIntent(context: Context, mxCall: MxCallDetail): Intent {
            return Intent(context, VectorCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(MvRx.KEY_ARG, CallArgs(mxCall.roomId, mxCall.callId, mxCall.otherUserId, !mxCall.isOutgoing, mxCall.isVideoCall, false))
                putExtra(EXTRA_MODE, OUTGOING_CREATED)
            }
        }

        fun newIntent(context: Context,
                      callId: String?,
                      roomId: String,
                      otherUserId: String,
                      isIncomingCall: Boolean,
                      isVideoCall: Boolean,
                      accept: Boolean,
                      mode: String?): Intent {
            return Intent(context, VectorCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(MvRx.KEY_ARG, CallArgs(roomId, callId, otherUserId, isIncomingCall, isVideoCall, accept))
                putExtra(EXTRA_MODE, mode)
            }
        }
    }

    override fun didAcceptIncomingCall() {
        callViewModel.handle(VectorCallViewActions.AcceptCall)
    }

    override fun didDeclineIncomingCall() {
        callViewModel.handle(VectorCallViewActions.DeclineCall)
    }

    override fun didEndCall() {
        callViewModel.handle(VectorCallViewActions.EndCall)
    }

    override fun didTapToggleMute() {
        callViewModel.handle(VectorCallViewActions.ToggleMute)
    }

    override fun didTapToggleVideo() {
        callViewModel.handle(VectorCallViewActions.ToggleVideo)
    }

    override fun returnToChat() {
        // TODO, what if the room is not in backstack??
        finish()
    }

    override fun didTapMore() {
        CallControlsBottomSheet().show(supportFragmentManager, "Controls")
    }
}
