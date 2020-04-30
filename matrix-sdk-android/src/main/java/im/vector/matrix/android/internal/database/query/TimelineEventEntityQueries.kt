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
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.TimelineEventEntity
import im.vector.matrix.android.internal.database.model.TimelineEventEntityFields
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.where

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
                                                       filterContentRelation: Boolean = false,
                                                       filterTypes: List<String> = emptyList()): TimelineEventEntity? {
    val roomEntity = RoomEntity.where(realm, roomId).findFirst() ?: return null
    val sendingTimelineEvents = roomEntity.sendingTimelineEvents.where().filterTypes(filterTypes)
    val liveEvents = ChunkEntity.findLastForwardChunkOfRoom(realm, roomId)?.timelineEvents?.where()?.filterTypes(filterTypes)
    if (filterContentRelation) {
        liveEvents
                ?.not()?.like(TimelineEventEntityFields.ROOT.CONTENT, FilterContent.EDIT_TYPE)
                ?.not()?.like(TimelineEventEntityFields.ROOT.CONTENT, FilterContent.RESPONSE_TYPE)
    }
    val query = if (includesSending && sendingTimelineEvents.findAll().isNotEmpty()) {
        sendingTimelineEvents
    } else {
        liveEvents
    }
    return query
            ?.sort(TimelineEventEntityFields.DISPLAY_INDEX, Sort.DESCENDING)
            ?.findFirst()
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
                                                                       sendStates: List<SendState>)
        : RealmResults<TimelineEventEntity> {
    val sendStatesStr = sendStates.map { it.name }.toTypedArray()
    return realm.where<TimelineEventEntity>()
            .equalTo(TimelineEventEntityFields.ROOM_ID, roomId)
            .`in`(TimelineEventEntityFields.ROOT.SEND_STATE_STR, sendStatesStr)
            .findAll()
}
