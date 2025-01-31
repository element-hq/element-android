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
import org.matrix.android.sdk.internal.database.model.PreviewUrlCacheEntity
import org.matrix.android.sdk.internal.database.model.PreviewUrlCacheEntityFields

/**
 * Get the current PreviewUrlCacheEntity, return null if it does not exist.
 */
internal fun PreviewUrlCacheEntity.Companion.get(realm: Realm, url: String): PreviewUrlCacheEntity? {
    return realm.where<PreviewUrlCacheEntity>()
            .equalTo(PreviewUrlCacheEntityFields.URL, url)
            .findFirst()
}

/**
 * Get the current PreviewUrlCacheEntity, create one if it does not exist.
 */
internal fun PreviewUrlCacheEntity.Companion.getOrCreate(realm: Realm, url: String): PreviewUrlCacheEntity {
    return get(realm, url) ?: realm.createObject(url)
}
