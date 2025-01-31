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
import io.realm.kotlin.where
import org.matrix.android.sdk.internal.database.model.UserEntity
import org.matrix.android.sdk.internal.database.model.UserEntityFields

internal fun UserEntity.Companion.where(realm: Realm, userId: String): RealmQuery<UserEntity> {
    return realm
            .where<UserEntity>()
            .equalTo(UserEntityFields.USER_ID, userId)
}
