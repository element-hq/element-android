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
 *
 */

package org.matrix.android.sdk.internal.database.query

import io.realm.kotlin.MutableRealm
import io.realm.kotlin.TypedRealm
import io.realm.kotlin.query.RealmQuery
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity

internal fun CurrentStateEventEntity.Companion.whereRoomId(
        realm: TypedRealm,
        roomId: String
): RealmQuery<CurrentStateEventEntity> {
    return realm.query(CurrentStateEventEntity::class)
            .query("roomId == $0", roomId)
}

internal fun CurrentStateEventEntity.Companion.whereType(
        realm: TypedRealm,
        roomId: String,
        type: String
): RealmQuery<CurrentStateEventEntity> {
    return whereRoomId(realm = realm, roomId = roomId)
            .query("type == $0", type)
}

internal fun CurrentStateEventEntity.Companion.whereStateKey(
        realm: TypedRealm,
        roomId: String,
        type: String,
        stateKey: String
): RealmQuery<CurrentStateEventEntity> {
    return whereType(realm = realm, roomId = roomId, type = type)
            .query("stateKey == $0", stateKey)
}

internal fun CurrentStateEventEntity.Companion.getOrNull(
        realm: TypedRealm,
        roomId: String,
        stateKey: String,
        type: String
): CurrentStateEventEntity? {
    return whereStateKey(realm = realm, roomId = roomId, type = type, stateKey = stateKey).first().find()
}

internal fun CurrentStateEventEntity.Companion.getOrCreate(
        realm: MutableRealm,
        roomId: String,
        stateKey: String,
        type: String
): CurrentStateEventEntity {
    return getOrNull(realm = realm, roomId = roomId, stateKey = stateKey, type = type) ?: create(realm, roomId, stateKey, type)
}

private fun create(
        realm: MutableRealm,
        roomId: String,
        stateKey: String,
        type: String
): CurrentStateEventEntity {
    val currentStateEventEntity = CurrentStateEventEntity().apply {
        this.type = type
        this.roomId = roomId
        this.stateKey = stateKey
    }
    return realm.copyToRealm(currentStateEventEntity)
}
