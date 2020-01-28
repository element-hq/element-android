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

package im.vector.matrix.android.internal.session.room.timeline

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.internal.database.helper.addOrUpdate
import im.vector.matrix.android.internal.database.helper.addTimelineEvent
import im.vector.matrix.android.internal.database.helper.deleteOnCascade
import im.vector.matrix.android.internal.database.helper.merge
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.mapper.toEntity
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.CurrentStateEventEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.model.TimelineEventEntity
import im.vector.matrix.android.internal.database.query.create
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.database.query.findAllIncludingEvents
import im.vector.matrix.android.internal.database.query.findLastLiveChunkFromRoom
import im.vector.matrix.android.internal.database.query.getOrNull
import im.vector.matrix.android.internal.database.query.latestEvent
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.room.RoomSummaryUpdater
import im.vector.matrix.android.internal.util.awaitTransaction
import io.realm.Realm
import io.realm.RealmList
import io.realm.kotlin.createObject
import timber.log.Timber
import javax.inject.Inject

/**
 * Insert Chunk in DB, and eventually merge with existing chunk event
 */
internal class TokenChunkEventPersistor @Inject constructor(private val monarchy: Monarchy) {

    /**
     * <pre>
     * ========================================================================================================
     * | Backward case                                                                                        |
     * ========================================================================================================
     *
     *                               *--------------------------*        *--------------------------*
     *                               | startToken1              |        | startToken1              |
     *                               *--------------------------*        *--------------------------*
     *                               |                          |        |                          |
     *                               |                          |        |                          |
     *                               |  receivedChunk backward  |        |                          |
     *                               |         Events           |        |                          |
     *                               |                          |        |                          |
     *                               |                          |        |                          |
     *                               |                          |        |                          |
     * *--------------------------*  *--------------------------*        |                          |
     * | startToken0              |  | endToken1                |   =>   |       Merged chunk       |
     * *--------------------------*  *--------------------------*        |          Events          |
     * |                          |                                      |                          |
     * |                          |                                      |                          |
     * |      Current Chunk       |                                      |                          |
     * |         Events           |                                      |                          |
     * |                          |                                      |                          |
     * |                          |                                      |                          |
     * |                          |                                      |                          |
     * *--------------------------*                                      *--------------------------*
     * | endToken0                |                                      | endToken0                |
     * *--------------------------*                                      *--------------------------*
     *
     *
     * ========================================================================================================
     * | Forward case                                                                                         |
     * ========================================================================================================
     *
     * *--------------------------*                                      *--------------------------*
     * | startToken0              |                                      | startToken0              |
     * *--------------------------*                                      *--------------------------*
     * |                          |                                      |                          |
     * |                          |                                      |                          |
     * |      Current Chunk       |                                      |                          |
     * |         Events           |                                      |                          |
     * |                          |                                      |                          |
     * |                          |                                      |                          |
     * |                          |                                      |                          |
     * *--------------------------*  *--------------------------*        |                          |
     * | endToken0                |  | startToken1              |   =>   |       Merged chunk       |
     * *--------------------------*  *--------------------------*        |          Events          |
     *                               |                          |        |                          |
     *                               |                          |        |                          |
     *                               |  receivedChunk forward   |        |                          |
     *                               |         Events           |        |                          |
     *                               |                          |        |                          |
     *                               |                          |        |                          |
     *                               |                          |        |                          |
     *                               *--------------------------*        *--------------------------*
     *                               | endToken1                |        | endToken1                |
     *                               *--------------------------*        *--------------------------*
     *
     * ========================================================================================================
     * </pre>
     */

    enum class Result {
        SHOULD_FETCH_MORE,
        REACHED_END,
        SUCCESS
    }

    suspend fun insertInDb(receivedChunk: TokenChunkEvent,
                           roomId: String,
                           direction: PaginationDirection): Result {
        monarchy
                .awaitTransaction { realm ->
                    Timber.v("Start persisting ${receivedChunk.events.size} events in $roomId towards $direction")

                    val nextToken: String?
                    val prevToken: String?
                    if (direction == PaginationDirection.FORWARDS) {
                        nextToken = receivedChunk.end
                        prevToken = receivedChunk.start
                    } else {
                        nextToken = receivedChunk.start
                        prevToken = receivedChunk.end
                    }

                    val prevChunk = ChunkEntity.find(realm, roomId, nextToken = prevToken)
                    val nextChunk = ChunkEntity.find(realm, roomId, prevToken = nextToken)

                    // The current chunk is the one we will keep all along the merge processChanges.
                    // We try to look for a chunk next to the token,
                    // otherwise we create a whole new one which is unlinked (not live)
                    val currentChunk = if (direction == PaginationDirection.FORWARDS) {
                        prevChunk?.apply { this.nextToken = nextToken }
                    } else {
                        nextChunk?.apply { this.prevToken = prevToken }
                    }
                            ?: ChunkEntity.create(realm, prevToken, nextToken)

                    if (receivedChunk.events.isEmpty() && receivedChunk.end == receivedChunk.start) {
                        handleReachEnd(realm, roomId, direction, currentChunk)
                    } else {
                        handlePagination(realm, roomId, direction, receivedChunk, currentChunk)
                    }
                }
        return if (receivedChunk.events.isEmpty()) {
            if (receivedChunk.start != receivedChunk.end) {
                Result.SHOULD_FETCH_MORE
            } else {
                Result.REACHED_END
            }
        } else {
            Result.SUCCESS
        }
    }

    private fun handleReachEnd(realm: Realm, roomId: String, direction: PaginationDirection, currentChunk: ChunkEntity) {
        Timber.v("Reach end of $roomId")
        if (direction == PaginationDirection.FORWARDS) {
            val currentLiveChunk = ChunkEntity.findLastLiveChunkFromRoom(realm, roomId)
            if (currentChunk != currentLiveChunk) {
                currentChunk.isLastForward = true
                currentLiveChunk?.deleteOnCascade()
                RoomSummaryEntity.where(realm, roomId).findFirst()?.apply {
                    latestPreviewableEvent = TimelineEventEntity.latestEvent(realm, roomId, includesSending = true, filterTypes = RoomSummaryUpdater.PREVIEWABLE_TYPES)
                }
            }
        } else {
            currentChunk.isLastBackward = true
        }
    }

    private fun handlePagination(
            realm: Realm,
            roomId: String,
            direction: PaginationDirection,
            receivedChunk: TokenChunkEvent,
            currentChunk: ChunkEntity
    ) {
        Timber.v("Add ${receivedChunk.events.size} events in chunk(${currentChunk.nextToken} | ${currentChunk.prevToken}")
        val roomMemberEventsByUser = HashMap<String, Event?>()
        val eventList = if (direction == PaginationDirection.FORWARDS) {
            receivedChunk.events
        } else {
            receivedChunk.events.asReversed()
        }
        val stateEvents = receivedChunk.stateEvents
        for (stateEvent in stateEvents) {
            if (stateEvent.type == EventType.STATE_ROOM_MEMBER && stateEvent.stateKey != null && !stateEvent.isRedacted()) {
                roomMemberEventsByUser[stateEvent.stateKey] = stateEvent
            }
        }
        val eventIds = ArrayList<String>(eventList.size)
        val eventEntities = ArrayList<EventEntity>(eventList.size)
        for (event in eventList) {
            if (event.eventId == null || event.senderId == null) {
                continue
            }
            eventIds.add(event.eventId)
            val eventEntity = event.toEntity(roomId, SendState.SYNCED).let {
                realm.copyToRealmOrUpdate(it)
            }
            if (direction == PaginationDirection.FORWARDS) {
                eventEntities.add(eventEntity)
            } else {
                eventEntities.add(0, eventEntity)
            }
            if (event.type == EventType.STATE_ROOM_MEMBER && event.stateKey != null && !event.isRedacted()) {
                roomMemberEventsByUser[event.stateKey] = event
            }
        }
        for (eventEntity in eventEntities) {
            val senderId = eventEntity.sender ?: continue
            val roomMemberEvent = roomMemberEventsByUser.getOrPut(senderId) {
                CurrentStateEventEntity.getOrNull(realm, roomId, senderId, EventType.STATE_ROOM_MEMBER)?.root?.asDomain()
            }
            currentChunk.addTimelineEvent(roomId, eventEntity, direction, roomMemberEvent)
        }

        val chunks = ChunkEntity.findAllIncludingEvents(realm, eventIds)
        val chunksToDelete = ArrayList<ChunkEntity>()
        chunks.forEach {
            if (it != currentChunk) {
                currentChunk.merge(it, direction)
                chunksToDelete.add(it)
            }
        }
        chunksToDelete.forEach {
            it.deleteFromRealm()
        }
        RoomEntity.where(realm, roomId).findFirst()?.addOrUpdate(currentChunk)
    }
}
