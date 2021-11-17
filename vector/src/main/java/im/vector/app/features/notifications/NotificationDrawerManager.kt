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
package im.vector.app.features.notifications

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.WorkerThread
import im.vector.app.ActiveSessionDataSource
import im.vector.app.BuildConfig
import im.vector.app.R
import im.vector.app.core.utils.FirstThrottler
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.settings.VectorPreferences
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.content.ContentUrlResolver
import org.matrix.android.sdk.api.util.toMatrixItem
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The NotificationDrawerManager receives notification events as they arrived (from event stream or fcm) and
 * organise them in order to display them in the notification drawer.
 * Events can be grouped into the same notification, old (already read) events can be removed to do some cleaning.
 */
@Singleton
class NotificationDrawerManager @Inject constructor(private val context: Context,
                                                    private val notificationDisplayer: NotificationDisplayer,
                                                    private val vectorPreferences: VectorPreferences,
                                                    private val activeSessionDataSource: ActiveSessionDataSource,
                                                    private val notifiableEventProcessor: NotifiableEventProcessor,
                                                    private val notificationRenderer: NotificationRenderer) {

    private val handlerThread: HandlerThread = HandlerThread("NotificationDrawerManager", Thread.MIN_PRIORITY)
    private var backgroundHandler: Handler

    init {
        handlerThread.start()
        backgroundHandler = Handler(handlerThread.looper)
    }

    /**
     * The notifiable events to render
     * this is our source of truth for notifications, any changes to this list will be rendered as notifications
     * when events are removed the previously rendered notifications will be cancelled
     * when adding or updating, the notifications will be notified
     *
     * Events are unique by their properties, we should be careful not to insert multiple events with the same event-id
     */
    private val queuedEvents = loadEventInfo()

    /**
     * The last known rendered notifiable events
     * we keep track of them in order to know which events have been removed from the eventList
     * allowing us to cancel any notifications previous displayed by now removed events
     */
    private var renderedEvents = emptyList<ProcessedEvent<NotifiableEvent>>()
    private val avatarSize = context.resources.getDimensionPixelSize(R.dimen.profile_avatar_size)
    private var currentRoomId: String? = null

    // TODO Multi-session: this will have to be improved
    private val currentSession: Session?
        get() = activeSessionDataSource.currentValue?.orNull()

    private var useCompleteNotificationFormat = vectorPreferences.useCompleteNotificationFormat()

    /**
     * An in memory FIFO cache of the seen events.
     * Acts as a notification debouncer to stop already dismissed push notifications from
     * displaying again when the /sync response is delayed.
     */
    private val seenEventIds = CircularCache.create<String>(cacheSize = 25)

    /**
    Should be called as soon as a new event is ready to be displayed.
    The notification corresponding to this event will not be displayed until
    #refreshNotificationDrawer() is called.
    Events might be grouped and there might not be one notification per event!
     */
    fun onNotifiableEventReceived(notifiableEvent: NotifiableEvent) {
        if (!vectorPreferences.areNotificationEnabledForDevice()) {
            Timber.i("Notification are disabled for this device")
            return
        }
        // If we support multi session, event list should be per userId
        // Currently only manage single session
        if (BuildConfig.LOW_PRIVACY_LOG_ENABLE) {
            Timber.d("onNotifiableEventReceived(): $notifiableEvent")
        } else {
            Timber.d("onNotifiableEventReceived(): is push: ${notifiableEvent.canBeReplaced}")
        }
        synchronized(queuedEvents) {
            val existing = queuedEvents.firstOrNull { it.eventId == notifiableEvent.eventId }
            if (existing != null) {
                if (existing.canBeReplaced) {
                    // Use the event coming from the event stream as it may contains more info than
                    // the fcm one (like type/content/clear text) (e.g when an encrypted message from
                    // FCM should be update with clear text after a sync)
                    // In this case the message has already been notified, and might have done some noise
                    // So we want the notification to be updated even if it has already been displayed
                    // Use setOnlyAlertOnce to ensure update notification does not interfere with sound
                    // from first notify invocation as outlined in:
                    // https://developer.android.com/training/notify-user/build-notification#Updating
                    queuedEvents.remove(existing)
                    queuedEvents.add(notifiableEvent)
                } else {
                    // keep the existing one, do not replace
                }
            } else {
                // Check if this is an edit
                if (notifiableEvent.editedEventId != null) {
                    // This is an edition
                    val eventBeforeEdition = queuedEvents.firstOrNull {
                        // Edition of an event
                        it.eventId == notifiableEvent.editedEventId ||
                                // or edition of an edition
                                it.editedEventId == notifiableEvent.editedEventId
                    }

                    if (eventBeforeEdition != null) {
                        // Replace the existing notification with the new content
                        queuedEvents.remove(eventBeforeEdition)

                        queuedEvents.add(notifiableEvent)
                    } else {
                        // Ignore an edit of a not displayed event in the notification drawer
                    }
                } else {
                    // Not an edit
                    if (seenEventIds.contains(notifiableEvent.eventId)) {
                        // we've already seen the event, lets skip
                        Timber.d("onNotifiableEventReceived(): skipping event, already seen")
                    } else {
                        seenEventIds.put(notifiableEvent.eventId)
                        queuedEvents.add(notifiableEvent)
                    }
                }
            }
        }
    }

    fun onEventRedacted(eventId: String) {
        synchronized(queuedEvents) {
            queuedEvents.replace(eventId) {
                when (it) {
                    is InviteNotifiableEvent  -> it.copy(isRedacted = true)
                    is NotifiableMessageEvent -> it.copy(isRedacted = true)
                    is SimpleNotifiableEvent  -> it.copy(isRedacted = true)
                }
            }
        }
    }

    /**
     * Clear all known events and refresh the notification drawer
     */
    fun clearAllEvents() {
        synchronized(queuedEvents) {
            queuedEvents.clear()
        }
        refreshNotificationDrawer()
    }

    /** Clear all known message events for this room */
    fun clearMessageEventOfRoom(roomId: String?) {
        Timber.v("clearMessageEventOfRoom $roomId")
        if (roomId != null) {
            val shouldUpdate = removeAll { it is NotifiableMessageEvent && it.roomId == roomId }
            if (shouldUpdate) {
                refreshNotificationDrawer()
            }
        }
    }

    /**
     * Should be called when the application is currently opened and showing timeline for the given roomId.
     * Used to ignore events related to that room (no need to display notification) and clean any existing notification on this room.
     */
    fun setCurrentRoom(roomId: String?) {
        var hasChanged: Boolean
        synchronized(queuedEvents) {
            hasChanged = roomId != currentRoomId
            currentRoomId = roomId
        }
        if (hasChanged) {
            clearMessageEventOfRoom(roomId)
        }
    }

    fun clearMemberShipNotificationForRoom(roomId: String) {
        val shouldUpdate = removeAll { it is InviteNotifiableEvent && it.roomId == roomId }
        if (shouldUpdate) {
            refreshNotificationDrawerBg()
        }
    }

    private fun removeAll(predicate: (NotifiableEvent) -> Boolean): Boolean {
        return synchronized(queuedEvents) {
            queuedEvents.removeAll(predicate)
        }
    }

    private var firstThrottler = FirstThrottler(200)

    fun refreshNotificationDrawer() {
        // Implement last throttler
        val canHandle = firstThrottler.canHandle()
        Timber.v("refreshNotificationDrawer(), delay: ${canHandle.waitMillis()} ms")
        backgroundHandler.removeCallbacksAndMessages(null)

        backgroundHandler.postDelayed(
                {
                    try {
                        refreshNotificationDrawerBg()
                    } catch (throwable: Throwable) {
                        // It can happen if for instance session has been destroyed. It's a bit ugly to try catch like this, but it's safer
                        Timber.w(throwable, "refreshNotificationDrawerBg failure")
                    }
                },
                canHandle.waitMillis())
    }

    @WorkerThread
    private fun refreshNotificationDrawerBg() {
        Timber.v("refreshNotificationDrawerBg()")

        val newSettings = vectorPreferences.useCompleteNotificationFormat()
        if (newSettings != useCompleteNotificationFormat) {
            // Settings has changed, remove all current notifications
            notificationDisplayer.cancelAllNotifications()
            useCompleteNotificationFormat = newSettings
        }

        val eventsToRender = synchronized(queuedEvents) {
            notifiableEventProcessor.process(queuedEvents, currentRoomId, renderedEvents).also {
                queuedEvents.clear()
                queuedEvents.addAll(it.onlyKeptEvents())
            }
        }

        if (renderedEvents == eventsToRender) {
            Timber.d("Skipping notification update due to event list not changing")
        } else {
            renderedEvents = eventsToRender
            val session = currentSession ?: return
            val user = session.getUser(session.myUserId)
            // myUserDisplayName cannot be empty else NotificationCompat.MessagingStyle() will crash
            val myUserDisplayName = user?.toMatrixItem()?.getBestName() ?: session.myUserId
            val myUserAvatarUrl = session.contentUrlResolver().resolveThumbnail(
                    contentUrl = user?.avatarUrl,
                    width = avatarSize,
                    height = avatarSize,
                    method = ContentUrlResolver.ThumbnailMethod.SCALE
            )
            notificationRenderer.render(session.myUserId, myUserDisplayName, myUserAvatarUrl, useCompleteNotificationFormat, eventsToRender)
        }
    }

    fun shouldIgnoreMessageEventInRoom(roomId: String?): Boolean {
        return currentRoomId != null && roomId == currentRoomId
    }

    fun persistInfo() {
        synchronized(queuedEvents) {
            if (queuedEvents.isEmpty()) {
                deleteCachedRoomNotifications()
                return
            }
            try {
                val file = File(context.applicationContext.cacheDir, ROOMS_NOTIFICATIONS_FILE_NAME)
                if (!file.exists()) file.createNewFile()
                FileOutputStream(file).use {
                    currentSession?.securelyStoreObject(queuedEvents, KEY_ALIAS_SECRET_STORAGE, it)
                }
            } catch (e: Throwable) {
                Timber.e(e, "## Failed to save cached notification info")
            }
        }
    }

    private fun loadEventInfo(): MutableList<NotifiableEvent> {
        try {
            val file = File(context.applicationContext.cacheDir, ROOMS_NOTIFICATIONS_FILE_NAME)
            if (file.exists()) {
                file.inputStream().use {
                    val events: ArrayList<NotifiableEvent>? = currentSession?.loadSecureSecret(it, KEY_ALIAS_SECRET_STORAGE)
                    if (events != null) {
                        return events.toMutableList()
                    }
                }
            }
        } catch (e: Throwable) {
            Timber.e(e, "## Failed to load cached notification info")
        }
        return ArrayList()
    }

    private fun deleteCachedRoomNotifications() {
        val file = File(context.applicationContext.cacheDir, ROOMS_NOTIFICATIONS_FILE_NAME)
        if (file.exists()) {
            file.delete()
        }
    }

    companion object {
        const val SUMMARY_NOTIFICATION_ID = 0
        const val ROOM_MESSAGES_NOTIFICATION_ID = 1
        const val ROOM_EVENT_NOTIFICATION_ID = 2
        const val ROOM_INVITATION_NOTIFICATION_ID = 3

        // TODO Mutliaccount
        private const val ROOMS_NOTIFICATIONS_FILE_NAME = "im.vector.notifications.cache"

        private const val KEY_ALIAS_SECRET_STORAGE = "notificationMgr"
    }
}

private fun MutableList<NotifiableEvent>.replace(eventId: String, block: (NotifiableEvent) -> NotifiableEvent) {
    val indexToReplace = indexOfFirst { it.eventId == eventId }
    if (indexToReplace == -1) {
        return
    }
    set(indexToReplace, block(get(indexToReplace)))
}
