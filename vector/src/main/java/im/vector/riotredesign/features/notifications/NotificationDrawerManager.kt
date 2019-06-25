/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.riotredesign.features.notifications

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.session.content.ContentUrlResolver
import im.vector.riotredesign.BuildConfig
import im.vector.riotredesign.R
import im.vector.riotredesign.core.utils.SecretStoringUtils
import im.vector.riotredesign.features.settings.PreferencesManager
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * The NotificationDrawerManager receives notification events as they arrived (from event stream or fcm) and
 * organise them in order to display them in the notification drawer.
 * Events can be grouped into the same notification, old (already read) events can be removed to do some cleaning.
 */
class NotificationDrawerManager(val context: Context,
                                private val outdatedDetector: OutdatedEventDetector?) {


    //The first time the notification drawer is refreshed, we force re-render of all notifications
    private var firstTime = true

    private var eventList = loadEventInfo()

    private val avatarSize = context.resources.getDimensionPixelSize(R.dimen.profile_avatar_size)

    private var currentRoomId: String? = null

    private var iconLoader = IconLoader(context,
            object : IconLoader.IconLoaderListener {
                override fun onIconsLoaded() {
                    // Force refresh
                    refreshNotificationDrawer()
                }
            })

    private var bitmapLoader = BitmapLoader(context,
            object : BitmapLoader.BitmapLoaderListener {
                override fun onBitmapsLoaded() {
                    // Force refresh
                    refreshNotificationDrawer()
                }
            })

    /**
    Should be called as soon as a new event is ready to be displayed.
    The notification corresponding to this event will not be displayed until
    #refreshNotificationDrawer() is called.
    Events might be grouped and there might not be one notification per event!
     */
    fun onNotifiableEventReceived(notifiableEvent: NotifiableEvent) {
        if (!PreferencesManager.areNotificationEnabledForDevice(context)) {
            Timber.i("Notification are disabled for this device")
            return
        }
        //If we support multi session, event list should be per userId
        //Currently only manage single session
        if (BuildConfig.LOW_PRIVACY_LOG_ENABLE) {
            Timber.v("%%%%%%%% onNotifiableEventReceived $notifiableEvent")
        }
        synchronized(eventList) {
            val existing = eventList.firstOrNull { it.eventId == notifiableEvent.eventId }
            if (existing != null) {
                if (existing.isPushGatewayEvent) {
                    //Use the event coming from the event stream as it may contains more info than
                    //the fcm one (like type/content/clear text)
                    // In this case the message has already been notified, and might have done some noise
                    // So we want the notification to be updated even if it has already been displayed
                    // But it should make no noise (e.g when an encrypted message from FCM should be
                    // update with clear text after a sync)
                    notifiableEvent.hasBeenDisplayed = false
                    notifiableEvent.noisy = false
                    eventList.remove(existing)
                    eventList.add(notifiableEvent)

                } else {
                    //keep the existing one, do not replace
                }
            } else {
                eventList.add(notifiableEvent)
            }
        }
    }

    /**
    Clear all known events and refresh the notification drawer
     */
    fun clearAllEvents() {
        synchronized(eventList) {
            eventList.clear()
        }
        refreshNotificationDrawer()
    }

    /** Clear all known message events for this room and refresh the notification drawer */
    fun clearMessageEventOfRoom(roomId: String?) {
        Timber.v("clearMessageEventOfRoom $roomId")

        if (roomId != null) {
            eventList.removeAll { e ->
                if (e is NotifiableMessageEvent) {
                    return@removeAll e.roomId == roomId
                }
                return@removeAll false
            }
            NotificationUtils.cancelNotificationMessage(context, roomId, ROOM_MESSAGES_NOTIFICATION_ID)
        }
        refreshNotificationDrawer()
    }

    /**
     * Should be called when the application is currently opened and showing timeline for the given roomId.
     * Used to ignore events related to that room (no need to display notification) and clean any existing notification on this room.
     */
    fun setCurrentRoom(roomId: String?) {
        var hasChanged: Boolean
        synchronized(eventList) {
            hasChanged = roomId != currentRoomId
            currentRoomId = roomId
        }
        if (hasChanged) {
            clearMessageEventOfRoom(roomId)
        }
    }

    fun homeActivityDidResume(matrixID: String?) {
        synchronized(eventList) {
            eventList.removeAll { e ->
                return@removeAll e !is NotifiableMessageEvent //messages are cleared when entering room
            }
        }
    }

    fun clearMemberShipNotificationForRoom(roomId: String) {
        synchronized(eventList) {
            eventList.removeAll { e ->
                if (e is InviteNotifiableEvent) {
                    return@removeAll e.roomId == roomId
                }
                return@removeAll false
            }
        }
    }


    fun refreshNotificationDrawer() {

        val session = Matrix.getInstance().currentSession ?: return
        val user = session.getUser(session.sessionParams.credentials.userId)
        val myUserDisplayName = user?.displayName ?: ""
        val myUserAvatarUrl = session.contentUrlResolver().resolveThumbnail(user?.avatarUrl, avatarSize, avatarSize, ContentUrlResolver.ThumbnailMethod.SCALE)
        synchronized(eventList) {

            Timber.v("%%%%%%%% REFRESH NOTIFICATION DRAWER ")
            //TMP code
            var hasNewEvent = false
            var summaryIsNoisy = false
            val summaryInboxStyle = NotificationCompat.InboxStyle()

            //group events by room to create a single MessagingStyle notif
            val roomIdToEventMap: MutableMap<String, ArrayList<NotifiableMessageEvent>> = HashMap()
            val simpleEvents: ArrayList<NotifiableEvent> = ArrayList()
            val notifications: ArrayList<Notification> = ArrayList()

            val eventIterator = eventList.listIterator()
            while (eventIterator.hasNext()) {
                val event = eventIterator.next()
                if (event is NotifiableMessageEvent) {
                    val roomId = event.roomId
                    var roomEvents = roomIdToEventMap[roomId]
                    if (roomEvents == null) {
                        roomEvents = ArrayList()
                        roomIdToEventMap[roomId] = roomEvents
                    }

                    if (shouldIgnoreMessageEventInRoom(roomId) || outdatedDetector?.isMessageOutdated(event) == true) {
                        //forget this event
                        eventIterator.remove()
                    } else {
                        roomEvents.add(event)
                    }
                } else {
                    simpleEvents.add(event)
                }
            }


            Timber.v("%%%%%%%% REFRESH NOTIFICATION DRAWER ${roomIdToEventMap.size} room groups")

            var globalLastMessageTimestamp = 0L

            //events have been grouped
            for ((roomId, events) in roomIdToEventMap) {

                if (events.isEmpty()) {
                    //Just clear this notification
                    Timber.v("%%%%%%%% REFRESH NOTIFICATION DRAWER $roomId has no more events")
                    NotificationUtils.cancelNotificationMessage(context, roomId, ROOM_MESSAGES_NOTIFICATION_ID)
                    continue
                }

                val roomName = events[0].roomName ?: events[0].senderName ?: ""

                val roomEventGroupInfo = RoomEventGroupInfo(
                        roomId = roomId,
                        isDirect = events[0].roomIsDirect,
                        roomDisplayName = roomName)

                val style = NotificationCompat.MessagingStyle(Person.Builder()
                        .setName(myUserDisplayName)
                        .setIcon(iconLoader.getUserIcon(myUserAvatarUrl))
                        .setKey(events[0].matrixID)
                        .build())

                style.isGroupConversation = !roomEventGroupInfo.isDirect

                if (!roomEventGroupInfo.isDirect) {
                    style.conversationTitle = roomEventGroupInfo.roomDisplayName
                }

                val largeBitmap = getRoomBitmap(events)


                for (event in events) {
                    //if all events in this room have already been displayed there is no need to update it
                    if (!event.hasBeenDisplayed) {
                        roomEventGroupInfo.shouldBing = roomEventGroupInfo.shouldBing || event.noisy
                        roomEventGroupInfo.customSound = event.soundName
                    }
                    roomEventGroupInfo.hasNewEvent = roomEventGroupInfo.hasNewEvent || !event.hasBeenDisplayed

                    val senderPerson = Person.Builder()
                            .setName(event.senderName)
                            .setIcon(iconLoader.getUserIcon(event.senderAvatarPath))
                            .setKey(event.senderId)
                            .build()

                    if (event.outGoingMessage && event.outGoingMessageFailed) {
                        style.addMessage(context.getString(R.string.notification_inline_reply_failed), event.timestamp, senderPerson)
                        roomEventGroupInfo.hasSmartReplyError = true
                    } else {
                        style.addMessage(event.body, event.timestamp, senderPerson)
                    }
                    event.hasBeenDisplayed = true //we can consider it as displayed

                    //It is possible that this event was previously shown as an 'anonymous' simple notif.
                    //And now it will be merged in a single MessageStyle notif, so we can clean to be sure
                    NotificationUtils.cancelNotificationMessage(context, event.eventId, ROOM_EVENT_NOTIFICATION_ID)
                }

                try {
                    val summaryLine = context.resources.getQuantityString(
                            R.plurals.notification_compat_summary_line_for_room, events.size, roomName, events.size)
                    summaryInboxStyle.addLine(summaryLine)
                } catch (e: Throwable) {
                    //String not found or bad format
                    Timber.v("%%%%%%%% REFRESH NOTIFICATION DRAWER failed to resolve string")
                    summaryInboxStyle.addLine(roomName)
                }

                if (firstTime || roomEventGroupInfo.hasNewEvent) {
                    //Should update displayed notification
                    Timber.v("%%%%%%%% REFRESH NOTIFICATION DRAWER $roomId need refresh")
                    val lastMessageTimestamp = events.last().timestamp

                    if (globalLastMessageTimestamp < lastMessageTimestamp) {
                        globalLastMessageTimestamp = lastMessageTimestamp
                    }

                    NotificationUtils.buildMessagesListNotification(context, style, roomEventGroupInfo, largeBitmap, lastMessageTimestamp, myUserDisplayName)
                            ?.let {
                                //is there an id for this room?
                                notifications.add(it)
                                NotificationUtils.showNotificationMessage(context, roomId, ROOM_MESSAGES_NOTIFICATION_ID, it)
                            }
                    hasNewEvent = true
                    summaryIsNoisy = summaryIsNoisy || roomEventGroupInfo.shouldBing
                } else {
                    Timber.v("%%%%%%%% REFRESH NOTIFICATION DRAWER $roomId is up to date")
                }
            }


            //Handle simple events
            for (event in simpleEvents) {
                //We build a simple event
                if (firstTime || !event.hasBeenDisplayed) {
                    NotificationUtils.buildSimpleEventNotification(context, event, null, myUserDisplayName)?.let {
                        notifications.add(it)
                        NotificationUtils.showNotificationMessage(context, event.eventId, ROOM_EVENT_NOTIFICATION_ID, it)
                        event.hasBeenDisplayed = true //we can consider it as displayed
                        hasNewEvent = true
                        summaryIsNoisy = summaryIsNoisy || event.noisy
                        summaryInboxStyle.addLine(event.description)
                    }
                }
            }


            //======== Build summary notification =========
            //On Android 7.0 (API level 24) and higher, the system automatically builds a summary for
            // your group using snippets of text from each notification. The user can expand this
            // notification to see each separate notification.
            // To support older versions, which cannot show a nested group of notifications,
            // you must create an extra notification that acts as the summary.
            // This appears as the only notification and the system hides all the others.
            // So this summary should include a snippet from all the other notifications,
            // which the user can tap to open your app.
            // The behavior of the group summary may vary on some device types such as wearables.
            // To ensure the best experience on all devices and versions, always include a group summary when you create a group
            // https://developer.android.com/training/notify-user/group

            if (eventList.isEmpty()) {
                NotificationUtils.cancelNotificationMessage(context, null, SUMMARY_NOTIFICATION_ID)
            } else {
                val nbEvents = roomIdToEventMap.size + simpleEvents.size
                val sumTitle = context.resources.getQuantityString(
                        R.plurals.notification_compat_summary_title, nbEvents, nbEvents)
                summaryInboxStyle.setBigContentTitle(sumTitle)
                NotificationUtils.buildSummaryListNotification(
                        context,
                        summaryInboxStyle,
                        sumTitle,
                        noisy = hasNewEvent && summaryIsNoisy,
                        lastMessageTimestamp = globalLastMessageTimestamp
                )?.let {
                    NotificationUtils.showNotificationMessage(context, null, SUMMARY_NOTIFICATION_ID, it)
                }

                if (hasNewEvent && summaryIsNoisy) {
                    try {
                        // turn the screen on for 3 seconds
                        /*
                        TODO
                        if (Matrix.getInstance(VectorApp.getInstance())!!.pushManager.isScreenTurnedOn) {
                            val pm = VectorApp.getInstance().getSystemService(Context.POWER_SERVICE) as PowerManager
                            val wl = pm.newWakeLock(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                                    NotificationDrawerManager::class.java.name)
                            wl.acquire(3000)
                            wl.release()
                        }
                         */
                    } catch (e: Throwable) {
                        Timber.e(e, "## Failed to turn screen on")
                    }

                }
            }
            //notice that we can get bit out of sync with actual display but not a big issue
            firstTime = false
        }
    }

    private fun getRoomBitmap(events: ArrayList<NotifiableMessageEvent>): Bitmap? {
        if (events.isEmpty()) return null

        //Use the last event (most recent?)
        val roomAvatarPath = events.last().roomAvatarPath
                ?: events.last().senderAvatarPath

        return bitmapLoader.getRoomBitmap(roomAvatarPath)
    }

    private fun shouldIgnoreMessageEventInRoom(roomId: String?): Boolean {
        return currentRoomId != null && roomId == currentRoomId
    }


    fun persistInfo() {
        if (eventList.isEmpty()) {
            deleteCachedRoomNotifications(context)
            return
        }
        try {
            val file = File(context.applicationContext.cacheDir, ROOMS_NOTIFICATIONS_FILE_NAME)
            if (!file.exists()) file.createNewFile()
            FileOutputStream(file).use {
                SecretStoringUtils.securelyStoreObject(eventList, "notificationMgr", it, this.context)
            }
        } catch (e: Throwable) {
            Timber.e(e, "## Failed to save cached notification info")
        }
    }

    private fun loadEventInfo(): ArrayList<NotifiableEvent> {
        try {
            val file = File(context.applicationContext.cacheDir, ROOMS_NOTIFICATIONS_FILE_NAME)
            if (file.exists()) {
                FileInputStream(file).use {
                    val events: ArrayList<NotifiableEvent>? = SecretStoringUtils.loadSecureSecret(it, "notificationMgr", this.context)
                    if (events != null) {
                        return ArrayList(events.mapNotNull { it as? NotifiableEvent })
                    }
                }
            }
        } catch (e: Throwable) {
            Timber.e(e, "## Failed to load cached notification info")
        }
        return ArrayList()
    }

    private fun deleteCachedRoomNotifications(context: Context) {
        val file = File(context.applicationContext.cacheDir, ROOMS_NOTIFICATIONS_FILE_NAME)
        if (file.exists()) {
            file.delete()
        }
    }

    companion object {
        private const val SUMMARY_NOTIFICATION_ID = 0
        private const val ROOM_MESSAGES_NOTIFICATION_ID = 1
        private const val ROOM_EVENT_NOTIFICATION_ID = 2

        private const val ROOMS_NOTIFICATIONS_FILE_NAME = "im.vector.notifications.cache"
    }
}
