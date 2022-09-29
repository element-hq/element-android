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
import io.realm.kotlin.query.RealmQuery
import io.realm.kotlin.query.RealmResults
import io.realm.kotlin.types.RealmList
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.TimelineEventFilters
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntityFields
import org.matrix.android.sdk.internal.database.queryIn

internal fun TimelineEventEntity.Companion.where(realm: TypedRealm): RealmQuery<TimelineEventEntity> {
    return realm.query(TimelineEventEntity::class)
}

internal fun TimelineEventEntity.Companion.where(
        realm: TypedRealm,
        roomId: String,
        eventId: String
): RealmQuery<TimelineEventEntity> {
    return where(realm)
            .query("roomId == $0", roomId)
            .query("eventId == $0", eventId)
}

internal fun TimelineEventEntity.Companion.where(
        realm: TypedRealm,
        roomId: String,
        eventIds: List<String>
): RealmQuery<TimelineEventEntity> {
    return where(realm)
            .query("roomId == $0", roomId)
            .queryIn("eventId", eventIds)
}

internal fun TimelineEventEntity.Companion.whereRoomId(
        realm: TypedRealm,
        roomId: String
): RealmQuery<TimelineEventEntity> {
    return where(realm)
            .query("roomId == $0", roomId)
}

internal fun TimelineEventEntity.Companion.findWithSenderMembershipEvent(
        realm: TypedRealm,
        senderMembershipEventId: String
): List<TimelineEventEntity> {
    return where(realm)
            .query("senderMembershipEventId == $0", senderMembershipEventId)
            .find()
}

internal fun TimelineEventEntity.Companion.latestEvent(
        realm: TypedRealm,
        roomId: String,
        includesSending: Boolean,
        filters: TimelineEventFilters = TimelineEventFilters()
): TimelineEventEntity? {
    val roomEntity = RoomEntity.where(realm, roomId).first().find() ?: return null
    val sendingTimelineEvents = roomEntity.sendingTimelineEvents.where().filterEvents(filters)

    val liveEvents = ChunkEntity.findLastForwardChunkOfRoom(realm, roomId)?.timelineEvents?.where()?.filterEvents(filters)
    val query = if (includesSending && sendingTimelineEvents.findAll().isNotEmpty()) {
        sendingTimelineEvents
    } else {
        liveEvents
    }
    return query
            ?.sort(TimelineEventEntityFields.DISPLAY_INDEX, Sort.DESCENDING)
            ?.findFirst()
}

internal fun RealmQuery<TimelineEventEntity>.filterEvents(filters: TimelineEventFilters): RealmQuery<TimelineEventEntity> {
    if (filters.filterTypes && filters.allowedTypes.isNotEmpty()) {
        beginGroup()
        filters.allowedTypes.forEachIndexed { index, filter ->
            if (filter.stateKey == null) {
                equalTo(TimelineEventEntityFields.ROOT.TYPE, filter.eventType)
            } else {
                beginGroup()
                equalTo(TimelineEventEntityFields.ROOT.TYPE, filter.eventType)
                and()
                equalTo(TimelineEventEntityFields.ROOT.STATE_KEY, filter.stateKey)
                endGroup()
            }
            if (index != filters.allowedTypes.size - 1) {
                or()
            }
        }
        endGroup()
    }
    if (filters.filterUseless) {
        not()
                .equalTo(TimelineEventEntityFields.ROOT.IS_USELESS, true)
    }
    if (filters.filterEdits) {
        not().like(TimelineEventEntityFields.ROOT.CONTENT, TimelineEventFilter.Content.EDIT)
        not().like(TimelineEventEntityFields.ROOT.CONTENT, TimelineEventFilter.Content.RESPONSE)
        not().like(TimelineEventEntityFields.ROOT.CONTENT, TimelineEventFilter.Content.REFERENCE)
    }
    if (filters.filterRedacted) {
        not().like(TimelineEventEntityFields.ROOT.UNSIGNED_DATA, TimelineEventFilter.Unsigned.REDACTED)
    }

    return this
}

internal fun RealmQuery<TimelineEventEntity>.filterTypes(filterTypes: List<String>): RealmQuery<TimelineEventEntity> {
    return if (filterTypes.isEmpty()) {
        this
    } else {
        queryIn("root.type", filterTypes)
    }
}

internal fun RealmList<TimelineEventEntity>.find(eventId: String): TimelineEventEntity? {
    return where()
            .equalTo(TimelineEventEntityFields.EVENT_ID, eventId)
            .findFirst()
}

internal fun TimelineEventEntity.Companion.findAllInRoomWithSendStates(
        realm: TypedRealm,
        roomId: String,
        sendStates: List<SendState>
): RealmResults<TimelineEventEntity> {
    return whereRoomId(realm, roomId)
            .filterSendStates(sendStates)
            .find()
}

internal fun RealmQuery<TimelineEventEntity>.filterSendStates(sendStates: List<SendState>): RealmQuery<TimelineEventEntity> {
    val sendStatesStr = sendStates.map { it.name }
    return queryIn("root.sendStateStr", sendStatesStr)
}

/**
 * Find all TimelineEventEntity items where sender is in senderIds collection, excluding state events.
 */
internal fun TimelineEventEntity.Companion.findAllFrom(
        realm: TypedRealm,
        senderIds: Collection<String>
): RealmResults<TimelineEventEntity> {
    return where(realm)
            .queryIn("root.sender", senderIds.toList())
            .query("root.stateKey == nil")
            .find()
}
