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

import io.realm.Realm
import io.realm.kotlin.createObject
import org.matrix.android.sdk.api.session.events.model.content.EncryptedEventContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.internal.crypto.model.SessionInfo
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntityFields
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.database.model.ReadReceiptEntity
import org.matrix.android.sdk.internal.database.model.ReadReceiptsSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntityFields
import org.matrix.android.sdk.internal.database.query.find
import org.matrix.android.sdk.internal.database.query.findLastForwardChunkOfRoom
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.session.room.timeline.PaginationDirection
import timber.log.Timber

internal fun ChunkEntity.addStateEvent(roomId: String, stateEvent: EventEntity, direction: PaginationDirection) {
    if (direction == PaginationDirection.BACKWARDS) {
        Timber.v("We don't keep chunk state events when paginating backward")
    } else {
        val stateKey = stateEvent.stateKey ?: return
        val type = stateEvent.type
        val pastStateEvent = stateEvents.where()
                .equalTo(EventEntityFields.ROOM_ID, roomId)
                .equalTo(EventEntityFields.STATE_KEY, stateKey)
                .equalTo(CurrentStateEventEntityFields.TYPE, type)
                .findFirst()

        if (pastStateEvent != null) {
            stateEvents.remove(pastStateEvent)
        }
        stateEvents.add(stateEvent)
    }
}

internal fun ChunkEntity.addTimelineEvent(
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
    val displayIndex = nextDisplayIndex(direction)
    val localId = TimelineEventEntity.nextId(realm)
    val senderId = eventEntity.sender ?: ""

    // Update RR for the sender of a new message with a dummy one
    val readReceiptsSummaryEntity = if (!ownedByThreadChunk) handleReadReceipts(realm, roomId, eventEntity, senderId) else null
    val timelineEventEntity = realm.createObject<TimelineEventEntity>().apply {
        this.localId = localId
        this.root = eventEntity
        this.eventId = eventId
        this.roomId = roomId
        this.annotations = EventAnnotationsSummaryEntity.where(realm, roomId, eventId).findFirst()
                ?.also { it.cleanUp(eventEntity.sender) }
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
    // numberOfTimelineEvents++
    timelineEvents.add(timelineEventEntity)
    return timelineEventEntity
}

internal fun computeIsUnique(
        realm: Realm,
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
                .equalTo(RoomMemberSummaryEntityFields.DISPLAY_NAME, senderRoomMemberContent.displayName)
                .findAll()
                .none {
                    !roomMemberContentsByUser.containsKey(it.userId)
                }
        isHistoricalUnique && isLiveUnique
    } else {
        isHistoricalUnique
    }
}

private fun handleReadReceipts(realm: Realm, roomId: String, eventEntity: EventEntity, senderId: String): ReadReceiptsSummaryEntity {
    val readReceiptsSummaryEntity = ReadReceiptsSummaryEntity.where(realm, eventEntity.eventId).findFirst()
            ?: realm.createObject<ReadReceiptsSummaryEntity>(eventEntity.eventId).apply {
                this.roomId = roomId
            }
    val originServerTs = eventEntity.originServerTs
    if (originServerTs != null) {
        val timestampOfEvent = originServerTs.toDouble()
        val readReceiptOfSender = ReadReceiptEntity.getOrCreate(realm, roomId = roomId, userId = senderId)
        // If the synced RR is older, update
        if (timestampOfEvent > readReceiptOfSender.originServerTs) {
            val previousReceiptsSummary = ReadReceiptsSummaryEntity.where(realm, eventId = readReceiptOfSender.eventId).findFirst()
            readReceiptOfSender.eventId = eventEntity.eventId
            readReceiptOfSender.originServerTs = timestampOfEvent
            previousReceiptsSummary?.readReceipts?.remove(readReceiptOfSender)
            readReceiptsSummaryEntity.readReceipts.add(readReceiptOfSender)
        }
    }
    return readReceiptsSummaryEntity
}

internal fun ChunkEntity.nextDisplayIndex(direction: PaginationDirection): Int {
    return when (direction) {
        PaginationDirection.FORWARDS -> {
            (timelineEvents.where().max(TimelineEventEntityFields.DISPLAY_INDEX)?.toInt() ?: 0) + 1
        }
        PaginationDirection.BACKWARDS -> {
            (timelineEvents.where().min(TimelineEventEntityFields.DISPLAY_INDEX)?.toInt() ?: 0) - 1
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

internal fun ChunkEntity.Companion.findLatestSessionInfo(realm: Realm, roomId: String): Set<SessionInfo>? =
        ChunkEntity.findLastForwardChunkOfRoom(realm, roomId)?.timelineEvents?.mapNotNull { timelineEvent ->
            timelineEvent?.root?.asDomain()?.content?.toModel<EncryptedEventContent>()?.let { content ->
                content.sessionId ?: return@mapNotNull null
                content.senderKey ?: return@mapNotNull null
                SessionInfo(content.sessionId, content.senderKey)
            }
        }?.toSet()
