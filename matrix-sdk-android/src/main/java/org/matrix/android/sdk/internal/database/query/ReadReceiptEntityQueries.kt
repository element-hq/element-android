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
import io.realm.RealmQuery
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.matrix.android.sdk.internal.database.model.ReadReceiptEntity
import org.matrix.android.sdk.internal.database.model.ReadReceiptEntityFields

internal fun ReadReceiptEntity.Companion.where(realm: Realm, roomId: String, userId: String): RealmQuery<ReadReceiptEntity> {
    return realm.where<ReadReceiptEntity>()
            .equalTo(ReadReceiptEntityFields.PRIMARY_KEY, buildPrimaryKey(roomId, userId))
}

internal fun ReadReceiptEntity.Companion.whereUserId(realm: Realm, userId: String): RealmQuery<ReadReceiptEntity> {
    return realm.where<ReadReceiptEntity>()
            .equalTo(ReadReceiptEntityFields.USER_ID, userId)
}

internal fun ReadReceiptEntity.Companion.createUnmanaged(roomId: String, eventId: String, userId: String, originServerTs: Double): ReadReceiptEntity {
    return ReadReceiptEntity().apply {
        this.primaryKey = "${roomId}_$userId"
        this.eventId = eventId
        this.roomId = roomId
        this.userId = userId
        this.originServerTs = originServerTs
    }
}

internal fun ReadReceiptEntity.Companion.getOrCreate(realm: Realm, roomId: String, userId: String): ReadReceiptEntity {
    return ReadReceiptEntity.where(realm, roomId, userId).findFirst()
            ?: realm.createObject<ReadReceiptEntity>(buildPrimaryKey(roomId, userId))
                    .apply {
                        this.roomId = roomId
                        this.userId = userId
                    }
}

private fun buildPrimaryKey(roomId: String, userId: String) = "${roomId}_$userId"
