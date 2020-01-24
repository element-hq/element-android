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

package im.vector.matrix.android.internal.database.helper

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.RoomMemberContent
import im.vector.matrix.android.internal.database.model.*
import im.vector.matrix.android.internal.database.query.getOrCreate
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.extensions.assertIsManaged
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import io.realm.kotlin.createObject

internal fun ChunkEntity.deleteOnCascade() {
    assertIsManaged()
    this.timelineEvents.deleteAllFromRealm()
    this.deleteFromRealm()
}

internal fun ChunkEntity.addTimelineEvent(roomId: String,
                                          eventEntity: EventEntity,
                                          direction: PaginationDirection,
                                          roomMemberEvent: Event?): TimelineEventEntity {

    var currentDisplayIndex = lastDisplayIndex(direction, 0)
    if (direction == PaginationDirection.FORWARDS) {
        currentDisplayIndex += 1
        forwardsDisplayIndex = currentDisplayIndex
    } else {
        currentDisplayIndex -= 1
        backwardsDisplayIndex = currentDisplayIndex
    }

    val localId = TimelineEventEntity.nextId(realm)
    val eventId = eventEntity.eventId
    val senderId = eventEntity.sender ?: ""

    val readReceiptsSummaryEntity = ReadReceiptsSummaryEntity.where(realm, eventId).findFirst()
            ?: realm.createObject<ReadReceiptsSummaryEntity>(eventId).apply {
                this.roomId = roomId
            }

    // Update RR for the sender of a new message with a dummy one

    val originServerTs = eventEntity.originServerTs
    if (originServerTs != null) {
        val timestampOfEvent = originServerTs.toDouble()
        val readReceiptOfSender = ReadReceiptEntity.getOrCreate(realm, roomId = roomId, userId = senderId)
        // If the synced RR is older, update
        if (timestampOfEvent > readReceiptOfSender.originServerTs) {
            val previousReceiptsSummary = ReadReceiptsSummaryEntity.where(realm, eventId = readReceiptOfSender.eventId).findFirst()
            readReceiptOfSender.eventId = eventId
            readReceiptOfSender.originServerTs = timestampOfEvent
            previousReceiptsSummary?.readReceipts?.remove(readReceiptOfSender)
            readReceiptsSummaryEntity.readReceipts.add(readReceiptOfSender)
        }
    }

    val timelineEventEntity = TimelineEventEntity().also {
        it.localId = localId
        it.root = realm.copyToRealm(eventEntity)
        it.eventId = eventId
        it.roomId = roomId
        it.annotations = EventAnnotationsSummaryEntity.where(realm, eventId).findFirst()
        it.readReceipts = readReceiptsSummaryEntity
        it.displayIndex = currentDisplayIndex
    }
    if (roomMemberEvent != null) {
        val roomMemberContent = roomMemberEvent.content.toModel<RoomMemberContent>()
        timelineEventEntity.senderAvatar = roomMemberContent?.avatarUrl
        timelineEventEntity.senderName = roomMemberContent?.displayName
        timelineEventEntity.isUniqueDisplayName = false
        timelineEventEntity.senderMembershipEventId = roomMemberEvent.eventId
    }
    timelineEvents.add(timelineEventEntity)
    return timelineEventEntity
}

internal fun ChunkEntity.lastDisplayIndex(direction: PaginationDirection, defaultValue: Int = 0): Int {
    return when (direction) {
        PaginationDirection.FORWARDS  -> forwardsDisplayIndex
        PaginationDirection.BACKWARDS -> backwardsDisplayIndex
    } ?: defaultValue
}
