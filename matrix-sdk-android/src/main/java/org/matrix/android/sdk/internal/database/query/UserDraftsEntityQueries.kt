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
import org.matrix.android.sdk.internal.database.model.UserDraftsEntity
import org.matrix.android.sdk.internal.database.model.UserDraftsEntityFields

internal fun UserDraftsEntity.Companion.where(realm: Realm, roomId: String? = null): RealmQuery<UserDraftsEntity> {
    val query = realm.where<UserDraftsEntity>()
    if (roomId != null) {
        query.equalTo(UserDraftsEntityFields.ROOM_SUMMARY_ENTITY.ROOM_ID, roomId)
    }
    return query
}
