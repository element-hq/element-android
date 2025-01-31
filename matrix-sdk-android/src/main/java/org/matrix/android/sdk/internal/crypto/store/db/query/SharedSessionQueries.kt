/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db.query

import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.internal.crypto.store.db.model.SharedSessionEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.SharedSessionEntityFields

internal fun SharedSessionEntity.Companion.get(
        realm: Realm,
        roomId: String?,
        sessionId: String,
        userId: String,
        deviceId: String,
        deviceIdentityKey: String?
): SharedSessionEntity? {
    return realm.where<SharedSessionEntity>()
            .equalTo(SharedSessionEntityFields.ROOM_ID, roomId)
            .equalTo(SharedSessionEntityFields.SESSION_ID, sessionId)
            .equalTo(SharedSessionEntityFields.ALGORITHM, MXCRYPTO_ALGORITHM_MEGOLM)
            .equalTo(SharedSessionEntityFields.USER_ID, userId)
            .equalTo(SharedSessionEntityFields.DEVICE_ID, deviceId)
            .equalTo(SharedSessionEntityFields.DEVICE_IDENTITY_KEY, deviceIdentityKey)
            .findFirst()
}

internal fun SharedSessionEntity.Companion.get(realm: Realm, roomId: String?, sessionId: String): RealmResults<SharedSessionEntity> {
    return realm.where<SharedSessionEntity>()
            .equalTo(SharedSessionEntityFields.ROOM_ID, roomId)
            .equalTo(SharedSessionEntityFields.SESSION_ID, sessionId)
            .equalTo(SharedSessionEntityFields.ALGORITHM, MXCRYPTO_ALGORITHM_MEGOLM)
            .findAll()
}

internal fun SharedSessionEntity.Companion.create(
        realm: Realm,
        roomId: String?,
        sessionId: String,
        userId: String,
        deviceId: String,
        deviceIdentityKey: String,
        chainIndex: Int
): SharedSessionEntity {
    return realm.createObject<SharedSessionEntity>().apply {
        this.roomId = roomId
        this.algorithm = MXCRYPTO_ALGORITHM_MEGOLM
        this.sessionId = sessionId
        this.userId = userId
        this.deviceId = deviceId
        this.deviceIdentityKey = deviceIdentityKey
        this.chainIndex = chainIndex
    }
}
