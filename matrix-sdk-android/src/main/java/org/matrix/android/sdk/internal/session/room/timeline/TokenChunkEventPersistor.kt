/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.timeline

import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.database.helper.addIfNecessary
import org.matrix.android.sdk.internal.database.helper.addStateEvent
import org.matrix.android.sdk.internal.database.helper.addTimelineEvent
import org.matrix.android.sdk.internal.database.helper.merge
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.deleteOnCascade
import org.matrix.android.sdk.internal.database.query.copyToRealmOrIgnore
import org.matrix.android.sdk.internal.database.query.create
import org.matrix.android.sdk.internal.database.query.find
import org.matrix.android.sdk.internal.database.query.findAllIncludingEvents
import org.matrix.android.sdk.internal.database.query.findLastForwardChunkOfRoom
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.room.summary.RoomSummaryEventsHelper
import org.matrix.android.sdk.internal.util.awaitTransaction
import timber.log.Timber
import javax.inject.Inject

/**
 * Insert Chunk in DB, and eventually merge with existing chunk event
 */
internal class TokenChunkEventPersistor @Inject constructor(@SessionDatabase private val monarchy: Monarchy) {

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

                    if (receivedChunk.events.isNullOrEmpty() && !receivedChunk.hasMore()) {
                        handleReachEnd(realm, roomId, direction, currentChunk)
                    } else {
                        handlePagination(realm, roomId, direction, receivedChunk, currentChunk)
                    }
                }
        return if (receivedChunk.events.isEmpty()) {
            if (receivedChunk.hasMore()) {
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
                currentLastForwardChunk?.deleteOnCascade(deleteStateEvents = false, canDeleteRoot = false)
                RoomSummaryEntity.where(realm, roomId).findFirst()?.apply {
                    latestPreviewableEvent = RoomSummaryEventsHelper.getLatestPreviewableEvent(realm, roomId)
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

        val now = System.currentTimeMillis()

        stateEvents?.forEach { stateEvent ->
            val ageLocalTs = stateEvent.unsignedData?.age?.let { now - it }
            val stateEventEntity = stateEvent.toEntity(roomId, SendState.SYNCED, ageLocalTs).copyToRealmOrIgnore(realm, EventInsertType.PAGINATION)
            currentChunk.addStateEvent(roomId, stateEventEntity, direction)
            if (stateEvent.type == EventType.STATE_ROOM_MEMBER && stateEvent.stateKey != null) {
                roomMemberContentsByUser[stateEvent.stateKey] = stateEvent.content.toModel<RoomMemberContent>()
            }
        }
        val eventIds = ArrayList<String>(eventList.size)
        eventList.forEach { event ->
            if (event.eventId == null || event.senderId == null) {
                return@forEach
            }
            val ageLocalTs = event.unsignedData?.age?.let { now - it }
            eventIds.add(event.eventId)
            val eventEntity = event.toEntity(roomId, SendState.SYNCED, ageLocalTs).copyToRealmOrIgnore(realm, EventInsertType.PAGINATION)
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
        // Find all the chunks which contain at least one event from the list of eventIds
        val chunks = ChunkEntity.findAllIncludingEvents(realm, eventIds)
        Timber.d("Found ${chunks.size} chunks containing at least one of the eventIds")
        val chunksToDelete = ArrayList<ChunkEntity>()
        chunks.forEach {
            if (it != currentChunk) {
                Timber.d("Merge $it")
                currentChunk.merge(roomId, it, direction)
                chunksToDelete.add(it)
            }
        }
        chunksToDelete.forEach {
            it.deleteOnCascade(deleteStateEvents = false, canDeleteRoot = false)
        }
        val roomSummaryEntity = RoomSummaryEntity.getOrCreate(realm, roomId)
        val shouldUpdateSummary = roomSummaryEntity.latestPreviewableEvent == null
                || (chunksToDelete.isNotEmpty() && currentChunk.isLastForward && direction == PaginationDirection.FORWARDS)
        if (shouldUpdateSummary) {
            roomSummaryEntity.latestPreviewableEvent = RoomSummaryEventsHelper.getLatestPreviewableEvent(realm, roomId)
        }
        if (currentChunk.isValid) {
            RoomEntity.where(realm, roomId).findFirst()?.addIfNecessary(currentChunk)
        }
    }
}
