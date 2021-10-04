/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.room.summary

import io.realm.Realm
import org.matrix.android.sdk.api.session.room.summary.RoomSummaryConstants
import org.matrix.android.sdk.api.session.room.timeline.EventTypeFilter
import org.matrix.android.sdk.api.session.room.timeline.TimelineEventFilters
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.query.latestEvent

internal object RoomSummaryEventsHelper {

    private val previewFilters = TimelineEventFilters(
            filterTypes = true,
            allowedTypes = RoomSummaryConstants.PREVIEWABLE_TYPES.map { EventTypeFilter(eventType = it, stateKey = null) },
            filterUseless = true,
            filterRedacted = false,
            filterEdits = true
    )

    fun getLatestPreviewableEvent(realm: Realm, roomId: String): TimelineEventEntity? {
        return TimelineEventEntity.latestEvent(
                realm = realm,
                roomId = roomId,
                includesSending = true,
                filters = previewFilters
        )
    }
}
