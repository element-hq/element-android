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
import org.matrix.android.sdk.internal.database.model.BreadcrumbsEntity

internal fun BreadcrumbsEntity.Companion.get(realm: Realm): BreadcrumbsEntity? {
    return realm.where<BreadcrumbsEntity>().findFirst()
}

internal fun BreadcrumbsEntity.Companion.getOrCreate(realm: Realm): BreadcrumbsEntity {
    return get(realm) ?: realm.createObject()
}
