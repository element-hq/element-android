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
import org.matrix.android.sdk.internal.crypto.store.db.model.CrossSigningInfoEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.UserEntityFields

internal fun CrossSigningInfoEntity.Companion.getOrCreate(realm: Realm, userId: String): CrossSigningInfoEntity {
    return realm.where<CrossSigningInfoEntity>()
            .equalTo(UserEntityFields.USER_ID, userId)
            .findFirst()
            ?: realm.createObject(userId)
}

internal fun CrossSigningInfoEntity.Companion.get(realm: Realm, userId: String): CrossSigningInfoEntity? {
    return realm.where<CrossSigningInfoEntity>()
            .equalTo(UserEntityFields.USER_ID, userId)
            .findFirst()
}
