/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db.query

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoRoomEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoRoomEntityFields

/**
 * Get or create a room.
 */
internal fun CryptoRoomEntity.Companion.getOrCreate(realm: Realm, roomId: String): CryptoRoomEntity {
    return getById(realm, roomId) ?: realm.createObject(roomId)
}

/**
 * Get a room.
 */
internal fun CryptoRoomEntity.Companion.getById(realm: Realm, roomId: String): CryptoRoomEntity? {
    return realm.where<CryptoRoomEntity>()
            .equalTo(CryptoRoomEntityFields.ROOM_ID, roomId)
            .findFirst()
}
