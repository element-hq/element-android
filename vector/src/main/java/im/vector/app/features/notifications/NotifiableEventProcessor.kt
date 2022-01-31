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
import im.vector.app.features.notifications.ProcessedEvent.Type.KEEP
import im.vector.app.features.notifications.ProcessedEvent.Type.REMOVE
import org.matrix.android.sdk.api.session.events.model.EventType
import timber.log.Timber
import javax.inject.Inject

private typealias ProcessedEvents = List<ProcessedEvent<NotifiableEvent>>

class NotifiableEventProcessor @Inject constructor(
        private val outdatedDetector: OutdatedEventDetector,
        private val autoAcceptInvites: AutoAcceptInvites
) {

    fun process(queuedEvents: List<NotifiableEvent>, currentRoomId: String?, renderedEvents: ProcessedEvents): ProcessedEvents {
        val processedEvents = queuedEvents.map {
            val type = when (it) {
                is InviteNotifiableEvent  -> if (autoAcceptInvites.hideInvites) REMOVE else KEEP
                is NotifiableMessageEvent -> when {
                    shouldIgnoreMessageEventInRoom(currentRoomId, it.roomId) -> REMOVE
                            .also { Timber.d("notification message removed due to currently viewing the same room") }
                    outdatedDetector.isMessageOutdated(it)                   -> REMOVE
                            .also { Timber.d("notification message removed due to being read") }
                    else                                                     -> KEEP
                }
                is SimpleNotifiableEvent  -> when (it.type) {
                    EventType.REDACTION -> REMOVE
                    else                -> KEEP
                }
            }
            ProcessedEvent(type, it)
        }

        val removedEventsDiff = renderedEvents.filter { renderedEvent ->
            queuedEvents.none { it.eventId == renderedEvent.event.eventId }
        }.map { ProcessedEvent(REMOVE, it.event) }

        return removedEventsDiff + processedEvents
    }

    private fun shouldIgnoreMessageEventInRoom(currentRoomId: String?, roomId: String?): Boolean {
        return currentRoomId != null && roomId == currentRoomId
    }
}
