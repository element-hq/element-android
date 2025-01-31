/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.raw

import io.realm.annotations.RealmModule
import org.matrix.android.sdk.internal.database.model.KnownServerUrlEntity
import org.matrix.android.sdk.internal.database.model.RawCacheEntity

/**
 * Realm module for global classes.
 */
@RealmModule(
        library = true,
        classes = [
            RawCacheEntity::class,
            KnownServerUrlEntity::class
        ]
)
internal class GlobalRealmModule
