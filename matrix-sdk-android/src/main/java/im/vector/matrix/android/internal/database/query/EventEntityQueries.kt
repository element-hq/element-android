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
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntity.LinkFilterMode.*
import im.vector.matrix.android.internal.database.model.EventEntityFields
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmQuery
import io.realm.Sort
import io.realm.kotlin.where

internal fun EventEntity.Companion.where(realm: Realm, eventId: String): RealmQuery<EventEntity> {
    return realm.where<EventEntity>().equalTo(EventEntityFields.EVENT_ID, eventId)
}

internal fun EventEntity.Companion.where(realm: Realm, eventIds: List<String>): RealmQuery<EventEntity> {
    return realm.where<EventEntity>().`in`(EventEntityFields.EVENT_ID, eventIds.toTypedArray())
}

internal fun EventEntity.Companion.where(realm: Realm,
                                         roomId: String? = null,
                                         type: String? = null,
                                         linkFilterMode: EventEntity.LinkFilterMode = LINKED_ONLY): RealmQuery<EventEntity> {
    val query = realm.where<EventEntity>()
    if (roomId != null) {
        query.equalTo(EventEntityFields.ROOM_ID, roomId)
    }
    if (type != null) {
        query.equalTo(EventEntityFields.TYPE, type)
    }
    return when (linkFilterMode) {
        LINKED_ONLY   -> query.equalTo(EventEntityFields.IS_UNLINKED, false)
        UNLINKED_ONLY -> query.equalTo(EventEntityFields.IS_UNLINKED, true)
        BOTH          -> query
    }
}


internal fun EventEntity.Companion.types(realm: Realm,
                                         typeList: List<String> = emptyList()): RealmQuery<EventEntity> {
    val query = realm.where<EventEntity>()
    query.`in`(EventEntityFields.TYPE, typeList.toTypedArray())
    return query
}


internal fun EventEntity.Companion.latestEvent(realm: Realm,
                                               roomId: String,
                                               includedTypes: List<String> = emptyList(),
                                               excludedTypes: List<String> = emptyList()): EventEntity? {
    val query = ChunkEntity.findLastLiveChunkFromRoom(realm, roomId)?.events?.where()
    if (includedTypes.isNotEmpty()) {
        query?.`in`(EventEntityFields.TYPE, includedTypes.toTypedArray())
    } else if (excludedTypes.isNotEmpty()) {
        query?.not()?.`in`(EventEntityFields.TYPE, excludedTypes.toTypedArray())
    }
    return query
            ?.sort(EventEntityFields.DISPLAY_INDEX, Sort.DESCENDING)
            ?.findFirst()
}


internal fun RealmQuery<EventEntity>.next(from: Int? = null, strict: Boolean = true): EventEntity? {
    if (from != null) {
        if (strict) {
            this.greaterThan(EventEntityFields.STATE_INDEX, from)
        } else {
            this.greaterThanOrEqualTo(EventEntityFields.STATE_INDEX, from)
        }
    }
    return this
            .sort(EventEntityFields.STATE_INDEX, Sort.ASCENDING)
            .findFirst()
}

internal fun RealmQuery<EventEntity>.prev(since: Int? = null, strict: Boolean = false): EventEntity? {
    if (since != null) {
        if (strict) {
            this.lessThan(EventEntityFields.STATE_INDEX, since)
        } else {
            this.lessThanOrEqualTo(EventEntityFields.STATE_INDEX, since)
        }
    }
    return this
            .sort(EventEntityFields.STATE_INDEX, Sort.DESCENDING)
            .findFirst()
}

internal fun RealmList<EventEntity>.find(eventId: String): EventEntity? {
    return this.where().equalTo(EventEntityFields.EVENT_ID, eventId).findFirst()
}

internal fun RealmList<EventEntity>.fastContains(eventId: String): Boolean {
    return this.find(eventId) != null
}
