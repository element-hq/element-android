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

import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.where
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.TimelineEventFilters
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntityFields

internal fun TimelineEventEntity.Companion.where(realm: Realm, roomId: String, eventId: String): RealmQuery<TimelineEventEntity> {
    return realm.where<TimelineEventEntity>()
            .equalTo(TimelineEventEntityFields.ROOM_ID, roomId)
            .equalTo(TimelineEventEntityFields.EVENT_ID, eventId)
}

internal fun TimelineEventEntity.Companion.where(realm: Realm, roomId: String, eventIds: List<String>): RealmQuery<TimelineEventEntity> {
    return realm.where<TimelineEventEntity>()
            .equalTo(TimelineEventEntityFields.ROOM_ID, roomId)
            .`in`(TimelineEventEntityFields.EVENT_ID, eventIds.toTypedArray())
}

internal fun TimelineEventEntity.Companion.whereRoomId(realm: Realm,
                                                       roomId: String): RealmQuery<TimelineEventEntity> {
    return realm.where<TimelineEventEntity>()
            .equalTo(TimelineEventEntityFields.ROOM_ID, roomId)
}

internal fun TimelineEventEntity.Companion.findWithSenderMembershipEvent(realm: Realm, senderMembershipEventId: String): List<TimelineEventEntity> {
    return realm.where<TimelineEventEntity>()
            .equalTo(TimelineEventEntityFields.SENDER_MEMBERSHIP_EVENT_ID, senderMembershipEventId)
            .findAll()
}

internal fun TimelineEventEntity.Companion.latestEvent(realm: Realm,
                                                       roomId: String,
                                                       includesSending: Boolean,
                                                       filters: TimelineEventFilters = TimelineEventFilters()): TimelineEventEntity? {
    val roomEntity = RoomEntity.where(realm, roomId).findFirst() ?: return null
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
        this.`in`(TimelineEventEntityFields.ROOT.TYPE, filterTypes.toTypedArray())
    }
}

internal fun RealmList<TimelineEventEntity>.find(eventId: String): TimelineEventEntity? {
    return this.where()
            .equalTo(TimelineEventEntityFields.EVENT_ID, eventId)
            .findFirst()
}

internal fun TimelineEventEntity.Companion.findAllInRoomWithSendStates(realm: Realm,
                                                                       roomId: String,
                                                                       sendStates: List<SendState>): RealmResults<TimelineEventEntity> {
    return whereRoomId(realm, roomId)
            .filterSendStates(sendStates)
            .findAll()
}

internal fun RealmQuery<TimelineEventEntity>.filterSendStates(sendStates: List<SendState>): RealmQuery<TimelineEventEntity> {
    val sendStatesStr = sendStates.map { it.name }.toTypedArray()
    return `in`(TimelineEventEntityFields.ROOT.SEND_STATE_STR, sendStatesStr)
}
