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

import im.vector.matrix.android.api.session.events.model.LocalEcho
import im.vector.matrix.sqldelight.session.SessionDatabase

internal fun SessionDatabase.isEventRead(userId: String?,
                         roomId: String?,
                         eventId: String?): Boolean {
    if (userId.isNullOrBlank() || roomId.isNullOrBlank() || eventId.isNullOrBlank()) {
        return false
    }
    if (LocalEcho.isLocalEchoId(eventId)) {
        return true
    }
    val eventToCheck = timelineEventQueries.getForReadQueries(eventId).executeAsOneOrNull()
    return if (eventToCheck == null || eventToCheck.sender_id == userId) {
        true
    } else {
        val readReceipt = readReceiptQueries.getEventIdForUser(roomId, userId).executeAsOneOrNull()
                ?: return false
        val readReceiptIndex = timelineEventQueries.getForReadQueries(readReceipt).executeAsOneOrNull()?.display_index
                ?: Int.MIN_VALUE
        val eventToCheckIndex = eventToCheck.display_index
        eventToCheckIndex <= readReceiptIndex
    }
}

internal fun SessionDatabase.isReadMarkerMoreRecent(roomId: String?,
                                    eventId: String?): Boolean {
    if (roomId.isNullOrBlank() || eventId.isNullOrBlank()) {
        return false
    }
    val eventToCheck = timelineEventQueries.getForReadQueries(eventId).executeAsOneOrNull()
    val readMarker = readMarkerQueries.get(roomId).executeAsOneOrNull()
            ?: return false
    val readMarkerEvent = timelineEventQueries.getForReadQueries(readMarker).executeAsOneOrNull()
    return if (eventToCheck?.chunk_id == readMarkerEvent?.chunk_id) {
        val readMarkerIndex = readMarkerEvent?.display_index ?: Int.MIN_VALUE
        val eventToCheckIndex = eventToCheck?.display_index ?: Int.MAX_VALUE
        eventToCheckIndex <= readMarkerIndex
    } else {
        val chunkId = eventToCheck?.chunk_id ?: return false
        chunkQueries.isLastForward(chunkId).executeAsOneOrNull() ?: false
    }
}
