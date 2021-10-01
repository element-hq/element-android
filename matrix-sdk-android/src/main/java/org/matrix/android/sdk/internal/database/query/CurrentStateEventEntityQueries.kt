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

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.createObject
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntityFields

internal fun CurrentStateEventEntity.Companion.whereType(realm: Realm,
                                                         roomId: String,
                                                         type: String): RealmQuery<CurrentStateEventEntity> {
    return realm.where(CurrentStateEventEntity::class.java)
            .equalTo(CurrentStateEventEntityFields.ROOM_ID, roomId)
            .equalTo(CurrentStateEventEntityFields.TYPE, type)
}

internal fun CurrentStateEventEntity.Companion.whereStateKey(realm: Realm,
                                                             roomId: String,
                                                             type: String,
                                                             stateKey: String): RealmQuery<CurrentStateEventEntity> {
    return whereType(realm = realm, roomId = roomId, type = type)
            .equalTo(CurrentStateEventEntityFields.STATE_KEY, stateKey)
}

internal fun CurrentStateEventEntity.Companion.getOrNull(realm: Realm,
                                                         roomId: String,
                                                         stateKey: String,
                                                         type: String): CurrentStateEventEntity? {
    return whereStateKey(realm = realm, roomId = roomId, type = type, stateKey = stateKey).findFirst()
}

internal fun CurrentStateEventEntity.Companion.getOrCreate(realm: Realm,
                                                           roomId: String,
                                                           stateKey: String,
                                                           type: String): CurrentStateEventEntity {
    return getOrNull(realm = realm, roomId = roomId, stateKey = stateKey, type = type) ?: create(realm, roomId, stateKey, type)
}

private fun create(realm: Realm,
                   roomId: String,
                   stateKey: String,
                   type: String): CurrentStateEventEntity {
    return realm.createObject<CurrentStateEventEntity>().apply {
        this.type = type
        this.roomId = roomId
        this.stateKey = stateKey
    }
}
