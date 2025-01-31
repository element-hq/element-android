/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
    if (prevToken == null && nextToken == null) return null
    if (prevToken != null) {
        query.equalTo(ChunkEntityFields.PREV_TOKEN, prevToken)
    }
    if (nextToken != null) {
        query.equalTo(ChunkEntityFields.NEXT_TOKEN, nextToken)
    }
    return query.findFirst()
}

internal fun ChunkEntity.Companion.findAll(realm: Realm, roomId: String, prevToken: String? = null, nextToken: String? = null): RealmResults<ChunkEntity> {
    val query = where(realm, roomId)
    if (prevToken != null) {
        query.equalTo(ChunkEntityFields.PREV_TOKEN, prevToken)
    }
    if (nextToken != null) {
        query.equalTo(ChunkEntityFields.NEXT_TOKEN, nextToken)
    }
    return query.findAll()
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
