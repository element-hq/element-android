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
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.RoomMemberContent
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.internal.database.helper.addOrUpdate
import im.vector.matrix.android.internal.database.helper.addStateEvent
import im.vector.matrix.android.internal.database.helper.addTimelineEvent
import im.vector.matrix.android.internal.database.helper.deleteOnCascade
import im.vector.matrix.android.internal.database.helper.merge
import im.vector.matrix.android.internal.database.mapper.toEntity
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.model.TimelineEventEntity
import im.vector.matrix.android.internal.database.query.copyToRealmOrIgnore
import im.vector.matrix.android.internal.database.query.create
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.database.query.findAllIncludingEvents
import im.vector.matrix.android.internal.database.query.findLastForwardChunkOfRoom
import im.vector.matrix.android.internal.database.query.getOrCreate
import im.vector.matrix.android.internal.database.query.latestEvent
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.room.RoomSummaryUpdater
import im.vector.matrix.android.internal.util.awaitTransaction
import io.realm.Realm
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
            val currentLastForwardChunk = ChunkEntity.findLastForwardChunkOfRoom(realm, roomId)
            if (currentChunk != currentLastForwardChunk) {
                currentChunk.isLastForward = true
                currentLastForwardChunk?.deleteOnCascade()
                RoomSummaryEntity.where(realm, roomId).findFirst()?.apply {
                    latestPreviewableEvent = TimelineEventEntity.latestEvent(
                            realm,
                            roomId,
                            includesSending = true,
                            filterTypes = RoomSummaryUpdater.PREVIEWABLE_TYPES
                    )
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
        val roomMemberContentsByUser = HashMap<String, RoomMemberContent?>()
        val eventList = receivedChunk.events
        val stateEvents = receivedChunk.stateEvents

        for (stateEvent in stateEvents) {
            val stateEventEntity = stateEvent.toEntity(roomId, SendState.SYNCED).copyToRealmOrIgnore(realm)
            currentChunk.addStateEvent(roomId, stateEventEntity, direction)
            if (stateEvent.type == EventType.STATE_ROOM_MEMBER && stateEvent.stateKey != null) {
                roomMemberContentsByUser[stateEvent.stateKey] = stateEvent.content.toModel<RoomMemberContent>()
            }
        }
        val eventIds = ArrayList<String>(eventList.size)
        for (event in eventList) {
            if (event.eventId == null || event.senderId == null) {
                continue
            }
            eventIds.add(event.eventId)
            val eventEntity = event.toEntity(roomId, SendState.SYNCED).copyToRealmOrIgnore(realm)
            if (event.type == EventType.STATE_ROOM_MEMBER && event.stateKey != null) {
                val contentToUse = if (direction == PaginationDirection.BACKWARDS) {
                    event.prevContent
                } else {
                    event.content
                }
                roomMemberContentsByUser[event.stateKey] = contentToUse.toModel<RoomMemberContent>()
            }

            currentChunk.addTimelineEvent(roomId, eventEntity, direction, roomMemberContentsByUser)
        }
        val chunks = ChunkEntity.findAllIncludingEvents(realm, eventIds)
        val chunksToDelete = ArrayList<ChunkEntity>()
        chunks.forEach {
            if (it != currentChunk) {
                currentChunk.merge(roomId, it, direction)
                chunksToDelete.add(it)
            }
        }
        val shouldUpdateSummary = chunksToDelete.isNotEmpty() && currentChunk.isLastForward && direction == PaginationDirection.FORWARDS
        chunksToDelete.forEach {
            it.deleteOnCascade()
        }
        if (shouldUpdateSummary) {
            val roomSummaryEntity = RoomSummaryEntity.getOrCreate(realm, roomId)
            val latestPreviewableEvent = TimelineEventEntity.latestEvent(
                    realm,
                    roomId,
                    includesSending = true,
                    filterTypes = RoomSummaryUpdater.PREVIEWABLE_TYPES
            )
            roomSummaryEntity.latestPreviewableEvent = latestPreviewableEvent
        }
        RoomEntity.where(realm, roomId).findFirst()?.addOrUpdate(currentChunk)
    }
}
