/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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

    fun process(queuedEvents: List<NotifiableEvent>, currentRoomId: String?, currentThreadId: String?, renderedEvents: ProcessedEvents): ProcessedEvents {
        val processedEvents = queuedEvents.map {
            val type = when (it) {
                is InviteNotifiableEvent -> if (autoAcceptInvites.hideInvites) REMOVE else KEEP
                is NotifiableMessageEvent -> when {
                    it.shouldIgnoreMessageEventInRoom(currentRoomId, currentThreadId) -> REMOVE
                            .also { Timber.d("notification message removed due to currently viewing the same room or thread") }
                    outdatedDetector.isMessageOutdated(it) -> REMOVE
                            .also { Timber.d("notification message removed due to being read") }
                    else -> KEEP
                }
                is SimpleNotifiableEvent -> when (it.type) {
                    EventType.REDACTION -> REMOVE
                    else -> KEEP
                }
            }
            ProcessedEvent(type, it)
        }

        val removedEventsDiff = renderedEvents.filter { renderedEvent ->
            queuedEvents.none { it.eventId == renderedEvent.event.eventId }
        }.map { ProcessedEvent(REMOVE, it.event) }

        return removedEventsDiff + processedEvents
    }
}
