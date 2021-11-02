/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.crypto.store.db.query

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoRoomEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoRoomEntityFields

/**
 * Get or create a room
 */
internal fun CryptoRoomEntity.Companion.getOrCreate(realm: Realm, roomId: String): CryptoRoomEntity {
    return getById(realm, roomId) ?: realm.createObject(roomId)
}

/**
 * Get a room
 */
internal fun CryptoRoomEntity.Companion.getById(realm: Realm, roomId: String): CryptoRoomEntity? {
    return realm.where<CryptoRoomEntity>()
            .equalTo(CryptoRoomEntityFields.ROOM_ID, roomId)
            .findFirst()
}
