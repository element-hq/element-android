/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.session.room.send

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.RoomMemberContent
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessageType
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.internal.database.awaitTransaction
import im.vector.matrix.android.internal.database.helper.addTimelineEvent
import im.vector.matrix.android.internal.database.mapper.ContentMapper
import im.vector.matrix.android.internal.database.mapper.EventMapper
import im.vector.matrix.android.internal.database.mapper.TimelineEventMapper
import im.vector.matrix.android.internal.database.mapper.toSQLEntity
import im.vector.matrix.android.internal.session.room.RoomSummaryUpdater
import im.vector.matrix.android.internal.session.room.membership.RoomMemberSummaryDataSource
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import im.vector.matrix.android.internal.session.room.timeline.SQLTimeline
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.sqldelight.session.SessionDatabase
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import javax.inject.Inject

internal class LocalEchoRepository @Inject constructor(private val sessionDatabase: SessionDatabase,
                                                       private val roomSummaryUpdater: RoomSummaryUpdater,
                                                       private val eventBus: EventBus,
                                                       private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                                       private val timelineEventMapper: TimelineEventMapper,
                                                       private val roomMemberSummaryDataSource: RoomMemberSummaryDataSource) {

    suspend fun createLocalEcho(event: Event) {
        val roomId = event.roomId
                ?: throw IllegalStateException("You should have set a roomId for your event")
        event.senderId
                ?: throw IllegalStateException("You should have set a senderIf for your event")
        if (event.eventId == null) {
            throw IllegalStateException("You should have set an eventId for your event")
        }
        eventBus.post(SQLTimeline.OnNewTimelineEvents(roomId = roomId, eventIds = listOf(event.eventId)))
        sessionDatabase.awaitTransaction(coroutineDispatchers) {
            val eventEntity = event.toSQLEntity(roomId, SendState.UNSENT)
            sessionDatabase.eventQueries.insert(eventEntity)
            val rootStateEvent = sessionDatabase.stateEventQueries.getCurrentStateEvent(roomId, stateKey = event.senderId, type = EventType.STATE_ROOM_MEMBER).executeAsOneOrNull()
            val roomMemberContentsByUser: Map<String, RoomMemberContent?> = mapOf(
                    event.senderId to ContentMapper.map(rootStateEvent?.content).toModel()
            )
            val localEchoChunkId = sessionDatabase.chunkQueries.getLocalEchoChunkId(roomId).executeAsOne()
            sessionDatabase.addTimelineEvent(roomId, localEchoChunkId, event, PaginationDirection.FORWARDS, roomMemberContentsByUser)
            sessionDatabase.timelineEventQueries.get(event.eventId).executeAsOne()
            roomSummaryUpdater.update(roomId)
        }
    }

    suspend fun deleteFailedEcho(localEcho: TimelineEvent) {
        sessionDatabase.awaitTransaction(coroutineDispatchers) {
            it.eventQueries.delete(localEcho.eventId)
        }
    }

    suspend fun clearSendingQueue(roomId: String) {
        sessionDatabase.awaitTransaction(coroutineDispatchers) {
            it.eventQueries.updateSendStateFromLocalEchoChunk(SendState.UNDELIVERED.name, roomId)
        }
    }

    suspend fun updateSendState(eventIds: List<String>, sendState: SendState) {
        sessionDatabase.awaitTransaction(coroutineDispatchers) {
            it.eventQueries.updateSendState(sendState.name, eventIds, emptyList())
        }
    }

    fun getAllFailedEventsToResend(roomId: String): List<Event> {
        val failedStates = listOf(SendState.UNDELIVERED.name, SendState.FAILED_UNKNOWN_DEVICES.name)
        return sessionDatabase.eventQueries.getEventsWithSendStatesFromLocalEchoChunk(roomId, failedStates).executeAsList()
                .map { eventEntity -> EventMapper.map(eventEntity) }
                .filter { event ->
                    when (event.getClearType()) {
                        EventType.MESSAGE,
                        EventType.REDACTION,
                        EventType.REACTION -> {
                            val content = event.getClearContent().toModel<MessageContent>()
                            if (content != null) {
                                when (content.msgType) {
                                    MessageType.MSGTYPE_EMOTE,
                                    MessageType.MSGTYPE_NOTICE,
                                    MessageType.MSGTYPE_LOCATION,
                                    MessageType.MSGTYPE_TEXT -> {
                                        true
                                    }
                                    MessageType.MSGTYPE_FILE,
                                    MessageType.MSGTYPE_VIDEO,
                                    MessageType.MSGTYPE_IMAGE,
                                    MessageType.MSGTYPE_AUDIO -> {
                                        // need to resend the attachment
                                        false
                                    }
                                    else -> {
                                        Timber.e("Cannot resend message ${event.type} / ${content.msgType}")
                                        false
                                    }
                                }
                            } else {
                                Timber.e("Unsupported message to resend ${event.type}")
                                false
                            }
                        }
                        else -> {
                            Timber.e("Unsupported message to resend ${event.type}")
                            false
                        }
                    }
                }
    }
}
