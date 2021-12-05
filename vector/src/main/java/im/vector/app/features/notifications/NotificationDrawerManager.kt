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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The NotificationDrawerManager receives notification events as they arrived (from event stream or fcm) and
 * organise them in order to display them in the notification drawer.
 * Events can be grouped into the same notification, old (already read) events can be removed to do some cleaning.
 */
@Singleton
class NotificationDrawerManager @Inject constructor(
        private val context: Context,
        private val notificationDisplayer: NotificationDisplayer,
        private val vectorPreferences: VectorPreferences,
        private val activeSessionDataSource: ActiveSessionDataSource,
        private val notifiableEventProcessor: NotifiableEventProcessor,
        private val notificationRenderer: NotificationRenderer,
        private val notificationEventPersistence: NotificationEventPersistence
) {

    private val handlerThread: HandlerThread = HandlerThread("NotificationDrawerManager", Thread.MIN_PRIORITY)
    private var backgroundHandler: Handler

    // TODO Multi-session: this will have to be improved
    private val currentSession: Session?
        get() = activeSessionDataSource.currentValue?.orNull()

    /**
     * Lazily initializes the NotificationState as we rely on having a current session in order to fetch the persisted queue of events
     */
    private val notificationState by lazy { createInitialNotificationState() }
    private val avatarSize = context.resources.getDimensionPixelSize(R.dimen.profile_avatar_size)
    private var currentRoomId: String? = null
    private val firstThrottler = FirstThrottler(200)

    private var useCompleteNotificationFormat = vectorPreferences.useCompleteNotificationFormat()

    init {
        handlerThread.start()
        backgroundHandler = Handler(handlerThread.looper)
    }

    private fun createInitialNotificationState(): NotificationState {
        val queuedEvents = notificationEventPersistence.loadEvents(currentSession, factory = { rawEvents ->
            NotificationEventQueue(rawEvents.toMutableList(), seenEventIds = CircularCache.create(cacheSize = 25))
        })
        val renderedEvents = queuedEvents.rawEvents().map { ProcessedEvent(ProcessedEvent.Type.KEEP, it) }.toMutableList()
        return NotificationState(queuedEvents, renderedEvents)
    }

    /**
    Should be called as soon as a new event is ready to be displayed.
    The notification corresponding to this event will not be displayed until
    #refreshNotificationDrawer() is called.
    Events might be grouped and there might not be one notification per event!
     */
    fun NotificationEventQueue.onNotifiableEventReceived(notifiableEvent: NotifiableEvent) {
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

        add(notifiableEvent)
    }

    /**
     * Clear all known events and refresh the notification drawer
     */
    fun clearAllEvents() {
        updateEvents { it.clear() }
    }

    /**
     * Should be called when the application is currently opened and showing timeline for the given roomId.
     * Used to ignore events related to that room (no need to display notification) and clean any existing notification on this room.
     */
    fun setCurrentRoom(roomId: String?) {
        updateEvents {
            val hasChanged = roomId != currentRoomId
            currentRoomId = roomId
            if (hasChanged && roomId != null) {
                it.clearMessagesForRoom(roomId)
            }
        }
    }

    fun notificationStyleChanged() {
        updateEvents {
            val newSettings = vectorPreferences.useCompleteNotificationFormat()
            if (newSettings != useCompleteNotificationFormat) {
                // Settings has changed, remove all current notifications
                notificationDisplayer.cancelAllNotifications()
                useCompleteNotificationFormat = newSettings
            }
        }
    }

    fun updateEvents(action: NotificationDrawerManager.(NotificationEventQueue) -> Unit) {
        notificationState.updateQueuedEvents(this) { queuedEvents, _ ->
            action(queuedEvents)
        }
        refreshNotificationDrawer()
    }

    private fun refreshNotificationDrawer() {
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
        val eventsToRender = notificationState.updateQueuedEvents(this) { queuedEvents, renderedEvents ->
            notifiableEventProcessor.process(queuedEvents.rawEvents(), currentRoomId, renderedEvents).also {
                queuedEvents.clearAndAdd(it.onlyKeptEvents())
            }
        }

        if (notificationState.hasAlreadyRendered(eventsToRender)) {
            Timber.d("Skipping notification update due to event list not changing")
        } else {
            notificationState.clearAndAddRenderedEvents(eventsToRender)
            val session = currentSession ?: return
            renderEvents(session, eventsToRender)
            persistEvents(session)
        }
    }

    private fun persistEvents(session: Session) {
        notificationState.queuedEvents { queuedEvents ->
            notificationEventPersistence.persistEvents(queuedEvents, session)
        }
    }

    private fun renderEvents(session: Session, eventsToRender: List<ProcessedEvent<NotifiableEvent>>) {
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

    fun shouldIgnoreMessageEventInRoom(roomId: String?): Boolean {
        return currentRoomId != null && roomId == currentRoomId
    }

    companion object {
        const val SUMMARY_NOTIFICATION_ID = 0
        const val ROOM_MESSAGES_NOTIFICATION_ID = 1
        const val ROOM_EVENT_NOTIFICATION_ID = 2
        const val ROOM_INVITATION_NOTIFICATION_ID = 3
    }
}
