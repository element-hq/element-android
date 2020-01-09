/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.matrix.android.internal.session.room.prune

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.LocalEcho
import im.vector.matrix.android.api.session.events.model.UnsignedData
import im.vector.matrix.android.internal.database.helper.TimelineEventSenderVisitor
import im.vector.matrix.android.internal.database.mapper.ContentMapper
import im.vector.matrix.android.internal.database.mapper.EventMapper
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.TimelineEventEntity
import im.vector.matrix.android.internal.database.query.findWithSenderMembershipEvent
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.awaitTransaction
import io.realm.Realm
import timber.log.Timber
import javax.inject.Inject

internal interface PruneEventTask : Task<PruneEventTask.Params, Unit> {

    data class Params(
            val redactionEvents: List<Event>
    )
}

internal class DefaultPruneEventTask @Inject constructor(private val monarchy: Monarchy,
                                                         private val timelineEventSenderVisitor: TimelineEventSenderVisitor) : PruneEventTask {

    override suspend fun execute(params: PruneEventTask.Params) {
        monarchy.awaitTransaction { realm ->
            params.redactionEvents.forEach { event ->
                pruneEvent(realm, event)
            }
        }
    }

    private fun pruneEvent(realm: Realm, redactionEvent: Event) {
        if (redactionEvent.redacts.isNullOrBlank()) {
            return
        }

        // Check that we know this event
        EventEntity.where(realm, eventId = redactionEvent.eventId ?: "").findFirst() ?: return

        val isLocalEcho = LocalEcho.isLocalEchoId(redactionEvent.eventId ?: "")
        Timber.v("Redact event for ${redactionEvent.redacts} localEcho=$isLocalEcho")

        val eventToPrune = EventEntity.where(realm, eventId = redactionEvent.redacts).findFirst()
                ?: return

        val typeToPrune = eventToPrune.type
        val stateKey = eventToPrune.stateKey
        val allowedKeys = computeAllowedKeys(typeToPrune)
        if (allowedKeys.isNotEmpty()) {
            val prunedContent = ContentMapper.map(eventToPrune.content)?.filterKeys { key -> allowedKeys.contains(key) }
            eventToPrune.content = ContentMapper.map(prunedContent)
        } else {
            when (typeToPrune) {
                EventType.ENCRYPTED,
                EventType.MESSAGE -> {
                    Timber.d("REDACTION for message ${eventToPrune.eventId}")
                    val unsignedData = EventMapper.map(eventToPrune).unsignedData
                            ?: UnsignedData(null, null)

                    // was this event a m.replace
//                    val contentModel = ContentMapper.map(eventToPrune.content)?.toModel<MessageContent>()
//                    if (RelationType.REPLACE == contentModel?.relatesTo?.type && contentModel.relatesTo?.eventId != null) {
//                        eventRelationsAggregationUpdater.handleRedactionOfReplace(eventToPrune, contentModel.relatesTo!!.eventId!!, realm)
//                    }

                    val modified = unsignedData.copy(redactedEvent = redactionEvent)
                    eventToPrune.content = ContentMapper.map(emptyMap())
                    eventToPrune.unsignedData = MoshiProvider.providesMoshi().adapter(UnsignedData::class.java).toJson(modified)
                    eventToPrune.decryptionResultJson = null
                    eventToPrune.decryptionErrorCode = null
                }
//                EventType.REACTION -> {
//                    eventRelationsAggregationUpdater.handleReactionRedact(eventToPrune, realm, userId)
//                }
            }
        }
        if (typeToPrune == EventType.STATE_ROOM_MEMBER && stateKey != null) {
            timelineEventSenderVisitor.clear(roomId = eventToPrune.roomId, senderId = stateKey)
            val timelineEventsToUpdate = TimelineEventEntity.findWithSenderMembershipEvent(realm, eventToPrune.eventId)
            timelineEventSenderVisitor.visit(timelineEventsToUpdate)
        }
    }

    private fun computeAllowedKeys(type: String): List<String> {
        // Add filtered content, allowed keys in content depends on the event type
        return when (type) {
            EventType.STATE_ROOM_MEMBER          -> listOf("membership")
            EventType.STATE_ROOM_CREATE          -> listOf("creator")
            EventType.STATE_ROOM_JOIN_RULES      -> listOf("join_rule")
            EventType.STATE_ROOM_POWER_LEVELS    -> listOf("users",
                    "users_default",
                    "events",
                    "events_default",
                    "state_default",
                    "ban",
                    "kick",
                    "redact",
                    "invite")
            EventType.STATE_ROOM_ALIASES         -> listOf("aliases")
            EventType.STATE_ROOM_CANONICAL_ALIAS -> listOf("alias")
            EventType.FEEDBACK                   -> listOf("type", "target_event_id")
            else                                 -> emptyList()
        }
    }
}
