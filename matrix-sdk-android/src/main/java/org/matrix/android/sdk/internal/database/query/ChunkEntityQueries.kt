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
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.ChunkEntityFields

internal fun ChunkEntity.Companion.where(realm: Realm, roomId: String): RealmQuery<ChunkEntity> {
    return realm.where<ChunkEntity>()
            .equalTo(ChunkEntityFields.ROOM.ROOM_ID, roomId)
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
internal fun ChunkEntity.Companion.findLastForwardChunkOfThread(realm: Realm, roomId: String, rootThreadEventId: String): ChunkEntity? {
    return where(realm, roomId)
            .equalTo(ChunkEntityFields.ROOT_THREAD_EVENT_ID, rootThreadEventId)
            .equalTo(ChunkEntityFields.IS_LAST_FORWARD_THREAD, true)
            .findFirst()
}
internal fun ChunkEntity.Companion.findEventInThreadChunk(realm: Realm, roomId: String, event: String): ChunkEntity? {
    return where(realm, roomId)
            .`in`(ChunkEntityFields.TIMELINE_EVENTS.EVENT_ID, arrayListOf(event).toTypedArray())
            .equalTo(ChunkEntityFields.IS_LAST_FORWARD_THREAD, true)
            .findFirst()
}
internal fun ChunkEntity.Companion.findAllIncludingEvents(realm: Realm, eventIds: List<String>): RealmResults<ChunkEntity> {
    return realm.where<ChunkEntity>()
            .`in`(ChunkEntityFields.TIMELINE_EVENTS.EVENT_ID, eventIds.toTypedArray())
            .isNull(ChunkEntityFields.ROOT_THREAD_EVENT_ID)
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
