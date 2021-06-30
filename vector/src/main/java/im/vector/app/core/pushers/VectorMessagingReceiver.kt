/*
 * Copyright 2019 New Vector Ltd
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.core.pushers

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import im.vector.app.BuildConfig
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.extensions.vectorComponent
import im.vector.app.core.network.WifiDetector
import im.vector.app.features.badge.BadgeProxy
import im.vector.app.features.notifications.NotifiableEventResolver
import im.vector.app.features.notifications.NotificationDrawerManager
import im.vector.app.features.notifications.NotificationUtils
import im.vector.app.features.settings.BackgroundSyncMode
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONException
import org.json.JSONObject
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.unifiedpush.android.connector.MessagingReceiver
import org.unifiedpush.android.connector.MessagingReceiverHandler
import timber.log.Timber

/**
 * Unifiedpush handler.
 */
val upHandler = object: MessagingReceiverHandler {

    private lateinit var notificationDrawerManager: NotificationDrawerManager
    private lateinit var notifiableEventResolver: NotifiableEventResolver
    private lateinit var pusherManager: PushersManager
    private lateinit var activeSessionHolder: ActiveSessionHolder
    private lateinit var vectorPreferences: VectorPreferences
    private lateinit var wifiDetector: WifiDetector

    private val coroutineScope = CoroutineScope(SupervisorJob())

    // UI handler
    private val mUIHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    fun initVar(context: Context) {
        with(context.vectorComponent()) {
            notificationDrawerManager = notificationDrawerManager()
            notifiableEventResolver = notifiableEventResolver()
            pusherManager = pusherManager()
            activeSessionHolder = activeSessionHolder()
            vectorPreferences = vectorPreferences()
            wifiDetector = wifiDetector()
        }
    }

    /**
     * Called when message is received.
     *
     * @param message the message
     * @param instance connection, for multi-account
     */
    override fun onMessage(context: Context?, message: String, instance: String) {
        initVar(context!!)
        if (BuildConfig.LOW_PRIVACY_LOG_ENABLE) {
            Timber.d("## onMessageReceived() %s", message)
        }
        Timber.d("## onMessage() received")

        lateinit var data: JSONObject
        lateinit var notification: JSONObject
        if (UPHelper.isEmbeddedDistributor(context)) {
            try {
                notification = JSONObject(message)
            } catch (e: JSONException) {
                Timber.e(e)
                return
            }
        } else {
            try {
                data = JSONObject(message)
                notification = data.getJSONObject("notification")
            } catch (e: JSONException) {
                Timber.e(e)
                return
            }
            try {
                notification.put("unread",
                        notification.getJSONObject("count").getInt("unread"))
            } catch (e: JSONException) {
                Timber.i("No unread on notification")
                notification.put("unread", 0)
            }
        }

        val eventId: String = try {
            notification.getString("event_id")
        } catch (e: JSONException) {
            Timber.i("No event_id on notification")
            notification.put("event_id", "")
            ""
        }

        try {
            notification.getString("room_id")
        } catch (e: JSONException) {
            Timber.i("No room_id on notification")
            notification.put("room_id", "")
        }

        // Diagnostic Push
        if (eventId == PushersManager.TEST_EVENT_ID) {
            val intent = Intent(NotificationUtils.PUSH_ACTION)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            return
        }

        if (!vectorPreferences.areNotificationEnabledForDevice()) {
            Timber.i("Notification are disabled for this device")
            return
        }

        mUIHandler.post {
            if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                // we are in foreground, let the sync do the things?
                Timber.d("PUSH received in a foreground state, ignore")
            } else {
                onMessageReceivedInternal(context, notification)
            }
        }
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is also called
     * when the InstanceID token is initially generated, so this is where
     * you retrieve the token.
     */
    override fun onNewEndpoint(context: Context?, endpoint: String, instance: String) {
        initVar(context!!)
        Timber.i("onNewEndpoint: adding $endpoint")
        UPHelper.storeUpEndpoint(context, endpoint)
        if (vectorPreferences.areNotificationEnabledForDevice() && activeSessionHolder.hasActiveSession()) {
            val gateway = UPHelper.customOrDefaultGateway(context, endpoint)
            UPHelper.storePushGateway(context, gateway)
            UPHelper.storeUpEndpoint(context, endpoint)
            pusherManager.registerPusher(context, endpoint, gateway)
        }
        val mode = BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_DISABLED
        vectorPreferences.setFdroidSyncBackgroundMode(mode)
    }

    override fun onRegistrationFailed(context: Context?, instance: String) {
        Toast.makeText(context, "Push service registration failed", Toast.LENGTH_SHORT).show()
    }

    override fun onRegistrationRefused(context: Context?, instance: String) {
        Toast.makeText(context, "Push service registration refused by server", Toast.LENGTH_LONG).show()
    }

    override fun onUnregistered(context: Context?, instance: String) {
        Timber.d("Unifiedpush: Unregistered")
        initVar(context!!)
        val mode = BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_FOR_BATTERY
        vectorPreferences.setFdroidSyncBackgroundMode(mode)
        runBlocking {
            try {
                pusherManager.unregisterPusher(context, UPHelper.getUpEndpoint(context)!!)
            } catch (e: Exception) {
                Timber.d("Probably unregistering a non existant pusher")
            }
        }
    }

    /**
     * Internal receive method
     *
     * @param data Data json containing message data.
     */
    private fun onMessageReceivedInternal(context: Context, data: JSONObject) {
        try {
            if (BuildConfig.LOW_PRIVACY_LOG_ENABLE) {
                Timber.d("## onMessageReceivedInternal() : $data")
            }

            // update the badge counter
            val unreadCount = data.getInt("unread")
            BadgeProxy.updateBadgeCount(context, unreadCount)

            val session = activeSessionHolder.getSafeActiveSession()

            if (session == null) {
                Timber.w("## Can't sync from push, no current session")
            } else {
                val eventId = data.getString("event_id")
                val roomId = data.getString("room_id")

                if (isEventAlreadyKnown(eventId, roomId)) {
                    Timber.d("Ignoring push, event already known")
                } else {
                    // Try to get the Event content faster
                    Timber.d("Requesting event in fast lane")
                    getEventFastLane(session, roomId, eventId)

                    Timber.d("Requesting background sync")
                    session.requireBackgroundSync()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "## onMessageReceivedInternal() failed")
        }
    }

    private fun getEventFastLane(session: Session, roomId: String?, eventId: String?) {
        roomId?.takeIf { it.isNotEmpty() } ?: return
        eventId?.takeIf { it.isNotEmpty() } ?: return

        // If the room is currently displayed, we will not show a notification, so no need to get the Event faster
        if (notificationDrawerManager.shouldIgnoreMessageEventInRoom(roomId)) {
            return
        }

        if (wifiDetector.isConnectedToWifi().not()) {
            Timber.d("No WiFi network, do not get Event")
            return
        }

        coroutineScope.launch {
            Timber.d("Fast lane: start request")
            val event = tryOrNull { session.getEvent(roomId, eventId) } ?: return@launch

            val resolvedEvent = notifiableEventResolver.resolveInMemoryEvent(session, event)

            resolvedEvent
                    ?.also { Timber.d("Fast lane: notify drawer") }
                    ?.let {
                        it.isPushGatewayEvent = true
                        notificationDrawerManager.onNotifiableEventReceived(it)
                        notificationDrawerManager.refreshNotificationDrawer()
                    }
        }
    }

    // check if the event was not yet received
    // a previous catchup might have already retrieved the notified event
    private fun isEventAlreadyKnown(eventId: String?, roomId: String?): Boolean {
        if (null != eventId && null != roomId) {
            try {
                val session = activeSessionHolder.getSafeActiveSession() ?: return false
                val room = session.getRoom(roomId) ?: return false
                return room.getTimeLineEvent(eventId) != null
            } catch (e: Exception) {
                Timber.e(e, "## isEventAlreadyKnown() : failed to check if the event was already defined")
            }
        }
        return false
    }
}

class VectorMessagingReceiver : MessagingReceiver(upHandler)
