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
import org.matrix.android.sdk.internal.database.model.ReadMarkerEntity

internal fun ReadMarkerEntity.Companion.where(realm: TypedRealm, roomId: String): RealmQuery<ReadMarkerEntity> {
    return realm.query(ReadMarkerEntity::class)
            .query("roomId == $0", roomId)
}

internal fun ReadMarkerEntity.Companion.getOrCreate(realm: MutableRealm, roomId: String): ReadMarkerEntity {
    return where(realm, roomId).first().find() ?: create(realm, roomId)
}

internal fun ReadMarkerEntity.Companion.create(realm: MutableRealm, roomId: String): ReadMarkerEntity {
    val readMarkerEntity = ReadMarkerEntity().apply {
        this.roomId = roomId
    }
    return realm.copyToRealm(readMarkerEntity)
}

