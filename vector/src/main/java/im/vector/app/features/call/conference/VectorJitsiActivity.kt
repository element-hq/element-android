/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.conference

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.viewModel
import com.facebook.react.modules.core.PermissionListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityJitsiBinding
import im.vector.lib.core.utils.compat.getParcelableExtraCompat
import im.vector.lib.strings.CommonStrings
import kotlinx.parcelize.Parcelize
import org.jitsi.meet.sdk.BroadcastIntentHelper
import org.jitsi.meet.sdk.JitsiMeet
import org.jitsi.meet.sdk.JitsiMeetActivityDelegate
import org.jitsi.meet.sdk.JitsiMeetActivityInterface
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import org.jitsi.meet.sdk.JitsiMeetOngoingConferenceService
import org.jitsi.meet.sdk.JitsiMeetView
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.util.JsonDict
import timber.log.Timber
import java.net.URL

@AndroidEntryPoint
class VectorJitsiActivity : VectorBaseActivity<ActivityJitsiBinding>(), JitsiMeetActivityInterface {

    @Parcelize
    data class Args(
            val roomId: String,
            val widgetId: String,
            val enableVideo: Boolean
    ) : Parcelable

    override fun getBinding() = ActivityJitsiBinding.inflate(layoutInflater)

    private var jitsiMeetView: JitsiMeetView? = null

    private val jitsiViewModel: JitsiCallViewModel by viewModel()

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val intent = Intent("onConfigurationChanged")
        intent.putExtra("newConfig", newConfig)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addOnPictureInPictureModeChangedListener(pictureInPictureModeChangedInfoConsumer)

        jitsiViewModel.onEach {
            renderState(it)
        }

        jitsiViewModel.observeViewEvents {
            when (it) {
                is JitsiCallViewEvents.JoinConference -> configureJitsiView(it)
                is JitsiCallViewEvents.ConfirmSwitchingConference -> handleConfirmSwitching(it)
                JitsiCallViewEvents.FailJoiningConference -> handleFailJoining()
                JitsiCallViewEvents.Finish -> finish()
                JitsiCallViewEvents.LeaveConference -> handleLeaveConference()
            }
        }
        lifecycle.addObserver(ConferenceEventObserver(this, this::onBroadcastEvent))
    }

    override fun onResume() {
        super.onResume()
        JitsiMeetActivityDelegate.onHostResume(this)
    }

    override fun initUiAndData() {
        super.initUiAndData()
        jitsiMeetView = JitsiMeetView(this)
        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        views.jitsiLayout.addView(jitsiMeetView, params)
    }

    override fun onStop() {
        JitsiMeetActivityDelegate.onHostPause(this)
        super.onStop()
    }

    override fun onDestroy() {
        val currentConf = JitsiMeet.getCurrentConference()
        handleLeaveConference()
        jitsiMeetView?.dispose()
        // Fake emitting CONFERENCE_TERMINATED event when currentConf is not null (probably when closing the PiP screen).
        if (currentConf != null) {
            ConferenceEventEmitter(this).emitConferenceEnded()
        }
        JitsiMeetOngoingConferenceService.abort(this)
        JitsiMeetActivityDelegate.onHostDestroy(this)
        removeOnPictureInPictureModeChangedListener(pictureInPictureModeChangedInfoConsumer)
        super.onDestroy()
    }

    // Activity lifecycle methods
    //
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        JitsiMeetActivityDelegate.onActivityResult(this, requestCode, resultCode, data)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        JitsiMeetActivityDelegate.onBackPressed()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            jitsiMeetView?.enterPictureInPicture()
        }
    }

    private fun handleLeaveConference() {
        val leaveBroadcastIntent = BroadcastIntentHelper.buildHangUpIntent()
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(leaveBroadcastIntent)
    }

    private fun handleConfirmSwitching(action: JitsiCallViewEvents.ConfirmSwitchingConference) {
        MaterialAlertDialogBuilder(this)
                .setTitle(CommonStrings.dialog_title_warning)
                .setMessage(CommonStrings.jitsi_leave_conf_to_join_another_one_content)
                .setPositiveButton(CommonStrings.action_switch) { _, _ ->
                    jitsiViewModel.handle(JitsiCallViewActions.SwitchTo(action.args, false))
                }
                .setNegativeButton(CommonStrings.action_cancel, null)
                .show()
    }

    private val pictureInPictureModeChangedInfoConsumer = Consumer<PictureInPictureModeChangedInfo> {
        checkIfActivityShouldBeFinished()
        Timber.w("onPictureInPictureModeChanged(${it.isInPictureInPictureMode})")
    }

    private fun checkIfActivityShouldBeFinished() {
        // OnStop is called when PiP mode is closed directly from the ui
        // If stopped is called and PiP mode is not active, we should finish the activity and remove the task as Android creates a new one for PiP.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && !isInPictureInPictureMode) {
            finishAndRemoveTask()
        }
    }

    private fun renderState(viewState: JitsiCallViewState) {
        when (viewState.widget) {
            is Fail -> finish()
            is Success -> {
                views.jitsiProgressLayout.isVisible = false
                jitsiMeetView?.isVisible = true
            }
            else -> {
                jitsiMeetView?.isVisible = false
                views.jitsiProgressLayout.isVisible = true
            }
        }
    }

    private fun handleFailJoining() {
        Toast.makeText(this, getString(CommonStrings.error_jitsi_join_conf), Toast.LENGTH_LONG).show()
        finish()
    }

    private fun configureJitsiView(joinConference: JitsiCallViewEvents.JoinConference) {
        val jitsiMeetConferenceOptions = JitsiMeetConferenceOptions.Builder()
                .setVideoMuted(!joinConference.enableVideo)
                .setUserInfo(joinConference.userInfo)
                .setToken(joinConference.token)
                .apply {
                    tryOrNull { URL(joinConference.jitsiUrl) }?.let {
                        setServerURL(it)
                    }
                }
                // https://github.com/jitsi/jitsi-meet/blob/master/react/features/base/flags/constants.ts
                .setFeatureFlag("chat.enabled", false)
                .setFeatureFlag("invite.enabled", false)
                .setFeatureFlag("add-people.enabled", false)
                .setFeatureFlag("video-share.enabled", false)
                .setFeatureFlag("call-integration.enabled", false)
                .setRoom(joinConference.confId)
                .setSubject(joinConference.subject)
                .build()
        jitsiMeetView?.join(jitsiMeetConferenceOptions)
    }

    override fun onNewIntent(intent: Intent) {
        JitsiMeetActivityDelegate.onNewIntent(intent)

        // Is it a switch to another conf?
        intent.takeIf { it.hasExtra(Mavericks.KEY_ARG) }
                ?.let { intent.getParcelableExtraCompat<Args>(Mavericks.KEY_ARG) }
                ?.let {
                    jitsiViewModel.handle(JitsiCallViewActions.SwitchTo(it, true))
                }

        super.onNewIntent(intent)
    }

    override fun requestPermissions(permissions: Array<out String>?, requestCode: Int, listener: PermissionListener?) {
        JitsiMeetActivityDelegate.requestPermissions(this, permissions, requestCode, listener)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        JitsiMeetActivityDelegate.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun onBroadcastEvent(event: ConferenceEvent) {
        Timber.v("Broadcast received: $event")
        when (event) {
            is ConferenceEvent.Terminated -> onConferenceTerminated(event.data)
            is ConferenceEvent.Joined -> onConferenceJoined(event.data)
            is ConferenceEvent.ReadyToClose -> onReadyToClose()
            is ConferenceEvent.WillJoin -> Unit
        }
    }

    private fun onConferenceJoined(extraData: Map<String, Any>) {
        // Launch the service for the ongoing notification.
        JitsiMeetOngoingConferenceService.launch(this, HashMap(extraData))
    }

    private fun onConferenceTerminated(data: JsonDict) {
        Timber.v("JitsiMeetViewListener.onConferenceTerminated()")
        // Do not finish if there is an error
        if (data["error"] == null) {
            jitsiViewModel.handle(JitsiCallViewActions.OnConferenceLeft)
        }
    }

    private fun onReadyToClose() {
        Timber.v("SDK is ready to close")
        finish()
    }

    companion object {
        fun newIntent(context: Context, roomId: String, widgetId: String, enableVideo: Boolean): Intent {
            return Intent(context, VectorJitsiActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, Args(roomId, widgetId, enableVideo))
            }
        }
    }
}
