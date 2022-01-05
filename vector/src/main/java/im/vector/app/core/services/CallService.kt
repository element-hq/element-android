/*
 * Copyright 2019 New Vector Ltd
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.core.services

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver
import com.airbnb.mvrx.Mavericks
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.singletonEntryPoint
import im.vector.app.features.call.CallArgs
import im.vector.app.features.call.VectorCallActivity
import im.vector.app.features.call.telecom.CallConnection
import im.vector.app.features.call.webrtc.WebRtcCall
import im.vector.app.features.call.webrtc.WebRtcCallManager
import im.vector.app.features.call.webrtc.getOpponentAsMatrixItem
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.notifications.NotificationUtils
import im.vector.app.features.popup.IncomingCallAlert
import im.vector.app.features.popup.PopupAlertManager
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.room.model.call.EndCallReason
import org.matrix.android.sdk.api.util.MatrixItem
import timber.log.Timber
import javax.inject.Inject

private val loggerTag = LoggerTag("CallService", LoggerTag.VOIP)

/**
 * Foreground service to manage calls
 */
@AndroidEntryPoint
class CallService : VectorService() {

    private val connections = mutableMapOf<String, CallConnection>()
    private val knownCalls = mutableMapOf<String, CallInformation>()
    private val connectedCallIds = mutableSetOf<String>()

    private lateinit var notificationManager: NotificationManagerCompat
    @Inject lateinit var notificationUtils: NotificationUtils
    @Inject lateinit var callManager: WebRtcCallManager
    @Inject lateinit var avatarRenderer: AvatarRenderer
    @Inject lateinit var alertManager: PopupAlertManager

    private var callRingPlayerIncoming: CallRingPlayerIncoming? = null
    private var callRingPlayerOutgoing: CallRingPlayerOutgoing? = null

    // A media button receiver receives and helps translate hardware media playback buttons,
    // such as those found on wired and wireless headsets, into the appropriate callbacks in your app
    private var mediaSession: MediaSessionCompat? = null
    private val mediaSessionButtonCallback = object : MediaSessionCompat.Callback() {
        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            val keyEvent = mediaButtonEvent?.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return false
            if (keyEvent.keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
                callManager.headSetButtonTapped()
                return true
            }
            return false
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        callRingPlayerIncoming = CallRingPlayerIncoming(applicationContext, notificationUtils)
        callRingPlayerOutgoing = CallRingPlayerOutgoing(applicationContext, callManager)
    }

    override fun onDestroy() {
        super.onDestroy()
        callRingPlayerIncoming?.stop()
        callRingPlayerOutgoing?.stop()
        mediaSession?.release()
        mediaSession = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(loggerTag.value).v("onStartCommand $intent")
        if (mediaSession == null) {
            mediaSession = MediaSessionCompat(applicationContext, CallService::class.java.name).apply {
                setCallback(mediaSessionButtonCallback)
            }
        }
        mediaSession?.let {
            // This ensures that the correct callbacks to MediaSessionCompat.Callback
            // will be triggered based on the incoming KeyEvent.
            MediaButtonReceiver.handleIntent(it, intent)
        }

        when (intent?.action) {
            ACTION_INCOMING_RINGING_CALL -> {
                mediaSession?.isActive = true
                val fromBg = intent.getBooleanExtra(EXTRA_IS_IN_BG, false)
                callRingPlayerIncoming?.start(fromBg)
                displayIncomingCallNotification(intent)
            }
            ACTION_OUTGOING_RINGING_CALL -> {
                mediaSession?.isActive = true
                callRingPlayerOutgoing?.start()
                displayOutgoingRingingCallNotification(intent)
            }
            ACTION_ONGOING_CALL          -> {
                callRingPlayerIncoming?.stop()
                callRingPlayerOutgoing?.stop()
                displayCallInProgressNotification(intent)
            }
            ACTION_CALL_TERMINATED       -> {
                handleCallTerminated(intent)
            }
            else                         -> {
                handleUnexpectedState(null)
            }
        }

        // We want the system to restore the service if killed
        return START_REDELIVER_INTENT
    }

    // ================================================================================
    // Call notification management
    // ================================================================================

    /**
     * Display a permanent notification when there is an incoming call.
     *
     */
    private fun displayIncomingCallNotification(intent: Intent) {
        Timber.tag(loggerTag.value).v("displayIncomingCallNotification $intent")
        val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: ""
        val call = callManager.getCallById(callId) ?: return Unit.also {
            handleUnexpectedState(callId)
        }
        val callInformation = call.toCallInformation()
        val isVideoCall = call.mxCall.isVideoCall
        val fromBg = intent.getBooleanExtra(EXTRA_IS_IN_BG, false)
        Timber.tag(loggerTag.value).v("displayIncomingCallNotification : display the dedicated notification")
        val incomingCallAlert = IncomingCallAlert(callId,
                shouldBeDisplayedIn = { activity ->
                    if (activity is VectorCallActivity) {
                        activity.intent.getParcelableExtra<CallArgs>(Mavericks.KEY_ARG)?.callId != call.callId
                    } else true
                }
        ).apply {
            viewBinder = IncomingCallAlert.ViewBinder(
                    matrixItem = callInformation.opponentMatrixItem,
                    avatarRenderer = avatarRenderer,
                    isVideoCall = isVideoCall,
                    onAccept = { showCallScreen(call, VectorCallActivity.INCOMING_ACCEPT) },
                    onReject = { call.endCall() }
            )
            dismissedAction = Runnable { call.endCall() }
            contentAction = Runnable { showCallScreen(call, VectorCallActivity.INCOMING_RINGING) }
        }
        alertManager.postVectorAlert(incomingCallAlert)
        val notification = notificationUtils.buildIncomingCallNotification(
                call = call,
                title = callInformation.opponentMatrixItem?.getBestName() ?: callInformation.opponentUserId,
                fromBg = fromBg
        )
        if (knownCalls.isEmpty()) {
            startForeground(callId.hashCode(), notification)
        } else {
            notificationManager.notify(callId.hashCode(), notification)
        }
        knownCalls[callId] = callInformation
    }

    private fun handleCallTerminated(intent: Intent) {
        val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: ""
        val endCallReason = intent.getSerializableExtra(EXTRA_END_CALL_REASON) as EndCallReason
        val rejected = intent.getBooleanExtra(EXTRA_END_CALL_REJECTED, false)
        alertManager.cancelAlert(callId)
        val terminatedCall = knownCalls.remove(callId)
        if (terminatedCall == null) {
            Timber.tag(loggerTag.value).v("Call terminated for unknown call $callId")
            handleUnexpectedState(callId)
            return
        }
        val notification = notificationUtils.buildCallEndedNotification(false)
        val notificationId = callId.hashCode()
        startForeground(notificationId, notification)
        if (knownCalls.isEmpty()) {
            Timber.tag(loggerTag.value).v("No more call, stop the service")
            stopForeground(true)
            mediaSession?.isActive = false
            myStopSelf()
        }
        val wasConnected = connectedCallIds.remove(callId)
        if (!wasConnected && !terminatedCall.isOutgoing && !rejected && endCallReason != EndCallReason.ANSWERED_ELSEWHERE) {
            val missedCallNotification = notificationUtils.buildCallMissedNotification(terminatedCall)
            notificationManager.notify(MISSED_CALL_TAG, terminatedCall.nativeRoomId.hashCode(), missedCallNotification)
        }
    }

    private fun showCallScreen(call: WebRtcCall, mode: String) {
        val intent = VectorCallActivity.newIntent(
                context = this,
                call = call,
                mode = mode
        )
        startActivity(intent)
    }

    private fun displayOutgoingRingingCallNotification(intent: Intent) {
        val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: ""
        val call = callManager.getCallById(callId) ?: return Unit.also {
            handleUnexpectedState(callId)
        }
        val callInformation = call.toCallInformation()
        Timber.tag(loggerTag.value).v("displayOutgoingCallNotification : display the dedicated notification")
        val notification = notificationUtils.buildOutgoingRingingCallNotification(
                call = call,
                title = callInformation.opponentMatrixItem?.getBestName() ?: callInformation.opponentUserId
        )
        if (knownCalls.isEmpty()) {
            startForeground(callId.hashCode(), notification)
        } else {
            notificationManager.notify(callId.hashCode(), notification)
        }
        knownCalls[callId] = callInformation
    }

    /**
     * Display a call in progress notification.
     */
    private fun displayCallInProgressNotification(intent: Intent) {
        Timber.tag(loggerTag.value).v("displayCallInProgressNotification")
        val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: ""
        connectedCallIds.add(callId)
        val call = callManager.getCallById(callId) ?: return Unit.also {
            handleUnexpectedState(callId)
        }
        alertManager.cancelAlert(callId)
        val callInformation = call.toCallInformation()
        val notification = notificationUtils.buildPendingCallNotification(
                call = call,
                title = callInformation.opponentMatrixItem?.getBestName() ?: callInformation.opponentUserId
        )
        if (knownCalls.isEmpty()) {
            startForeground(callId.hashCode(), notification)
        } else {
            notificationManager.notify(callId.hashCode(), notification)
        }
        knownCalls[callId] = callInformation
    }

    private fun handleUnexpectedState(callId: String?) {
        Timber.tag(loggerTag.value).v("Fallback to clear everything")
        callRingPlayerIncoming?.stop()
        callRingPlayerOutgoing?.stop()
        val notification = notificationUtils.buildCallEndedNotification(false)
        if (callId != null) {
            startForeground(callId.hashCode(), notification)
        } else {
            startForeground(DEFAULT_NOTIFICATION_ID, notification)
        }
        if (knownCalls.isEmpty()) {
            mediaSession?.isActive = false
            myStopSelf()
        }
    }

    fun addConnection(callConnection: CallConnection) {
        connections[callConnection.callId] = callConnection
    }

    private fun WebRtcCall.toCallInformation(): CallInformation {
        return CallInformation(
                callId = this.callId,
                nativeRoomId = this.nativeRoomId,
                opponentUserId = this.mxCall.opponentUserId,
                opponentMatrixItem = singletonEntryPoint().activeSessionHolder().getSafeActiveSession()?.let {
                    this.getOpponentAsMatrixItem(it)
                },
                isVideoCall = this.mxCall.isVideoCall,
                isOutgoing = this.mxCall.isOutgoing
        )
    }

    data class CallInformation(
            val callId: String,
            val nativeRoomId: String,
            val opponentUserId: String,
            val opponentMatrixItem: MatrixItem?,
            val isVideoCall: Boolean,
            val isOutgoing: Boolean
    )

    companion object {
        private const val DEFAULT_NOTIFICATION_ID = 6480
        private const val MISSED_CALL_TAG = "MISSED_CALL_TAG"

        private const val ACTION_INCOMING_RINGING_CALL = "im.vector.app.core.services.CallService.ACTION_INCOMING_RINGING_CALL"
        private const val ACTION_OUTGOING_RINGING_CALL = "im.vector.app.core.services.CallService.ACTION_OUTGOING_RINGING_CALL"
        private const val ACTION_ONGOING_CALL = "im.vector.app.core.services.CallService.ACTION_ONGOING_CALL"
        private const val ACTION_CALL_TERMINATED = "im.vector.app.core.services.CallService.ACTION_CALL_TERMINATED"

        private const val EXTRA_CALL_ID = "EXTRA_CALL_ID"
        private const val EXTRA_IS_IN_BG = "EXTRA_IS_IN_BG"
        private const val EXTRA_END_CALL_REJECTED = "EXTRA_END_CALL_REJECTED"
        private const val EXTRA_END_CALL_REASON = "EXTRA_END_CALL_REASON"

        fun onIncomingCallRinging(context: Context,
                                  callId: String,
                                  isInBackground: Boolean) {
            val intent = Intent(context, CallService::class.java)
                    .apply {
                        action = ACTION_INCOMING_RINGING_CALL
                        putExtra(EXTRA_CALL_ID, callId)
                        putExtra(EXTRA_IS_IN_BG, isInBackground)
                    }
            ContextCompat.startForegroundService(context, intent)
        }

        fun onOutgoingCallRinging(context: Context,
                                  callId: String) {
            val intent = Intent(context, CallService::class.java)
                    .apply {
                        action = ACTION_OUTGOING_RINGING_CALL
                        putExtra(EXTRA_CALL_ID, callId)
                    }
            ContextCompat.startForegroundService(context, intent)
        }

        fun onPendingCall(context: Context,
                          callId: String) {
            val intent = Intent(context, CallService::class.java)
                    .apply {
                        action = ACTION_ONGOING_CALL
                        putExtra(EXTRA_CALL_ID, callId)
                    }
            ContextCompat.startForegroundService(context, intent)
        }

        fun onCallTerminated(context: Context,
                             callId: String,
                             endCallReason: EndCallReason,
                             rejected: Boolean) {
            val intent = Intent(context, CallService::class.java)
                    .apply {
                        action = ACTION_CALL_TERMINATED
                        putExtra(EXTRA_CALL_ID, callId)
                        putExtra(EXTRA_END_CALL_REASON, endCallReason)
                        putExtra(EXTRA_END_CALL_REJECTED, rejected)
                    }
            context.startService(intent)
        }
    }

    inner class CallServiceBinder : Binder() {
        fun getCallService(): CallService {
            return this@CallService
        }
    }
}
