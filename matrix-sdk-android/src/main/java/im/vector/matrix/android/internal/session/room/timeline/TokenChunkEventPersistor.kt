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

import arrow.core.Try
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.internal.database.helper.addAll
import im.vector.matrix.android.internal.database.helper.addOrUpdate
import im.vector.matrix.android.internal.database.helper.addStateEvents
import im.vector.matrix.android.internal.database.helper.deleteOnCascade
import im.vector.matrix.android.internal.database.helper.isUnlinked
import im.vector.matrix.android.internal.database.helper.merge
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.query.create
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.database.query.findAllIncludingEvents
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.util.tryTransactionSync
import io.realm.kotlin.createObject


internal class TokenChunkEventPersistor(private val monarchy: Monarchy) {

    enum class Result {
        SHOULD_FETCH_MORE,
        SUCCESS
    }

    fun insertInDb(receivedChunk: TokenChunkEvent,
                   roomId: String,
                   direction: PaginationDirection): Try<Result> {

        return monarchy
                .tryTransactionSync { realm ->
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
                    val prevChunk = ChunkEntity.find(realm, roomId, nextToken = prevToken)
                    val nextChunk = ChunkEntity.find(realm, roomId, prevToken = nextToken)

                    // The current chunk is the one we will keep all along the merge processChanges.
                    // We try to look for a chunk next to the token,
                    // otherwise we create a whole new one

                    var currentChunk = if (direction == PaginationDirection.FORWARDS) {
                        prevChunk?.apply { this.nextToken = nextToken }
                        ?: ChunkEntity.create(realm, prevToken, nextToken)
                    } else {
                        nextChunk?.apply { this.prevToken = prevToken }
                        ?: ChunkEntity.create(realm, prevToken, nextToken)
                    }
                    if (receivedChunk.events.isEmpty() && receivedChunk.end == receivedChunk.start) {
                        currentChunk.isLastBackward = true
                    } else {
                        currentChunk.addAll(roomId, receivedChunk.events, direction, isUnlinked = currentChunk.isUnlinked())

                        // Then we merge chunks if needed
                        if (currentChunk != prevChunk && prevChunk != null) {
                            currentChunk = handleMerge(roomEntity, direction, currentChunk, prevChunk)
                        } else if (currentChunk != nextChunk && nextChunk != null) {
                            currentChunk = handleMerge(roomEntity, direction, currentChunk, nextChunk)
                        } else {
                            val newEventIds = receivedChunk.events.mapNotNull { it.eventId }
                            ChunkEntity
                                    .findAllIncludingEvents(realm, newEventIds)
                                    .filter { it != currentChunk }
                                    .forEach { overlapped ->
                                        currentChunk = handleMerge(roomEntity, direction, currentChunk, overlapped)
                                    }
                        }
                        roomEntity.addOrUpdate(currentChunk)
                        roomEntity.addStateEvents(receivedChunk.stateEvents, isUnlinked = currentChunk.isUnlinked())
                    }
                }
                .map {
                    if (receivedChunk.events.isEmpty() && receivedChunk.stateEvents.isEmpty() && receivedChunk.start != receivedChunk.end) {
                        Result.SHOULD_FETCH_MORE
                    } else {
                        Result.SUCCESS
                    }
                }
    }

    private fun handleMerge(roomEntity: RoomEntity,
                            direction: PaginationDirection,
                            currentChunk: ChunkEntity,
                            otherChunk: ChunkEntity): ChunkEntity {

        // We always merge the bottom chunk into top chunk, so we are always merging backwards
        return if (direction == PaginationDirection.BACKWARDS) {
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