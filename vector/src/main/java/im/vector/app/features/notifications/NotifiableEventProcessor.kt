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

import im.vector.app.features.invite.AutoAcceptInvites
import timber.log.Timber
import javax.inject.Inject

class NotifiableEventProcessor @Inject constructor(
        private val outdatedDetector: OutdatedEventDetector,
        private val autoAcceptInvites: AutoAcceptInvites
) {

    fun modifyAndProcess(eventList: MutableList<NotifiableEvent>, currentRoomId: String?): ProcessedNotificationEvents {
        val roomIdToEventMap: MutableMap<String, MutableList<NotifiableMessageEvent>> = LinkedHashMap()
        val simpleEvents: MutableMap<String, SimpleNotifiableEvent?> = LinkedHashMap()
        val invitationEvents: MutableMap<String, InviteNotifiableEvent?> = LinkedHashMap()

        val eventIterator = eventList.listIterator()
        while (eventIterator.hasNext()) {
            when (val event = eventIterator.next()) {
                is NotifiableMessageEvent -> {
                    val roomId = event.roomId
                    val roomEvents = roomIdToEventMap.getOrPut(roomId) { ArrayList() }

                    // should we limit to last 7 messages per room?
                    if (shouldIgnoreMessageEventInRoom(currentRoomId, roomId) || outdatedDetector.isMessageOutdated(event)) {
                        // forget this event
                        eventIterator.remove()
                    } else {
                        roomEvents.add(event)
                    }
                }
                is InviteNotifiableEvent  -> {
                    if (autoAcceptInvites.hideInvites) {
                        // Forget this event
                        eventIterator.remove()
                        invitationEvents[event.roomId] = null
                    } else {
                        invitationEvents[event.roomId] = event
                    }
                }
                is SimpleNotifiableEvent  -> simpleEvents[event.eventId] = event
                else                      -> Timber.w("Type not handled")
            }
        }
        return ProcessedNotificationEvents(roomIdToEventMap, simpleEvents, invitationEvents)
    }

    private fun shouldIgnoreMessageEventInRoom(currentRoomId: String?, roomId: String?): Boolean {
        return currentRoomId != null && roomId == currentRoomId
    }
}

data class ProcessedNotificationEvents(
        val roomEvents: Map<String, List<NotifiableMessageEvent>>,
        val simpleEvents: Map<String, SimpleNotifiableEvent?>,
        val invitationEvents: Map<String, InviteNotifiableEvent?>
)
