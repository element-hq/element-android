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
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver
import im.vector.app.core.extensions.vectorComponent
import im.vector.app.features.call.WebRtcPeerConnectionManager
import im.vector.app.features.call.telecom.CallConnection
import im.vector.app.features.notifications.NotificationUtils
import timber.log.Timber

/**
 * Foreground service to manage calls
 */
class CallService : VectorService(), WiredHeadsetStateReceiver.HeadsetEventListener, BluetoothHeadsetReceiver.EventListener {

    private val connections = mutableMapOf<String, CallConnection>()

    private lateinit var notificationUtils: NotificationUtils
    private lateinit var webRtcPeerConnectionManager: WebRtcPeerConnectionManager

    private var callRingPlayer: CallRingPlayer? = null

    private var wiredHeadsetStateReceiver: WiredHeadsetStateReceiver? = null
    private var bluetoothHeadsetStateReceiver: BluetoothHeadsetReceiver? = null

    // A media button receiver receives and helps translate hardware media playback buttons,
    // such as those found on wired and wireless headsets, into the appropriate callbacks in your app
    private var mediaSession: MediaSessionCompat? = null
    private val mediaSessionButtonCallback = object : MediaSessionCompat.Callback() {
        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            val keyEvent = mediaButtonEvent?.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return false
            if (keyEvent.keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
                webRtcPeerConnectionManager.headSetButtonTapped()
                return true
            }
            return false
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationUtils = vectorComponent().notificationUtils()
        webRtcPeerConnectionManager = vectorComponent().webRtcPeerConnectionManager()
        callRingPlayer = CallRingPlayer(applicationContext)
        wiredHeadsetStateReceiver = WiredHeadsetStateReceiver.createAndRegister(this, this)
        bluetoothHeadsetStateReceiver = BluetoothHeadsetReceiver.createAndRegister(this, this)
    }

    override fun onDestroy() {
        super.onDestroy()
        callRingPlayer?.stop()
        wiredHeadsetStateReceiver?.let { WiredHeadsetStateReceiver.unRegister(this, it) }
        wiredHeadsetStateReceiver = null
        bluetoothHeadsetStateReceiver?.let { BluetoothHeadsetReceiver.unRegister(this, it) }
        bluetoothHeadsetStateReceiver = null
        mediaSession?.release()
        mediaSession = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.v("## VOIP onStartCommand $intent")
        if (mediaSession == null) {
            mediaSession = MediaSessionCompat(applicationContext, CallService::class.java.name).apply {
                setCallback(mediaSessionButtonCallback)
            }
        }
        if (intent == null) {
            // Service started again by the system.
            // TODO What do we do here?
            return START_STICKY
        }
        mediaSession?.let {
            // This ensures that the correct callbacks to MediaSessionCompat.Callback
            // will be triggered based on the incoming KeyEvent.
            MediaButtonReceiver.handleIntent(it, intent)
        }

        when (intent.action) {
            ACTION_INCOMING_RINGING_CALL -> {
                mediaSession?.isActive = true
                callRingPlayer?.start()
                displayIncomingCallNotification(intent)
            }
            ACTION_OUTGOING_RINGING_CALL -> {
                mediaSession?.isActive = true
                callRingPlayer?.start()
                displayOutgoingRingingCallNotification(intent)
            }
            ACTION_ONGOING_CALL          -> {
                callRingPlayer?.stop()
                displayCallInProgressNotification(intent)
            }
            ACTION_NO_ACTIVE_CALL        -> hideCallNotifications()
            ACTION_CALL_CONNECTING       -> {
                // lower notification priority
                displayCallInProgressNotification(intent)
                // stop ringing
                callRingPlayer?.stop()
            }
            ACTION_ONGOING_CALL_BG       -> {
                // there is an ongoing call but call activity is in background
                displayCallOnGoingInBackground(intent)
            }
            else                         -> {
                // Should not happen
                callRingPlayer?.stop()
                myStopSelf()
            }
        }

        // We want the system to restore the service if killed
        return START_STICKY
    }

    // ================================================================================
    // Call notification management
    // ================================================================================

    /**
     * Display a permanent notification when there is an incoming call.
     *
     * @param session  the session
     * @param isVideo  true if this is a video call, false for voice call
     * @param room     the room
     * @param callId   the callId
     */
    private fun displayIncomingCallNotification(intent: Intent) {
        Timber.v("## VOIP displayIncomingCallNotification $intent")

        // the incoming call in progress is already displayed
//        if (!TextUtils.isEmpty(mIncomingCallId)) {
//            Timber.v("displayIncomingCallNotification : the incoming call in progress is already displayed")
//        } else if (!TextUtils.isEmpty(mCallIdInProgress)) {
//            Timber.v("displayIncomingCallNotification : a 'call in progress' notification is displayed")
//        } else
// //            if (null == webRtcPeerConnectionManager.currentCall)
//        {
        val callId = intent.getStringExtra(EXTRA_CALL_ID)

        Timber.v("displayIncomingCallNotification : display the dedicated notification")
        val notification = notificationUtils.buildIncomingCallNotification(
                intent.getBooleanExtra(EXTRA_IS_VIDEO, false),
                intent.getStringExtra(EXTRA_ROOM_NAME) ?: "",
                intent.getStringExtra(EXTRA_ROOM_ID) ?: "",
                callId ?: "")
        startForeground(NOTIFICATION_ID, notification)

//            mIncomingCallId = callId

        // turn the screen on for 3 seconds
//            if (Matrix.getInstance(VectorApp.getInstance())!!.pushManager.isScreenTurnedOn) {
//                try {
//                    val pm = getSystemService<PowerManager>()!!
//                    val wl = pm.newWakeLock(
//                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or PowerManager.ACQUIRE_CAUSES_WAKEUP,
//                            CallService::class.java.simpleName)
//                    wl.acquire(3000)
//                    wl.release()
//                } catch (re: RuntimeException) {
//                    Timber.e(re, "displayIncomingCallNotification : failed to turn screen on ")
//                }
//
//            }
//        }
//        else {
//            Timber.i("displayIncomingCallNotification : do not display the incoming call notification because there is a pending call")
//        }
    }

    private fun displayOutgoingRingingCallNotification(intent: Intent) {
        val callId = intent.getStringExtra(EXTRA_CALL_ID)

        Timber.v("displayOutgoingCallNotification : display the dedicated notification")
        val notification = notificationUtils.buildOutgoingRingingCallNotification(
                intent.getBooleanExtra(EXTRA_IS_VIDEO, false),
                intent.getStringExtra(EXTRA_ROOM_NAME) ?: "",
                intent.getStringExtra(EXTRA_ROOM_ID) ?: "",
                callId ?: "")
        startForeground(NOTIFICATION_ID, notification)
    }

    /**
     * Display a call in progress notification.
     */
    private fun displayCallInProgressNotification(intent: Intent) {
        Timber.v("## VOIP displayCallInProgressNotification")
        val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: ""

        val notification = notificationUtils.buildPendingCallNotification(
                intent.getBooleanExtra(EXTRA_IS_VIDEO, false),
                intent.getStringExtra(EXTRA_ROOM_NAME) ?: "",
                intent.getStringExtra(EXTRA_ROOM_ID) ?: "",
                intent.getStringExtra(EXTRA_MATRIX_ID) ?: "",
                callId)

        startForeground(NOTIFICATION_ID, notification)

        // mCallIdInProgress = callId
    }

    /**
     * Display a call in progress notification.
     */
    private fun displayCallOnGoingInBackground(intent: Intent) {
        Timber.v("## VOIP displayCallInProgressNotification")
        val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: ""

        val notification = notificationUtils.buildPendingCallNotification(
                isVideo = intent.getBooleanExtra(EXTRA_IS_VIDEO, false),
                roomName = intent.getStringExtra(EXTRA_ROOM_NAME) ?: "",
                roomId = intent.getStringExtra(EXTRA_ROOM_ID) ?: "",
                matrixId = intent.getStringExtra(EXTRA_MATRIX_ID) ?: "",
                callId = callId,
                fromBg = true)

        startForeground(NOTIFICATION_ID, notification)

        // mCallIdInProgress = callId
    }

    /**
     * Hide the permanent call notifications
     */
    private fun hideCallNotifications() {
        val notification = notificationUtils.buildCallEndedNotification()

        mediaSession?.isActive = false
        // It's mandatory to startForeground to avoid crash
        startForeground(NOTIFICATION_ID, notification)

        myStopSelf()
    }

    fun addConnection(callConnection: CallConnection) {
        connections[callConnection.callId] = callConnection
    }

    companion object {
        private const val NOTIFICATION_ID = 6480

        private const val ACTION_INCOMING_RINGING_CALL = "im.vector.app.core.services.CallService.ACTION_INCOMING_RINGING_CALL"
        private const val ACTION_OUTGOING_RINGING_CALL = "im.vector.app.core.services.CallService.ACTION_OUTGOING_RINGING_CALL"
        private const val ACTION_CALL_CONNECTING = "im.vector.app.core.services.CallService.ACTION_CALL_CONNECTING"
        private const val ACTION_ONGOING_CALL = "im.vector.app.core.services.CallService.ACTION_ONGOING_CALL"
        private const val ACTION_ONGOING_CALL_BG = "im.vector.app.core.services.CallService.ACTION_ONGOING_CALL_BG"
        private const val ACTION_NO_ACTIVE_CALL = "im.vector.app.core.services.CallService.NO_ACTIVE_CALL"
//        private const val ACTION_ACTIVITY_VISIBLE = "im.vector.app.core.services.CallService.ACTION_ACTIVITY_VISIBLE"
//        private const val ACTION_STOP_RINGING = "im.vector.app.core.services.CallService.ACTION_STOP_RINGING"

        private const val EXTRA_IS_VIDEO = "EXTRA_IS_VIDEO"
        private const val EXTRA_ROOM_NAME = "EXTRA_ROOM_NAME"
        private const val EXTRA_ROOM_ID = "EXTRA_ROOM_ID"
        private const val EXTRA_MATRIX_ID = "EXTRA_MATRIX_ID"
        private const val EXTRA_CALL_ID = "EXTRA_CALL_ID"

        fun onIncomingCallRinging(context: Context,
                                  isVideo: Boolean,
                                  roomName: String,
                                  roomId: String,
                                  matrixId: String,
                                  callId: String) {
            val intent = Intent(context, CallService::class.java)
                    .apply {
                        action = ACTION_INCOMING_RINGING_CALL
                        putExtra(EXTRA_IS_VIDEO, isVideo)
                        putExtra(EXTRA_ROOM_NAME, roomName)
                        putExtra(EXTRA_ROOM_ID, roomId)
                        putExtra(EXTRA_MATRIX_ID, matrixId)
                        putExtra(EXTRA_CALL_ID, callId)
                    }

            ContextCompat.startForegroundService(context, intent)
        }

        fun onOnGoingCallBackground(context: Context,
                                    isVideo: Boolean,
                                    roomName: String,
                                    roomId: String,
                                    matrixId: String,
                                    callId: String) {
            val intent = Intent(context, CallService::class.java)
                    .apply {
                        action = ACTION_ONGOING_CALL_BG
                        putExtra(EXTRA_IS_VIDEO, isVideo)
                        putExtra(EXTRA_ROOM_NAME, roomName)
                        putExtra(EXTRA_ROOM_ID, roomId)
                        putExtra(EXTRA_MATRIX_ID, matrixId)
                        putExtra(EXTRA_CALL_ID, callId)
                    }

            ContextCompat.startForegroundService(context, intent)
        }

        fun onOutgoingCallRinging(context: Context,
                                  isVideo: Boolean,
                                  roomName: String,
                                  roomId: String,
                                  matrixId: String,
                                  callId: String) {
            val intent = Intent(context, CallService::class.java)
                    .apply {
                        action = ACTION_OUTGOING_RINGING_CALL
                        putExtra(EXTRA_IS_VIDEO, isVideo)
                        putExtra(EXTRA_ROOM_NAME, roomName)
                        putExtra(EXTRA_ROOM_ID, roomId)
                        putExtra(EXTRA_MATRIX_ID, matrixId)
                        putExtra(EXTRA_CALL_ID, callId)
                    }

            ContextCompat.startForegroundService(context, intent)
        }

        fun onPendingCall(context: Context,
                          isVideo: Boolean,
                          roomName: String,
                          roomId: String,
                          matrixId: String,
                          callId: String) {
            val intent = Intent(context, CallService::class.java)
                    .apply {
                        action = ACTION_ONGOING_CALL
                        putExtra(EXTRA_IS_VIDEO, isVideo)
                        putExtra(EXTRA_ROOM_NAME, roomName)
                        putExtra(EXTRA_ROOM_ID, roomId)
                        putExtra(EXTRA_MATRIX_ID, matrixId)
                        putExtra(EXTRA_CALL_ID, callId)
                    }

            ContextCompat.startForegroundService(context, intent)
        }

        fun onNoActiveCall(context: Context) {
            val intent = Intent(context, CallService::class.java)
                    .apply {
                        action = ACTION_NO_ACTIVE_CALL
                    }

            ContextCompat.startForegroundService(context, intent)
        }
    }

    inner class CallServiceBinder : Binder() {
        fun getCallService(): CallService {
            return this@CallService
        }
    }

    override fun onHeadsetEvent(event: WiredHeadsetStateReceiver.HeadsetPlugEvent) {
        Timber.v("## VOIP: onHeadsetEvent $event")
        webRtcPeerConnectionManager.onWiredDeviceEvent(event)
    }

    override fun onBTHeadsetEvent(event: BluetoothHeadsetReceiver.BTHeadsetPlugEvent) {
        Timber.v("## VOIP: onBTHeadsetEvent $event")
        webRtcPeerConnectionManager.onWirelessDeviceEvent(event)
    }
}
