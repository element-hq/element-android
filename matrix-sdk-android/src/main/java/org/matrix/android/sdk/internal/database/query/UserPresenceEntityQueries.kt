/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.query

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import org.matrix.android.sdk.internal.database.model.presence.UserPresenceEntity
import org.matrix.android.sdk.internal.database.model.presence.UserPresenceEntityFields

internal fun UserPresenceEntity.Companion.where(realm: Realm, userId: String): RealmQuery<UserPresenceEntity> {
    return realm
            .where<UserPresenceEntity>()
            .equalTo(UserPresenceEntityFields.USER_ID, userId)
}
