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
package org.matrix.android.sdk.internal.database.query

import io.realm.kotlin.TypedRealm
import org.matrix.android.sdk.api.session.events.model.LocalEcho
import org.matrix.android.sdk.internal.database.helper.isMoreRecentThan
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.ReadMarkerEntity
import org.matrix.android.sdk.internal.database.model.ReadReceiptEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity

internal fun isEventRead(
        realm: TypedRealm,
        userId: String?,
        roomId: String?,
        eventId: String?
): Boolean {
    if (userId.isNullOrBlank() || roomId.isNullOrBlank() || eventId.isNullOrBlank()) {
        return false
    }
    if (LocalEcho.isLocalEchoId(eventId)) {
        return true
    }

    val eventToCheck = TimelineEventEntity.where(realm, roomId, eventId).first().find()
    return when {
        // The event doesn't exist locally, let's assume it hasn't been read
        eventToCheck == null -> false
        eventToCheck.root?.sender == userId -> true
        // If new event exists and the latest event is from ourselves we can infer the event is read
        latestEventIsFromSelf(realm, roomId, userId) -> true
        eventToCheck.isBeforeLatestReadReceipt(realm, roomId, userId) -> true
        else -> false
    }
}

private fun latestEventIsFromSelf(realm: TypedRealm, roomId: String, userId: String) = TimelineEventEntity.latestEvent(realm, roomId, true)
        ?.root?.sender == userId

private fun TimelineEventEntity.isBeforeLatestReadReceipt(realm: TypedRealm, roomId: String, userId: String): Boolean {
    return ReadReceiptEntity.where(realm, roomId, userId).first().find()?.let { readReceipt ->
        val readReceiptEvent = TimelineEventEntity.where(realm, roomId, readReceipt.eventId).first().find()
        readReceiptEvent?.isMoreRecentThan(realm, this)
    } ?: false
}

/**
 * Missing events can be caused by the latest timeline chunk no longer contain an older event or
 * by fast lane eagerly displaying events before the database has finished updating.
 */
private fun hasReadMissingEvent(realm: TypedRealm, latestChunkEntity: ChunkEntity, roomId: String, userId: String, eventId: String): Boolean {
    return realm.doesEventExistInChunkHistory(eventId) && realm.hasReadReceiptInLatestChunk(latestChunkEntity, roomId, userId)
}

private fun TypedRealm.doesEventExistInChunkHistory(eventId: String): Boolean {
    return ChunkEntity.findIncludingEvent(this, eventId) != null
}

private fun TypedRealm.hasReadReceiptInLatestChunk(latestChunkEntity: ChunkEntity, roomId: String, userId: String): Boolean {
    return ReadReceiptEntity.where(this, roomId = roomId, userId = userId).first().find()?.let { readReceipt ->
        val readReceiptEvent = TimelineEventEntity.where(this, roomId = roomId, eventId = readReceipt.eventId).first().find()
        latestChunkEntity.chunkId == readReceiptEvent?.chunkId
    } != null
}

internal fun isReadMarkerMoreRecent(
        realm: TypedRealm,
        roomId: String?,
        eventId: String?
): Boolean {
    if (roomId.isNullOrBlank() || eventId.isNullOrBlank()) {
        return false
    }
    val eventToCheck = TimelineEventEntity.where(realm, roomId = roomId, eventId = eventId).first().find()
    val chunkIdToCheck = eventToCheck?.chunkId ?: return false
    val eventToCheckChunk = ChunkEntity.find(realm, chunkId = chunkIdToCheck)
    val readMarker = ReadMarkerEntity.where(realm, roomId).first().find() ?: return false
    val readMarkerEvent = TimelineEventEntity.where(realm, roomId = roomId, eventId = readMarker.eventId).first().find()
    val readMarkerChunkId = readMarkerEvent?.chunkId ?: return false
    val readMarkerChunk = ChunkEntity.find(realm, chunkId = readMarkerChunkId)
    return if (eventToCheckChunk == readMarkerChunk) {
        val readMarkerIndex = readMarkerEvent.displayIndex
        val eventToCheckIndex = eventToCheck.displayIndex
        eventToCheckIndex <= readMarkerIndex
    } else {
        eventToCheckChunk != null && readMarkerChunk?.isMoreRecentThan(eventToCheckChunk) == true
    }
}
