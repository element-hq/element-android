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
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.internal.crypto.store.db.model.WithHeldSessionEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.WithHeldSessionEntityFields

internal fun WithHeldSessionEntity.Companion.get(realm: Realm, roomId: String, sessionId: String): WithHeldSessionEntity? {
    return realm.where<WithHeldSessionEntity>()
            .equalTo(WithHeldSessionEntityFields.ROOM_ID, roomId)
            .equalTo(WithHeldSessionEntityFields.SESSION_ID, sessionId)
            .equalTo(WithHeldSessionEntityFields.ALGORITHM, MXCRYPTO_ALGORITHM_MEGOLM)
            .findFirst()
}

internal fun WithHeldSessionEntity.Companion.getOrCreate(realm: Realm, roomId: String, sessionId: String): WithHeldSessionEntity? {
    return get(realm, roomId, sessionId)
            ?: realm.createObject<WithHeldSessionEntity>().apply {
                this.roomId = roomId
                this.algorithm = MXCRYPTO_ALGORITHM_MEGOLM
                this.sessionId = sessionId
            }
}
