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
package im.vector.matrix.android.internal.database.query

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.model.LocalEcho
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.ReadMarkerEntity
import im.vector.matrix.android.internal.database.model.ReadReceiptEntity
import im.vector.matrix.android.internal.database.model.TimelineEventEntity
import io.realm.Realm

internal fun isEventRead(monarchy: Monarchy,
                         userId: String?,
                         roomId: String?,
                         eventId: String?): Boolean {
    if (userId.isNullOrBlank() || roomId.isNullOrBlank() || eventId.isNullOrBlank()) {
        return false
    }
    if (LocalEcho.isLocalEchoId(eventId)) {
        return true
    }
    var isEventRead = false

    monarchy.doWithRealm { realm ->
        val liveChunk = ChunkEntity.findLastForwardChunkOfRoom(realm, roomId) ?: return@doWithRealm
        val eventToCheck = liveChunk.timelineEvents.find(eventId)
        isEventRead = if (eventToCheck == null || eventToCheck.root?.sender == userId) {
            true
        } else {
            val readReceipt = ReadReceiptEntity.where(realm, roomId, userId).findFirst()
                    ?: return@doWithRealm
            val readReceiptIndex = liveChunk.timelineEvents.find(readReceipt.eventId)?.displayIndex
                    ?: Int.MIN_VALUE
            val eventToCheckIndex = eventToCheck.displayIndex

            eventToCheckIndex <= readReceiptIndex
        }
    }

    return isEventRead
}

internal fun isReadMarkerMoreRecent(monarchy: Monarchy,
                                    roomId: String?,
                                    eventId: String?): Boolean {
    if (roomId.isNullOrBlank() || eventId.isNullOrBlank()) {
        return false
    }
    return Realm.getInstance(monarchy.realmConfiguration).use { realm ->
        val eventToCheck = TimelineEventEntity.where(realm, roomId = roomId, eventId = eventId).findFirst()
        val eventToCheckChunk = eventToCheck?.chunk?.firstOrNull()
        val readMarker = ReadMarkerEntity.where(realm, roomId).findFirst() ?: return false
        val readMarkerEvent = TimelineEventEntity.where(realm, roomId = roomId, eventId = readMarker.eventId).findFirst()
        val readMarkerChunk = readMarkerEvent?.chunk?.firstOrNull()
        if (eventToCheckChunk == readMarkerChunk) {
            val readMarkerIndex = readMarkerEvent?.displayIndex ?: Int.MIN_VALUE
            val eventToCheckIndex = eventToCheck?.displayIndex ?: Int.MAX_VALUE
            eventToCheckIndex <= readMarkerIndex
        } else {
            eventToCheckChunk?.isLastForward == false
        }
    }
}
