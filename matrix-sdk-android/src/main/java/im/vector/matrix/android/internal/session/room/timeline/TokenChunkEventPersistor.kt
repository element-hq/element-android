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
import im.vector.matrix.android.internal.database.helper.*
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.query.create
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.database.query.findAllIncludingEvents
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.user.UserEntityFactory
import im.vector.matrix.android.internal.util.awaitTransaction
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

                    val roomEntity = RoomEntity.where(realm, roomId).findFirst()
                                     ?: realm.createObject(roomId)

                    val nextToken: String?
                    val prevToken: String?
                    if (direction == PaginationDirection.FORWARDS) {
                        nextToken = receivedChunk.end
                        prevToken = receivedChunk.start
                    } else {
                        nextToken = receivedChunk.start
                        prevToken = receivedChunk.end
                    }

                    val shouldSkip = ChunkEntity.find(realm, roomId, nextToken = nextToken) != null
                                     || ChunkEntity.find(realm, roomId, prevToken = prevToken) != null

                    val prevChunk = ChunkEntity.find(realm, roomId, nextToken = prevToken)
                    val nextChunk = ChunkEntity.find(realm, roomId, prevToken = nextToken)

                    // The current chunk is the one we will keep all along the merge processChanges.
                    // We try to look for a chunk next to the token,
                    // otherwise we create a whole new one

                    var currentChunk = if (direction == PaginationDirection.FORWARDS) {
                        prevChunk?.apply { this.nextToken = nextToken }
                    } else {
                        nextChunk?.apply { this.prevToken = prevToken }
                    }
                                       ?: ChunkEntity.create(realm, prevToken, nextToken)

                    if (receivedChunk.events.isEmpty() && receivedChunk.end == receivedChunk.start) {
                        Timber.v("Reach end of $roomId")
                        currentChunk.isLastBackward = true
                    } else if (!shouldSkip) {
                        Timber.v("Add ${receivedChunk.events.size} events in chunk(${currentChunk.nextToken} | ${currentChunk.prevToken}")
                        val eventIds = ArrayList<String>(receivedChunk.events.size)
                        for (event in receivedChunk.events) {
                            event.eventId?.also { eventIds.add(it) }
                            currentChunk.add(roomId, event, direction, isUnlinked = currentChunk.isUnlinked())
                            UserEntityFactory.createOrNull(event)?.also {
                                realm.insertOrUpdate(it)
                            }
                        }
                        // Then we merge chunks if needed
                        if (currentChunk != prevChunk && prevChunk != null) {
                            currentChunk = handleMerge(roomEntity, direction, currentChunk, prevChunk)
                        } else if (currentChunk != nextChunk && nextChunk != null) {
                            currentChunk = handleMerge(roomEntity, direction, currentChunk, nextChunk)
                        } else {
                            val newEventIds = receivedChunk.events.mapNotNull { it.eventId }
                            val overlappedChunks = ChunkEntity.findAllIncludingEvents(realm, newEventIds)
                            overlappedChunks
                                    .filter { it != currentChunk }
                                    .forEach { overlapped ->
                                        currentChunk = handleMerge(roomEntity, direction, currentChunk, overlapped)
                                    }
                        }
                        roomEntity.addOrUpdate(currentChunk)
                        for (stateEvent in receivedChunk.stateEvents) {
                            roomEntity.addStateEvent(stateEvent, isUnlinked = currentChunk.isUnlinked())
                            UserEntityFactory.createOrNull(stateEvent)?.also {
                                realm.insertOrUpdate(it)
                            }
                        }
                        currentChunk.updateSenderDataFor(eventIds)
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

    private fun handleMerge(roomEntity: RoomEntity,
                            direction: PaginationDirection,
                            currentChunk: ChunkEntity,
                            otherChunk: ChunkEntity): ChunkEntity {

        // We always merge the bottom chunk into top chunk, so we are always merging backwards
        Timber.v("Merge ${currentChunk.prevToken} | ${currentChunk.nextToken} with ${otherChunk.prevToken} | ${otherChunk.nextToken}")
        return if (direction == PaginationDirection.BACKWARDS && !otherChunk.isLastForward) {
            currentChunk.merge(roomEntity.roomId, otherChunk, PaginationDirection.BACKWARDS)
            roomEntity.deleteOnCascade(otherChunk)
            currentChunk
        } else {
            otherChunk.merge(roomEntity.roomId, currentChunk, PaginationDirection.BACKWARDS)
            roomEntity.deleteOnCascade(currentChunk)
            otherChunk
        }
    }

}