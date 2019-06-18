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

package im.vector.riotredesign.push.fcm

import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.riotredesign.BuildConfig
import im.vector.riotredesign.R
import im.vector.riotredesign.core.preference.BingRule
import im.vector.riotredesign.features.badge.BadgeProxy
import im.vector.riotredesign.features.notifications.NotifiableEventResolver
import im.vector.riotredesign.features.notifications.NotifiableMessageEvent
import im.vector.riotredesign.features.notifications.NotificationDrawerManager
import im.vector.riotredesign.features.notifications.SimpleNotifiableEvent
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * Class extending FirebaseMessagingService.
 */
class VectorFirebaseMessagingService : FirebaseMessagingService() {

    val notificationDrawerManager by inject<NotificationDrawerManager>()

    private val notifiableEventResolver by lazy {
        NotifiableEventResolver(this)
    }

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
        if (message == null || message.data == null) {
            Timber.e("## onMessageReceived() : received a null message or message with no data")
            return
        }
        if (BuildConfig.LOW_PRIVACY_LOG_ENABLE) {
            Timber.i("## onMessageReceived()" + message.data.toString())
            Timber.i("## onMessageReceived() from FCM with priority " + message.priority)
        }

        //safe guard
        /* TODO
        val pushManager = Matrix.getInstance(applicationContext).pushManager
        if (!pushManager.areDeviceNotificationsAllowed()) {
            Timber.i("## onMessageReceived() : the notifications are disabled")
            return
        }
        */

        //TODO if the app is in foreground, we could just ignore this. The sync loop is already going?
        // TODO mUIHandler.post { onMessageReceivedInternal(message.data, pushManager) }
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is also called
     * when the InstanceID token is initially generated, so this is where
     * you retrieve the token.
     */
    override fun onNewToken(refreshedToken: String?) {
        Timber.i("onNewToken: FCM Token has been updated")
        FcmHelper.storeFcmToken(this, refreshedToken)
        // TODO Matrix.getInstance(this)?.pushManager?.resetFCMRegistration(refreshedToken)
    }

    override fun onDeletedMessages() {
        Timber.v("## onDeletedMessages()")
    }

    /**
     * Internal receive method
     *
     * @param data Data map containing message data as key/value pairs.
     * For Set of keys use data.keySet().
     */
    private fun onMessageReceivedInternal(data: Map<String, String> /*, pushManager: PushManager*/) {
        try {
            if (BuildConfig.LOW_PRIVACY_LOG_ENABLE) {
                Timber.i("## onMessageReceivedInternal() : $data")
            }
            // update the badge counter
            val unreadCount = data.get("unread")?.let { Integer.parseInt(it) } ?: 0
            BadgeProxy.updateBadgeCount(applicationContext, unreadCount)

            /* TODO
            val session = Matrix.getInstance(applicationContext)?.defaultSession

            if (VectorApp.isAppInBackground() && !pushManager.isBackgroundSyncAllowed) {
                //Notification contains metadata and maybe data information
                handleNotificationWithoutSyncingMode(data, session)
            } else {
                // Safe guard... (race?)
                if (isEventAlreadyKnown(data["event_id"], data["room_id"])) return
                //Catch up!!
                EventStreamServiceX.onPushReceived(this)
            }
            */
        } catch (e: Exception) {
            Timber.e(e, "## onMessageReceivedInternal() failed : " + e.message)
        }
    }

    // check if the event was not yet received
    // a previous catchup might have already retrieved the notified event
    private fun isEventAlreadyKnown(eventId: String?, roomId: String?): Boolean {
        if (null != eventId && null != roomId) {
            try {
                /* TODO
                val sessions = Matrix.getInstance(applicationContext).sessions

                if (null != sessions && !sessions.isEmpty()) {
                    for (session in sessions) {
                        if (session.dataHandler?.store?.isReady == true) {
                            session.dataHandler.store?.getEvent(eventId, roomId)?.let {
                                Timber.e("## isEventAlreadyKnown() : ignore the event " + eventId
                                        + " in room " + roomId + " because it is already known")
                                return true
                            }
                        }
                    }
                }
                */
            } catch (e: Exception) {
                Timber.e(e, "## isEventAlreadyKnown() : failed to check if the event was already defined " + e.message)
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
            notificationDrawerManager.refreshNotificationDrawer(null)

            return
        } else {

            val event = parseEvent(data)
            if (event?.roomId == null) {
                //unsupported event
                Timber.e("Received an event with no room id")
                return
            } else {

                var notifiableEvent = notifiableEventResolver.resolveEvent(event, null, null /* TODO session.fulfillRule(event) */, session)

                if (notifiableEvent == null) {
                    Timber.e("Unsupported notifiable event ${eventId}")
                    if (BuildConfig.LOW_PRIVACY_LOG_ENABLE) {
                        Timber.e("--> ${event}")
                    }
                } else {


                    if (notifiableEvent is NotifiableMessageEvent) {
                        if (TextUtils.isEmpty(notifiableEvent.senderName)) {
                            notifiableEvent.senderName = data["sender_display_name"] ?: data["sender"] ?: ""
                        }
                        if (TextUtils.isEmpty(notifiableEvent.roomName)) {
                            notifiableEvent.roomName = findRoomNameBestEffort(data, session) ?: ""
                        }
                    }

                    notifiableEvent.isPushGatewayEvent = true
                    notifiableEvent.matrixID = session.sessionParams.credentials.userId
                    notificationDrawerManager.onNotifiableEventReceived(notifiableEvent)
                    notificationDrawerManager.refreshNotificationDrawer(null)
                }
            }
        }
    }

    private fun findRoomNameBestEffort(data: Map<String, String>, session: Session?): String? {
        var roomName: String? = data["room_name"]
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
            Timber.e(e, "buildEvent fails " + e.localizedMessage)
        }

        return null
    }
}
