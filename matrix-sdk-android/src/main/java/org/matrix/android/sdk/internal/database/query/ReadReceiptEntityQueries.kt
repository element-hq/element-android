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

internal fun ReadReceiptEntity.Companion.whereRoomId(realm: Realm, roomId: String): RealmQuery<ReadReceiptEntity> {
    return realm.where<ReadReceiptEntity>()
            .equalTo(ReadReceiptEntityFields.ROOM_ID, roomId)
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
