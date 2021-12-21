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
import io.realm.kotlin.where
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.database.model.EventInsertEntity
import org.matrix.android.sdk.internal.database.model.EventInsertType

internal fun EventEntity.copyToRealmOrIgnore(realm: Realm, insertType: EventInsertType): EventEntity {
    val eventEntity = realm.where<EventEntity>()
            .equalTo(EventEntityFields.EVENT_ID, eventId)
            .equalTo(EventEntityFields.ROOM_ID, roomId)
            .findFirst()
    return if (eventEntity == null) {
        val canBeProcessed = type != EventType.ENCRYPTED || decryptionResultJson != null
        val insertEntity = EventInsertEntity(eventId = eventId, eventType = type, canBeProcessed = canBeProcessed).apply {
            this.insertType = insertType
        }
        realm.insert(insertEntity)
        // copy this event entity and return it
        realm.copyToRealm(this)
    } else {
        eventEntity
    }
}

internal fun EventEntity.Companion.where(realm: Realm, eventId: String): RealmQuery<EventEntity> {
    return realm.where<EventEntity>()
            .equalTo(EventEntityFields.EVENT_ID, eventId)
}

internal fun EventEntity.Companion.whereRoomId(realm: Realm, roomId: String): RealmQuery<EventEntity> {
    return realm.where<EventEntity>()
            .equalTo(EventEntityFields.ROOM_ID, roomId)
}

internal fun EventEntity.Companion.where(realm: Realm, eventIds: List<String>): RealmQuery<EventEntity> {
    return realm.where<EventEntity>()
            .`in`(EventEntityFields.EVENT_ID, eventIds.toTypedArray())
}

internal fun EventEntity.Companion.whereType(realm: Realm,
                                             type: String,
                                             roomId: String? = null
): RealmQuery<EventEntity> {
    val query = realm.where<EventEntity>()
    if (roomId != null) {
        query.equalTo(EventEntityFields.ROOM_ID, roomId)
    }
    return query.equalTo(EventEntityFields.TYPE, type)
}

internal fun EventEntity.Companion.whereTypes(realm: Realm,
                                              typeList: List<String> = emptyList(),
                                              roomId: String? = null): RealmQuery<EventEntity> {
    val query = realm.where<EventEntity>()
    query.`in`(EventEntityFields.TYPE, typeList.toTypedArray())
    if (roomId != null) {
        query.equalTo(EventEntityFields.ROOM_ID, roomId)
    }
    return query
}

internal fun RealmList<EventEntity>.find(eventId: String): EventEntity? {
    return this.where()
            .equalTo(EventEntityFields.EVENT_ID, eventId)
            .findFirst()
}

internal fun RealmList<EventEntity>.fastContains(eventId: String): Boolean {
    return this.find(eventId) != null
}

internal fun EventEntity.Companion.whereRootThreadEventId(realm: Realm, rootThreadEventId: String): RealmQuery<EventEntity> {
    return realm.where<EventEntity>()
            .equalTo(EventEntityFields.ROOT_THREAD_EVENT_ID, rootThreadEventId)
}
