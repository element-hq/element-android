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

import io.realm.kotlin.MutableRealm
import io.realm.kotlin.TypedRealm
import io.realm.kotlin.query.RealmQuery
import io.realm.kotlin.query.RealmResults
import io.realm.kotlin.types.ObjectId
import org.matrix.android.sdk.internal.database.andIf
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.queryIn

internal fun ChunkEntity.Companion.where(realm: TypedRealm, roomId: String): RealmQuery<ChunkEntity> {
    return realm.query(ChunkEntity::class)
            .query("roomId == $0", roomId)
}

internal fun ChunkEntity.Companion.find(realm: TypedRealm, chunkId: ObjectId): ChunkEntity? {
    return realm.query(ChunkEntity::class)
            .query("chunkId == $0", chunkId)
            .first()
            .find()
}

internal fun ChunkEntity.Companion.find(realm: TypedRealm, roomId: String, prevToken: String? = null, nextToken: String? = null): ChunkEntity? {
    if (prevToken == null && nextToken == null) return null
    return where(realm, roomId)
            .andIf(prevToken != null) {
                query("prevToken == $0", prevToken!!)
            }
            .andIf(nextToken != null) {
                query("nextToken == $0", nextToken!!)
            }
            .first()
            .find()
}

internal fun ChunkEntity.Companion.findAll(realm: TypedRealm, roomId: String, prevToken: String? = null, nextToken: String? = null): RealmResults<ChunkEntity> {
    return where(realm, roomId)
            .andIf(prevToken != null) {
                query("prevToken == $0", prevToken!!)
            }
            .andIf(nextToken != null) {
                query("nextToken == $0", nextToken!!)
            }
            .find()
}

internal fun ChunkEntity.Companion.findLastForwardChunkOfRoom(realm: TypedRealm, roomId: String): ChunkEntity? {
    return where(realm, roomId)
            .query("isLastForward == true")
            .first()
            .find()
}

internal fun ChunkEntity.Companion.findLastForwardChunkOfThread(realm: TypedRealm, roomId: String, rootThreadEventId: String): ChunkEntity? {
    return where(realm, roomId)
            .query("rootThreadEventId == $0", rootThreadEventId)
            .query("isLastForwardThread == true")
            .first()
            .find()
}

internal fun ChunkEntity.Companion.findEventInThreadChunk(realm: TypedRealm, roomId: String, event: String): ChunkEntity? {
    return where(realm, roomId)
            .queryIn("timelineEvents.eventId", arrayListOf(event))
            .query("isLastForwardThread == true")
            .first()
            .find()
}

internal fun ChunkEntity.Companion.findAllIncludingEvents(realm: TypedRealm, eventIds: List<String>): RealmResults<ChunkEntity> {
    return realm.query(ChunkEntity::class)
            .queryIn("timelineEvents.eventId", eventIds)
            .query("rootThreadEventId == nil")
            .find()
}

internal fun ChunkEntity.Companion.findIncludingEvent(realm: TypedRealm, eventId: String): ChunkEntity? {
    return findAllIncludingEvents(realm, listOf(eventId)).firstOrNull()
}

internal fun ChunkEntity.Companion.create(
        realm: MutableRealm,
        roomId: String,
        prevToken: String?,
        nextToken: String?
): ChunkEntity {
    val chunkEntity = ChunkEntity().apply {
        this.roomId = roomId
        this.prevToken = prevToken
        this.nextToken = nextToken
    }
    return realm.copyToRealm(chunkEntity)
}
