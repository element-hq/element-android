/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.query

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.matrix.android.sdk.internal.database.model.RawCacheEntity
import org.matrix.android.sdk.internal.database.model.RawCacheEntityFields

/**
 * Get the current RawCacheEntity, return null if it does not exist.
 */
internal fun RawCacheEntity.Companion.get(realm: Realm, url: String): RawCacheEntity? {
    return realm.where<RawCacheEntity>()
            .equalTo(RawCacheEntityFields.URL, url)
            .findFirst()
}

/**
 * Get the current RawCacheEntity, create one if it does not exist.
 */
internal fun RawCacheEntity.Companion.getOrCreate(realm: Realm, url: String): RawCacheEntity {
    return get(realm, url) ?: realm.createObject(url)
}
