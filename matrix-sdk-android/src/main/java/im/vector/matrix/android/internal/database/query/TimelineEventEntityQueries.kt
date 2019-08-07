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

import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.internal.database.model.*
import im.vector.matrix.android.internal.database.model.EventEntity.LinkFilterMode.*
import io.realm.*
import io.realm.kotlin.where

internal fun TimelineEventEntity.Companion.where(realm: Realm, eventId: String): RealmQuery<TimelineEventEntity> {
    return realm.where<TimelineEventEntity>()
            .equalTo(TimelineEventEntityFields.EVENT_ID, eventId)
}

internal fun TimelineEventEntity.Companion.where(realm: Realm, eventIds: List<String>): RealmQuery<TimelineEventEntity> {
    return realm.where<TimelineEventEntity>()
            .`in`(TimelineEventEntityFields.EVENT_ID, eventIds.toTypedArray())
}

internal fun TimelineEventEntity.Companion.where(realm: Realm,
                                                 roomId: String? = null,
                                                 type: String? = null,
                                                 linkFilterMode: EventEntity.LinkFilterMode = LINKED_ONLY): RealmQuery<TimelineEventEntity> {
    val query = realm.where<TimelineEventEntity>()
    if (roomId != null) {
        query.equalTo(TimelineEventEntityFields.ROOM_ID, roomId)
    }
    if (type != null) {
        query.equalTo(TimelineEventEntityFields.ROOT.TYPE, type)
    }
    return when (linkFilterMode) {
        LINKED_ONLY   -> query.equalTo(TimelineEventEntityFields.ROOT.IS_UNLINKED, false)
        UNLINKED_ONLY -> query.equalTo(TimelineEventEntityFields.ROOT.IS_UNLINKED, true)
        BOTH          -> query
    }
}

internal fun TimelineEventEntity.Companion.findWithSenderMembershipEvent(realm: Realm, senderMembershipEventId: String): List<TimelineEventEntity> {
    return realm.where<TimelineEventEntity>()
            .equalTo(TimelineEventEntityFields.SENDER_MEMBERSHIP_EVENT.EVENT_ID, senderMembershipEventId)
            .findAll()
}


internal fun TimelineEventEntity.Companion.latestEvent(realm: Realm,
                                                       roomId: String,
                                                       includesSending: Boolean,
                                                       includedTypes: List<String> = emptyList(),
                                                       excludedTypes: List<String> = emptyList()): TimelineEventEntity? {

    val roomEntity = RoomEntity.where(realm, roomId).findFirst() ?: return null
    val eventList = if (includesSending && roomEntity.sendingTimelineEvents.isNotEmpty()) {
        roomEntity.sendingTimelineEvents
    } else {
        ChunkEntity.findLastLiveChunkFromRoom(realm, roomId)?.timelineEvents
    }
    val query = eventList?.where()
    if (includedTypes.isNotEmpty()) {
        query?.`in`(TimelineEventEntityFields.ROOT.TYPE, includedTypes.toTypedArray())
    } else if (excludedTypes.isNotEmpty()) {
        query?.not()?.`in`(TimelineEventEntityFields.ROOT.TYPE, excludedTypes.toTypedArray())
    }
    return query
            ?.sort(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, Sort.DESCENDING)
            ?.findFirst()
}

internal fun RealmQuery<TimelineEventEntity>.next(from: Int? = null, strict: Boolean = true): TimelineEventEntity? {
    if (from != null) {
        if (strict) {
            this.greaterThan(TimelineEventEntityFields.ROOT.STATE_INDEX, from)
        } else {
            this.greaterThanOrEqualTo(TimelineEventEntityFields.ROOT.STATE_INDEX, from)
        }
    }
    return this
            .sort(TimelineEventEntityFields.ROOT.STATE_INDEX, Sort.ASCENDING)
            .findFirst()
}

internal fun RealmQuery<TimelineEventEntity>.prev(since: Int? = null, strict: Boolean = false): TimelineEventEntity? {
    if (since != null) {
        if (strict) {
            this.lessThan(TimelineEventEntityFields.ROOT.STATE_INDEX, since)
        } else {
            this.lessThanOrEqualTo(TimelineEventEntityFields.ROOT.STATE_INDEX, since)
        }
    }
    return this
            .sort(TimelineEventEntityFields.ROOT.STATE_INDEX, Sort.DESCENDING)
            .findFirst()
}


internal fun RealmList<TimelineEventEntity>.find(eventId: String): TimelineEventEntity? {
    return this.where()
            .equalTo(TimelineEventEntityFields.ROOT.EVENT_ID, eventId)
            .findFirst()
}

internal fun TimelineEventEntity.Companion.findAllInRoomWithSendStates(realm: Realm,
                                                                       roomId: String,
                                                                       sendStates: List<SendState>)
        : RealmResults<TimelineEventEntity> {

    val sendStatesStr = sendStates.map { it.name }.toTypedArray()
    return realm.where<TimelineEventEntity>()
            .equalTo(TimelineEventEntityFields.ROOM_ID, roomId)
            .`in`(TimelineEventEntityFields.ROOT.SEND_STATE_STR,sendStatesStr)
            .findAll()
}