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

package im.vector.riotredesign.gplay.push.fcm

import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.riotredesign.BuildConfig
import im.vector.riotredesign.R
import im.vector.riotredesign.core.preference.BingRule
import im.vector.riotredesign.core.pushers.PushersManager
import im.vector.riotredesign.features.badge.BadgeProxy
import im.vector.riotredesign.features.notifications.NotifiableEventResolver
import im.vector.riotredesign.features.notifications.NotifiableMessageEvent
import im.vector.riotredesign.features.notifications.NotificationDrawerManager
import im.vector.riotredesign.features.notifications.SimpleNotifiableEvent
import im.vector.riotredesign.features.settings.PreferencesManager
import im.vector.riotredesign.push.fcm.FcmHelper
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * Class extending FirebaseMessagingService.
 */
class VectorFirebaseMessagingService : FirebaseMessagingService() {

    private val notificationDrawerManager by inject<NotificationDrawerManager>()
    private val pusherManager by inject<PushersManager>()

    private val notifiableEventResolver by inject<NotifiableEventResolver>()
    // UI handler
    private val mUIHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    /**
     * Called when message is received.
     *
     * @param message the message
     */
    override fun onMessageReceived(message: RemoteMessage?) {
        if (!PreferencesManager.areNotificationEnabledForDevice(applicationContext)) {
            Timber.i("Notification are disabled for this device")
            return
        }

        if (message == null || message.data == null) {
            Timber.e("## onMessageReceived() : received a null message or message with no data")
            return
        }
        if (BuildConfig.LOW_PRIVACY_LOG_ENABLE) {
            Timber.i("## onMessageReceived() %s", message.data.toString())
            Timber.i("## onMessageReceived() from FCM with priority %s", message.priority)
        }
        mUIHandler.post {
            if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                //we are in foreground, let the sync do the things?
                Timber.v("PUSH received in a foreground state, ignore")
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
    override fun onNewToken(refreshedToken: String?) {
        if (Matrix.getInstance().currentSession == null) return
        Timber.i("onNewToken: FCM Token has been updated")
        FcmHelper.storeFcmToken(this, refreshedToken)
        if (refreshedToken == null) {
            Timber.w("onNewToken:received null token")
        } else {
            if (PreferencesManager.areNotificationEnabledForDevice(applicationContext)) {
                pusherManager.registerPusherWithFcmKey(refreshedToken)
            }
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
                Timber.i("## onMessageReceivedInternal() : $data")
            }
            val eventId = data["event_id"]
            val roomId = data["room_id"]
            if (eventId == null || roomId == null) {
                Timber.e("## onMessageReceivedInternal() missing eventId and/or roomId")
                return
            }
            // update the badge counter
            val unreadCount = data.get("unread")?.let { Integer.parseInt(it) } ?: 0
            BadgeProxy.updateBadgeCount(applicationContext, unreadCount)

            val session = safeGetCurrentSession()

            if (session == null) {
                Timber.w("## Can't sync from push, no current session")
            } else {
                if (isEventAlreadyKnown(eventId, roomId)) {
                    Timber.i("Ignoring push, event already knwown")
                } else {
                    Timber.v("Requesting background sync")
                    session.requireBackgroundSync()
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "## onMessageReceivedInternal() failed : " + e.message)
        }
    }

    fun safeGetCurrentSession(): Session? {
        try {
            return Matrix.getInstance().currentSession
        } catch (e: Throwable) {
            Timber.e(e, "## Failed to get current session")
            return null
        }
    }

    // check if the event was not yet received
    // a previous catchup might have already retrieved the notified event
    private fun isEventAlreadyKnown(eventId: String?, roomId: String?): Boolean {
        if (null != eventId && null != roomId) {
            try {
                val session = safeGetCurrentSession() ?: return false
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
        val eventId = data["event_id"] ?: return //Just ignore


        val eventType = data["type"]
        if (eventType == null) {
            //Just add a generic unknown event
            val simpleNotifiableEvent = SimpleNotifiableEvent(
                    session.sessionParams.credentials.userId,
                    eventId,
                    true, //It's an issue in this case, all event will bing even if expected to be silent.
                    title = getString(R.string.notification_unknown_new_event),
                    description = "",
                    type = null,
                    timestamp = System.currentTimeMillis(),
                    soundName = BingRule.ACTION_VALUE_DEFAULT,
                    isPushGatewayEvent = true
            )
            notificationDrawerManager.onNotifiableEventReceived(simpleNotifiableEvent)
            notificationDrawerManager.refreshNotificationDrawer()

            return
        } else {

            val event = parseEvent(data) ?: return

            val notifiableEvent = notifiableEventResolver.resolveEvent(event, session)

            if (notifiableEvent == null) {
                Timber.e("Unsupported notifiable event ${eventId}")
                if (BuildConfig.LOW_PRIVACY_LOG_ENABLE) {
                    Timber.e("--> ${event}")
                }
            } else {


                if (notifiableEvent is NotifiableMessageEvent) {
                    if (TextUtils.isEmpty(notifiableEvent.senderName)) {
                        notifiableEvent.senderName = data["sender_display_name"]
                                ?: data["sender"] ?: ""
                    }
                    if (TextUtils.isEmpty(notifiableEvent.roomName)) {
                        notifiableEvent.roomName = findRoomNameBestEffort(data, session) ?: ""
                    }
                }

                notifiableEvent.isPushGatewayEvent = true
                notifiableEvent.matrixID = session.sessionParams.credentials.userId
                notificationDrawerManager.onNotifiableEventReceived(notifiableEvent)
                notificationDrawerManager.refreshNotificationDrawer()
            }
        }

    }

    private fun findRoomNameBestEffort(data: Map<String, String>, session: Session?): String? {
        val roomName: String? = data["room_name"]
        val roomId = data["room_id"]
        if (null == roomName && null != roomId) {
            // Try to get the room name from our store
            /*
            TODO
            if (session?.dataHandler?.store?.isReady == true) {
                val room = session.getRoom(roomId)
                roomName = room?.getRoomDisplayName(this)
            }
            */
        }
        return roomName
    }

    /**
     * Try to create an event from the FCM data
     *
     * @param data the FCM data
     * @return the event
     */
    private fun parseEvent(data: Map<String, String>?): Event? {
        // accept only event with room id.
        if (null == data || !data.containsKey("room_id") || !data.containsKey("event_id")) {
            return null
        }

        try {
            return Event(eventId = data["event_id"],
                    senderId = data["sender"],
                    roomId = data["room_id"],
                    type = data.getValue("type"),
                    // TODO content = data.getValue("content"),
                    originServerTs = System.currentTimeMillis())
        } catch (e: Exception) {
            Timber.e(e, "buildEvent fails ")
        }

        return null
    }
}
