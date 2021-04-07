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

package im.vector.app.gplay.push.fcm

import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import im.vector.app.BuildConfig
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.extensions.vectorComponent
import im.vector.app.core.network.WifiDetector
import im.vector.app.core.pushers.PushersManager
import im.vector.app.features.badge.BadgeProxy
import im.vector.app.features.notifications.NotifiableEventResolver
import im.vector.app.features.notifications.NotifiableMessageEvent
import im.vector.app.features.notifications.NotificationDrawerManager
import im.vector.app.features.notifications.NotificationUtils
import im.vector.app.features.notifications.SimpleNotifiableEvent
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.push.fcm.FcmHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.pushrules.Action
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.Event
import timber.log.Timber

/**
 * Class extending FirebaseMessagingService.
 */
class VectorFirebaseMessagingService : FirebaseMessagingService() {

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

    override fun onCreate() {
        super.onCreate()
        with(vectorComponent()) {
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
     */
    override fun onMessageReceived(message: RemoteMessage) {
        if (BuildConfig.LOW_PRIVACY_LOG_ENABLE) {
            Timber.d("## onMessageReceived() %s", message.data.toString())
        }
        Timber.d("## onMessageReceived() from FCM with priority %s", message.priority)

        // Diagnostic Push
        if (message.data["event_id"] == PushersManager.TEST_EVENT_ID) {
            val intent = Intent(NotificationUtils.PUSH_ACTION)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
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
                onMessageReceivedInternal(message.data)
            }
        }
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is also called
     * when the InstanceID token is initially generated, so this is where
     * you retrieve the token.
     */
    override fun onNewToken(refreshedToken: String) {
        Timber.i("onNewToken: FCM Token has been updated")
        FcmHelper.storeFcmToken(this, refreshedToken)
        if (vectorPreferences.areNotificationEnabledForDevice() && activeSessionHolder.hasActiveSession()) {
            pusherManager.registerPusherWithFcmKey(refreshedToken)
        }
    }

    /**
     * Called when the FCM server deletes pending messages. This may be due to:
     *      - Too many messages stored on the FCM server.
     *          This can occur when an app's servers send a bunch of non-collapsible messages to FCM servers while the device is offline.
     *      - The device hasn't connected in a long time and the app server has recently (within the last 4 weeks)
     *          sent a message to the app on that device.
     *
     *  It is recommended that the app do a full sync with the app server after receiving this call.
     */
    override fun onDeletedMessages() {
        Timber.v("## onDeletedMessages()")
    }

    /**
     * Internal receive method
     *
     * @param data Data map containing message data as key/value pairs.
     * For Set of keys use data.keySet().
     */
    private fun onMessageReceivedInternal(data: Map<String, String>) {
        try {
            if (BuildConfig.LOW_PRIVACY_LOG_ENABLE) {
                Timber.d("## onMessageReceivedInternal() : $data")
            } else {
                Timber.d("## onMessageReceivedInternal() : $data")
            }

            // update the badge counter
            val unreadCount = data["unread"]?.let { Integer.parseInt(it) } ?: 0
            BadgeProxy.updateBadgeCount(applicationContext, unreadCount)

            val session = activeSessionHolder.getSafeActiveSession()

            if (session == null) {
                Timber.w("## Can't sync from push, no current session")
            } else {
                val eventId = data["event_id"]
                val roomId = data["room_id"]

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

    private fun handleNotificationWithoutSyncingMode(data: Map<String, String>, session: Session?) {
        if (session == null) {
            Timber.e("## handleNotificationWithoutSyncingMode cannot find session")
            return
        }

        // The Matrix event ID of the event being notified about.
        // This is required if the notification is about a particular Matrix event.
        // It may be omitted for notifications that only contain updated badge counts.
        // This ID can and should be used to detect duplicate notification requests.
        val eventId = data["event_id"] ?: return // Just ignore

        val eventType = data["type"]
        if (eventType == null) {
            // Just add a generic unknown event
            val simpleNotifiableEvent = SimpleNotifiableEvent(
                    session.myUserId,
                    eventId,
                    null,
                    true, // It's an issue in this case, all event will bing even if expected to be silent.
                    title = getString(R.string.notification_unknown_new_event),
                    description = "",
                    type = null,
                    timestamp = System.currentTimeMillis(),
                    soundName = Action.ACTION_OBJECT_VALUE_VALUE_DEFAULT,
                    isPushGatewayEvent = true
            )
            notificationDrawerManager.onNotifiableEventReceived(simpleNotifiableEvent)
            notificationDrawerManager.refreshNotificationDrawer()
        } else {
            val event = parseEvent(data) ?: return

            val notifiableEvent = notifiableEventResolver.resolveEvent(event, session)

            if (notifiableEvent == null) {
                Timber.e("Unsupported notifiable event $eventId")
                if (BuildConfig.LOW_PRIVACY_LOG_ENABLE) {
                    Timber.e("--> $event")
                }
            } else {
                if (notifiableEvent is NotifiableMessageEvent) {
                    if (notifiableEvent.senderName.isNullOrEmpty()) {
                        notifiableEvent.senderName = data["sender_display_name"] ?: data["sender"] ?: ""
                    }
                    if (notifiableEvent.roomName.isNullOrEmpty()) {
                        notifiableEvent.roomName = findRoomNameBestEffort(data, session) ?: ""
                    }
                }

                notifiableEvent.isPushGatewayEvent = true
                notifiableEvent.matrixID = session.myUserId
                notificationDrawerManager.onNotifiableEventReceived(notifiableEvent)
                notificationDrawerManager.refreshNotificationDrawer()
            }
        }
    }

    private fun findRoomNameBestEffort(data: Map<String, String>, session: Session?): String? {
        var roomName: String? = data["room_name"]
        val roomId = data["room_id"]
        if (null == roomName && null != roomId) {
            // Try to get the room name from our store
            roomName = session?.getRoom(roomId)?.roomSummary()?.displayName
        }
        return roomName
    }

    /**
     * Try to create an event from the FCM data
     *
     * @param data the FCM data
     * @return the event or null if required data are missing
     */
    private fun parseEvent(data: Map<String, String>?): Event? {
        return Event(
                eventId = data?.get("event_id") ?: return null,
                senderId = data["sender"],
                roomId = data["room_id"] ?: return null,
                type = data["type"] ?: return null,
                originServerTs = System.currentTimeMillis()
        )
    }
}
