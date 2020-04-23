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

import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.RoomMemberContent
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.internal.database.awaitTransaction
import im.vector.matrix.android.internal.database.helper.addTimelineEvent
import im.vector.matrix.android.internal.database.mapper.toSQLEntity
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.sqldelight.session.SessionDatabase
import timber.log.Timber
import javax.inject.Inject
import kotlin.collections.set

/**
 * Insert Chunk in DB, and eventually merge with existing chunk event
 */
internal class TokenChunkEventPersistor @Inject constructor(private val sessionDatabase: SessionDatabase,
                                                            private val coroutineDispatchers: MatrixCoroutineDispatchers ) {

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
                           direction: PaginationDirection,
                           chunkId: Long?): Result {
        sessionDatabase
                .awaitTransaction(coroutineDispatchers) {
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

                    val prevChunk = sessionDatabase.chunkQueries.getWithNextToken(roomId = roomId, nextToken = prevToken).executeAsOneOrNull()
                    val nextChunk = sessionDatabase.chunkQueries.getWithPrevToken(roomId = roomId, prevToken = nextToken).executeAsOneOrNull()

                    Timber.v("Prev chunk: $prevChunk")
                    Timber.v("Next chunk: $nextChunk")

                    val currentChunkId = if (direction == PaginationDirection.FORWARDS) {
                        if (prevChunk == null) {
                            if (chunkId != null) {
                                sessionDatabase.chunkQueries.setPrevToken(prevToken, chunkId)
                                sessionDatabase.chunkQueries.setNextToken(nextToken, chunkId)
                            } else {
                                sessionDatabase.chunkQueries.insert(roomId, nextToken, prevToken, false, false)
                            }
                            sessionDatabase.chunkQueries.getChunkIdWithNextAndPrevToken(roomId, nextToken, prevToken).executeAsOne()
                        } else {
                            sessionDatabase.chunkQueries.setNextToken(nextToken, prevChunk.chunk_id)
                            prevChunk.chunk_id
                        }
                    } else {
                        if (nextChunk == null) {
                            if (chunkId != null) {
                                sessionDatabase.chunkQueries.setPrevToken(prevToken, chunkId)
                                sessionDatabase.chunkQueries.setNextToken(nextToken, chunkId)
                            } else {
                                sessionDatabase.chunkQueries.insert(roomId, nextToken, prevToken, false, false)
                            }
                            sessionDatabase.chunkQueries.getChunkIdWithNextAndPrevToken(roomId, nextToken, prevToken).executeAsOne()
                        } else {
                            sessionDatabase.chunkQueries.setPrevToken(prevToken, nextChunk.chunk_id)
                            nextChunk.chunk_id
                        }
                    }
                    if (receivedChunk.events.isEmpty() && receivedChunk.end == receivedChunk.start) {
                        handleReachEnd(roomId, direction, currentChunkId)
                    } else {
                        handlePagination(roomId, direction, receivedChunk, currentChunkId)
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

    private fun handleReachEnd(roomId: String, direction: PaginationDirection, currentChunkId: Long) {
        Timber.v("Reach end of $roomId")
        if (direction == PaginationDirection.FORWARDS) {
            val currentLiveChunkId = sessionDatabase.chunkQueries.getChunkIdOfLive(roomId).executeAsOne()
            if (currentChunkId != currentLiveChunkId) {
                sessionDatabase.chunkQueries.deleteWithId(currentLiveChunkId)
                sessionDatabase.chunkQueries.setIsLastForwards(true, currentChunkId)
                /*
                RoomSummaryEntity.where(realm, roomId).findFirst()?.apply {
                    latestPreviewableEvent = TimelineEventEntity.latestEvent(
                            realm,
                            roomId,
                            includesSending = true,
                            filterTypes = RoomSummaryUpdater.PREVIEWABLE_TYPES
                    )
                }

                 */
            }
        } else {
            sessionDatabase.chunkQueries.setIsLastBackwards(true, currentChunkId)
        }
    }

    private fun handlePagination(
            roomId: String,
            direction: PaginationDirection,
            receivedChunk: TokenChunkEvent,
            currentChunkId: Long
    ) {
        Timber.v("Add ${receivedChunk.events.size} events in chunk($currentChunkId")
        val roomMemberContentsByUser = HashMap<String, RoomMemberContent?>()
        val eventList = receivedChunk.events
        val stateEvents = receivedChunk.stateEvents

        for (stateEvent in stateEvents) {
            if (stateEvent.eventId == null || stateEvent.stateKey == null) {
                continue
            }
            val eventEntity = stateEvent.toSQLEntity(roomId, SendState.SYNCED)
            sessionDatabase.eventQueries.insert(eventEntity)
            if (stateEvent.type == EventType.STATE_ROOM_MEMBER) {
                roomMemberContentsByUser[stateEvent.stateKey] = stateEvent.content.toModel<RoomMemberContent>()
            }
        }

        val eventIds = ArrayList<String>(eventList.size)
        for (event in eventList) {
            if (event.eventId == null || event.senderId == null) {
                continue
            }
            eventIds.add(event.eventId)
            if (sessionDatabase.eventQueries.exist(roomId = roomId, eventId = event.eventId).executeAsOneOrNull() == null) {
                val eventEntity = event.toSQLEntity(roomId, SendState.SYNCED)
                sessionDatabase.eventQueries.insert(eventEntity)
            }
            if (event.type == EventType.STATE_ROOM_MEMBER && event.stateKey != null) {
                val contentToUse = if (direction == PaginationDirection.BACKWARDS) {
                    event.prevContent
                } else {
                    event.content
                }
                roomMemberContentsByUser[event.stateKey] = contentToUse.toModel<RoomMemberContent>()
            }
            sessionDatabase.addTimelineEvent(
                    roomId = roomId,
                    chunkId = currentChunkId,
                    direction = direction,
                    event = event,
                    roomMemberContentsByUser = roomMemberContentsByUser
            )
        }

        /*
        val chunks = sessionDatabase.chunkQueriesChunkEntity.findAllIncludingEvents(realm, eventIds)
        val chunksToDelete = ArrayList<ChunkEntity>()
        chunks.forEach {
            if (it != currentChunk) {s
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
         */
    }
}
