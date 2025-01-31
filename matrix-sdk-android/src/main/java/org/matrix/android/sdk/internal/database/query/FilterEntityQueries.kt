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
import org.matrix.android.sdk.internal.database.model.FilterEntity
import org.matrix.android.sdk.internal.session.filter.FilterFactory

/**
 * Get the current filter.
 */
internal fun FilterEntity.Companion.get(realm: Realm): FilterEntity? {
    return realm.where<FilterEntity>().findFirst()
}

/**
 * Get the current filter, create one if it does not exist.
 */
internal fun FilterEntity.Companion.getOrCreate(realm: Realm): FilterEntity {
    return get(realm) ?: realm.createObject<FilterEntity>()
            .apply {
                filterBodyJson = FilterFactory.createDefaultFilter().toJSONString()
                roomEventFilterJson = FilterFactory.createDefaultRoomFilter().toJSONString()
                filterId = ""
            }
}
