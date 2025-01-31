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
import org.matrix.android.sdk.internal.crypto.store.db.model.UserEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.UserEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.deleteOnCascade

/**
 * Get or create a user.
 */
internal fun UserEntity.Companion.getOrCreate(realm: Realm, userId: String): UserEntity {
    return realm.where<UserEntity>()
            .equalTo(UserEntityFields.USER_ID, userId)
            .findFirst()
            ?: realm.createObject(userId)
}

/**
 * Delete a user.
 */
internal fun UserEntity.Companion.delete(realm: Realm, userId: String) {
    realm.where<UserEntity>()
            .equalTo(UserEntityFields.USER_ID, userId)
            .findFirst()
            ?.deleteOnCascade()
}
