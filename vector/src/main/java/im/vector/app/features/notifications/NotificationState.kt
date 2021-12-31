/*
 * Copyright (c) 2021 New Vector Ltd
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

class NotificationState(
        /**
         * The notifiable events queued for rendering or currently rendered
         *
         * this is our source of truth for notifications, any changes to this list will be rendered as notifications
         * when events are removed the previously rendered notifications will be cancelled
         * when adding or updating, the notifications will be notified
         *
         * Events are unique by their properties, we should be careful not to insert multiple events with the same event-id
         */
        private val queuedEvents: NotificationEventQueue,

        /**
         * The last known rendered notifiable events
         * we keep track of them in order to know which events have been removed from the eventList
         * allowing us to cancel any notifications previous displayed by now removed events
         */
        private val renderedEvents: MutableList<ProcessedEvent<NotifiableEvent>>,
) {

    fun <T> updateQueuedEvents(drawerManager: NotificationDrawerManager,
                               action: NotificationDrawerManager.(NotificationEventQueue, List<ProcessedEvent<NotifiableEvent>>) -> T): T {
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
