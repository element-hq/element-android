/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Rational
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.activity.result.ActivityResult
import androidx.annotation.StringRes
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.util.Consumer
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.viewModel
import com.airbnb.mvrx.withState
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.platform.VectorMenuProvider
import im.vector.app.core.utils.PERMISSIONS_FOR_AUDIO_IP_CALL
import im.vector.app.core.utils.PERMISSIONS_FOR_VIDEO_IP_CALL
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.databinding.ActivityCallBinding
import im.vector.app.features.call.audio.MicrophoneAccessService
import im.vector.app.features.call.dialpad.CallDialPadBottomSheet
import im.vector.app.features.call.dialpad.DialPadFragment
import im.vector.app.features.call.transfer.CallTransferActivity
import im.vector.app.features.call.utils.EglUtils
import im.vector.app.features.call.webrtc.ScreenCaptureAndroidService
import im.vector.app.features.call.webrtc.ScreenCaptureServiceConnection
import im.vector.app.features.call.webrtc.WebRtcCall
import im.vector.app.features.call.webrtc.WebRtcCallManager
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.RoomDetailActivity
import im.vector.app.features.home.room.detail.arguments.TimelineArgs
import im.vector.lib.core.utils.compat.getParcelableExtraCompat
import im.vector.lib.strings.CommonStrings
import io.github.hyuwah.draggableviewlib.DraggableView
import io.github.hyuwah.draggableviewlib.setupDraggable
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.call.CallState
import org.matrix.android.sdk.api.session.call.MxPeerConnectionState
import org.matrix.android.sdk.api.session.call.TurnServerResponse
import org.matrix.android.sdk.api.session.room.model.call.EndCallReason
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.ScreenCapturerAndroid
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

@AndroidEntryPoint
class VectorCallActivity :
        VectorBaseActivity<ActivityCallBinding>(),
        CallControlsView.InteractionListener,
        VectorMenuProvider {

    override fun getBinding() = ActivityCallBinding.inflate(layoutInflater)

    @Inject lateinit var callManager: WebRtcCallManager
    @Inject lateinit var avatarRenderer: AvatarRenderer
    @Inject lateinit var screenCaptureServiceConnection: ScreenCaptureServiceConnection

    private val callViewModel: VectorCallViewModel by viewModel()

    private val dialPadCallback = object : DialPadFragment.Callback {
        override fun onDigitAppended(digit: String) {
            callViewModel.handle(VectorCallViewActions.SendDtmfDigit(digit))
        }
    }

    private var rootEglBase: EglBase? = null
    private var pipDraggrableView: DraggableView<MaterialCardView>? = null
    private var otherCallDraggableView: DraggableView<MaterialCardView>? = null

    var surfaceRenderersAreInitialized = false

    override fun doBeforeSetContentView() {
        setContentView(R.layout.activity_call)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.BLACK
        super.onCreate(savedInstanceState)
        addOnPictureInPictureModeChangedListener(pictureInPictureModeChangedInfoConsumer)

        Timber.tag(loggerTag.value).v("EXTRA_MODE is ${intent.getStringExtra(EXTRA_MODE)}")
        if (intent.getStringExtra(EXTRA_MODE) == INCOMING_RINGING) {
            turnScreenOnAndKeyguardOff()
        }
        if (savedInstanceState != null) {
            (supportFragmentManager.findFragmentByTag(FRAGMENT_DIAL_PAD_TAG) as? CallDialPadBottomSheet)?.callback = dialPadCallback
        }
        setupToolbar(views.callToolbar)
        configureCallViews()

        callViewModel.onEach {
            renderState(it)
        }

        callViewModel.onAsync(VectorCallViewState::callState) {
            if (it is CallState.Ended) {
                handleCallEnded(it)
            }
        }

        callViewModel.observeViewEvents {
            handleViewEvents(it)
        }

        callViewModel.onEach(VectorCallViewState::callId, VectorCallViewState::isVideoCall) { _, isVideoCall ->
            if (isVideoCall) {
                if (checkPermissions(PERMISSIONS_FOR_VIDEO_IP_CALL, this, permissionCameraLauncher, CommonStrings.permissions_rationale_msg_camera_and_audio)) {
                    setupRenderersIfNeeded()
                }
            } else {
                if (checkPermissions(PERMISSIONS_FOR_AUDIO_IP_CALL, this, permissionCameraLauncher, CommonStrings.permissions_rationale_msg_record_audio)) {
                    setupRenderersIfNeeded()
                }
            }
        }

        // Bind to service in case of user killed the app while there is an ongoing call
        bindToScreenCaptureService()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.takeIf { it.hasExtra(Mavericks.KEY_ARG) }
                ?.let { intent.getParcelableExtraCompat<CallArgs>(Mavericks.KEY_ARG) }
                ?.let {
                    callViewModel.handle(VectorCallViewActions.SwitchCall(it))
                }
        this.intent = intent
    }

    override fun getMenuRes() = R.menu.vector_call

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPictureInPictureIfRequired()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (!enterPictureInPictureIfRequired()) {
            super.onBackPressed()
        }
    }

    private fun enterPictureInPictureIfRequired(): Boolean = withState(callViewModel) {
        if (!it.isVideoCall) {
            false
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val aspectRatio = Rational(
                    resources.getDimensionPixelSize(im.vector.lib.ui.styles.R.dimen.call_pip_width),
                    resources.getDimensionPixelSize(im.vector.lib.ui.styles.R.dimen.call_pip_height)
            )
            val params = PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build()
            renderPiPMode(it)
            enterPictureInPictureMode(params)
        } else {
            false
        }
    }

    private fun isInPictureInPictureModeSafe(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode
    }

    private val pictureInPictureModeChangedInfoConsumer = Consumer<PictureInPictureModeChangedInfo> {
        withState(callViewModel) {
            renderState(it)
        }
    }

    override fun handleMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_call_open_chat -> {
                returnToChat()
                true
            }
            android.R.id.home -> {
                // We check here as we want PiP in some cases
                @Suppress("DEPRECATION")
                onBackPressed()
                true
            }
            else -> false
        }
    }

    private fun startMicrophoneService() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            // Only start the service if the app is in the foreground
            if (isAppInForeground()) {
                withState(callViewModel) {
                    // Starting in Android 14, you can't create a microphone foreground service while your app is in
                    // the background. If we call startForegroundService while the call state is ringing (i.e. the
                    // user has not interacted with the device at all) the app will crash. Make sure the call has
                    // already been answered before starting the MicrophoneAccessService
                    // https://github.com/element-hq/element-android/issues/8964
                    val callState = it.callState.invoke()
                    if (callState !is CallState.LocalRinging && callState !is CallState.Ended && callState != null) {
                        Timber.tag(loggerTag.value).v("Starting microphone foreground service")
                        val intent = Intent(this, MicrophoneAccessService::class.java)
                        ContextCompat.startForegroundService(this, intent)
                    } else {
                        Timber.tag(loggerTag.value).v("Call is in ringing or ended state; cannot start microphone service. callState: $callState")
                    }
                }
            } else {
                Timber.tag(loggerTag.value).v("App is not in foreground; cannot start microphone service")
            }
        } else {
            Timber.tag(loggerTag.value).v("Microphone permission not granted; cannot start service")
        }
    }

    private fun isAppInForeground(): Boolean {
        val appProcess = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        return appProcess
    }
    private fun stopMicrophoneService() {
        Timber.tag(loggerTag.value).d("Stopping MicrophoneAccessService (if needed).")
        val intent = Intent(this, MicrophoneAccessService::class.java)
        stopService(intent)
    }

    override fun onPause() {
        super.onPause()

        // Start the microphone service to keep access to the microphone when the call is in the background
        // https://github.com/element-hq/element-android/issues/8881
        startMicrophoneService()
    }

    override fun onResume() {
        super.onResume()
        stopMicrophoneService()
    }

    override fun onDestroy() {
        detachRenderersIfNeeded()
        turnScreenOffAndKeyguardOn()
        removeOnPictureInPictureModeChangedListener(pictureInPictureModeChangedInfoConsumer)
        screenCaptureServiceConnection.unbind()
        super.onDestroy()
    }

    private fun detachRenderersIfNeeded() {
        val callId = withState(callViewModel) { it.callId }
        callManager.getCallById(callId)?.detachRenderers(listOf(views.pipRenderer, views.fullscreenRenderer))
        if (surfaceRenderersAreInitialized) {
            views.pipRenderer.release()
            views.fullscreenRenderer.release()
            surfaceRenderersAreInitialized = false
        }
    }

    private fun renderState(state: VectorCallViewState) {
        Timber.tag(loggerTag.value).v("renderState call $state")
        if (state.callState is Fail) {
            finish()
            return
        }
        if (isInPictureInPictureModeSafe()) {
            renderPiPMode(state)
        } else {
            renderFullScreenMode(state)
        }
    }

    private fun renderFullScreenMode(state: VectorCallViewState) {
        views.callToolbar.isVisible = true
        views.callControlsView.isVisible = true
        views.callControlsView.updateForState(state)
        val callState = state.callState.invoke()
        views.callActionText.setOnClickListener(null)
        views.callActionText.isVisible = false
        views.smallIsHeldIcon.isVisible = false
        when (callState) {
            is CallState.Idle,
            is CallState.CreateOffer,
            is CallState.LocalRinging,
            is CallState.Dialing -> {
                views.fullscreenRenderer.isVisible = false
                views.pipRendererWrapper.isVisible = false
                views.callInfoGroup.isVisible = true
                toolbar?.setSubtitle(CommonStrings.call_ringing)
                configureCallInfo(state)
            }
            is CallState.Answering -> {
                views.fullscreenRenderer.isVisible = false
                views.pipRendererWrapper.isVisible = false
                views.callInfoGroup.isVisible = true
                toolbar?.setSubtitle(CommonStrings.call_connecting)
                configureCallInfo(state)
            }
            is CallState.Connected -> {
                toolbar?.subtitle = state.formattedDuration
                if (callState.iceConnectionState == MxPeerConnectionState.CONNECTED) {
                    if (state.isLocalOnHold || state.isRemoteOnHold) {
                        views.smallIsHeldIcon.isVisible = true
                        views.fullscreenRenderer.isVisible = false
                        views.pipRendererWrapper.isVisible = false
                        views.callInfoGroup.isVisible = true
                        configureCallInfo(state, blurAvatar = true)
                        if (state.isRemoteOnHold) {
                            views.callActionText.setText(CommonStrings.call_resume_action)
                            views.callActionText.isVisible = true
                            views.callActionText.setOnClickListener { callViewModel.handle(VectorCallViewActions.ToggleHoldResume) }
                            toolbar?.setSubtitle(CommonStrings.call_held_by_you)
                        } else {
                            views.callActionText.isInvisible = true
                            state.callInfo?.opponentUserItem?.let {
                                toolbar?.subtitle = getString(CommonStrings.call_held_by_user, it.getBestName())
                            }
                        }
                    } else if (state.transferee !is VectorCallViewState.TransfereeState.NoTransferee) {
                        val transfereeName = if (state.transferee is VectorCallViewState.TransfereeState.KnownTransferee) {
                            state.transferee.name
                        } else {
                            getString(CommonStrings.call_transfer_unknown_person)
                        }
                        views.callActionText.text = getString(CommonStrings.call_transfer_transfer_to_title, transfereeName)
                        views.callActionText.isVisible = true
                        views.callActionText.setOnClickListener { callViewModel.handle(VectorCallViewActions.TransferCall) }
                        configureCallInfo(state)
                    } else {
                        configureCallInfo(state)
                        if (state.isVideoCall) {
                            views.fullscreenRenderer.isVisible = true
                            views.pipRendererWrapper.isVisible = true
                            views.callInfoGroup.isVisible = false
                            views.pipRenderer.isVisible = !state.isVideoCaptureInError && state.otherKnownCallInfo == null
                        } else {
                            views.fullscreenRenderer.isVisible = false
                            views.pipRendererWrapper.isVisible = false
                            views.callInfoGroup.isVisible = true
                        }
                    }
                } else {
                    // This state is not final, if you change network, new candidates will be sent
                    views.fullscreenRenderer.isVisible = false
                    views.pipRendererWrapper.isVisible = false
                    views.callInfoGroup.isVisible = true
                    configureCallInfo(state)
                    toolbar?.setSubtitle(CommonStrings.call_connecting)
                }
            }
            is CallState.Ended -> {
                views.fullscreenRenderer.isVisible = false
                views.pipRendererWrapper.isVisible = false
                views.callInfoGroup.isVisible = true
                toolbar?.setSubtitle(CommonStrings.call_ended)
                configureCallInfo(state)
            }
            else -> {
                views.fullscreenRenderer.isVisible = false
                views.pipRendererWrapper.isVisible = false
                views.callInfoGroup.isInvisible = true
            }
        }
    }

    private fun renderPiPMode(state: VectorCallViewState) {
        val callState = state.callState.invoke()
        views.callToolbar.isVisible = false
        views.callControlsView.isVisible = false
        views.pipRendererWrapper.isVisible = false
        views.pipRenderer.isVisible = false
        views.callActionText.isVisible = false
        when (callState) {
            is CallState.Idle,
            is CallState.CreateOffer,
            is CallState.LocalRinging,
            is CallState.Dialing,
            is CallState.Answering -> {
                views.fullscreenRenderer.isVisible = false
                views.callInfoGroup.isVisible = false
            }
            is CallState.Connected -> {
                if (callState.iceConnectionState == MxPeerConnectionState.CONNECTED) {
                    if (state.isLocalOnHold || state.isRemoteOnHold) {
                        views.smallIsHeldIcon.isVisible = true
                        views.fullscreenRenderer.isVisible = false
                        views.callInfoGroup.isVisible = true
                        configureCallInfo(state, blurAvatar = true)
                    } else {
                        configureCallInfo(state)
                        views.fullscreenRenderer.isVisible = true
                        views.callInfoGroup.isVisible = false
                    }
                } else {
                    views.callInfoGroup.isVisible = false
                }
            }
            else -> {
                views.fullscreenRenderer.isVisible = false
                views.callInfoGroup.isVisible = false
            }
        }
    }

    private fun handleCallEnded(callState: CallState.Ended) {
        if (isInPictureInPictureModeSafe()) {
            val startIntent = Intent(this, VectorCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(startIntent)
        }
        when (callState.reason) {
            EndCallReason.USER_BUSY -> {
                showEndCallDialog(CommonStrings.call_ended_user_busy_title, CommonStrings.call_ended_user_busy_description)
            }
            EndCallReason.INVITE_TIMEOUT -> {
                showEndCallDialog(CommonStrings.call_ended_invite_timeout_title, CommonStrings.call_error_user_not_responding)
            }
            else -> {
                finish()
            }
        }
    }

    private fun showEndCallDialog(@StringRes title: Int, @StringRes description: Int) {
        MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(description)
                .setNegativeButton(CommonStrings.ok, null)
                .setOnDismissListener {
                    finish()
                }
                .show()
    }

    private fun configureCallInfo(state: VectorCallViewState, blurAvatar: Boolean = false) {
        state.callInfo?.opponentUserItem?.let {
            val colorFilter = ContextCompat.getColor(this, im.vector.lib.ui.styles.R.color.bg_call_screen_blur)
            avatarRenderer.renderBlur(it, views.bgCallView, sampling = 20, rounded = false, colorFilter = colorFilter, addPlaceholder = false)
            if (state.transferee is VectorCallViewState.TransfereeState.NoTransferee) {
                views.participantNameText.setTextOrHide(null)
                toolbar?.title = if (state.isVideoCall) {
                    getString(CommonStrings.video_call_with_participant, it.getBestName())
                } else {
                    getString(CommonStrings.audio_call_with_participant, it.getBestName())
                }
            } else {
                views.participantNameText.setTextOrHide(getString(CommonStrings.call_transfer_consulting_with, it.getBestName()))
            }
            if (blurAvatar) {
                avatarRenderer.renderBlur(it, views.otherMemberAvatar, sampling = 2, rounded = true, colorFilter = colorFilter, addPlaceholder = true)
            } else {
                avatarRenderer.render(it, views.otherMemberAvatar)
            }
        }
        if (state.otherKnownCallInfo?.opponentUserItem == null || isInPictureInPictureModeSafe()) {
            views.otherKnownCallLayout.isVisible = false
        } else {
            val otherCall = callManager.getCallById(state.otherKnownCallInfo.callId)
            val colorFilter = ContextCompat.getColor(this, im.vector.lib.ui.styles.R.color.bg_call_screen_blur)
            avatarRenderer.renderBlur(
                    matrixItem = state.otherKnownCallInfo.opponentUserItem,
                    imageView = views.otherKnownCallAvatarView,
                    sampling = 20,
                    rounded = true,
                    colorFilter = colorFilter,
                    addPlaceholder = true
            )
            views.otherKnownCallLayout.isVisible = true
            views.otherSmallIsHeldIcon.isVisible = otherCall?.let { it.isLocalOnHold || it.isRemoteOnHold }.orFalse()
        }
    }

    private fun configureCallViews() {
        views.callControlsView.interactionListener = this
        views.otherKnownCallLayout.setOnClickListener {
            withState(callViewModel) {
                val otherCall = callManager.getCallById(it.otherKnownCallInfo?.callId ?: "") ?: return@withState
                val callArgs = CallArgs(
                        signalingRoomId = otherCall.nativeRoomId,
                        callId = otherCall.callId,
                        participantUserId = otherCall.mxCall.opponentUserId,
                        isIncomingCall = !otherCall.mxCall.isOutgoing,
                        isVideoCall = otherCall.mxCall.isVideoCall
                )
                callViewModel.handle(VectorCallViewActions.SwitchCall(callArgs))
            }
        }
        views.pipRendererWrapper.setOnClickListener {
            callViewModel.handle(VectorCallViewActions.ToggleCamera)
        }
        pipDraggrableView = views.pipRendererWrapper.setupDraggable()
                .setStickyMode(DraggableView.Mode.STICKY_XY)
                .build()

        otherCallDraggableView = views.otherKnownCallLayout.setupDraggable()
                .setStickyMode(DraggableView.Mode.STICKY_XY)
                .build()
    }

    private val permissionCameraLauncher = registerForPermissionsResult { allGranted, _ ->
        if (allGranted) {
            setupRenderersIfNeeded()
        } else {
            // TODO display something
            finish()
        }
    }

    private fun setupRenderersIfNeeded() {
        detachRenderersIfNeeded()
        rootEglBase = EglUtils.rootEglBase ?: return Unit.also {
            Timber.tag(loggerTag.value).v("rootEglBase is null")
            finish()
        }

        // Init Picture in Picture renderer
        views.pipRenderer.apply {
            init(rootEglBase!!.eglBaseContext, null)
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED)
            setEnableHardwareScaler(true)
            setZOrderMediaOverlay(true)
        }
        // Init Full Screen renderer
        views.fullscreenRenderer.init(rootEglBase!!.eglBaseContext, null)
        views.fullscreenRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        views.fullscreenRenderer.setEnableHardwareScaler(true /* enabled */)

        val callId = withState(callViewModel) { it.callId }
        callManager.getCallById(callId)?.also { webRtcCall ->
            webRtcCall.attachViewRenderers(views.pipRenderer, views.fullscreenRenderer, intent.getStringExtra(EXTRA_MODE))
            intent.removeExtra(EXTRA_MODE)
        }
        surfaceRenderersAreInitialized = true
    }

    private fun handleViewEvents(event: VectorCallViewEvents?) {
        Timber.tag(loggerTag.value).v("handleViewEvents $event")
        when (event) {
            is VectorCallViewEvents.ConnectionTimeout -> {
                onErrorTimoutConnect(event.turn)
            }
            is VectorCallViewEvents.ShowDialPad -> {
                CallDialPadBottomSheet.newInstance(false).apply {
                    callback = dialPadCallback
                }.show(supportFragmentManager, FRAGMENT_DIAL_PAD_TAG)
            }
            is VectorCallViewEvents.ShowCallTransferScreen -> {
                val callId = withState(callViewModel) { it.callId }
                navigator.openCallTransfer(this, callTransferActivityResultLauncher, callId)
            }
            is VectorCallViewEvents.FailToTransfer -> showSnackbar(getString(CommonStrings.call_transfer_failure))
            is VectorCallViewEvents.ShowScreenSharingPermissionDialog -> handleShowScreenSharingPermissionDialog()
            is VectorCallViewEvents.StopScreenSharingService -> handleStopScreenSharingService()
            else -> Unit
        }
    }

    private val callTransferActivityResultLauncher = registerStartForActivityResult { activityResult ->
        when (activityResult.resultCode) {
            Activity.RESULT_CANCELED -> {
                callViewModel.handle(VectorCallViewActions.CallTransferSelectionCancelled)
            }
            Activity.RESULT_OK -> {
                CallTransferActivity.getCallTransferResult(activityResult.data)
                        ?.let { callViewModel.handle(VectorCallViewActions.CallTransferSelectionResult(it)) }
            }
        }
    }

    private fun onErrorTimoutConnect(turn: TurnServerResponse?) {
        Timber.tag(loggerTag.value).d("onErrorTimoutConnect $turn")
        // TODO ask to use default stun, etc...
        MaterialAlertDialogBuilder(this)
                .setTitle(CommonStrings.call_failed_no_connection)
                .setMessage(CommonStrings.call_failed_no_connection_description)
                .setNegativeButton(CommonStrings.ok) { _, _ ->
                    callViewModel.handle(VectorCallViewActions.EndCall)
                }
                .show()
    }

    override fun didTapAudioSettings() {
        CallSoundDeviceChooserBottomSheet().show(supportFragmentManager, "SoundDeviceChooser")
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

    private fun returnToChat() {
        val roomId = withState(callViewModel) { it.roomId }
        val args = TimelineArgs(roomId)
        val intent = RoomDetailActivity.newIntent(this, args, false).apply {
            flags = FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        // is it needed?
        finish()
    }

    override fun didTapMore() {
        CallControlsBottomSheet().show(supportFragmentManager, "Controls")
    }

    // Needed to let you answer call when phone is locked
    private fun turnScreenOnAndKeyguardOff() {
        Timber.tag(loggerTag.value).v("turnScreenOnAndKeyguardOff")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        with(getSystemService<KeyguardManager>()!!) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requestDismissKeyguard(this@VectorCallActivity, null)
            }
        }
    }

    private fun turnScreenOffAndKeyguardOn() {
        Timber.tag(loggerTag.value).v("turnScreenOnAndKeyguardOn")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(false)
            setTurnScreenOn(false)
        } else {
            @Suppress("DEPRECATION")
            window.clearFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    private val screenSharingPermissionActivityResultLauncher = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // We need to start a foreground service with a sticky notification during screen sharing
                startScreenSharingService(activityResult)
            } else {
                startScreenSharing(activityResult)
            }
        }
    }

    private fun startScreenSharing(activityResult: ActivityResult) {
        val videoCapturer = ScreenCapturerAndroid(activityResult.data, object : MediaProjection.Callback() {
            override fun onStop() {
                Timber.i("User revoked the screen capturing permission")
            }
        })
        callViewModel.handle(VectorCallViewActions.StartScreenSharing(videoCapturer))
    }

    private fun startScreenSharingService(activityResult: ActivityResult) {
        ContextCompat.startForegroundService(
                this,
                Intent(this, ScreenCaptureAndroidService::class.java)
        )
        bindToScreenCaptureService(activityResult)
    }

    private fun bindToScreenCaptureService(activityResult: ActivityResult? = null) {
        screenCaptureServiceConnection.bind(object : ScreenCaptureServiceConnection.Callback {
            override fun onServiceConnected() {
                activityResult?.let { startScreenSharing(it) }
            }
        })
    }

    private fun handleShowScreenSharingPermissionDialog() {
        getSystemService<MediaProjectionManager>()?.let {
            navigator.openScreenSharingPermissionDialog(it.createScreenCaptureIntent(), screenSharingPermissionActivityResultLauncher)
        }
    }

    private fun handleStopScreenSharingService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            screenCaptureServiceConnection.stopScreenCapturing()
        }
    }

    companion object {
        private const val EXTRA_MODE = "EXTRA_MODE"
        private const val FRAGMENT_DIAL_PAD_TAG = "FRAGMENT_DIAL_PAD_TAG"

        const val OUTGOING_CREATED = "OUTGOING_CREATED"
        const val INCOMING_RINGING = "INCOMING_RINGING"
        const val INCOMING_ACCEPT = "INCOMING_ACCEPT"

        fun newIntent(context: Context, call: WebRtcCall, mode: String?): Intent {
            val callArgs = CallArgs(call.nativeRoomId, call.callId, call.mxCall.opponentUserId, !call.mxCall.isOutgoing, call.mxCall.isVideoCall)
            return Intent(context, VectorCallActivity::class.java).apply {
                // what could be the best flags?
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(Mavericks.KEY_ARG, callArgs)
                putExtra(EXTRA_MODE, mode)
            }
        }

        fun newIntent(
                context: Context,
                callId: String,
                signalingRoomId: String,
                otherUserId: String,
                isIncomingCall: Boolean,
                isVideoCall: Boolean,
                mode: String?
        ): Intent {
            val callArgs = CallArgs(signalingRoomId, callId, otherUserId, isIncomingCall, isVideoCall)
            return Intent(context, VectorCallActivity::class.java).apply {
                // what could be the best flags?
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(Mavericks.KEY_ARG, callArgs)
                putExtra(EXTRA_MODE, mode)
            }
        }
    }
}
