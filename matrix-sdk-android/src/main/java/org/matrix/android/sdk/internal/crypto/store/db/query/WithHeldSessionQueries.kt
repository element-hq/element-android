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
