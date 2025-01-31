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
import org.matrix.android.sdk.internal.database.model.HomeServerCapabilitiesEntity

/**
 * Get the current HomeServerCapabilitiesEntity, return null if it does not exist.
 */
internal fun HomeServerCapabilitiesEntity.Companion.get(realm: Realm): HomeServerCapabilitiesEntity? {
    return realm.where<HomeServerCapabilitiesEntity>().findFirst()
}

/**
 * Get the current HomeServerCapabilitiesEntity, create one if it does not exist.
 */
internal fun HomeServerCapabilitiesEntity.Companion.getOrCreate(realm: Realm): HomeServerCapabilitiesEntity {
    return get(realm) ?: realm.createObject()
}
