/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.notifications

class NotificationState(
        /**
         * The notifiable events queued for rendering or currently rendered.
         *
         * This is our source of truth for notifications, any changes to this list will be rendered as notifications.
         * When events are removed the previously rendered notifications will be cancelled.
         * When adding or updating, the notifications will be notified.
         *
         * Events are unique by their properties, we should be careful not to insert multiple events with the same event-id.
         */
        private val queuedEvents: NotificationEventQueue,

        /**
         * The last known rendered notifiable events.
         * We keep track of them in order to know which events have been removed from the eventList
         * allowing us to cancel any notifications previous displayed by now removed events
         */
        private val renderedEvents: MutableList<ProcessedEvent<NotifiableEvent>>,
) {

    fun <T> updateQueuedEvents(
            drawerManager: NotificationDrawerManager,
            action: NotificationDrawerManager.(NotificationEventQueue, List<ProcessedEvent<NotifiableEvent>>) -> T
    ): T {
        return synchronized(queuedEvents) {
            action(drawerManager, queuedEvents, renderedEvents)
        }
    }

    fun clearAndAddRenderedEvents(eventsToRender: List<ProcessedEvent<NotifiableEvent>>) {
        renderedEvents.clear()
        renderedEvents.addAll(eventsToRender)
    }

    fun hasAlreadyRendered(eventsToRender: List<ProcessedEvent<NotifiableEvent>>) = renderedEvents == eventsToRender

    fun queuedEvents(block: (NotificationEventQueue) -> Unit) {
        synchronized(queuedEvents) {
            block(queuedEvents)
        }
    }
}
