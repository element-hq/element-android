/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.notifications

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.WorkerThread
import im.vector.app.ActiveSessionDataSource
import im.vector.app.R
import im.vector.app.core.resources.BuildMeta
import im.vector.app.core.utils.FirstThrottler
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.session.coroutineScope
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.content.ContentUrlResolver
import org.matrix.android.sdk.api.session.getUserOrDefault
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
        context: Context,
        private val notificationDisplayer: NotificationDisplayer,
        private val vectorPreferences: VectorPreferences,
        private val activeSessionDataSource: ActiveSessionDataSource,
        private val notifiableEventProcessor: NotifiableEventProcessor,
        private val notificationRenderer: NotificationRenderer,
        private val notificationEventPersistence: NotificationEventPersistence,
        private val filteredEventDetector: FilteredEventDetector,
        private val buildMeta: BuildMeta,
) {

    private val handlerThread: HandlerThread = HandlerThread("NotificationDrawerManager", Thread.MIN_PRIORITY)
    private var backgroundHandler: Handler

    // TODO Multi-session: this will have to be improved
    private val currentSession: Session?
        get() = activeSessionDataSource.currentValue?.orNull()

    /**
     * Lazily initializes the NotificationState as we rely on having a current session in order to fetch the persisted queue of events.
     */
    private val notificationState by lazy { createInitialNotificationState() }
    private val avatarSize = context.resources.getDimensionPixelSize(im.vector.lib.ui.styles.R.dimen.profile_avatar_size)
    private var currentRoomId: String? = null
    private var currentThreadId: String? = null
    private val firstThrottler = FirstThrottler(200)

    private var useCompleteNotificationFormat = vectorPreferences.useCompleteNotificationFormat()

    init {
        handlerThread.start()
        backgroundHandler = Handler(handlerThread.looper)
    }

    private fun createInitialNotificationState(): NotificationState {
        val queuedEvents = notificationEventPersistence.loadEvents(factory = { rawEvents ->
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
        if (buildMeta.lowPrivacyLoggingEnabled) {
            Timber.d("onNotifiableEventReceived(): $notifiableEvent")
        } else {
            Timber.d("onNotifiableEventReceived(): is push: ${notifiableEvent.canBeReplaced}")
        }

        if (filteredEventDetector.shouldBeIgnored(notifiableEvent)) {
            Timber.d("onNotifiableEventReceived(): ignore the event")
            return
        }

        add(notifiableEvent)
    }

    /**
     * Clear all known events and refresh the notification drawer.
     */
    fun clearAllEvents() {
        updateEvents { it.clear() }
    }

    /**
     * Should be called when the application is currently opened and showing timeline for the given roomId.
     * Used to ignore events related to that room (no need to display notification) and clean any existing notification on this room.
     */
    fun setCurrentRoom(roomId: String?) {
        val dispatcher = currentSession?.coroutineDispatchers?.io ?: return
        val scope = currentSession?.coroutineScope ?: return
        scope.launch(dispatcher) {
            updateEvents {
                val hasChanged = roomId != currentRoomId
                currentRoomId = roomId
                if (hasChanged && roomId != null) {
                    it.clearMessagesForRoom(roomId)
                }
            }
        }
    }

    /**
     * Should be called when the application is currently opened and showing timeline for the given threadId.
     * Used to ignore events related to that thread (no need to display notification) and clean any existing notification on this room.
     */
    fun setCurrentThread(threadId: String?) {
        val dispatcher = currentSession?.coroutineDispatchers?.io ?: return
        val scope = currentSession?.coroutineScope ?: return
        scope.launch(dispatcher) {
            updateEvents {
                val hasChanged = threadId != currentThreadId
                currentThreadId = threadId
                currentRoomId?.let { roomId ->
                    if (hasChanged && threadId != null) {
                        it.clearMessagesForThread(roomId, threadId)
                    }
                }
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
                canHandle.waitMillis()
        )
    }

    @WorkerThread
    private fun refreshNotificationDrawerBg() {
        Timber.v("refreshNotificationDrawerBg()")
        val eventsToRender = notificationState.updateQueuedEvents(this) { queuedEvents, renderedEvents ->
            notifiableEventProcessor.process(queuedEvents.rawEvents(), currentRoomId, currentThreadId, renderedEvents).also {
                queuedEvents.clearAndAdd(it.onlyKeptEvents())
            }
        }

        if (notificationState.hasAlreadyRendered(eventsToRender)) {
            Timber.d("Skipping notification update due to event list not changing")
        } else {
            notificationState.clearAndAddRenderedEvents(eventsToRender)
            val session = currentSession ?: return
            renderEvents(session, eventsToRender)
            persistEvents()
        }
    }

    private fun persistEvents() {
        notificationState.queuedEvents { queuedEvents ->
            notificationEventPersistence.persistEvents(queuedEvents)
        }
    }

    private fun renderEvents(session: Session, eventsToRender: List<ProcessedEvent<NotifiableEvent>>) {
        val user = session.getUserOrDefault(session.myUserId)
        // myUserDisplayName cannot be empty else NotificationCompat.MessagingStyle() will crash
        val myUserDisplayName = user.toMatrixItem().getBestName()
        val myUserAvatarUrl = session.contentUrlResolver().resolveThumbnail(
                contentUrl = user.avatarUrl,
                width = avatarSize,
                height = avatarSize,
                method = ContentUrlResolver.ThumbnailMethod.SCALE
        )
        notificationRenderer.render(session.myUserId, myUserDisplayName, myUserAvatarUrl, useCompleteNotificationFormat, eventsToRender)
    }

    fun shouldIgnoreMessageEventInRoom(resolvedEvent: NotifiableMessageEvent): Boolean {
        return resolvedEvent.shouldIgnoreMessageEventInRoom(currentRoomId, currentThreadId)
    }

    companion object {
        const val SUMMARY_NOTIFICATION_ID = 0
        const val ROOM_MESSAGES_NOTIFICATION_ID = 1
        const val ROOM_EVENT_NOTIFICATION_ID = 2
        const val ROOM_INVITATION_NOTIFICATION_ID = 3
    }
}
