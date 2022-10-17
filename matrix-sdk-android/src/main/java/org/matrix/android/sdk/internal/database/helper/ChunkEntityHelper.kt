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

package org.matrix.android.sdk.internal.database.helper

import io.realm.kotlin.MutableRealm
import io.realm.kotlin.TypedRealm
import org.matrix.android.sdk.api.session.events.model.content.EncryptedEventContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.internal.crypto.model.SessionInfo
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.ReadReceiptEntity
import org.matrix.android.sdk.internal.database.model.ReadReceiptsSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.model.cleanUp
import org.matrix.android.sdk.internal.database.query.find
import org.matrix.android.sdk.internal.database.query.findLastForwardChunkOfRoom
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.database.query.whereChunkId
import org.matrix.android.sdk.internal.session.room.timeline.PaginationDirection
import timber.log.Timber

internal fun ChunkEntity.addStateEvent(roomId: String, stateEvent: EventEntity, direction: PaginationDirection) {
    if (direction == PaginationDirection.BACKWARDS) {
        Timber.v("We don't keep chunk state events when paginating backward")
    } else {
        val stateKey = stateEvent.stateKey ?: return
        val type = stateEvent.type
        val indexOfStateEvent = stateEvents.indexOfFirst {
            it.roomId == roomId && it.stateKey == stateKey && it.type == type
        }
        if (indexOfStateEvent != -1) {
            stateEvents.removeAt(indexOfStateEvent)
        }
        stateEvents.add(stateEvent)
    }
}

internal fun ChunkEntity.addTimelineEvent(
        realm: MutableRealm,
        roomId: String,
        eventEntity: EventEntity,
        direction: PaginationDirection,
        ownedByThreadChunk: Boolean = false,
        roomMemberContentsByUser: Map<String, RoomMemberContent?>? = null
): TimelineEventEntity? {
    val eventId = eventEntity.eventId
    if (timelineEvents.find(eventId) != null) {
        return null
    }
    val chunkId = this.chunkId
    val displayIndex = nextDisplayIndex(realm, direction)
    val localId = TimelineEventEntity.nextId(realm)
    val senderId = eventEntity.sender ?: ""

    // Update RR for the sender of a new message with a dummy one
    val readReceiptsSummaryEntity = if (!ownedByThreadChunk) handleReadReceipts(realm, roomId, eventEntity, senderId) else null
    val timelineEventEntity = realm.copyToRealm(
            TimelineEventEntity().apply {
                this.chunkId = chunkId
                this.localId = localId
                this.root = eventEntity
                this.eventId = eventId
                this.roomId = roomId
                this.annotations = EventAnnotationsSummaryEntity.where(realm, roomId, eventId).first().find()
                        ?.also { realm.cleanUp(it, eventEntity.sender) }
                this.readReceipts = readReceiptsSummaryEntity
                this.displayIndex = displayIndex
                this.ownedByThreadChunk = ownedByThreadChunk
                val roomMemberContent = roomMemberContentsByUser?.get(senderId)
                this.senderAvatar = roomMemberContent?.avatarUrl
                this.senderName = roomMemberContent?.displayName
                isUniqueDisplayName = if (roomMemberContent?.displayName != null) {
                    computeIsUnique(realm, roomId, isLastForward, roomMemberContent, roomMemberContentsByUser)
                } else {
                    true
                }
            }
    )
    timelineEvents.add(timelineEventEntity)
    return timelineEventEntity
}

internal fun computeIsUnique(
        realm: TypedRealm,
        roomId: String,
        isLastForward: Boolean,
        senderRoomMemberContent: RoomMemberContent,
        roomMemberContentsByUser: Map<String, RoomMemberContent?>
): Boolean {
    val isHistoricalUnique = roomMemberContentsByUser.values.find {
        it != senderRoomMemberContent && it?.displayName == senderRoomMemberContent.displayName
    } == null
    return if (isLastForward) {
        val isLiveUnique = RoomMemberSummaryEntity
                .where(realm, roomId)
                .query("displayName == $0", senderRoomMemberContent.displayName)
                .find()
                .none {
                    !roomMemberContentsByUser.containsKey(it.userId)
                }
        isHistoricalUnique && isLiveUnique
    } else {
        isHistoricalUnique
    }
}

private fun handleReadReceipts(realm: MutableRealm, roomId: String, eventEntity: EventEntity, senderId: String): ReadReceiptsSummaryEntity {
    val readReceiptsSummaryEntity = ReadReceiptsSummaryEntity.where(realm, eventEntity.eventId).find()
            ?: realm.copyToRealm(ReadReceiptsSummaryEntity().apply {
                this.eventId = eventEntity.eventId
                this.roomId = roomId
            })
    val originServerTs = eventEntity.originServerTs
    if (originServerTs != null) {
        val timestampOfEvent = originServerTs.toDouble()
        val readReceiptOfSender = ReadReceiptEntity.getOrCreate(realm, roomId = roomId, userId = senderId)
        // If the synced RR is older, update
        if (timestampOfEvent > readReceiptOfSender.originServerTs) {
            val previousReceiptsSummary = ReadReceiptsSummaryEntity.where(realm, eventId = readReceiptOfSender.eventId).find()
            readReceiptOfSender.eventId = eventEntity.eventId
            readReceiptOfSender.originServerTs = timestampOfEvent
            previousReceiptsSummary?.readReceipts?.remove(readReceiptOfSender)
            readReceiptsSummaryEntity.readReceipts.add(readReceiptOfSender)
        }
    }
    return readReceiptsSummaryEntity
}

internal fun ChunkEntity.nextDisplayIndex(realm: TypedRealm, direction: PaginationDirection): Int {
    return when (direction) {
        PaginationDirection.FORWARDS -> {
            (TimelineEventEntity.whereChunkId(realm, chunkId)
                    .max("displayIndex", Int::class)
                    .find() ?: 0) + 1
        }
        PaginationDirection.BACKWARDS -> {
            (TimelineEventEntity.whereChunkId(realm, chunkId)
                    .min("displayIndex", Int::class)
                    .find() ?: 0) - 1
        }
    }
}

internal fun ChunkEntity.doesNextChunksVerifyCondition(linkCondition: (ChunkEntity) -> Boolean): Boolean {
    var nextChunkToCheck = this.nextChunk
    while (nextChunkToCheck != null) {
        if (linkCondition(nextChunkToCheck)) {
            return true
        }
        nextChunkToCheck = nextChunkToCheck.nextChunk
    }
    return false
}

internal fun ChunkEntity.isMoreRecentThan(chunkToCheck: ChunkEntity): Boolean {
    if (this.isLastForward) return true
    if (chunkToCheck.isLastForward) return false
    // Check if the chunk to check is linked to this one
    if (chunkToCheck.doesNextChunksVerifyCondition { it == this }) {
        return true
    }
    if (this.doesNextChunksVerifyCondition { it == chunkToCheck }) {
        return false
    }
    // Otherwise check if this chunk is linked to last forward
    if (this.doesNextChunksVerifyCondition { it.isLastForward }) {
        return true
    }
    // We don't know, so we assume it's false
    return false
}

internal fun ChunkEntity.Companion.findLatestSessionInfo(realm: TypedRealm, roomId: String): Set<SessionInfo>? =
        ChunkEntity.findLastForwardChunkOfRoom(realm, roomId)?.timelineEvents?.mapNotNull { timelineEvent ->
            timelineEvent.root?.asDomain()?.content?.toModel<EncryptedEventContent>()?.let { content ->
                content.sessionId ?: return@mapNotNull null
                content.senderKey ?: return@mapNotNull null
                SessionInfo(content.sessionId, content.senderKey)
            }
        }?.toSet()
