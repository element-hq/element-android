/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.timeline

import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.EventAnnotationsSummary
import org.matrix.android.sdk.api.session.room.model.ReactionAggregatedSummary
import org.matrix.android.sdk.api.session.room.model.relation.ReactionContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import timber.log.Timber
import java.util.Collections

internal class UIEchoManager(private val listener: Listener) {

    interface Listener {
        fun rebuildEvent(eventId: String, builder: (TimelineEvent) -> TimelineEvent?): Boolean
    }

    private val inMemorySendingEvents = Collections.synchronizedList<TimelineEvent>(ArrayList())

    fun getInMemorySendingEvents(): List<TimelineEvent> {
        return inMemorySendingEvents.toList()
    }

    /**
     * Due to lag of DB updates, we keep some UI echo of some properties to update timeline faster
     */
    private val inMemorySendingStates = Collections.synchronizedMap<String, SendState>(HashMap())

    private val inMemoryReactions = Collections.synchronizedMap<String, MutableList<ReactionUiEchoData>>(HashMap())

    fun onSentEventsInDatabase(eventIds: List<String>) {
        // Remove in memory as soon as they are known by database
        eventIds.forEach { eventId ->
            inMemorySendingEvents.removeAll { eventId == it.eventId }
        }
        inMemoryReactions.forEach { (_, uiEchoData) ->
            uiEchoData.removeAll { data ->
                // I remove the uiEcho, when the related event is not anymore in the sending list
                // (means that it is synced)!
                eventIds.find { it == data.localEchoId } == null
            }
        }
    }

    fun onSendStateUpdated(eventId: String, sendState: SendState): Boolean {
        val existingState = inMemorySendingStates[eventId]
        inMemorySendingStates[eventId] = sendState
        return existingState != sendState
    }

    fun onLocalEchoCreated(timelineEvent: TimelineEvent): Boolean {
        when (timelineEvent.root.getClearType()) {
            EventType.REDACTION -> {
            }
            EventType.REACTION  -> {
                val content: ReactionContent? = timelineEvent.root.content?.toModel<ReactionContent>()
                if (RelationType.ANNOTATION == content?.relatesTo?.type) {
                    val reaction = content.relatesTo.key
                    val relatedEventID = content.relatesTo.eventId
                    inMemoryReactions.getOrPut(relatedEventID) { mutableListOf() }
                            .add(
                                    ReactionUiEchoData(
                                            localEchoId = timelineEvent.eventId,
                                            reactedOnEventId = relatedEventID,
                                            reaction = reaction
                                    )
                            )
                    listener.rebuildEvent(relatedEventID) {
                        decorateEventWithReactionUiEcho(it)
                    }
                }
            }
        }
        Timber.v("On local echo created: ${timelineEvent.eventId}")
        inMemorySendingEvents.add(0, timelineEvent)
        return true
    }

    fun decorateEventWithReactionUiEcho(timelineEvent: TimelineEvent): TimelineEvent {
        val relatedEventID = timelineEvent.eventId
        val contents = inMemoryReactions[relatedEventID] ?: return timelineEvent

        var existingAnnotationSummary = timelineEvent.annotations ?: EventAnnotationsSummary(
                relatedEventID
        )
        val updateReactions = existingAnnotationSummary.reactionsSummary.toMutableList()

        contents.forEach { uiEchoReaction ->
            val indexOfExistingReaction = updateReactions.indexOfFirst { it.key == uiEchoReaction.reaction }
            if (indexOfExistingReaction == -1) {
                // just add the new key
                ReactionAggregatedSummary(
                        key = uiEchoReaction.reaction,
                        count = 1,
                        addedByMe = true,
                        firstTimestamp = System.currentTimeMillis(),
                        sourceEvents = emptyList(),
                        localEchoEvents = listOf(uiEchoReaction.localEchoId)
                ).let { updateReactions.add(it) }
            } else {
                // update Existing Key
                val existing = updateReactions[indexOfExistingReaction]
                if (!existing.localEchoEvents.contains(uiEchoReaction.localEchoId)) {
                    updateReactions.remove(existing)
                    // only update if echo is not yet there
                    ReactionAggregatedSummary(
                            key = existing.key,
                            count = existing.count + 1,
                            addedByMe = true,
                            firstTimestamp = existing.firstTimestamp,
                            sourceEvents = existing.sourceEvents,
                            localEchoEvents = existing.localEchoEvents + uiEchoReaction.localEchoId

                    ).let { updateReactions.add(indexOfExistingReaction, it) }
                }
            }
        }

        existingAnnotationSummary = existingAnnotationSummary.copy(
                reactionsSummary = updateReactions
        )
        return timelineEvent.copy(
                annotations = existingAnnotationSummary
        )
    }

    fun updateSentStateWithUiEcho(timelineEvent: TimelineEvent): TimelineEvent {
        if (timelineEvent.root.sendState.isSent()) return timelineEvent
        val inMemoryState = inMemorySendingStates[timelineEvent.eventId] ?: return timelineEvent
        // Timber.v("## ${System.currentTimeMillis()} Send event refresh echo with live state $inMemoryState from state ${element.root.sendState}")
        return timelineEvent.copy(
                root = timelineEvent.root.copyAll()
                        .also { it.sendState = inMemoryState }
        )
    }

    fun onSyncedEvent(transactionId: String?) {
        val sendingEvent = inMemorySendingEvents.find {
            it.eventId == transactionId
        }
        inMemorySendingEvents.remove(sendingEvent)
        // Is it too early to clear it? will be done when removed from sending anyway?
        inMemoryReactions.forEach { (_, u) ->
            u.filterNot { it.localEchoId == transactionId }
        }
        inMemorySendingStates.remove(transactionId)
    }
}
