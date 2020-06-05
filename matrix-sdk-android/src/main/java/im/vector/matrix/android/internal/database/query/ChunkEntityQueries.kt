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

import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.ChunkEntityFields
import im.vector.matrix.android.internal.database.model.RoomEntityFields
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.kotlin.createObject
import io.realm.kotlin.where

internal fun ChunkEntity.Companion.where(realm: Realm, roomId: String): RealmQuery<ChunkEntity> {
    return realm.where<ChunkEntity>()
            .equalTo("${ChunkEntityFields.ROOM}.${RoomEntityFields.ROOM_ID}", roomId)
}

internal fun ChunkEntity.Companion.find(realm: Realm, roomId: String, prevToken: String? = null, nextToken: String? = null): ChunkEntity? {
    val query = where(realm, roomId)
    if (prevToken != null) {
        query.equalTo(ChunkEntityFields.PREV_TOKEN, prevToken)
    }
    if (nextToken != null) {
        query.equalTo(ChunkEntityFields.NEXT_TOKEN, nextToken)
    }
    return query.findFirst()
}

internal fun ChunkEntity.Companion.findLastForwardChunkOfRoom(realm: Realm, roomId: String): ChunkEntity? {
    return where(realm, roomId)
            .equalTo(ChunkEntityFields.IS_LAST_FORWARD, true)
            .findFirst()
}

internal fun ChunkEntity.Companion.findAllIncludingEvents(realm: Realm, eventIds: List<String>): RealmResults<ChunkEntity> {
    return realm.where<ChunkEntity>()
            .`in`(ChunkEntityFields.TIMELINE_EVENTS.EVENT_ID, eventIds.toTypedArray())
            .findAll()
}

internal fun ChunkEntity.Companion.findIncludingEvent(realm: Realm, eventId: String): ChunkEntity? {
    return findAllIncludingEvents(realm, listOf(eventId)).firstOrNull()
}

internal fun ChunkEntity.Companion.create(
        realm: Realm,
        prevToken: String?,
        nextToken: String?
): ChunkEntity {
    return realm.createObject<ChunkEntity>().apply {
        this.prevToken = prevToken
        this.nextToken = nextToken
    }
}
