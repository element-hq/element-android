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
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.internal.database.andIf
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.database.model.EventInsertEntity
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.queryIn

internal fun EventEntity.copyToRealmOrIgnore(realm: MutableRealm, insertType: EventInsertType): EventEntity {
    val eventId = this.eventId
    val type = this.type
    val eventEntity = realm.query(EventEntity::class)
            .query("eventId == $0", eventId)
            .query("roomId == $0", roomId)
            .first()
            .find()
    return if (eventEntity == null) {
        val canBeProcessed = type != EventType.ENCRYPTED || decryptionResultJson != null
        val insertEntity = EventInsertEntity().apply {
            this.eventId = eventId
            this.eventType = type
            this.canBeProcessed = canBeProcessed
            this.insertType = insertType
        }
        realm.copyToRealm(insertEntity)
        // copy this event entity and return it
        realm.copyToRealm(this)
    } else {
        eventEntity
    }
}

internal fun EventEntity.Companion.where(realm: TypedRealm, eventId: String): RealmQuery<EventEntity> {
    return realm.query(EventEntity::class)
            .query("eventId == $0", eventId)
}

internal fun EventEntity.Companion.whereRoomId(realm: TypedRealm, roomId: String): RealmQuery<EventEntity> {
    return realm.query(EventEntity::class)
            .query("roomId == $0", roomId)
}

internal fun EventEntity.Companion.where(realm: TypedRealm, eventIds: List<String>): RealmQuery<EventEntity> {
    return realm.query(EventEntity::class)
            .queryIn("eventId", eventIds)
}

internal fun EventEntity.Companion.whereType(
        realm: TypedRealm,
        type: String,
        roomId: String? = null
): RealmQuery<EventEntity> {
    return realm.query(EventEntity::class)
            .query("type == $0", type)
            .andIf(roomId != null) {
                query("roomId == $0", roomId!!)
            }
}

internal fun EventEntity.Companion.whereTypes(
        realm: TypedRealm,
        typeList: List<String> = emptyList(),
        roomId: String? = null
): RealmQuery<EventEntity> {
    return realm.query(EventEntity::class)
            .queryIn("type", typeList)
            .andIf(roomId != null) {
                query("roomId == $0", roomId!!)
            }
}

internal fun RealmList<EventEntity>.find(eventId: String): EventEntity? {
    return this.where()
            .equalTo(EventEntityFields.EVENT_ID, eventId)
            .findFirst()
}

internal fun RealmList<EventEntity>.fastContains(eventId: String): Boolean {
    return this.find(eventId) != null
}

internal fun EventEntity.Companion.whereRootThreadEventId(realm: TypedRealm, rootThreadEventId: String): RealmQuery<EventEntity> {
    return realm.query(EventEntity::class)
            .query("rootThreadEventId == $0", rootThreadEventId)
}
