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

import timber.log.Timber

class NotificationEventQueue(
        private val queue: MutableList<NotifiableEvent> = mutableListOf()
) {

    fun markRedacted(eventIds: List<String>) {
        eventIds.forEach { redactedId ->
            queue.replace(redactedId) {
                when (it) {
                    is InviteNotifiableEvent  -> it.copy(isRedacted = true)
                    is NotifiableMessageEvent -> it.copy(isRedacted = true)
                    is SimpleNotifiableEvent  -> it.copy(isRedacted = true)
                }
            }
        }
    }

    fun syncRoomEvents(roomsLeft: Collection<String>, roomsJoined: Collection<String>) {
        if (roomsLeft.isNotEmpty() || roomsJoined.isNotEmpty()) {
            queue.removeAll {
                when (it) {
                    is NotifiableMessageEvent -> roomsLeft.contains(it.roomId)
                    is InviteNotifiableEvent  -> roomsLeft.contains(it.roomId) || roomsJoined.contains(it.roomId)
                    else                      -> false
                }
            }
        }
    }

    private fun MutableList<NotifiableEvent>.replace(eventId: String, block: (NotifiableEvent) -> NotifiableEvent) {
        val indexToReplace = indexOfFirst { it.eventId == eventId }
        if (indexToReplace == -1) {
            return
        }
        set(indexToReplace, block(get(indexToReplace)))
    }

    fun isEmpty() = queue.isEmpty()

    fun clearAndAdd(events: List<NotifiableEvent>) {
        queue.clear()
        queue.addAll(events)
    }

    fun findExistingById(notifiableEvent: NotifiableEvent): NotifiableEvent? {
        return queue.firstOrNull { it.eventId == notifiableEvent.eventId }
    }

    fun findEdited(notifiableEvent: NotifiableEvent): NotifiableEvent? {
        return notifiableEvent.editedEventId?.let { editedId ->
            queue.firstOrNull {
                it.eventId == editedId || it.editedEventId == editedId
            }
        }
    }

    fun replace(replace: NotifiableEvent, with: NotifiableEvent) {
        queue.remove(replace)
        queue.add(with)
    }

    fun clear() {
        queue.clear()
    }

    fun add(notifiableEvent: NotifiableEvent) {
        queue.add(notifiableEvent)
    }

    fun clearMemberShipNotificationForRoom(roomId: String) {
        Timber.v("clearMemberShipOfRoom $roomId")
        queue.removeAll { it is InviteNotifiableEvent && it.roomId == roomId }
    }

    fun clearMessagesForRoom(roomId: String) {
        Timber.v("clearMessageEventOfRoom $roomId")
        queue.removeAll { it is NotifiableMessageEvent && it.roomId == roomId }
    }

    fun rawEvents(): List<NotifiableEvent> = queue
}
